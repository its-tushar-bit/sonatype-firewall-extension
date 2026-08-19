/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import com.sonatype.insight.brain.model.Application;

/**
 * Container repository summary for Firewall container evaluation webhooks.
 * Semantically represents a Firewall repository in container evaluation context.
 *
 * @since 1.203.0
 */
public class ContainerRepositorySummary
{
  public String id;

  public String publicId;

  public String name;

  public String organizationId;

  public ContainerRepositorySummary() {
  }

  public ContainerRepositorySummary(final Application application) {
    this.id = application.getId();
    this.publicId = application.getPublicId();
    this.name = application.getName();
    this.organizationId = application.getOrganizationId();
  }

  public ContainerRepositorySummary(final ApplicationSummary applicationSummary) {
    this.id = applicationSummary.id;
    this.publicId = applicationSummary.publicId;
    this.name = applicationSummary.name;
    this.organizationId = applicationSummary.organizationId;
  }
}
