/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.version;

import java.util.Properties;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

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

  default int compare(final String version1, final String version2) {
    try {
      final GenericVersionScheme scheme = new GenericVersionScheme();
      final Version ver1 = scheme.parseVersion(version1);
      final Version ver2 = scheme.parseVersion(version2);
      return ver1.compareTo(ver2);
    }
    catch (final InvalidVersionSpecificationException e) {
      // the generic version scheme should accept anything
      throw new IllegalStateException(e);
    }
  }
}
