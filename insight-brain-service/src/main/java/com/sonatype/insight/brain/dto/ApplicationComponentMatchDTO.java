/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dto;

import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.utils.CsvWritable;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;

/**
 * DTO representing an application containing a matched component.
 */
@Schema(description = "Application containing a matched component")
public class ApplicationComponentMatchDTO implements CsvWritable
{
  @Schema(description = "Application public ID", example = "my-application")
  private final String applicationPublicId;

  @Schema(description = "Application name for display")
  private final String applicationName;

  @Schema(description = "Application internal ID")
  private final String applicationInternalId;

  @Schema(description = "Report stage", example = "build")
  private final String stage;

  @Schema(description = "Evaluation date (ISO 8601)")
  private final String evaluationDate;

  @Schema(description = "Component Package URL", example = "pkg:npm/lodash@4.17.21")
  private final String packageUrl;

  @Schema(description = "Component display name", example = "lodash-4.17.21")
  private final String componentDisplayName;

  @Schema(description = "Component hash")
  private final String hash;

  @Schema(description = "Matched component name")
  private final String matchedName;

  @Schema(description = "Matched component version")
  private final String matchedVersion;

  @Schema(description = "Vulnerability IDs (comma-separated)", example = "CVE-2025-55182, sonatype-2025-007429")
  private final String vulnerabilityIds;

  @Schema(description = "Recommended action", example = "Upgrade to 19.0.1")
  private final String recommendedAction;

  @Schema(description = "Recommended version", example = "19.0.1")
  private final String recommendedVersion;

  @Schema(description = "Active waiver", example = "Yes")
  private final String activeWaiver;

  @Schema(description = "Implicated files", example = "Yes")
  private final String implicatedFiles;

  @Schema(description = "Report ID")
  private final String reportId;

  private String baseUrl;

  public ApplicationComponentMatchDTO(
      @JsonProperty("applicationPublicId") String applicationPublicId,
      @JsonProperty("applicationName") String applicationName,
      @JsonProperty("applicationInternalId") String applicationInternalId,
      @JsonProperty("stage") String stage,
      @JsonProperty("evaluationDate") String evaluationDate,
      @JsonProperty("packageUrl") String packageUrl,
      @JsonProperty("componentDisplayName") String componentDisplayName,
      @JsonProperty("hash") String hash,
      @JsonProperty("matchedName") String matchedName,
      @JsonProperty("matchedVersion") String matchedVersion,
      @JsonProperty("vulnerabilityIds") String vulnerabilityIds,
      @JsonProperty("recommendedAction") String recommendedAction,
      @JsonProperty("recommendedVersion") String recommendedVersion,
      @JsonProperty("activeWaiver") String activeWaiver,
      @JsonProperty("implicatedFiles") String implicatedFiles,
      @JsonProperty("reportId") String reportId)
  {
    this.applicationPublicId = applicationPublicId;
    this.applicationName = applicationName;
    this.applicationInternalId = applicationInternalId;
    this.stage = stage;
    this.evaluationDate = evaluationDate;
    this.packageUrl = packageUrl;
    this.componentDisplayName = componentDisplayName;
    this.hash = hash;
    this.matchedName = matchedName;
    this.matchedVersion = matchedVersion;
    this.vulnerabilityIds = vulnerabilityIds;
    this.recommendedAction = recommendedAction;
    this.recommendedVersion = recommendedVersion;
    this.activeWaiver = activeWaiver;
    this.implicatedFiles = implicatedFiles;
    this.reportId = reportId;
  }

  public String getApplicationPublicId() {
    return applicationPublicId;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public String getApplicationInternalId() {
    return applicationInternalId;
  }

  public String getStage() {
    return stage;
  }

  public String getEvaluationDate() {
    return evaluationDate;
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public String getComponentDisplayName() {
    return componentDisplayName;
  }

  public String getHash() {
    return hash;
  }

  public String getMatchedName() {
    return matchedName;
  }

  public String getMatchedVersion() {
    return matchedVersion;
  }

  public String getVulnerabilityIds() {
    return vulnerabilityIds;
  }

  public String getRecommendedAction() {
    return recommendedAction;
  }

  public String getRecommendedVersion() {
    return recommendedVersion;
  }

  public String getActiveWaiver() {
    return activeWaiver;
  }

  public String getImplicatedFiles() {
    return implicatedFiles;
  }

  public String getReportId() {
    return reportId;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public String toCsvLine() {
    String evaluationUrl = buildEvaluationUrl();
    return joiner.join(
        formatField(applicationName),
        formatField(applicationInternalId),
        formatField(componentDisplayName),
        formatField(matchedVersion),
        formatField(vulnerabilityIds),
        formatField(recommendedAction),
        formatField(recommendedVersion),
        formatField(evaluationDate),
        formatField(activeWaiver),
        formatField(implicatedFiles),
        formatField(evaluationUrl)
    );
  }

  private String buildEvaluationUrl() {
    if (applicationPublicId == null || reportId == null) {
      return "";
    }

    String relativePath = UserInterfaceLinksHelper.getReportUrl(applicationPublicId, reportId);

    if (StringUtils.isBlank(baseUrl)) {
      return relativePath;
    }

    return UriBuilder.fromUri(baseUrl).path(relativePath).toString();
  }

  private String formatField(String field) {
    if (field == null || field.isEmpty()) {
      return "";
    }
    String escaped = CsvWritable.escapeDoubleQuotes(field);
    return CsvWritable.quoteFieldWhenSpecialCsvCharactersPresent(escaped);
  }
}
