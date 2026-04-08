/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.webhook.dto;

import java.util.List;

public class OrganizationApplicationSummaryPayload
    extends WebhookPayload
{
  public List<OrganizationSummary> organizations;

  public List<ApplicationSummary> applications;
}
