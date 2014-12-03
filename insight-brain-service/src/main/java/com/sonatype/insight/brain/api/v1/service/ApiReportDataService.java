/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v1.service;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v1.dto.ApiMavenCoordinatesDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiReportComponentDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiReportDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 *
 * @deprecated since 1.13.0, use {@link ApiReportDataServiceV2}
 *
 * @since 1.9.1
 */
@Named
public class ApiReportDataService
{
  private final ApiReportDataServiceV2 reportDataService;

  @Inject
  public ApiReportDataService(ApiReportDataServiceV2 reportDataService) {
    this.reportDataService = reportDataService;
  }

  public ApiReportDataDTO getData(String applicationPublicId, String scanId) throws IOException {
    ApiReportDataDTOV2 reportDataV2 = reportDataService.getData(applicationPublicId, scanId);
    ApiReportDataDTO reportData = null;
    if (reportDataV2 != null) {
      reportData = new ApiReportDataDTO();
      for (ApiReportComponentDTOV2 reportComponentV2 : reportDataV2.components) {
        reportData.components.add(convert(reportComponentV2));
      }
    }
    return reportData;
  }

  private ApiReportComponentDTO convert(ApiReportComponentDTOV2 reportComponentV2) {
    ApiReportComponentDTO reportComponent = new ApiReportComponentDTO();
    reportComponent.hash = reportComponentV2.hash;
    reportComponent.matchState = reportComponentV2.matchState;
    reportComponent.proprietary = reportComponentV2.proprietary;
    reportComponent.pathnames = reportComponentV2.pathnames;
    reportComponent.licenseData = reportComponentV2.licenseData;
    reportComponent.securityData = reportComponentV2.securityData;
    if (reportComponentV2.componentIdentifier != null
        && ComponentIdentifier.FORMAT_MAVEN.equals(reportComponentV2.componentIdentifier.getFormat())) {
      reportComponent.mavenCoordinates = new ApiMavenCoordinatesDTO();
      reportComponent.mavenCoordinates.groupId = reportComponentV2.componentIdentifier.getCoordinates().get(
          ComponentIdentifier.MAVEN_GROUP_ID);
      reportComponent.mavenCoordinates.artifactId = reportComponentV2.componentIdentifier.getCoordinates().get(
          ComponentIdentifier.MAVEN_ARTIFACT_ID);
      reportComponent.mavenCoordinates.version = reportComponentV2.componentIdentifier.getCoordinates().get(
          ComponentIdentifier.VERSION);
    }
    return reportComponent;
  }
}
