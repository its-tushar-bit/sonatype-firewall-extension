/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.util.List;
import java.util.function.Function;

import com.sonatype.insight.brain.search.catalog.CatalogEntityType;
import com.sonatype.insight.brain.search.catalog.CatalogRow;

/**
 * Column sets for the My-Scan-Data (LOCAL source) catalog list exports.
 * <p>
 * Mirrors the fields the corresponding V1 page row carries, reading from
 * {@link CatalogRow#getFields()} with the keys {@code CatalogRowMapper} writes plus the computed
 * columns {@code CatalogService} enriches onto each page ({@code affectedApps},
 * {@code affectedComponents}, and the component severity counts).
 * <p>
 * There is no column set for the CATALOG (Guide/HDS) source: that leg is not exportable from here
 * (see the per-module CSV notes on the export endpoint).
 */
public final class CatalogCsvColumns
{
  private CatalogCsvColumns() {
  }

  public static List<CsvColumn<CatalogRow>> forLocalType(final CatalogEntityType entityType) {
    return switch (entityType) {
      case COMPONENT -> COMPONENT;
      case VULNERABILITY -> VULNERABILITY;
    };
  }

  public static String fileNamePrefix(final CatalogEntityType entityType) {
    return switch (entityType) {
      case COMPONENT -> "components";
      case VULNERABILITY -> "vulnerabilities";
    };
  }

  private static final List<CsvColumn<CatalogRow>> COMPONENT = List.of(
      CsvColumn.of("Component", field("componentName")),
      CsvColumn.of("Version", field("version")),
      CsvColumn.of("Coordinates", field("coordinates")),
      CsvColumn.of("Ecosystem", field("ecosystem")),
      CsvColumn.of("Organization", field("organization")),
      CsvColumn.of("Application", field("application")),
      CsvColumn.of("Affected Applications", field("affectedApps")),
      CsvColumn.of("Critical Violations", field("latest_critical_count")),
      CsvColumn.of("High Violations", field("latest_high_count")),
      CsvColumn.of("Medium Violations", field("latest_medium_count")),
      CsvColumn.of("Low Violations", field("latest_low_count")),
      CsvColumn.of("Component Hash", field("componentHash")));

  private static final List<CsvColumn<CatalogRow>> VULNERABILITY = List.of(
      CsvColumn.of("Vulnerability", field("reference")),
      CsvColumn.of("Severity", field("severity")),
      CsvColumn.of("Status", field("status")),
      CsvColumn.of("Ecosystem", field("ecosystem")),
      CsvColumn.of("Component", field("componentName")),
      CsvColumn.of("Organization", field("organization")),
      CsvColumn.of("Application", field("application")),
      CsvColumn.of("Affected Applications", field("affectedApps")),
      CsvColumn.of("Affected Components", field("affectedComponents")),
      CsvColumn.of("First Seen", field("firstSeen")),
      CsvColumn.of("Description", CatalogRow::getSubtitle));

  private static Function<CatalogRow, Object> field(final String key) {
    return row -> row.getFields().get(key);
  }
}
