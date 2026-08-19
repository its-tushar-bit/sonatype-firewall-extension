/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.util.List;

public record AffectedComponentDTO(
    String format,
    String namespace,
    String name,
    String version,
    List<String> refIds)
{
  private static String normalizeNamespace(String namespace) {
    return (namespace == null || namespace.isEmpty()) ? null : namespace;
  }

  public AffectedComponentDTO {
    namespace = normalizeNamespace(namespace);
  }

  public AffectedCoordinates getCoordinates() {
    return new AffectedCoordinates(format, namespace, name, version);
  }
}
