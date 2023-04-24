/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Properties;

public interface VersionService
{
  String getBuild();

  String getBuild(String defaultValue);

  void setBuild(String build);

  String getName();

  String getName(String defaultValue);

  Properties getProperties();

  String getTag();

  String getTag(String defaultValue);

  String getTimestamp();

  String getTimestamp(String defaultValue);

  String getVersion();

  String getVersion(String defaultValue);

  String getLogDisplayVersion();

  void setVersion(String version);

  String getShortVersion();

  String getFullVersion();
}
