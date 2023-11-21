/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.doc;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsRequestListDto;

import io.swagger.v3.oas.annotations.Operation;

public interface ApiComponentNearestFixedVersionsResourceDoc
{
  @Operation(hidden = true)
  List<ComponentNearestFixedVersions> getNearestFixedVersions(ApiComponentNearestFixedVersionsRequestListDto listDto);
}
