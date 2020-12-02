/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.hds.HdsClient;

/**
 * @since 1.16.0
 */
@Named
@Singleton
public class DefaultApiComponentDetailsServiceV2 extends AbstractApiComponentDetailsServiceV2
{
  public static final String HDS_COMPONENT_DETAILS_PATH = "rest/component/details/{purpose: evaluation|integration}";

  private final ApiComponentDetailsAdapter componentDetailsAdapter;

  private final HdsClient client;

  @Inject
  public DefaultApiComponentDetailsServiceV2(ApiComponentDetailsAdapter componentDetailsAdapter, HdsClient client) {
    this.componentDetailsAdapter = componentDetailsAdapter;
    this.client = client;
  }

  @Override
  protected ApiComponentDetailsDTOV2 convertToDTO(ComponentEvaluationData componentDetailsFromHds) {
    return componentDetailsAdapter.convertToDTO(componentDetailsFromHds);
  }

  @Override
  protected ComponentEvaluationDataList post(ComponentEvaluationDataRequestList requestList, String purpose) {
    return client.post(ComponentEvaluationDataList.class, HDS_COMPONENT_DETAILS_PATH, requestList, purpose);
  }
}
