/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import org.apache.commons.lang3.StringUtils;

public class ComponentDependencyUtils
{
  public static final String DEPENDENCY_PREFIX = "dependency:/";

  public static String buildDependencyFromCoordinates(final ThirdPartyFileCoordinate fileCoordinate) {
    if (fileCoordinate.getPackageUrl() != null) {
      return getDependencyStringFromPackageUrlIdentifier(fileCoordinate.getPackageUrl());
    }
    else {
      try {
        PackageURL packageURL = PackageURLBuilder.aPackageURL()
            .withType(fileCoordinate.getFormat())
            .withName(fileCoordinate.getName())
            .withVersion(fileCoordinate.getVersion())
            .build();
        return getDependencyStringFromPackageUrlIdentifier(packageURL.canonicalize());
      }
      catch (MalformedPackageURLException e) {
        return getDependencyStringForCoordinates(fileCoordinate.getFormat(), fileCoordinate.getName(),
            fileCoordinate.getVersion());
      }
    }
  }

  public static String getDependencyStringFromPackageUrlIdentifier(String packageUrlIdentifier) {
    if (StringUtils.isBlank(packageUrlIdentifier)) {
      return null;
    }
    return DEPENDENCY_PREFIX + packageUrlIdentifier.replace('/', '\\');
  }

  public static String getDependencyStringForCoordinates(String format, String name, String version) {
    return DEPENDENCY_PREFIX + (format == null ? "generic" : format + ":" + name + ":" + version);
  }
}
