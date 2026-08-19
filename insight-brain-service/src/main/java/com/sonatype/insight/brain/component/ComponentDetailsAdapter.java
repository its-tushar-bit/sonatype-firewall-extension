/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;

public class ComponentDetailsAdapter
{
  public static NamedComponentDetails convert(final ComponentEvaluationData componentEvaluationData) {
    NamedComponentDetails componentDetails = new NamedComponentDetails();
    componentDetails.setCatalogDate(componentEvaluationData.catalogDate);
    componentDetails.setHash(componentEvaluationData.hash);
    componentDetails.setComponentIdentifier(componentEvaluationData.componentIdentifier);
    componentDetails.setMatchState(componentEvaluationData.matchState);
    componentDetails.setDeclaredLicenses(componentEvaluationData.declaredLicenses);
    componentDetails.setObservedLicenses(componentEvaluationData.observedLicenses);
    componentDetails.setSecurityVulnerabilities(componentEvaluationData.securityVulnerabilities);
    componentDetails.setRelativePopularity(componentEvaluationData.relativePopularity);
    componentDetails.setComponentCategories(componentEvaluationData.componentCategories);
    componentDetails.setHygieneRating(componentEvaluationData.hygieneRating);
    componentDetails.setIntegrityRating(componentEvaluationData.integrityRating);
    componentDetails.setEndOfLife(componentEvaluationData.endOfLife);
    componentDetails.setDerivedFromAiModel(componentEvaluationData.derivedFromAiModel);
    componentDetails.setAiModelContentTypes(componentEvaluationData.aiModelContentTypes);
    return componentDetails;
  }
}
