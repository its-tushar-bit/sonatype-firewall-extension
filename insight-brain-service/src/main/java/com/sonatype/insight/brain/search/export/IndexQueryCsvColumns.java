/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.search.indexquery.IndexQueryRow;
import com.sonatype.insight.brain.search.indexquery.IndexQueryType;

/**
 * Column sets for the index-query list exports, one per entity type.
 * <p>
 * Each column set mirrors the fields the corresponding V1 page row carries, so the CSV matches what
 * the page shows. The readers pull from {@link IndexQueryRow#getFields()} using the same keys
 * {@code IndexQueryRowMapper} writes, so a rename on the mapper surfaces as an empty column in the
 * export test rather than drifting silently.
 */
public final class IndexQueryCsvColumns
{
  private IndexQueryCsvColumns() {
  }

  public static List<CsvColumn<IndexQueryRow>> forType(final IndexQueryType queryType) {
    return switch (queryType) {
      case APPLICATION -> APPLICATION;
      case VIOLATION -> VIOLATION;
      case WAIVER -> WAIVER;
      case POLICY -> POLICY;
    };
  }

  /** Filename stem per entity type; the timestamp is appended by the response builder. */
  public static String fileNamePrefix(final IndexQueryType queryType) {
    return switch (queryType) {
      case APPLICATION -> "applications";
      case VIOLATION -> "violations";
      case WAIVER -> "waivers";
      case POLICY -> "policies";
    };
  }

  private static final List<CsvColumn<IndexQueryRow>> APPLICATION = List.of(
      CsvColumn.of("Application", field("applicationName")),
      CsvColumn.of("Application ID", field("applicationPublicId")),
      CsvColumn.of("Organization", field("organizationName")),
      CsvColumn.of("Categories", field("applicationCategories")),
      CsvColumn.of("Max Policy Threat Level", field("maxPolicyThreatLevel")),
      CsvColumn.of("Total Risk", field("totalRisk")),
      CsvColumn.of("Last Evaluation Time", epochMillis("lastEvaluationTimeEpochMs")));

  private static final List<CsvColumn<IndexQueryRow>> VIOLATION = List.of(
      CsvColumn.of("Policy", field("policyName")),
      CsvColumn.of("Policy Type", field("policyType")),
      CsvColumn.of("Threat Level", field("threat")),
      CsvColumn.of("Application", field("applicationName")),
      CsvColumn.of("Organization", field("organizationName")),
      CsvColumn.of("Component", field("componentName")),
      CsvColumn.of("Component Version", field("componentVersion")),
      CsvColumn.of("Stage", field("stage")),
      CsvColumn.of("State", field("state")),
      CsvColumn.of("Waiver Type", field("waiverType")),
      CsvColumn.of("Effective License", field("effectiveLicense")),
      CsvColumn.of("Violation ID", IndexQueryRow::getId));

  private static final List<CsvColumn<IndexQueryRow>> WAIVER = List.of(
      CsvColumn.of("Policy", field("policyName")),
      CsvColumn.of("Policy Type", field("policyType")),
      CsvColumn.of("Threat Level", field("threatLevel")),
      CsvColumn.of("Scope", field("scope")),
      CsvColumn.of("Application", field("applicationName")),
      CsvColumn.of("Organization", field("organizationName")),
      CsvColumn.of("Waived By", field("waivedBy")),
      CsvColumn.of("Reason", field("reason")),
      CsvColumn.of("Comment", field("comment")),
      CsvColumn.of("Auto Waiver", field("auto")),
      CsvColumn.of("Requested", field("isRequested")),
      CsvColumn.of("Request Status", field("status")),
      CsvColumn.of("Created At", field("createdAt")),
      CsvColumn.of("Expires At", field("expiresAt")),
      CsvColumn.of("Waiver ID", IndexQueryRow::getId));

  private static final List<CsvColumn<IndexQueryRow>> POLICY = List.of(
      CsvColumn.of("Policy", field("name")),
      CsvColumn.of("Policy Type", field("policyType")),
      CsvColumn.of("Threat Level", field("threatLevel")),
      CsvColumn.of("Owner Type", field("ownerType")),
      CsvColumn.of("Organization", field("organizationName")),
      CsvColumn.of("Waiver Count", field("waiverCount")),
      CsvColumn.of("Policy ID", IndexQueryRow::getId));

  private static java.util.function.Function<IndexQueryRow, Object> field(final String key) {
    return row -> row.getFields().get(key);
  }

  /**
   * Renders an epoch-millis row field as an ISO-8601 instant so the exported timestamp is readable
   * rather than a raw long. Kept tolerant: a value that is not a number renders as-is rather than
   * failing the whole export.
   */
  private static java.util.function.Function<IndexQueryRow, Object> epochMillis(final String key) {
    return row -> {
      final Map<String, Object> fields = row.getFields();
      final Object value = fields.get(key);
      if (value instanceof Number number) {
        return Instant.ofEpochMilli(number.longValue()).toString();
      }
      return value;
    };
  }
}
