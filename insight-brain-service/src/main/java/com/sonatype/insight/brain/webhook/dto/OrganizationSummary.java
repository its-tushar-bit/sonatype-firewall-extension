/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.webhook.dto;

import com.sonatype.insight.brain.model.Organization;

public class OrganizationSummary
{
  public final String id;

  public final String name;

  public final String parentOrgId;

  public OrganizationSummary(final Organization organization) {
    this.id = organization.getId();
    this.name = organization.getName();
    this.parentOrgId = organization.getParentOrganizationId();
  }
}
