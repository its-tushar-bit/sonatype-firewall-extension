/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.policy.ConstraintFactDTO;

/**
 * Per-component DTO carried inside a {@link FirewallPolicyAlertViolationDTO}.
 *
 * @since 1.205.0
 */
public class FirewallPolicyAlertComponentDTO
{
  public String hash;

  public String displayName;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public List<String> pathNames = new ArrayList<>();

  public List<ConstraintFactDTO> constraintFacts = new ArrayList<>();
}
