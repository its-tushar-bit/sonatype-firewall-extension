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

/**
 * Resource for API License Legal
 */
public interface ApiLicenseLegalResourceV2
{
  ApiLicenseLegalApplicationReportDTO getLicenseLegalApplicationReport(String applicationId);

  String getLicenseLegalApplicationHTMLReport(String applicationId);

  ApiLicenseLegalComponentReportDTO getLicenseLegalComponentReport(
      OwnerType ownerType,
      String ownerId,
      ComponentIdentifier componentIdentifier,
      String packageUrl,
      String hash,
      String identificationSource,
      String scanId) throws IOException;
}
