/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** @since 1.205.0 */
public class FirewallPolicyAlertPayload
    extends WebhookPayload
{
  public RepositorySummary repository = new RepositorySummary();

  public List<FirewallPolicyAlertViolationDTO> policyAlerts = new ArrayList<>();

  public QuarantineStatus quarantineStatus = new QuarantineStatus();

  public static class RepositorySummary
  {
    public String id;

    public String publicId;

    public String format;
  }

  public static class QuarantineStatus
  {
    public boolean quarantined;

    public Date quarantineTime;
  }
}
