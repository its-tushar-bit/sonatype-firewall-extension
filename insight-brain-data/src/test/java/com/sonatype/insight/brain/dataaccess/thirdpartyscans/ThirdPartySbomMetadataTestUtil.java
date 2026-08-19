/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;

import org.apache.commons.lang3.RandomStringUtils;

public class ThirdPartySbomMetadataTestUtil
{
  public static ThirdPartySbomMetadata createSbomMetadata(
      ThirdPartySbomMetadataStatus status,
      String applicationId,
      String thirdPartyFileId)
  {
    DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata();
    thirdPartySbomMetadata.setCreatedAt(new Date());
    thirdPartySbomMetadata.setApplicationId(applicationId);
    thirdPartySbomMetadata.setThirdPartyFileId(thirdPartyFileId);
    thirdPartySbomMetadata.setSbomVersion(dtFormatter.format(LocalDateTime.now()));
    thirdPartySbomMetadata.setFilename(RandomStringUtils.randomAscii(10));
    thirdPartySbomMetadata.setSerialNumber(RandomStringUtils.randomAscii(10));
    thirdPartySbomMetadata.setSpec(RandomStringUtils.randomAscii(10));
    thirdPartySbomMetadata.setSpecFormat(RandomStringUtils.randomAscii(10));
    thirdPartySbomMetadata.setSpecVersion(RandomStringUtils.randomAscii(10));
    thirdPartySbomMetadata.setStatus(status);
    thirdPartySbomMetadata.setMetadataJson(RandomStringUtils.randomAscii(1500));
    thirdPartySbomMetadata.setScanType("SBOM");
    thirdPartySbomMetadata.setIsValid(true);

    return thirdPartySbomMetadata;
  }
}
