/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.application.ApplicationSummaryList;

/**
 * Resource for integrations points to conform to Application Summary.
 */
public interface ApplicationSummaryResource
{
  /**
   * Gets all applications for which the current user has permissions required for the specified goal, sorted by
   * (case-insensitive) name.
   *
   * @param goal The goal for getting the list of applications. Defaults to READ permission for backward compatibility
   *             (Jenkins/Hudson plugin <= 2.12.1, Bamboo plugin <=1.0.0, Eclipse plugin <= 2.8.0, SonarQube plugin <=
   *             1.0.2, Nexus plugins <= 3.0.0).
   */
  ApplicationSummaryList getApplications(final Goal goal);

  /**
   * Verifies if the user can access the application identified by applicationPublicId for the specified goal.
   * If an application with the specified applicationPublicId already exists, then the method checks access for the
   * current user and the specified goal to that application.
   * If such an application does not exist and automatic application creation is enabled, then the method creates the
   * new application and returns true to indicate the application will now be available.
   *
   * @param applicationPublicId public shared id
   * @param goal                {@link Goal}
   * @param request             {@link HttpServletRequest}
   * @return true if false otherwise.
   */
  boolean verifyOrCreateApplication(final String applicationPublicId,
                                    final Goal goal,
                                    final String organizationId,
                                    final HttpServletRequest request);
}
