/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.policy;

import java.io.IOException;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class SbomPolicyuServiceAuthzTest
    extends AbstractComponentH2AuthzTest
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

  @Test
  public void testGetPolicyViolations_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> sbomPolicyService.getPolicyViolations(app.getId(), "12345678"));
  }

  @Test
  public void testGetPolicyViolations_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> sbomPolicyService.getPolicyViolations(app.getId(), "12345678"));
  }

  @Test
  public void testGetPolicyViolationsReportEntry_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolationsReportEntry(app.getId(), "12345678"))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test
  public void testGetPolicyViolationsReportEntry_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> sbomPolicyService.getPolicyViolationsReportEntry(app.getId(), "12345678"));
  }

  @Test
  public void testGetPolicyViolationsReportEntry_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> sbomPolicyService.getPolicyViolationsReportEntry(app.getId(), "12345678"));
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), "12345678",
            "componentRef", "fileCoordinateId1", null, new ReportEntry("report", 12345L, new byte[20]), null))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> sbomPolicyService.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), "12345678",
            "componentRef", "fileCoordinateId1", null, new ReportEntry("report", 12345L, new byte[20]), null));
  }

  @Test
  public void testGetPolicyViolationsJsonNodeByFileCoordinateIdOrHash_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> sbomPolicyService.getPolicyViolationsJsonNodeByComponentRefOrHash(app.getId(), "12345678",
            "componentRef", "fileCoordinateId1", null, new ReportEntry("report", 12345L, new byte[20]), null));
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_Authorized() {
    grantReadPermission(app.getId());

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> sbomPolicyService.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), "12345678",
            "componentRef", "fileCoordinateId1", null, null, null))
        .withMessage("Cannot find version 12345678 for application with ID " + app.getId() + ".");
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_Unauthenticated() throws IOException {
    assertThrows(UnauthenticatedException.class,
        () -> sbomPolicyService.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), "12345678",
            "componentRef", "fileCoordinateId1", null, null, null));
  }

  @Test
  public void testGetPolicyViolationsByFileCoordinateIdOrHash_Unauthorized() throws IOException {
    login();
    assertThrows(UnauthorizedException.class,
        () -> sbomPolicyService.getPolicyViolationsByFileCoordinateIdOrHash(app.getId(), "12345678",
            "componentRef", "fileCoordinateId1", null, null, null));
  }
}
