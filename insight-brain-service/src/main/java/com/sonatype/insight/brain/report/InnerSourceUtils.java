/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class InnerSourceUtils
{
  private static final Map<String, Set<String>> FORMAT_INNERSOURCE_COORDINATES = ImmutableMap.of(
      ComponentIdentifier.FORMAT_MAVEN,
      ImmutableSet.of(ComponentIdentifier.MAVEN_GROUP_ID, ComponentIdentifier.MAVEN_ARTIFACT_ID),
      ComponentIdentifier.FORMAT_NPM,
      ImmutableSet.of(ComponentIdentifier.NPM_PACKAGE_ID)
  );

  private InnerSourceUtils() {
    //noop
  }

  public static PackageUrlIdentifier getVersionlessPackageUrl(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null || !FORMAT_INNERSOURCE_COORDINATES.containsKey(componentIdentifier.getFormat())) {
      return null;
    }

    String format = componentIdentifier.getFormat();
    TreeMap<String, String> coords = new TreeMap<>();
    FORMAT_INNERSOURCE_COORDINATES.get(format)
        .forEach(coordName -> coords.put(coordName, componentIdentifier.get(coordName)));
    return PackageUrlIdentifier.fromComponentIdentifier(new ComponentIdentifier(format, coords));
  }
}
