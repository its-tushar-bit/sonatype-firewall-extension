/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import com.sonatype.clm.dto.model.organization.OrganizationSummaryList;

/**
 * Resource for integrations points to conform to Organization Summary.
 */
public interface OrganizationSummaryResource
{
  /**
   * Gets all organizations for which the current user has permissions required for the specified goal, sorted by
   * (case-insensitive) name.
   *
   * @param goal The goal for getting the list of organizations.
   */
  OrganizationSummaryList getOrganizations(final Goal goal);
}
