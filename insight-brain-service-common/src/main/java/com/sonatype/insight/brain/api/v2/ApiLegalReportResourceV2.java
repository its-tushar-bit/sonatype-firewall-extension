/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalApplicationReportDTO;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalComponentReportDTO;
import com.sonatype.insight.brain.model.OwnerType;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.ContainerRequest;

/**
 * Resource for API Legal Report
 */
public interface ApiLegalReportResourceV2
{
  ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(String applicationId);

  ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(String applicationId, String stageId);

  String getLicenseLegalMultiApplicationReportFromActiveUserFilter();

  String getLicenseLegalApplicationHTMLReport(String applicationId, String stageId);

  String getLicenseLegalCustomApplicationHTMLReport(String applicationId, String stageId, FormDataMultiPart formData);

  String getLicenseLegalCustomMultiApplicationHTMLReport(FormDataMultiPart formData);
  
  String getLicenseLegalMultiApplicationHTMLReport(
      ContainerRequest request);

  String getLicenseLegalCustomApplicationHTMLReport(
      String applicationId,
      String stageId,
      String templateId,
      ContainerRequest request);

  String getLicenseLegalCustomMultiApplicationHTMLReport(
      String templateId,
      ContainerRequest request);

  ApiLicenseLegalComponentReportDTO getLicenseLegalComponentReport(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      String identificationSource,
      String scanId) throws IOException;
}
