/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.io.IOException;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DevelopmentPrioritiesReportService
{
  private static final Logger log = LoggerFactory.getLogger(DevelopmentPrioritiesReportService.class);

  private static final String NOT_FOUND_ERROR_MESSAGE = "Could not find the requested report for prioritization.";

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  public DevelopmentPrioritiesReportService(
      final ApiReportDataServiceV2 apiReportDataServiceV2)
  {
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
  }

  @Authorize(permission = Permission.READ)
  public ApiReportRawDataDTOV2 getDependencyInformation(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String scanId)
  {
    try {
      return this.apiReportDataServiceV2.getDataForPrioritization(applicationPublicId, scanId);
    }
    catch (final IOException ioException) {
      log.warn("IOException fetching bom and dependencies data from report files ({}, {}): {}",
          applicationPublicId, scanId, ioException.getMessage());
      throw new NotFoundException(NOT_FOUND_ERROR_MESSAGE);
    }
  }

  @Authorize(permission = Permission.READ)
  public ApiReportRawDataDTOV2 getDependencyInformation(
      @AuthzContext(AuthzContext.Key.OWNER) final Owner owner,
      final String scanId)
  {
    try {
      return this.apiReportDataServiceV2.getDataForPrioritization(owner, scanId);
    }
    catch (final IOException ioException) {
      log.warn("IOException fetching bom and dependencies data from report files ({}, {}): {}",
          owner.getId(), scanId, ioException.getMessage());
      throw new NotFoundException(NOT_FOUND_ERROR_MESSAGE);
    }
  }
}
