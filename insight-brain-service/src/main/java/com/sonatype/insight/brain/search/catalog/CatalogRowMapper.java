/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sonatype.guide.api.dto.ComponentLicense;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.springframework.web.util.UriUtils;

public final class CatalogRowMapper
{
  // Local row field names shared with CatalogService.LOCAL_FACET_FIELDS; keep the coupling
  // compile-checked so a rename here cannot silently zero out facets there.
  static final String LOCAL_FIELD_ECOSYSTEM = "ecosystem";

  static final String LOCAL_FIELD_ORGANIZATION = "organization";

  static final String LOCAL_FIELD_STATUS = "status";

  private CatalogRowMapper() {
  }

  public static CatalogRow catalogComponent(final GuideComponentDocument c) {
    final String coordinate = coordinateOf(c);
    if (coordinate == null) {
      return null;
    }
    return CatalogRow.builder()
        .entityType(CatalogEntityType.COMPONENT.name())
        .source(SearchSource.CATALOG.value())
        .id(coordinate)
        .title(c.name() != null ? c.name() : coordinate)
        .subtitle(c.version())
        .field("ecosystem", c.format())
        .field("name", c.name())
        .field("namespace", c.namespace())
        .field("latest", c.version())
        .field("licenses", licenseNames(c.licenses()))
        .field("categories", c.categories())
        .field("latestStable", c.latestStable())
        .field("versionScore", c.versionScore())
        .field("latestMaxCvss", c.maxCvss())
        .field("publishedDate", c.publishedDate() == null ? null : c.publishedDate().toString())
        // No href: catalog rows stay within Lifecycle and do not link out to the Guide UI. An
        // in-Lifecycle link may be added once the frontend defines the destination route.
        .field("malware", c.isMalware())
        .build();
  }

  public static CatalogRow catalogVulnerability(final GuideVulnerabilityDocument v) {
    final String refid = v.refid();
    if (refid == null || refid.isBlank()) {
      return null;
    }
    return CatalogRow.builder()
        .entityType(CatalogEntityType.VULNERABILITY.name())
        .source(SearchSource.CATALOG.value())
        .id(refid)
        .title(refid)
        .subtitle(v.summary())
        .field("reference", refid)
        .field("aliases", v.aliases())
        .field("vulnerabilitySource", v.source())
        .field("severity", v.cvssSeverity())
        .field("sonatypeSeverity", v.sonatypeCvssSeverity())
        .field("cwe", v.cwes())
        .field("affectedEcosystems", v.affectedEcosystems())
        .field("isKev", v.kev())
        .field("epssScore", v.epss())
        .field("isMalware", v.isMalware())
        .field("researchType", v.researchType())
        .field("publishedAt", v.publishedAt() == null ? null : v.publishedAt().toString())
        .build();
  }

  public static CatalogRow localComponent(final SearchResultItemDTO d) {
    if (d.componentName == null) {
      return null;
    }
    return CatalogRow.builder()
        .entityType(CatalogEntityType.COMPONENT.name())
        .source(SearchSource.LOCAL.value())
        .id(d.componentName)
        .title(d.componentName)
        // No subtitle: effective-license lives on LEGAL_VIOLATION docs, never on component docs.
        .field("name", d.componentName)
        .field(LOCAL_FIELD_ECOSYSTEM, d.componentIdentifier == null ? null : d.componentIdentifier.getFormat())
        .field(LOCAL_FIELD_ORGANIZATION, d.organizationName)
        .build();
  }

  public static CatalogRow localVulnerability(final SearchResultItemDTO d) {
    if (d.vulnerabilityId == null) {
      return null;
    }
    return CatalogRow.builder()
        .entityType(CatalogEntityType.VULNERABILITY.name())
        .source(SearchSource.LOCAL.value())
        .id(d.vulnerabilityId)
        .title(d.vulnerabilityId)
        .subtitle(d.vulnerabilityDescription)
        .field("reference", d.vulnerabilityId)
        .field(LOCAL_FIELD_STATUS, d.vulnerabilityStatus)
        .field("componentName", d.componentName)
        .build();
  }

  private static List<String> licenseNames(final List<? extends ComponentLicense> licenses) {
    if (licenses == null || licenses.isEmpty()) {
      return null;
    }
    final List<String> names = new ArrayList<>(licenses.size());
    for (ComponentLicense l : licenses) {
      if (l != null && l.licenseName() != null) {
        names.add(l.licenseName());
      }
    }
    return names.isEmpty() ? null : names;
  }

  static String coordinateOf(final GuideComponentDocument c) {
    if (c.format() == null || c.format().isBlank() || c.name() == null || c.name().isBlank()) {
      return null;
    }
    final StringBuilder sb = new StringBuilder("pkg:");
    sb.append(encode(c.format())).append('/');
    if (c.namespace() != null && !c.namespace().isBlank()) {
      sb.append(encode(c.namespace())).append('/');
    }
    sb.append(encode(c.name()));
    if (c.version() != null && !c.version().isBlank()) {
      sb.append('@').append(encode(c.version()));
    }
    return sb.toString();
  }

  private static String encode(final String s) {
    // Path-segment encoding (not form encoding), so '+' stays literal and spaces become %20.
    return UriUtils.encodePathSegment(s, StandardCharsets.UTF_8);
  }
}
