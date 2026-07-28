/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.catalog;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURLBuilder;
import com.sonatype.guide.api.dto.ComponentLicense;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.dto.GuideVulnerabilityDocument;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

public final class CatalogRowMapper
{
  private static final Logger log = LoggerFactory.getLogger(CatalogRowMapper.class);

  // Local row field names shared with CatalogService.LOCAL_FACET_FIELDS; keep the coupling
  // compile-checked so a rename here cannot silently zero out facets there.
  static final String LOCAL_FIELD_ECOSYSTEM = "ecosystem";

  static final String LOCAL_FIELD_ORGANIZATION = "organization";

  static final String LOCAL_FIELD_APPLICATION = "application";

  static final String LOCAL_FIELD_STATUS = "status";

  static final String LOCAL_FIELD_COMPONENT_HASH = "componentHash";

  static final String LOCAL_FIELD_COORDINATES = "coordinates";

  static final String LOCAL_FIELD_VERSION = "version";

  static final String LOCAL_FIELD_SEVERITY = "severity";

  static final String LOCAL_FIELD_COMPONENT_NAME = "componentName";

  /**
   * Vulnerability row id field, shared with {@link CatalogService}'s affected-app/component
   * aggregation so a rename cannot silently zero out the counts (the count query keys off this
   * field's value).
   */
  static final String LOCAL_FIELD_REFERENCE = "reference";

  /**
   * Coordinate names, in preference order, that map to the purl name segment. The stable local
   * component id is the componentHash; the purl is a human-readable coordinate rendering only.
   */
  private static final List<String> PURL_NAME_COORDINATES = List.of("packageId", "artifactId", "name");

  private static final List<String> PURL_NAMESPACE_COORDINATES = List.of("namespace", "groupId");

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

  /**
   * Local component row. The stable, component-centric id is the componentHash (falls back to the
   * display name only when a doc predates hash indexing). Coordinate/version rendering is best-effort:
   * a malformed purl is dropped, never fatal, since the hash is the identity. Affected-app counts and
   * severity facets are query-time aggregations added by {@link CatalogService}, not stored fields.
   */
  public static CatalogRow localComponent(final SearchResultItemDTO d) {
    if (d.componentHash == null && d.componentName == null) {
      return null;
    }
    final String id = d.componentHash != null ? d.componentHash : d.componentName;
    final String title = d.componentName != null ? d.componentName : d.componentHash;
    final ApiComponentIdentifierDTOV2 identifier = d.componentIdentifier;
    return CatalogRow.builder()
        .entityType(CatalogEntityType.COMPONENT.name())
        .source(SearchSource.LOCAL.value())
        .id(id)
        .title(title)
        // No subtitle: effective-license lives on LEGAL_VIOLATION docs, never on component docs.
        .field(LOCAL_FIELD_COMPONENT_HASH, d.componentHash)
        .field(LOCAL_FIELD_COMPONENT_NAME, d.componentName)
        .field("name", d.componentName)
        .field(LOCAL_FIELD_ECOSYSTEM, identifier == null ? null : identifier.getFormat())
        .field(LOCAL_FIELD_VERSION, versionOf(identifier))
        .field(LOCAL_FIELD_COORDINATES, coordinateOf(identifier))
        .field(LOCAL_FIELD_ORGANIZATION, d.organizationName)
        // applicationName seeds the apps facet. A component doc is per-app-per-stage, so this is the
        // owning app of the dedup-winning doc; the facet's whole-corpus distinct count is app-complete.
        .field(LOCAL_FIELD_APPLICATION, d.applicationName)
        // No href: the IQ component detail route requires an application/report context, so there is
        // no context-free deep link. The frontend routes on the componentHash id.
        .build();
  }

  /**
   * Local vulnerability row. Severity is the numeric CVSS carried on the doc. The href is the
   * context-free classic Vulnerability Lookup route ({@code #/vulnerabilities/<id>}); the frontend
   * prefixes the context-path / MTIQ bundle path, so this stays a relative hash path (NOUX-safe).
   * Affected component/app counts and corpus ecosystems are query-time aggregations added by
   * {@link CatalogService}.
   */
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
        .field(LOCAL_FIELD_REFERENCE, d.vulnerabilityId)
        .field(LOCAL_FIELD_STATUS, d.vulnerabilityStatus)
        .field(LOCAL_FIELD_SEVERITY, d.vulnerabilitySeverity)
        .field(LOCAL_FIELD_ECOSYSTEM, d.componentIdentifier == null ? null : d.componentIdentifier.getFormat())
        .field(LOCAL_FIELD_COMPONENT_NAME, d.componentName)
        // organizationName / applicationName seed the orgs + apps facets. A vuln doc is per-app-per-stage,
        // so these come from the dedup-winning doc; each facet's whole-corpus distinct count is complete.
        .field(LOCAL_FIELD_ORGANIZATION, d.organizationName)
        .field(LOCAL_FIELD_APPLICATION, d.applicationName)
        .href(vulnerabilityHref(d.vulnerabilityId))
        .build();
  }

  /** Relative classic hash path for the Vulnerability Lookup detail route; the frontend adds the prefix. */
  static String vulnerabilityHref(final String vulnerabilityId) {
    if (StringUtils.isBlank(vulnerabilityId)) {
      return null;
    }
    return "#/vulnerabilities/" + encode(vulnerabilityId);
  }

  private static String versionOf(final ApiComponentIdentifierDTOV2 identifier) {
    if (identifier == null || identifier.getCoordinates() == null) {
      return null;
    }
    final String version = identifier.getCoordinates().get("version");
    return StringUtils.isBlank(version) ? null : version;
  }

  /**
   * Best-effort canonical purl from the local component coordinates via the shared
   * {@link PackageURLBuilder}. Returns {@code null} (rather than throwing) when the format/name is
   * missing or the coordinate is malformed, since the componentHash is the row identity.
   */
  static String coordinateOf(final ApiComponentIdentifierDTOV2 identifier) {
    if (identifier == null || StringUtils.isBlank(identifier.getFormat())) {
      return null;
    }
    final Map<String, String> coordinates = identifier.getCoordinates();
    if (coordinates == null || coordinates.isEmpty()) {
      return null;
    }
    final String name = firstNonBlank(coordinates, PURL_NAME_COORDINATES);
    if (name == null) {
      return null;
    }
    final PackageURLBuilder builder = PackageURLBuilder.aPackageURL()
        .withType(identifier.getFormat())
        .withName(name);
    final String namespace = firstNonBlank(coordinates, PURL_NAMESPACE_COORDINATES);
    if (namespace != null) {
      builder.withNamespace(namespace);
    }
    final String version = coordinates.get("version");
    if (StringUtils.isNotBlank(version)) {
      builder.withVersion(version);
    }
    try {
      return builder.build().toString();
    }
    catch (MalformedPackageURLException e) {
      log.debug("Local component produced a malformed purl for format {}", identifier.getFormat());
      return null;
    }
  }

  private static String firstNonBlank(final Map<String, String> coordinates, final List<String> keys) {
    for (String key : keys) {
      final String value = coordinates.get(key);
      if (StringUtils.isNotBlank(value)) {
        return value;
      }
    }
    return null;
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

  /**
   * Catalog (Guide) component purl. Kept as an explicit path-segment build rather than routed through
   * {@link PackageURLBuilder}: the builder percent-encodes a namespace {@code @} (e.g. {@code %40scope}),
   * whereas this leg preserves it literally to match the coordinate rendering the Guide store returns.
   * {@code encode()} does path-segment encoding, so {@code @} and {@code +} stay literal.
   */
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
