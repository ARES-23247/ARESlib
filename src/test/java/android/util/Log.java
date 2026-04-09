package android.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class Log {
  private static final Logger logger = Logger.getLogger("AndroidLog");

  public static int v(String tag, String msg) {
    logger.log(Level.FINE, tag + ": " + msg);
    return 0;
  }

  public static int v(String tag, String msg, Throwable tr) {
    logger.log(Level.FINE, tag + ": " + msg, tr);
    return 0;
  }

  public static int d(String tag, String msg) {
    logger.log(Level.FINE, tag + ": " + msg);
    return 0;
  }

  public static int d(String tag, String msg, Throwable tr) {
    logger.log(Level.FINE, tag + ": " + msg, tr);
    return 0;
  }

  public static int i(String tag, String msg) {
    logger.log(Level.INFO, tag + ": " + msg);
    return 0;
  }

  public static int i(String tag, String msg, Throwable tr) {
    logger.log(Level.INFO, tag + ": " + msg, tr);
    return 0;
  }

  public static int w(String tag, String msg) {
    logger.log(Level.WARNING, tag + ": " + msg);
    return 0;
  }

  public static int w(String tag, String msg, Throwable tr) {
    logger.log(Level.WARNING, tag + ": " + msg, tr);
    return 0;
  }

  public static int w(String tag, Throwable tr) {
    logger.log(Level.WARNING, tag, tr);
    return 0;
  }

  public static int e(String tag, String msg) {
    logger.log(Level.SEVERE, tag + ": " + msg);
    return 0;
  }

  public static int e(String tag, String msg, Throwable tr) {
    logger.log(Level.SEVERE, tag + ": " + msg, tr);
    return 0;
  }

  public static int wtf(String tag, String msg) {
    logger.log(Level.SEVERE, "WTF/" + tag + ": " + msg);
    return 0;
  }

  public static int wtf(String tag, Throwable tr) {
    logger.log(Level.SEVERE, tag, tr);
    return 0;
  }

  public static int wtf(String tag, String msg, Throwable tr) {
    logger.log(Level.SEVERE, "WTF/" + tag + ": " + msg, tr);
    return 0;
  }

  public static String getStackTraceString(Throwable tr) {
    return tr == null ? "" : tr.toString();
  }

  public static boolean isLoggable(String tag, int level) {
    return true;
  }

  public static int println(int priority, String tag, String msg) {
    logger.log(Level.INFO, tag + ": " + msg);
    return 0;
  }
}
