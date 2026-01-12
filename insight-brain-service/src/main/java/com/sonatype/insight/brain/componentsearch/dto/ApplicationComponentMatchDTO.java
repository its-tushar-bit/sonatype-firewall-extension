/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.dto;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.utils.CsvWritable;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DTO representing an application containing a matched component.
 */
@Schema(description = "Application containing a matched component")
public class ApplicationComponentMatchDTO implements CsvWritable
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationComponentMatchDTO.class);

  private static final ThreadLocal<SimpleDateFormat> CSV_DATE_FORMATTER = ThreadLocal.withInitial(() -> {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return sdf;
  });

  private final String applicationPublicId;

  private final String applicationName;

  private final String applicationInternalId;

  private final String stage;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
  private final Date evaluationDate;

  private final String packageUrl;

  private final String componentDisplayName;

  private final String hash;

  private final String cveId;

  private final String recommendedAction;

  private final boolean activeWaiver;

  private final boolean violating;

  private final String reportId;

  private String baseUrl;

  @JsonCreator
  public ApplicationComponentMatchDTO(
      @JsonProperty("applicationPublicId") String applicationPublicId,
      @JsonProperty("applicationName") String applicationName,
      @JsonProperty("applicationInternalId") String applicationInternalId,
      @JsonProperty("stage") String stage,
      @JsonProperty("evaluationDate") Date evaluationDate,
      @JsonProperty("packageUrl") String packageUrl,
      @JsonProperty("componentDisplayName") String componentDisplayName,
      @JsonProperty("hash") String hash,
      @JsonProperty("cveId") String cveId,
      @JsonProperty("recommendedAction") String recommendedAction,
      @JsonProperty("activeWaiver") boolean activeWaiver,
      @JsonProperty("violating") boolean violating,
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
    this.cveId = cveId;
    this.recommendedAction = recommendedAction;
    this.activeWaiver = activeWaiver;
    this.violating = violating;
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

  public Date getEvaluationDate() {
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

  public String getCveId() {
    return cveId;
  }

  public String getRecommendedAction() {
    return recommendedAction;
  }

  public boolean getActiveWaiver() {
    return activeWaiver;
  }

  public boolean getViolating() {
    return violating;
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
    String formattedDate = evaluationDate != null ? CSV_DATE_FORMATTER.get().format(evaluationDate) : "";
    return joiner.join(
        formatField(applicationName),
        formatField(applicationPublicId),
        formatField(stage),
        formatField(componentDisplayName),
        formatField(extractVersionFromPackageUrl(packageUrl)),
        formatField(cveId),
        formatField(recommendedAction),
        formatField(formattedDate),
        formatField(activeWaiver ? "True" : "False"),
        formatField(violating ? "True" : "False"),
        formatField(evaluationUrl)
    );
  }

  private String extractVersionFromPackageUrl(String purl) {
    if (purl == null || purl.isEmpty()) {
      return "";
    }
    try {
      PackageUrlIdentifier purlIdentifier = new PackageUrlIdentifier(purl);
      String version = purlIdentifier.getVersion();
      return version != null ? version : "";
    }
    catch (Exception e) {
      log.warn("Failed to extract version from package URL: {}", purl, e);
      return "";
    }
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
