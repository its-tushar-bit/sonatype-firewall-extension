/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * DTO representing a component affected by a CVE vulnerability. Maps to the response from HDS endpoint: GET
 * /rest/vulnerability/cve/{cveId}
 *
 * This is a record with normalized namespace handling: null and empty strings are treated as equivalent.
 */
public record AffectedComponentDTO(
    String format,
    String namespace,
    String name,
    String version)
{
  /**
   * Creates an AffectedComponentDTO from a ComponentIdentifier with proper namespace normalization.
   * Null and empty namespaces are normalized to null for consistent equality comparisons.
   */
  public static AffectedComponentDTO fromComponentIdentifier(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      throw new IllegalArgumentException("ComponentIdentifier cannot be null");
    }

    PackageUrlIdentifier purlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);

    // Normalize namespace: treat null and empty as equivalent (both become null)
    String normalizedNamespace = normalizeNamespace(purlIdentifier.getNamespace());

    return new AffectedComponentDTO(
        componentIdentifier.getFormat(),
        normalizedNamespace,
        purlIdentifier.getName(),
        purlIdentifier.getVersion()
    );
  }

  /**
   * Normalizes namespace by converting empty strings to null for consistent equality.
   */
  private static String normalizeNamespace(String namespace) {
    return (namespace == null || namespace.isEmpty()) ? null : namespace;
  }

  /**
   * Canonical constructor with namespace normalization.
   */
  public AffectedComponentDTO {
    // Normalize namespace in the canonical constructor to ensure all instances have normalized values
    namespace = normalizeNamespace(namespace);
  }
}
