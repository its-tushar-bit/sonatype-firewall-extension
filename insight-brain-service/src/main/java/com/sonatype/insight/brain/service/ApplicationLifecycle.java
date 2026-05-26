/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.net.InetAddress;
import java.util.UUID;

public abstract class ApplicationLifecycle
{
  private static final String INSTANCE_ID = UUID.randomUUID().toString();

  private static volatile File configFile;

  private static final String LOCAL_HOST_STRING;

  static {
    String localHostStr;
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      localHostStr = "hostname " + localHost.getHostName() + " (IP address " + localHost.getHostAddress() + ")";
    }
    catch (Exception e) {
      localHostStr = "unknown";
    }
    LOCAL_HOST_STRING = localHostStr;
  }

  public abstract void boot() throws Exception;

  public static String getServerInstanceId() {
    return INSTANCE_ID;
  }

  public static File getConfigFile() {
    return configFile;
  }

  public static void setConfigFile(File file) {
    configFile = file;
  }

  public static String getLocalHostString() {
    return LOCAL_HOST_STRING;
  }
}
