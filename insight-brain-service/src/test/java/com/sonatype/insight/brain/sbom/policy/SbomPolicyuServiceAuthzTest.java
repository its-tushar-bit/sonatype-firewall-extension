/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import java.io.IOException;
import javax.inject.Inject;

import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class SbomPolicyuServiceAuthzTest extends AbstractServiceAuthzTest
{
  @Inject
  private SbomPolicyService sbomPolicyService;

  @Test
  public void testGetPolicyViolations_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolations(app.getId(), "12345678"))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolations_Unauthenticated() throws Exception {
    sbomPolicyService.getPolicyViolations(app.getId(), "12345678");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolations_Unauthorized() throws Exception {
    login();
    sbomPolicyService.getPolicyViolations(app.getId(), "12345678");
  }

  @Test
  public void testGetPolicyViolationsReportEntry_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolationsReportEntry(app.getId(), "12345678"))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolationsReportEntry_Unauthenticated() throws Exception {
    sbomPolicyService.getPolicyViolationsReportEntry(app.getId(), "12345678");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolationsReportEntry_Unauthorized() throws Exception {
    login();
    sbomPolicyService.getPolicyViolationsReportEntry(app.getId(), "12345678");
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateId_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolationsJsonNodeByFileCoordinateId(app.getId(), "12345678",
            "fileCoordinateId1", new ReportEntry("report", 12345L, new byte[20])))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolationsJsonNodeByFileCoordinateId_Unauthenticated() throws Exception {
    sbomPolicyService.getPolicyViolationsJsonNodeByFileCoordinateId(app.getId(), "12345678",
        "fileCoordinateId1", new ReportEntry("report", 12345L, new byte[20]));
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolationsJsonNodeByFileCoordinateId_Unauthorized() throws Exception {
    login();
    sbomPolicyService.getPolicyViolationsJsonNodeByFileCoordinateId(app.getId(), "12345678",
        "fileCoordinateId1", new ReportEntry("report", 12345L, new byte[20]));
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateId_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolationsByFileCoordinateId(app.getId(), "12345678",
            "fileCoordinateId1"))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolationsByFileCoordinateId_Unauthenticated() throws IOException {
    sbomPolicyService.getPolicyViolationsByFileCoordinateId(app.getId(), "12345678",
        "fileCoordinateId1");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolationsByFileCoordinateId_Unauthorized() throws IOException {
    login();
    sbomPolicyService.getPolicyViolationsByFileCoordinateId(app.getId(), "12345678",
        "fileCoordinateId1");
  }
}
