/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.webhook.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Payload for organization/application/repository management webhook events.
 * Null fields are excluded from JSON serialization to provide context-specific payloads:
 * - Firewall webhooks receive only repository-related fields
 * - Lifecycle webhooks receive only application-related fields
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationApplicationSummaryPayload
    extends WebhookPayload
{
  public List<OrganizationSummary> organizations;

  public List<ApplicationSummary> applications;

  public List<RepositoryManagerSummary> repositoryManagers;

  public List<RepositorySummary> repositories;
}
