/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for API Component Remediation
 */
public interface ApiComponentRemediationResource
{
  ApiComponentRemediationDTO getSuggestedRemediationForComponent(ApiComponentDTOV2 component,
                                                                 OwnerType ownerType,
                                                                 String ownerId,
                                                                 String stageId,
                                                                 String identificationSource,
                                                                 String scanId);
}
