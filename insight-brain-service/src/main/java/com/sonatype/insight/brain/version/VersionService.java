/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.inject.Named;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class VersionService
{
  private static final Logger log = LoggerFactory.getLogger(VersionService.class);

  private static final String FILE_NAME = "version.properties";

  private static final Properties FILE_PROPERTIES = new Properties();

  static {
    loadProperties();
  }

  private final Properties properties;

  public VersionService() {
    this(FILE_PROPERTIES);
  }

  VersionService(Properties properties) {
    this.properties = properties;
  }

  public String getBuild() {
    return getBuild(null);
  }

  public String getBuild(String defaultValue) {
    return properties.getProperty("build", defaultValue);
  }

  public String getName() {
    return getName(null);
  }

  public String getName(String defaultValue) {
    return properties.getProperty("name", defaultValue);
  }

  public Properties getProperties() {
    return properties;
  }

  public String getTag() {
    return getTag(null);
  }

  public String getTag(String defaultValue) {
    return properties.getProperty("tag", defaultValue);
  }

  public String getTimestamp() {
    return getTimestamp(null);
  }

  public String getTimestamp(String defaultValue) {
    return properties.getProperty("timestamp", defaultValue);
  }

  public String getVersion() {
    return getVersion(null);
  }

  public String getVersion(String defaultValue) {
    return properties.getProperty("version", defaultValue);
  }

  /**
   * @since 1.50
   */
  public String getLogDisplayVersion() {
    String version = getVersion("Unknown");
    if (version.startsWith("1.")) {
      version = version.substring(2);
    }
    return version;
  }

  // For tests only
  public void setVersion(String version) {
    properties.setProperty("version", version);
  }

  private static void loadProperties() {
    InputStream is = VersionService.class.getResourceAsStream(FILE_NAME);
    if (is != null) {
      try {
        FILE_PROPERTIES.load(is);
      }
      catch (IOException e) {
        log.error(e.getMessage(), e);
      }
      finally {
        IOUtil.close(is);
      }
    }
    else {
      log.error("Missing properties file {}", FILE_NAME);
    }
  }
}
