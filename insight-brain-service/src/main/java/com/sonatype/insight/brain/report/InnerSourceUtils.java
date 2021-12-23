/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InnerSourceUtils
{
  private static final Logger log = LoggerFactory.getLogger(InnerSourceUtils.class);

  private InnerSourceUtils() {
    //noop
  }

  public static PackageUrlIdentifier getVersionlessPackageUrl(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return null;
    }

    return PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier.createAlternativeVersion(null));
  }

  public static PackageUrlIdentifier getPackageUrl(final DependencyNode dependencyNode) {
    String purl = dependencyNode.getPackageUrl();
    if (purl != null) {
      return new PackageUrlIdentifier(purl);
    }

    ComponentIdentifier componentIdentifier = dependencyNode.getComponentIdentifier();
    if (componentIdentifier != null) {
      try {
        return PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
      }
      catch (Exception e) {
        log.debug(e.getMessage(), e);
        // we don't need to fail if the package url can't be determined
      }
    }
    return null;
  }
}
