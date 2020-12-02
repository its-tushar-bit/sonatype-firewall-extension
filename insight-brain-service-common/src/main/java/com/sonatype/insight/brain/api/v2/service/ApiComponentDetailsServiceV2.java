/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsRequestDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsResultDTOV2;

import com.google.common.annotations.VisibleForTesting;

public interface ApiComponentDetailsServiceV2
{
  ApiComponentDetailsResultDTOV2 getComponentDetails(ApiComponentDetailsRequestDTOV2 componentDetailsRequest);

  @VisibleForTesting
  void setChunkSize(int chunkSize);

  List<ComponentEvaluationDataList.ComponentEvaluationData> getComponentDetailsListFromHds(
      ApiComponentDetailsRequestDTOV2 componentDetailsRequestDTO,
      String purpose);
}
