/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class SbomPolicyServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SbomPolicyService service;

  private Application app;

  private Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
    app = tempEntity.newApplicationWithParent(org);
  }

  @Test
  public void testGetScanIdForPolicyViolation() {
    String sbomVersion = "sbomVersion1";
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        app.getId(),
        sbomVersion,
        "Active",
        "fileName",
        "spec",
        "specFormat",
        "specVersion");
    tempEntity.newThirdPartyScan(thirdPartyFile);
    String scanId = service.getScanIdForPolicyViolation(app.getId(), sbomVersion);
    assertThat(scanId).isEqualTo("scanId");
  }

  @Test
  public void testGetScanIdForPolicyViolation_AppIdAndSbomVersionNotFound() {
    String sbomVersion = "sbomVersion1";
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(),
        app.getId(),
        sbomVersion,
        "Active",
        "fileName",
        "spec",
        "specFormat",
        "specVersion");
    tempEntity.newThirdPartyScan(thirdPartyFile);
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        service.getScanIdForPolicyViolation(app.getId(), "sbomVersion2")
    ).withMessage("Cannot find version sbomVersion2 for application with ID " + app.getId() + ".");
  }
}
