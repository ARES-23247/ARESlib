package org.areslib.hardware.coprocessors.photon;

import java.lang.reflect.Field;

@SuppressWarnings({"rawtypes"})
/**
 * ReflectionUtils standard implementation.
 *
 * <p>This class provides the core structural components or hardware abstraction for {@code
 * ReflectionUtils}. Extracted and compiled as part of the ARESLib2 Code Audit for missing
 * documentation coverage.
 */
public class ReflectionUtils {
  public static Field getField(Class clazz, String fieldName) {
    try {
      Field f = clazz.getDeclaredField(fieldName);
      f.setAccessible(true);
      return f;
    } catch (NoSuchFieldException e) {
      Class superClass = clazz.getSuperclass();
      if (superClass != null) {
        return getField(superClass, fieldName);
      }
    }
    return null;
  }

  public static Field getField(Class clazz, Class target) {
    for (Field f : clazz.getDeclaredFields()) {
      if (f.getType().equals(target)) {
        f.setAccessible(true);
        return f;
      }
    }
    Class superClass = clazz.getSuperclass();
    if (superClass != null) {
      return getField(clazz.getSuperclass(), target);
    } else {
      return null;
    }
  }

  public static void deepCopy(Object org, Object target) {
    Field[] fields = org.getClass().getDeclaredFields();
    for (Field f : fields) {
      f.setAccessible(true);
      Field f2 = getField(target.getClass(), f.getName());
      if (f2 != null) {
        f2.setAccessible(true);
        try {
          f2.set(target, f.get(org));
        } catch (IllegalAccessException e) {
          com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
        }
      }
    }
  }
}
