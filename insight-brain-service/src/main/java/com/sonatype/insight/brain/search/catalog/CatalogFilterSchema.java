/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.util.Map;

public final class CatalogFilterSchema
{
  public enum Kind
  {
    TEXT,
    TERMS,
    RANGE,
    SCALAR
  }

  private static final Map<CatalogEntityType, Map<String, Kind>> SCHEMA = Map.of(
      CatalogEntityType.COMPONENT, Map.ofEntries(
          Map.entry("query", Kind.TEXT),
          Map.entry("ecosystems", Kind.TERMS),
          // Local-source only (My Scan Data). The catalog source rejects it in CatalogRequestBuilder;
          // organizationName is not part of the public Guide/HDS corpus.
          Map.entry("organizations", Kind.TERMS),
          // applications / stages are local-source only: they filter the my-scan estate by the app
          // and evaluation stage denormalized on component docs. The catalog (Guide/HDS) corpus has
          // no app/stage dimension, so CatalogRequestBuilder rejects them there.
          Map.entry("applications", Kind.TERMS),
          Map.entry("stages", Kind.TERMS),
          // Local-source only (My Scan Data): these filter the my-scan estate by the policy violations
          // denormalized on component docs (componentViolation* fields). The catalog (Guide/HDS) corpus
          // has no policy-violation dimension, so CatalogRequestBuilder rejects them there.
          Map.entry("policyTypes", Kind.TERMS),
          Map.entry("violationStates", Kind.TERMS),
          Map.entry("policyThreatLevel", Kind.RANGE),
          Map.entry("categories", Kind.TERMS),
          Map.entry("severities", Kind.TERMS),
          Map.entry("licenseFamilies", Kind.TERMS),
          Map.entry("licenses", Kind.TERMS),
          Map.entry("cvss", Kind.RANGE),
          Map.entry("epss", Kind.RANGE),
          Map.entry("versionScore", Kind.RANGE),
          Map.entry("latestStable", Kind.SCALAR),
          Map.entry("publishedWindow", Kind.SCALAR),
          Map.entry("malware", Kind.SCALAR)),
      CatalogEntityType.VULNERABILITY, Map.ofEntries(
          Map.entry("query", Kind.TEXT),
          Map.entry("severities", Kind.TERMS),
          // Local-source only (My Scan Data) triage dimensions. Guide/HDS vulns are not scoped to an
          // org/app/stage, so CatalogRequestBuilder rejects these on the catalog source.
          Map.entry("organizations", Kind.TERMS),
          Map.entry("applications", Kind.TERMS),
          Map.entry("stages", Kind.TERMS),
          Map.entry("cwes", Kind.TERMS),
          Map.entry("affectedEcosystems", Kind.TERMS),
          Map.entry("cvss", Kind.RANGE),
          Map.entry("epss", Kind.RANGE),
          Map.entry("publishedWindow", Kind.SCALAR),
          // Local-source only (My Scan Data): a relative window ([now-window, now]) on the vuln's
          // first-seen (open_time) date. The catalog (Guide/HDS) corpus keeps publishedWindow on the
          // Guide publish date; firstSeenWindow has no catalog equivalent, so CatalogRequestBuilder
          // rejects it on the catalog source.
          Map.entry("firstSeenWindow", Kind.SCALAR),
          Map.entry("malware", Kind.SCALAR),
          Map.entry("kev", Kind.SCALAR),
          Map.entry("patchAvailable", Kind.SCALAR)));

  private CatalogFilterSchema() {
  }

  public static Map<String, Kind> forEntityType(final CatalogEntityType entityType) {
    return SCHEMA.getOrDefault(entityType, Map.of());
  }
}
