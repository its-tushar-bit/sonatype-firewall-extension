/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.results;

/**
 * @since GLOBAL_SEARCH
 */
public class SearchResultItemDTO
{
  public String itemType;

  public String organizationId;

  public String organizationName;

  public String applicationId;

  public String applicationPublicId;

  public String applicationName;

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

  public int policyThreatLevel;

  public int resultIndex;
}
