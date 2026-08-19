/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileCoordinateDisplayNameGenerator
{
  private static final Logger log = LoggerFactory.getLogger(FileCoordinateDisplayNameGenerator.class);

  public static String generateDisplayName(String packageUrl, String format, String name, String version) {
    ComponentIdentifier componentIdentifier = null;
    try {
      if (StringUtils.isNotBlank(packageUrl)) {
        componentIdentifier =
            new PackageUrlIdentifier(packageUrl).toComponentIdentifier();
      }
    }
    catch (Exception ex) {
      log.warn("Cannot determine the component identifier for package URL: {}", packageUrl, ex);
    }

    return generateDisplayName(componentIdentifier, format, name, version);
  }

  public static String generateDisplayName(
      ComponentIdentifier componentIdentifier,
      String format,
      String name,
      String version)
  {
    try {
      if (componentIdentifier == null &&
          StringUtils.isNotBlank(format) &&
          StringUtils.isNotBlank(name) &&
          StringUtils.isNotBlank(version))
      {
        componentIdentifier =
            ComponentIdentifierAdapter.toComponentIdentifier(format, name, version);
      }
      if (componentIdentifier != null) {
        String displayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString();
        if (displayName.length() > 1000) {
          log.warn("Display name {} is too long", displayName);
        }
        else {
          return displayName;
        }
      }
    }
    catch (Exception ex) {
      log.warn("Cannot determine the component identifier for format, name and version: {}, {}, {}", format, name,
          version, ex);
    }
    return name + " : " + version;
  }
}
