/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import com.sonatype.insight.brain.model.Application;

public class ApplicationSummary
{
  public String id;

  public String publicId;

  public String name;

  public String organizationId;

  public ApplicationSummary() {
  }

  public ApplicationSummary(final Application application) {
    this.id = application.getId();
    this.publicId = application.getPublicId();
    this.name = application.getName();
    this.organizationId = application.getOrganizationId();
  }
}
