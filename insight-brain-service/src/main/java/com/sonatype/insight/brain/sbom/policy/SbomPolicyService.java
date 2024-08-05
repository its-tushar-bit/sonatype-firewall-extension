/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
public class SbomPolicyService
{
  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  public SbomPolicyService(
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO
  )
  {
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
  }

  public String getScanIdForPolicyViolation(String applicationId, String sbomVersion) {
    ThirdPartySbomMetadata
        thirdPartySbomMetadata = thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, sbomVersion);
    if (thirdPartySbomMetadata == null ||
        thirdPartySbomMetadata.getStatus().replace("\n", "").equalsIgnoreCase("PENDING")) {
      throw new NotFoundException(
          String.format("Cannot find version %s for application with ID %s.", sbomVersion, applicationId));
    }
    ThirdPartyScan thirdPartyScan =
        thirdPartyScanDAO.getSingleByThirdPartyFileId(thirdPartySbomMetadata.getThirdPartyFileId());
    return thirdPartyScan.getScanId();
  }
}
