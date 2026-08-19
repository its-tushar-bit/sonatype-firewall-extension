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
import com.sonatype.insight.brain.webhook.dto.RepositoryManagerSummary;
import com.sonatype.insight.brain.webhook.dto.RepositorySummary;

public class OrganizationApplicationManagementEvent
    extends WebhookEvent
{
  public List<OrganizationSummary> organizations;

  public List<ApplicationSummary> applications;

  public List<RepositoryManagerSummary> repositoryManagers;

  public List<RepositorySummary> repositories;

  public OrganizationApplicationManagementEvent(
      final List<OrganizationSummary> organizations,
      final List<ApplicationSummary> applications,
      final List<RepositoryManagerSummary> repositoryManagers,
      final List<RepositorySummary> repositories)
  {
    this.organizations = organizations;
    this.applications = applications;
    this.repositoryManagers = repositoryManagers;
    this.repositories = repositories;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "organizations=" + organizations +
        ", applications=" + applications +
        ", repositoryManagers=" + repositoryManagers +
        ", repositories=" + repositories +
        '}';
  }
}
