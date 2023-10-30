/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.webhook;

import java.util.List;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.webhook.dto.ApplicationSummary;
import com.sonatype.insight.brain.webhook.dto.OrganizationSummary;

public class OrganizationApplicationManagementEvent
    extends WebhookEvent
{
  public List<OrganizationSummary> organizations;

  public List<ApplicationSummary> applications;

  public OrganizationApplicationManagementEvent(
      final List<OrganizationSummary> organizations,
      final List<ApplicationSummary> applications)
  {
    this.organizations = organizations;
    this.applications = applications;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "organizations=" + organizations +
        ", applications=" + applications +
        '}';
  }
}
