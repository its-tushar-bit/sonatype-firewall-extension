/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiWaiverOptionsDTO;
import com.sonatype.insight.brain.model.OwnerType;

/**
 * Resource for API Policy Waiver
 */
public interface ApiPolicyWaiverResource
{
  void addPolicyWaiverByPolicyViolationId(OwnerType ownerType,
                                          String ownerId,
                                          String policyViolationId,
                                          ApiWaiverOptionsDTO waiverOptionsDTO);

  void deletePolicyWaiver(OwnerType ownerType, String ownerId, String policyWaiverId);

  List<ApiPolicyWaiverDTO> getPolicyWaivers(OwnerType ownerType, String ownerId);

  void addWaiverToTransitivePolicyViolationsByAppScanComponent(
      OwnerType ownerType,
      String ownerId,
      String scanId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      ApiWaiverOptionsDTO apiWaiverOptionsDTO);

  void addWaiverToTransitivePolicyViolationsByOwnerStageComponent(
      OwnerType ownerType,
      String ownerId,
      String stageId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      ApiWaiverOptionsDTO apiWaiverOptionsDTO);
}
