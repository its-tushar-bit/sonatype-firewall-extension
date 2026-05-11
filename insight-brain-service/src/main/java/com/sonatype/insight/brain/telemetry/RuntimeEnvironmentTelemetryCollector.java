/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.90
 */
@Named
@Singleton
public class RuntimeEnvironmentTelemetryCollector
    implements TelemetryCollector
{
  private static final Logger log = LoggerFactory.getLogger(RuntimeEnvironmentTelemetryCollector.class);

  static final String JVM_NAME = "jvm_name";

  static final String JVM_VERSION = "jvm_version";

  static final String JVM_VENDOR = "jvm_vendor";

  static final String OS_NAME = "os_name";

  static final String OS_VERSION = "os_version";

  static final String OS_ARCHITECTURE = "os_architecture";

  static final String NATIVE_GIT_AVAILABLE = "native_git_available";

  static final String GIT_VERSION = "git_version";

  static final String GIT_PARTIAL_CLONE_SUPPORTED = "git_partial_clone_supported";

  static final String GIT_IMPLEMENTATION_CONFIGURED = "git_implementation_configured";

  private static final Pattern VERSION_PATTERN = Pattern.compile("git version (\\d+)\\.(\\d+)\\.(\\d+)");

  private static final int PARTIAL_CLONE_MAJOR = 2;

  private static final int PARTIAL_CLONE_MINOR = 27;

  private final Configuration configuration;

  @Inject
  public RuntimeEnvironmentTelemetryCollector(final Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public TelemetryData collectData() {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.RUNTIME_ENVIRONMENT);
    Map<String, Object> attributes = telemetryData.getAttributes();

    attributes.put(JVM_NAME, System.getProperty("java.vm.name"));
    attributes.put(JVM_VERSION, System.getProperty("java.version"));
    attributes.put(JVM_VENDOR, System.getProperty("java.vendor"));

    attributes.put(OS_NAME, System.getProperty("os.name"));
    attributes.put(OS_VERSION, System.getProperty("os.version"));
    attributes.put(OS_ARCHITECTURE, System.getProperty("os.arch"));

    addGitVersionAttributes(attributes);

    return telemetryData;
  }

  private void addGitVersionAttributes(final Map<String, Object> attributes) {
    SourceControlConfiguration scmConfig = configuration.getSourceControlConfigurationOrDefault();
    GitImplementation configuredImpl = scmConfig.getGitImplementation();
    attributes.put(GIT_IMPLEMENTATION_CONFIGURED, configuredImpl != null ? configuredImpl.toString() : null);

    String versionOutput = getGitVersionOutput(scmConfig.getGitExecutable());
    boolean available = versionOutput != null;
    attributes.put(NATIVE_GIT_AVAILABLE, available);
    attributes.put(GIT_VERSION, available ? parseVersion(versionOutput) : null);
    attributes.put(GIT_PARTIAL_CLONE_SUPPORTED, available && supportsPartialClone(versionOutput));
  }

  private String getGitVersionOutput(final String gitExecutable) {
    try {
      String executable = gitExecutable != null ? gitExecutable : "git";
      Process process = new ProcessBuilder(executable, "--version").start();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        boolean finished = process.waitFor(5, TimeUnit.SECONDS);
        if (finished && process.exitValue() == 0) {
          return reader.readLine();
        }
        else if (!finished) {
          process.destroyForcibly();
        }
      }
    }
    catch (Exception e) {
      log.debug("Failed to determine native git version", e);
    }
    return null;
  }

  /**
   * Parse "git version 2.39.1" or "git version 2.39.1 (Apple Git-154)" and return the version string.
   */
  static String parseVersion(final String versionOutput) {
    if (versionOutput == null) {
      return null;
    }
    Matcher matcher = VERSION_PATTERN.matcher(versionOutput);
    return matcher.find() ? matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3) : null;
  }

  /**
   * Check if the git version supports partial clone (requires >= 2.27).
   */
  static boolean supportsPartialClone(final String versionOutput) {
    if (versionOutput == null) {
      return false;
    }
    Matcher matcher = VERSION_PATTERN.matcher(versionOutput);
    if (!matcher.find()) {
      return false;
    }
    int major = Integer.parseInt(matcher.group(1));
    int minor = Integer.parseInt(matcher.group(2));
    return major > PARTIAL_CLONE_MAJOR || (major == PARTIAL_CLONE_MAJOR && minor >= PARTIAL_CLONE_MINOR);
  }

  @Override
  public boolean isClusterTelemetry() {
    return false;
  }
}
