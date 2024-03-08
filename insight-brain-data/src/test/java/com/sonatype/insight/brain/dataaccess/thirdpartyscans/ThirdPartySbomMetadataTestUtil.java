/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Date;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

public class ThirdPartySbomMetadataTestUtil
{
  public static ThirdPartySbomMetadata createSbomMetadata(
      String status,
      String applicationId,
      String thirdPartyFileId)
  {
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata();
    thirdPartySbomMetadata.setCreatedAt(new Date());
    thirdPartySbomMetadata.setApplicationId(applicationId);
    thirdPartySbomMetadata.setThirdPartyFileId(thirdPartyFileId);
    thirdPartySbomMetadata.setSbomVersion(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setFilename(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setSerialNumber(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setSpec(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setSpecFormat(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setSpecVersion(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setStatus(status);
    thirdPartySbomMetadata.setMetadataJson(RandomStringUtils.random(1500));

    return thirdPartySbomMetadata;
  }
}
