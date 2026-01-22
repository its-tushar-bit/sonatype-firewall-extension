/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DefaultVersionService
    implements VersionService
{
  private static final Logger log = LoggerFactory.getLogger(DefaultVersionService.class);

  private static final String FILE_NAME = "version.properties";

  private static final Properties FILE_PROPERTIES = new Properties();

  static {
    loadProperties();
  }

  private final Properties properties;

  public DefaultVersionService() {
    this(FILE_PROPERTIES);
  }

  DefaultVersionService(Properties properties) {
    this.properties = properties;
  }

  @Override
  public String getBuild() {
    return getBuild(null);
  }

  @Override
  public String getBuild(String defaultValue) {
    return properties.getProperty("build", defaultValue);
  }

  // For tests only
  @Override
  public void setBuild(String build) {
    properties.setProperty("build", build);
  }

  @Override
  public String getName() {
    return getName(null);
  }

  @Override
  public String getName(String defaultValue) {
    return properties.getProperty("name", defaultValue);
  }

  @Override
  public Properties getProperties() {
    return properties;
  }

  @Override
  public String getTag() {
    return getTag(null);
  }

  @Override
  public String getTag(String defaultValue) {
    return properties.getProperty("tag", defaultValue);
  }

  @Override
  public String getTimestamp() {
    return getTimestamp(null);
  }

  @Override
  public String getTimestamp(String defaultValue) {
    return properties.getProperty("timestamp", defaultValue);
  }

  @Override
  public String getVersion() {
    return getVersion(null);
  }

  @Override
  public String getVersion(String defaultValue) {
    return properties.getProperty("version", defaultValue);
  }

  /**
   * @since 1.50
   */
  @Override
  public String getLogDisplayVersion() {
    String version = getVersion("Unknown");
    if (version.startsWith("1.")) {
      version = version.substring(2);
    }
    return version;
  }

  // For tests only
  @Override
  public void setVersion(String version) {
    properties.setProperty("version", version);
  }

  private static void loadProperties() {
    try (InputStream is = VersionService.class.getResourceAsStream(FILE_NAME)) {
      if (is != null) {
        FILE_PROPERTIES.load(is);
      }
      else {
        log.error("Missing properties file {}", FILE_NAME);
      }
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public String getShortVersion() {
    return getLogDisplayVersion();
  }

  @Override
  public String getFullVersion() {
    return getVersion();
  }
}
