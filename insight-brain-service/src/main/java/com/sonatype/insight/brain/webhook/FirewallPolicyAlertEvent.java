/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.webhook.dto.FirewallPolicyAlertViolationDTO;

/** @since 1.205.0 */
public class FirewallPolicyAlertEvent
    extends WebhookEvent
{
  public String repositoryId;

  public String repositoryPublicId;

  public String repositoryFormat;

  public Date quarantineTime;

  public String targetId;

  public List<FirewallPolicyAlertViolationDTO> violations = new ArrayList<>();

  public FirewallPolicyAlertEvent(final String targetId) {
    this.targetId = targetId;
  }

  @Override
  public String toString() {
    return "FirewallPolicyAlertEvent{" +
        "targetId='" + targetId + '\'' +
        ", repositoryId='" + repositoryId + '\'' +
        ", repositoryPublicId='" + repositoryPublicId + '\'' +
        ", repositoryFormat='" + repositoryFormat + '\'' +
        ", quarantineTime=" + quarantineTime +
        ", violations=" + violations +
        '}';
  }
}
