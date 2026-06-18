/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-policy-violation DTO carried inside a {@code FirewallPolicyAlertEvent} and the corresponding payload.
 *
 * @since 1.205.0
 */
public class FirewallPolicyAlertViolationDTO
{
  public String policyId;

  public String policyName;

  public int threatLevel;

  public String policyViolationId;

  public List<FirewallPolicyAlertComponentDTO> componentFacts = new ArrayList<>();
}
