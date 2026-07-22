/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.results;

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

  public String applicationCategoryId;

  public String applicationCategoryName;

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

  public int resultIndex;

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
    applicationCategoryId = document.get(FieldIdentifier.APPLICATION_CATEGORY_ID.label);
    applicationCategoryName = document.get(FieldIdentifier.APPLICATION_CATEGORY_NAME.label);
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
}
