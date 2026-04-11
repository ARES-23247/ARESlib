package org.areslib.hardware.coprocessors.photon;

import android.content.Context;
import com.qualcomm.ftccommon.FtcEventLoop;
import com.qualcomm.hardware.lynx.LynxI2cDeviceSynch;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.LynxUnsupportedCommandException;
import com.qualcomm.hardware.lynx.LynxUsbDevice;
import com.qualcomm.hardware.lynx.LynxUsbDeviceDelegate;
import com.qualcomm.hardware.lynx.LynxUsbDeviceImpl;
import com.qualcomm.hardware.lynx.commands.LynxCommand;
import com.qualcomm.hardware.lynx.commands.LynxDatagram;
import com.qualcomm.hardware.lynx.commands.LynxMessage;
import com.qualcomm.hardware.lynx.commands.LynxRespondable;
import com.qualcomm.hardware.lynx.commands.core.LynxSetMotorConstantPowerCommand;
import com.qualcomm.hardware.lynx.commands.core.LynxSetServoPulseWidthCommand;
import com.qualcomm.hardware.lynx.commands.standard.LynxAck;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchSimple;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.hardware.usb.RobotUsbDevice;
import com.qualcomm.robotcore.util.RobotLog;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop;
import org.firstinspires.ftc.robotcore.internal.usb.exception.RobotUsbException;

@SuppressWarnings({"rawtypes", "unchecked", "unused", "SynchronizeOnNonFinalField"})
/*
 * PhotonCore standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * PhotonCore}. Extracted and compiled as part of the ARESLib Code Audit for missing
 * documentation coverage.
 */
public class PhotonCore implements Runnable, OpModeManagerNotifier.Notifications {
  protected static final PhotonCore INSTANCE = new PhotonCore();
  protected AtomicBoolean enabled, threadEnabled;

  private List<LynxModule> modules;
  private Thread thisThread = null;
  private Object syncLock = new Object();

  private final Object messageSync = new Object();

  private RobotUsbDevice robotUsbDevice;
  private HashMap<LynxModule, RobotUsbDevice> usbDeviceMap;

  public static LynxModule controlHub, expansionHub;

  public static boolean parallelizeServos = true;

  private OpModeManagerImpl opModeManager;

  public static class ExperimentalParameters {
    private final AtomicBoolean singlethreadedOptimized = new AtomicBoolean(true);
    private final AtomicInteger maximumParallelCommands = new AtomicInteger(8);

    public void setSinglethreadedOptimized(boolean state) {
      this.singlethreadedOptimized.set(state);
    }

    public boolean setMaximumParallelCommands(int maximumParallelCommands) {
      if (maximumParallelCommands <= 0) {
        return false;
      }
      this.maximumParallelCommands.set(maximumParallelCommands);
      return true;
    }
  }

  public static ExperimentalParameters experimental = new ExperimentalParameters();

  public PhotonCore() {
    enabled = new AtomicBoolean(false);
    threadEnabled = new AtomicBoolean(false);
    usbDeviceMap = new HashMap<>();
  }

  public static void enable() {
    INSTANCE.enabled.set(true);
    if (controlHub != null && controlHub.getBulkCachingMode() == LynxModule.BulkCachingMode.OFF) {
      controlHub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
    }
    if (expansionHub != null
        && expansionHub.getBulkCachingMode() == LynxModule.BulkCachingMode.OFF) {
      expansionHub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
    }
  }

  public static void disable() {
    INSTANCE.enabled.set(false);
  }

  @OnCreateEventLoop
  public static void attachEventLoop(Context context, FtcEventLoop eventLoop) {
    eventLoop.getOpModeManager().registerListener(INSTANCE);
    INSTANCE.opModeManager = eventLoop.getOpModeManager();
  }

  protected static boolean registerSend(LynxCommand command)
      throws LynxUnsupportedCommandException, InterruptedException {

    PhotonLynxModule photonModule = (PhotonLynxModule) command.getModule();

    if (!INSTANCE.usbDeviceMap.containsKey(photonModule)) {
      return false;
    }

    synchronized (INSTANCE.messageSync) {
      while (((PhotonLynxModule) photonModule).getUnfinishedCommands().size()
          > experimental.maximumParallelCommands.get()) {
        // Spinning waiting for parallel commands to finish
        try {
          Thread.sleep(1);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        // RobotLog.ee("PhotonCore", ((PhotonLynxModule)controlHub).getUnfinishedCommands().size()
        // + " | " + ((PhotonLynxModule)expansionHub).getUnfinishedCommands().size());
      }

      if (!experimental.singlethreadedOptimized.get()) {
        boolean noSimilar = false;
        while (!noSimilar) {
          noSimilar = true;
          for (LynxRespondable respondable : photonModule.getUnfinishedCommands().values()) {
            if (INSTANCE.isSimilar(respondable, command)) {
              noSimilar = false;
            }
          }
        }
      }

      byte messageNum = photonModule.getNewMessageNumber();

      command.setMessageNumber(messageNum);

      try {
        LynxDatagram datagram = new LynxDatagram(command);
        command.setSerialization(datagram);

        if (command.isAckable() || command.isResponseExpected()) {
          photonModule
              .getUnfinishedCommands()
              .put(command.getMessageNumber(), (LynxRespondable) command);
        }

        byte[] bytes = datagram.toByteArray();

        double msLatency = 0;
        synchronized (INSTANCE.syncLock) {
          long start = System.nanoTime();
          INSTANCE.usbDeviceMap.get(photonModule).write(bytes);
          long stop = System.nanoTime();
          msLatency = (stop - start) * 1.0e-6;
        }
        // RobotLog.ii("PhotonCore", "Wrote " + bytes.length + " bytes " +
        // photonModule.getUnfinishedCommands().size() + " | " + (msLatency));

        if (shouldAckImmediately(command)) {
          command.onAckReceived(new LynxAck(photonModule, false));
        }
      } catch (LynxUnsupportedCommandException | RobotUsbException e) {
        com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
      }
    }

    return true;
  }

  protected static boolean shouldParallelize(LynxCommand command) {
    return (command instanceof LynxSetMotorConstantPowerCommand
        || (parallelizeServos && command instanceof LynxSetServoPulseWidthCommand));
  }

  protected static boolean shouldAckImmediately(LynxCommand command) {
    return (command instanceof LynxSetMotorConstantPowerCommand
        || command instanceof LynxSetServoPulseWidthCommand);
  }

  private boolean isSimilar(LynxRespondable respondable1, LynxRespondable respondable2) {
    return (respondable1.getDestModuleAddress() == respondable2.getDestModuleAddress())
        && (respondable1.getCommandNumber() == respondable2.getCommandNumber());
  }

  protected static LynxMessage getCacheResponse(LynxCommand command) {
    return null;
  }

  @Override
  public void run() {
    while (threadEnabled.get()) {

      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
      }
    }
  }

  @Override
  public void onOpModePreInit(OpMode opMode) {
    if (opModeManager.getActiveOpModeName().equals(OpModeManager.DEFAULT_OP_MODE_NAME)) {
      return;
    }

    HardwareMap map = opMode.hardwareMap;

    boolean replacedPrev = false;
    boolean hasChub = false;
    for (LynxModule module : map.getAll(LynxModule.class)) {
      if (module instanceof PhotonLynxModule) {
        replacedPrev = true;
      }
      if (LynxConstants.isEmbeddedSerialNumber(module.getSerialNumber())) {
        hasChub = true;
      }
    }
    if (replacedPrev) {
      HashMap<String, HardwareDevice> toRemove = new HashMap<>();
      for (LynxModule module : map.getAll(LynxModule.class)) {
        if (!(module instanceof PhotonLynxModule)) {
          toRemove.put((String) map.getNamesOf(module).toArray()[0], module);
        }
      }
      for (String s : toRemove.keySet()) {
        map.remove(s, toRemove.get(s));
      }
    } else {
      controlHub = null;
      expansionHub = null;
    }

    INSTANCE.modules = map.getAll(LynxModule.class);
    ArrayList<String> moduleNames = new ArrayList<>();
    HashMap<LynxModule, PhotonLynxModule> replacements = new HashMap<>();
    for (LynxModule module : INSTANCE.modules) {
      moduleNames.add((String) map.getNamesOf(module).toArray()[0]);
    }
    LynxUsbDeviceImpl usbDevice = null, usbDevice1 = null;
    for (String s : moduleNames) {
      LynxModule module = (LynxModule) map.get(LynxModule.class, s);
      if (module instanceof PhotonLynxModule) {
        continue;
      }
      try {
        PhotonLynxModule photonLynxModule =
            new PhotonLynxModule(
                (LynxUsbDevice)
                    ReflectionUtils.getField(module.getClass(), "lynxUsbDevice").get(module),
                (Integer) ReflectionUtils.getField(module.getClass(), "moduleAddress").get(module),
                (Boolean) ReflectionUtils.getField(module.getClass(), "isParent").get(module),
                (Boolean) ReflectionUtils.getField(module.getClass(), "isUserModule").get(module));
        RobotLog.ee("PhotonCoreLynxNames", s);
        ReflectionUtils.deepCopy(module, photonLynxModule);
        map.remove(s, module);
        map.put(s, photonLynxModule);
        replacements.put(module, photonLynxModule);

        if (module.isParent()
            && (hasChub && LynxConstants.isEmbeddedSerialNumber(module.getSerialNumber()))
            && controlHub == null) {
          controlHub = photonLynxModule;

          try {
            Field f1 = module.getClass().getDeclaredField("lynxUsbDevice");
            f1.setAccessible(true);
            LynxUsbDevice tmp = (LynxUsbDevice) f1.get(module);
            if (tmp instanceof LynxUsbDeviceDelegate) {
              Field tmp2 = LynxUsbDeviceDelegate.class.getDeclaredField("delegate");
              tmp2.setAccessible(true);
              usbDevice = (LynxUsbDeviceImpl) tmp2.get(tmp);
            } else {
              usbDevice = (LynxUsbDeviceImpl) tmp;
            }
            Field f2 = usbDevice.getClass().getSuperclass().getDeclaredField("robotUsbDevice");
            f2.setAccessible(true);
            Field f3 = usbDevice.getClass().getDeclaredField("engageLock");
            f3.setAccessible(true);
            syncLock = f3.get(usbDevice);

            robotUsbDevice = (RobotUsbDevice) f2.get(usbDevice);
            usbDeviceMap.put(photonLynxModule, robotUsbDevice);
          } catch (IllegalAccessException e) {
            com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
          } catch (NoSuchFieldException e) {
            com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
          }
        } else {
          if (module.isParent()) {
            try {
              Field f1 = module.getClass().getDeclaredField("lynxUsbDevice");
              f1.setAccessible(true);
              LynxUsbDevice tmp = (LynxUsbDevice) f1.get(module);
              if (tmp instanceof LynxUsbDeviceDelegate) {
                Field tmp2 = LynxUsbDeviceDelegate.class.getDeclaredField("delegate");
                tmp2.setAccessible(true);
                usbDevice = (LynxUsbDeviceImpl) tmp2.get(tmp);
              } else {
                usbDevice = (LynxUsbDeviceImpl) tmp;
              }
              Field f2 = usbDevice.getClass().getSuperclass().getDeclaredField("robotUsbDevice");
              f2.setAccessible(true);
              Field f3 = usbDevice.getClass().getDeclaredField("engageLock");
              f3.setAccessible(true);
              syncLock = f3.get(usbDevice);

              robotUsbDevice = (RobotUsbDevice) f2.get(usbDevice);
              usbDeviceMap.put(photonLynxModule, robotUsbDevice);
            } catch (NoSuchFieldException e) {
              com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
            }
          }
          expansionHub = photonLynxModule;
        }
      } catch (IllegalAccessException e) {
        com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
      }
    }

    for (LynxModule m : replacements.keySet()) {
      usbDevice.removeConfiguredModule(m);
      try {
        ConcurrentHashMap<Integer, LynxModule> knownModules =
            (ConcurrentHashMap<Integer, LynxModule>)
                ReflectionUtils.getField(usbDevice.getClass(), "knownModules").get(usbDevice);
        synchronized (knownModules) {
          PhotonLynxModule photonLynxModule = replacements.get(m);
          knownModules.put(photonLynxModule.getModuleAddress(), photonLynxModule);
          RobotLog.vv(
              LynxUsbDeviceImpl.TAG,
              "addConfiguredModule() name#=%s",
              photonLynxModule.getDeviceName());
        }
      } catch (IllegalAccessException e) {
        com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
      }
    }

    HashMap<String, HardwareDevice> replacedNeutrino = new HashMap<>(),
        removedNeutrino = new HashMap<>();
    for (HardwareDevice device : map.getAll(HardwareDevice.class)) {
      if (!(device instanceof LynxModule)) {
        RobotLog.i(map.getNamesOf(device).toArray()[0].toString());
        if (device instanceof I2cDeviceSynchDevice) {
          try {
            I2cDeviceSynchSimple device2 =
                (I2cDeviceSynchSimple)
                    ReflectionUtils.getField(device.getClass(), "deviceClient").get(device);
            if (!(device2 instanceof LynxI2cDeviceSynch)) {
              device2 =
                  (I2cDeviceSynchSimple)
                      ReflectionUtils.getField(device2.getClass(), "i2cDeviceSynchSimple")
                          .get(device2);
            }
            setLynxObject(device2, replacements);
            RobotLog.e("" + (device2 instanceof LynxI2cDeviceSynch));
          } catch (Exception ignored) {
            com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(ignored));
          }
        } else if (device instanceof I2cDeviceSynchSimple) {
          try {
            I2cDeviceSynchSimple device2 =
                (I2cDeviceSynchSimple)
                    ReflectionUtils.getField(device.getClass(), "deviceClient").get(device);
            setLynxObject(device2, replacements);
          } catch (Exception ignored) {
            com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(ignored));
          }
        } else {
          setLynxObject(device, replacements);
        }
      }
    }

    for (String s : replacedNeutrino.keySet()) {
      map.remove(s, removedNeutrino.get(s));
      map.put(s, replacedNeutrino.get(s));
    }

    if (thisThread == null || !thisThread.isAlive()) {
      thisThread = new Thread(this);
      threadEnabled.set(true);
      thisThread.start();
    }
  }

  private void setLynxObject(Object device, Map<LynxModule, PhotonLynxModule> replacements) {
    Field f = ReflectionUtils.getField(device.getClass(), LynxModule.class);
    if (f != null) {
      f.setAccessible(true);
      try {
        LynxModule module = (LynxModule) f.get(device);
        if (module == null) {
          return;
        }
        if (replacements.containsKey(module)) {
          f.set(device, replacements.get(module));
        }
      } catch (IllegalAccessException e) {
        com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
      }
    }
  }

  @Override
  public void onOpModePreStart(OpMode opMode) {}

  @Override
  public void onOpModePostStop(OpMode opMode) {
    enabled.set(false);
    threadEnabled.set(false);
  }

  public static AtomicBoolean isEnabled() {
    return INSTANCE.enabled;
  }
}
