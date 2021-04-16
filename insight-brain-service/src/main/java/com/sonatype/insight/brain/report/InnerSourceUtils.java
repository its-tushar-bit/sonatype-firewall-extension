/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

public class InnerSourceUtils
{
  private InnerSourceUtils() {
    //noop
  }

  public static PackageUrlIdentifier getVersionlessPackageUrl(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier != null && ComponentIdentifier.FORMAT_MAVEN.equals(componentIdentifier.getFormat())) {
      return new PackageUrlIdentifier(String.format("pkg:maven/%s/%s",
          componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
          componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID)));
    }
    return null;
  }
}
