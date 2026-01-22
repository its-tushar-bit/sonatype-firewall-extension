/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class PdfGeneratorServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private PdfGeneratorService pdfGeneratorService;

  @Test(expected = UnauthenticatedException.class)
  public void testPrintReport_Unauthenticated() throws Exception {
    pdfGeneratorService.printReport(app.getPublicId(), "scanId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testPrintReport_Unauthorized() throws Exception {
    login();
    pdfGeneratorService.printReport(app.getPublicId(), "scanId");
  }

  @Test
  public void testPrintReport_Authorized() {
    grantReadPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        pdfGeneratorService.printReport(app.getPublicId(), "scanId")
    ).withMessage("Could not find a report with ID scanId");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPrintSbomReport_withAppId_Unauthenticated() throws Exception {
    pdfGeneratorService.printSbomReport(app.getPublicId(), "sbomVersion");
  }

  @Test(expected = UnauthorizedException.class)
  public void testPrintSbomReport_withAppId_Unauthorized() throws Exception {
    login();
    pdfGeneratorService.printSbomReport(app.getPublicId(), "sbomVersion");
  }

  @Test
  public void testPrintSbomReport_withAppId_Authorized() {
    grantReadPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        pdfGeneratorService.printSbomReport(app.getPublicId(), "sbomVersion")
    ).withMessage("SBOM version 'sbomVersion' not found for application '" + app.getPublicId() + "'.");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPrintSbomReport_withApp_Unauthenticated() throws Exception {
    pdfGeneratorService.printSbomReport(app, "sbomVersion");
  }

  @Test(expected = UnauthorizedException.class)
  public void testPrintSbomReport_withApp_Unauthorized() throws Exception {
    login();
    pdfGeneratorService.printSbomReport(app, "sbomVersion");
  }

  @Test
  public void testPrintSbomReport_withApp_Authorized() {
    grantReadPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() ->
        pdfGeneratorService.printSbomReport(app, "sbomVersion")
    ).withMessage("SBOM version 'sbomVersion' not found for application '" + app.getPublicId() + "'.");
  }
}
