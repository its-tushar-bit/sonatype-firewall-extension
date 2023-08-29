/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.database;

/**
 * <p>
 * This is a temporary class to help in the MTIQ Database Layer Cleanup effort. The intent is to mark places in the code
 * that need to be revisited as part of the completion of the total work. The work is quite comprehensive so this helper
 * is actually quite useful. This class will be removed once the work is done.
 * </p>
 * <p>
 * Note that the output will only occur if the system property `-DmtiqTodo` is set.
 * </p>
 * See https://sonatype.atlassian.net/wiki/spaces/~cpeters/pages/126681137/Database+Layer+Cleanup
 */
public class MtiqTempUtils
{
  /**
   * See {@link #logTodo(String)}
   */
  public static void logTodo() {
    logTodo("");
  }

  /**
   * Log the provided message to the console with a prefix including the text 'MTIQ TODO', the class name, method, and
   * line number where the log entry occurred.
   */
  public static void logTodo(final String message) {
    if (System.getProperty("mtiqTodo") != null) {
      StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
      StackTraceElement ste = stackTraceElements[2];
      System.out.println(
          "MTIQ TODO - " + ste.getClassName() + "#" + ste.getMethodName() + ":" + ste.getLineNumber() + " - " +
              message);
    }
  }
}
