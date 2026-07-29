/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.results;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.lucene.DocumentBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.88
 */
@JsonInclude(Include.NON_NULL)
public class SearchResultItemDTO
{
  private static final Logger log = LoggerFactory.getLogger(SearchResultItemDTO.class);

  public String itemType;

  public String organizationId;

  public String organizationName;

  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

  public String applicationVersion;

  public String sbomSpecification;

  public String policyEvaluationStage;

  public String reportId;

  public String componentHash;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String componentName;

  public String vulnerabilityId;

  public String vulnerabilityDescription;

  /** CVSS score from {@code vulnerabilitySeverity} when present on the indexed document. */
  public Float vulnerabilitySeverity;

  public String vulnerabilityStatus;

  /** Epoch-millis when this IQ first detected the vulnerability; null when it triggered no policy violation. */
  public Long vulnerabilityFirstSeenEpochMs;

  public String applicationCategoryId;

  public String applicationCategoryName;

  /** Multi-valued category names denormalized onto APPLICATION and violation docs. */
  public List<String> applicationCategoryNames;

  /** Epoch-millis of the application's latest evaluation; null when never evaluated. */
  public Long applicationLastEvaluationTimeEpochMs;

  /** Raw {@code "stage:severity:count"} tokens for the application evaluation-card breakdown. */
  public List<String> applicationStageSeverityCounts;

  public String applicationCategoryColor;

  public String applicationCategoryDescription;

  public String componentLabelId;

  public String componentLabelName;

  public String componentLabelColor;

  public String componentLabelDescription;

  public String policyId;

  public String policyName;

  public String policyThreatCategory;

  public Integer policyThreatLevel;

  public String policyViolationId;

  public String policyViolationThreatCategory;

  public Integer policyViolationThreatLevel;

  public String policyViolationPolicyName;

  public String policyViolationPolicyId;

  public String policyViolationWaiverStatus;

  public String policyViolationConstraintName;

  public String componentEffectiveLicenseId;

  public String componentEffectiveLicenseName;

  public String componentLicenseThreatGroupName;

  public Integer componentLicenseThreatLevel;

  public String policyWaiverId;

  public String policyWaiverPolicyName;

  public String policyWaiverPolicyId;

  public String policyWaiverReason;

  public String policyWaiverComment;

  public String policyWaiverCreatedAt;

  public String policyWaiverExpiresAt;

  public String policyWaiverScopeOwnerId;

  public String policyWaiverScopeOwnerType;

  public Integer policyWaiverThreatLevel;

  public String policyWaiverWaivedBy;

  public Boolean policyWaiverAuto;

  public Boolean policyWaiverIsAuto;

  public String policyWaiverExpiryStatus;

  public String policyWaiverPolicyType;

  public String policyWaiverScope;

  public String policyWaiverRequestStatus;

  public String requesterName;

  public String reviewerName;

  public String reviewTime;

  public String rejectionReason;

  public String noteToReviewer;

  public int resultIndex;

  /**
   * Auto-vs-manual for WAIVER rows. Prefers Ana {@link #policyWaiverIsAuto}; falls back to Classic
   * {@link #policyWaiverAuto} when the Ana field is absent (pre-reindex docs or DTOs not built from a
   * Lucene {@link Document}). Null/absent → not auto.
   */
  public boolean resolvedPolicyWaiverIsAuto() {
    return Boolean.TRUE.equals(policyWaiverIsAuto != null ? policyWaiverIsAuto : policyWaiverAuto);
  }

  public SearchResultItemDTO() {
  }

  public SearchResultItemDTO(final Document document) {
    itemType = document.get(FieldIdentifier.ITEM_TYPE.label);
    organizationId = document.get(FieldIdentifier.ORGANIZATION_ID.label);
    organizationName = document.get(FieldIdentifier.ORGANIZATION_NAME.label);
    applicationId = document.get(FieldIdentifier.APPLICATION_ID.label);
    applicationPublicId = document.get(FieldIdentifier.APPLICATION_PUBLIC_ID.label);
    applicationName = document.get(FieldIdentifier.APPLICATION_NAME.label);
    applicationVersion = document.get(FieldIdentifier.APPLICATION_VERSION.label);
    sbomSpecification = document.get(FieldIdentifier.SBOM_SPECIFICATION.label);
    policyEvaluationStage = document.get(FieldIdentifier.POLICY_EVALUATION_STAGE.label);
    if (policyEvaluationStage != null) {
      policyEvaluationStage = StageTypes.getById(policyEvaluationStage).getName();
    }
    reportId = document.get(FieldIdentifier.REPORT_ID.label);
    componentHash = document.get(FieldIdentifier.COMPONENT_HASH.label);
    String format = document.get(FieldIdentifier.COMPONENT_FORMAT.label);
    if (format != null) {
      ApiComponentIdentifierDTOV2 apiComponentIdentifierDTOV2 = new ApiComponentIdentifierDTOV2();
      apiComponentIdentifierDTOV2.setFormat(format);
      Map<String, String> coordinates = new TreeMap<>();
      for (String coordinateName : ComponentIdentifier.getAllCoordinateNames(format)) {
        String coordinateValue = document.get(DocumentBuilder.getFieldNameForCoordinate(coordinateName));
        if (coordinateValue != null) {
          coordinates.put(coordinateName, coordinateValue);
        }
      }
      apiComponentIdentifierDTOV2.setCoordinates(coordinates);
      componentIdentifier = apiComponentIdentifierDTOV2;
    }
    componentName = document.get(FieldIdentifier.COMPONENT_NAME.label);
    vulnerabilityId = document.get(FieldIdentifier.VULNERABILITY_ID.label);
    vulnerabilityDescription = document.get(FieldIdentifier.VULNERABILITY_DESCRIPTION.label);
    vulnerabilitySeverity = parseFloatOrNull(document.get(FieldIdentifier.VULNERABILITY_SEVERITY.label));
    vulnerabilityStatus = document.get(FieldIdentifier.VULNERABILITY_STATUS.label);
    vulnerabilityFirstSeenEpochMs =
        parseLongOrNull(document.get(FieldIdentifier.VULNERABILITY_FIRST_SEEN_EPOCH_MS.label));
    applicationCategoryId = document.get(FieldIdentifier.APPLICATION_CATEGORY_ID.label);
    applicationCategoryName = document.get(FieldIdentifier.APPLICATION_CATEGORY_NAME.label);
    applicationCategoryNames = valuesOrNull(document.getValues(FieldIdentifier.APPLICATION_CATEGORY_NAME.label));
    applicationLastEvaluationTimeEpochMs =
        parseLongOrNull(document.get(FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label));
    applicationStageSeverityCounts =
        valuesOrNull(document.getValues(FieldIdentifier.APPLICATION_STAGE_SEVERITY_COUNT.label));
    applicationCategoryColor = document.get(FieldIdentifier.APPLICATION_CATEGORY_COLOR.label);
    applicationCategoryDescription = document.get(FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION.label);
    componentLabelId = document.get(FieldIdentifier.COMPONENT_LABEL_ID.label);
    componentLabelName = document.get(FieldIdentifier.COMPONENT_LABEL_NAME.label);
    componentLabelColor = document.get(FieldIdentifier.COMPONENT_LABEL_COLOR.label);
    componentLabelDescription = document.get(FieldIdentifier.COMPONENT_LABEL_DESCRIPTION.label);
    policyId = document.get(FieldIdentifier.POLICY_ID.label);
    policyName = document.get(FieldIdentifier.POLICY_NAME.label);
    policyThreatCategory = document.get(FieldIdentifier.POLICY_THREAT_CATEGORY.label);
    String policyThreatLevelString = document.get(FieldIdentifier.POLICY_THREAT_LEVEL.label);
    policyThreatLevel = policyThreatLevelString == null ? null : Integer.valueOf(policyThreatLevelString);
    policyViolationId = document.get(FieldIdentifier.POLICY_VIOLATION_ID.label);
    policyViolationThreatCategory = document.get(FieldIdentifier.POLICY_VIOLATION_THREAT_CATEGORY.label);
    String policyViolationThreatLevelString = document.get(FieldIdentifier.POLICY_VIOLATION_THREAT_LEVEL.label);
    policyViolationThreatLevel =
        policyViolationThreatLevelString == null ? null : Integer.valueOf(policyViolationThreatLevelString);
    policyViolationPolicyName = document.get(FieldIdentifier.POLICY_VIOLATION_POLICY_NAME.label);
    policyViolationPolicyId = document.get(FieldIdentifier.POLICY_VIOLATION_POLICY_ID.label);
    policyViolationWaiverStatus = document.get(FieldIdentifier.POLICY_VIOLATION_WAIVER_STATUS.label);
    policyViolationConstraintName = document.get(FieldIdentifier.POLICY_VIOLATION_CONSTRAINT_NAME.label);
    componentEffectiveLicenseId = document.get(FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_ID.label);
    componentEffectiveLicenseName = document.get(FieldIdentifier.COMPONENT_EFFECTIVE_LICENSE_NAME.label);
    componentLicenseThreatGroupName = document.get(FieldIdentifier.COMPONENT_LICENSE_THREAT_GROUP_NAME.label);
    String componentLicenseThreatLevelString = document.get(FieldIdentifier.COMPONENT_LICENSE_THREAT_LEVEL.label);
    componentLicenseThreatLevel =
        componentLicenseThreatLevelString == null ? null : Integer.valueOf(componentLicenseThreatLevelString);
    policyWaiverId = document.get(FieldIdentifier.POLICY_WAIVER_ID.label);
    policyWaiverPolicyName = document.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label);
    policyWaiverPolicyId = document.get(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label);
    policyWaiverReason = document.get(FieldIdentifier.POLICY_WAIVER_REASON.label);
    policyWaiverComment = document.get(FieldIdentifier.POLICY_WAIVER_COMMENT.label);
    policyWaiverCreatedAt = document.get(FieldIdentifier.POLICY_WAIVER_CREATED_AT.label);
    policyWaiverExpiresAt = document.get(FieldIdentifier.POLICY_WAIVER_EXPIRES_AT.label);
    policyWaiverScopeOwnerId = document.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_ID.label);
    policyWaiverScopeOwnerType = document.get(FieldIdentifier.POLICY_WAIVER_SCOPE_OWNER_TYPE.label);
    String policyWaiverThreatLevelString = document.get(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label);
    policyWaiverThreatLevel =
        policyWaiverThreatLevelString == null ? null : Integer.valueOf(policyWaiverThreatLevelString);
    policyWaiverWaivedBy = document.get(FieldIdentifier.POLICY_WAIVER_WAIVED_BY.label);
    String policyWaiverAutoString = document.get(FieldIdentifier.POLICY_WAIVER_AUTO.label);
    policyWaiverAuto = policyWaiverAutoString == null ? null : Boolean.valueOf(policyWaiverAutoString);
    String policyWaiverIsAutoString = document.get(FieldIdentifier.POLICY_WAIVER_IS_AUTO.label);
    policyWaiverIsAuto = policyWaiverIsAutoString == null ? null : Boolean.valueOf(policyWaiverIsAutoString);
    // Prefer Ana isAuto when present; fall back to left-nav discriminator for pre-reindex docs.
    if (policyWaiverIsAuto == null) {
      policyWaiverIsAuto = policyWaiverAuto;
    }
    policyWaiverExpiryStatus = document.get(FieldIdentifier.POLICY_WAIVER_EXPIRY_STATUS.label);
    policyWaiverPolicyType = document.get(FieldIdentifier.POLICY_WAIVER_POLICY_TYPE.label);
    policyWaiverScope = document.get(FieldIdentifier.POLICY_WAIVER_SCOPE.label);
    policyWaiverRequestStatus = document.get(FieldIdentifier.POLICY_WAIVER_REQUEST_STATUS.label);
    requesterName = document.get(FieldIdentifier.REQUESTER_NAME.label);
    reviewerName = document.get(FieldIdentifier.REVIEWER_NAME.label);
    reviewTime = document.get(FieldIdentifier.REVIEW_TIME.label);
    rejectionReason = document.get(FieldIdentifier.REJECTION_REASON.label);
    noteToReviewer = document.get(FieldIdentifier.NOTE_TO_REVIEWER.label);
  }

  /**
   * Parses indexed severity without failing the whole search response on malformed documents.
   */
  static Float parseFloatOrNull(final String value) {
    if (value == null) {
      return null;
    }
    try {
      return Float.valueOf(value);
    }
    catch (NumberFormatException e) {
      log.warn("Ignoring non-numeric vulnerabilitySeverity index value: {}", value);
      return null;
    }
  }

  /** Parses an indexed epoch-millis long without failing the whole response on a malformed document. */
  static Long parseLongOrNull(final String value) {
    if (value == null) {
      return null;
    }
    try {
      return Long.valueOf(value);
    }
    catch (NumberFormatException e) {
      log.warn("Ignoring non-numeric applicationLastEvaluationTimeEpochMs index value: {}", value);
      return null;
    }
  }

  /** Wraps a multi-valued field's stored values as an immutable list, or {@code null} when absent/empty. */
  static List<String> valuesOrNull(final String[] values) {
    if (values == null || values.length == 0) {
      return null;
    }
    return List.copyOf(Arrays.asList(values));
  }
}
