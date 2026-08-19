/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

public class ApiHostedRepositoryComponentDTO
{
  public String id;

  public String pathname;

  public String displayName;

  public String hash;

  public String matchStateId;

  public Long lastEvaluationTime;

  public boolean quarantined;

  public int violationCount;

  public int criticalViolationCount;

  public int severeViolationCount;

  public int moderateViolationCount;

  public int maxThreatLevel;

  public ComponentIdentifier componentIdentifier;

  public String scanId;

  public String applicationPublicId;

  public String stageTypeId;

  public Integer componentCount;
}
