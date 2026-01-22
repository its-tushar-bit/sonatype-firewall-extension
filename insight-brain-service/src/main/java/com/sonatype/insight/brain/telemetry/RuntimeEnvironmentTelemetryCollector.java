/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

/**
 * @since 1.90
 */
@Named
@Singleton
public class RuntimeEnvironmentTelemetryCollector
    implements TelemetryCollector
{
  static final String JVM_NAME = "jvm_name";

  static final String JVM_VERSION = "jvm_version";

  static final String JVM_VENDOR = "jvm_vendor";

  static final String OS_NAME = "os_name";

  static final String OS_VERSION = "os_version";

  static final String OS_ARCHITECTURE = "os_architecture";

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.RUNTIME_ENVIRONMENT);
    Map<String, Object> attributes = telemetryData.getAttributes();

    attributes.put(JVM_NAME, System.getProperty("java.vm.name"));
    // Don't take the JVM version from java.vm.version. For ex, for Java 1.8.0_202, java.vm.version=25.202-b08.
    attributes.put(JVM_VERSION, System.getProperty("java.version"));
    attributes.put(JVM_VENDOR, System.getProperty("java.vendor"));

    attributes.put(OS_NAME, System.getProperty("os.name"));
    attributes.put(OS_VERSION, System.getProperty("os.version"));
    attributes.put(OS_ARCHITECTURE, System.getProperty("os.arch"));

    return telemetryData;
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }
}
