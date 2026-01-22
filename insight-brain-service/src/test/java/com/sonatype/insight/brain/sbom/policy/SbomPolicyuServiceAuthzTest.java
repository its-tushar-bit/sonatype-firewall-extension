/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import java.io.IOException;
import jakarta.inject.Inject;

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
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), "12345678",
            "componentRef", "fileCoordinateId1", null, new ReportEntry("report", 12345L, new byte[20]), null))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_Unauthenticated() throws Exception {
    sbomPolicyService.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), "12345678",
        "componentRef", "fileCoordinateId1", null, new ReportEntry("report", 12345L, new byte[20]), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_Unauthorized() throws Exception {
    login();
    sbomPolicyService.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), "12345678",
        "componentRef","fileCoordinateId1", null, new ReportEntry("report", 12345L, new byte[20]), null);
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), "12345678",
            "componentRef","fileCoordinateId1", null, null, null))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_Unauthenticated() throws IOException {
    sbomPolicyService.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), "12345678",
        "componentRef","fileCoordinateId1", null, null, null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_Unauthorized() throws IOException {
    login();
    sbomPolicyService.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), "12345678",
        "componentRef","fileCoordinateId1", null, null, null);
  }
}
