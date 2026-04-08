package android.util;

public final class Log {
  public static int v(String tag, String msg) {
    System.out.println("V/" + tag + ": " + msg);
    return 0;
  }

  public static int v(String tag, String msg, Throwable tr) {
    System.out.println("V/" + tag + ": " + msg);
    tr.printStackTrace();
    return 0;
  }

  public static int d(String tag, String msg) {
    System.out.println("D/" + tag + ": " + msg);
    return 0;
  }

  public static int d(String tag, String msg, Throwable tr) {
    System.out.println("D/" + tag + ": " + msg);
    tr.printStackTrace();
    return 0;
  }

  public static int i(String tag, String msg) {
    System.out.println("I/" + tag + ": " + msg);
    return 0;
  }

  public static int i(String tag, String msg, Throwable tr) {
    System.out.println("I/" + tag + ": " + msg);
    tr.printStackTrace();
    return 0;
  }

  public static int w(String tag, String msg) {
    System.out.println("W/" + tag + ": " + msg);
    return 0;
  }

  public static int w(String tag, String msg, Throwable tr) {
    System.out.println("W/" + tag + ": " + msg);
    tr.printStackTrace();
    return 0;
  }

  public static int w(String tag, Throwable tr) {
    tr.printStackTrace();
    return 0;
  }

  public static int e(String tag, String msg) {
    System.err.println("E/" + tag + ": " + msg);
    return 0;
  }

  public static int e(String tag, String msg, Throwable tr) {
    System.err.println("E/" + tag + ": " + msg);
    tr.printStackTrace();
    return 0;
  }

  public static int wtf(String tag, String msg) {
    System.err.println("WTF/" + tag + ": " + msg);
    return 0;
  }

  public static int wtf(String tag, Throwable tr) {
    tr.printStackTrace();
    return 0;
  }

  public static int wtf(String tag, String msg, Throwable tr) {
    System.err.println("WTF/" + tag + ": " + msg);
    tr.printStackTrace();
    return 0;
  }

  public static String getStackTraceString(Throwable tr) {
    return tr == null ? "" : tr.toString();
  }

  public static boolean isLoggable(String tag, int level) {
    return true;
  }

  public static int println(int priority, String tag, String msg) {
    System.out.println(tag + ": " + msg);
    return 0;
  }
}
