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

/**
 * @since 1.88
 */
@JsonInclude(Include.NON_NULL)
public class SearchResultItemDTO
{
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
  }
}
