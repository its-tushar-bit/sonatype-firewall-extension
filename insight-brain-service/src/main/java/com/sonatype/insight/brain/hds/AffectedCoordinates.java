/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

public record AffectedCoordinates(
    String format,
    String namespace,
    String name,
    String version)
{
  public static AffectedCoordinates fromComponentIdentifier(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      throw new IllegalArgumentException("ComponentIdentifier cannot be null");
    }

    PackageUrlIdentifier purlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    String normalizedNamespace = normalizeNamespace(purlIdentifier.getNamespace());

    return new AffectedCoordinates(
        componentIdentifier.getFormat(),
        normalizedNamespace,
        purlIdentifier.getName(),
        purlIdentifier.getVersion());
  }

  private static String normalizeNamespace(String namespace) {
    return (namespace == null || namespace.isEmpty()) ? null : namespace;
  }

  public AffectedCoordinates {
    namespace = normalizeNamespace(namespace);
  }
}
