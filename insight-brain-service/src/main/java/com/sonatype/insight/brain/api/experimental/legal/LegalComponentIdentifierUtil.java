/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.Set;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;

public class LegalComponentIdentifierUtil
{
  private LegalComponentIdentifierUtil() {
  }

  public static ComponentIdentifier removeClassifierAndExtension(ComponentIdentifier componentIdentifier) {
    TreeMap<String, String> coordinates = new TreeMap<>(componentIdentifier.getCoordinates());
    coordinates.remove(ComponentIdentifier.MAVEN_CLASSIFIER);
    coordinates.remove(ComponentIdentifier.MAVEN_EXTENSION);
    return new ComponentIdentifier(componentIdentifier.getFormat(), coordinates);
  }

  public static boolean isComponentAKnownInnerSource(
      Set<String> innerSourcePackageUrls,
      ComponentIdentifier componentIdentifier)
  {
    if (componentIdentifier != null) {
      PackageUrlIdentifier versionlessPackageUrl = InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier);
      return versionlessPackageUrl != null && innerSourcePackageUrls.contains(versionlessPackageUrl.getPackageUrl());
    }
    return false;
  }
}
