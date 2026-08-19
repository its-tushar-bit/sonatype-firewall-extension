/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class PdfGeneratorServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private PdfGeneratorService pdfGeneratorService;

  @Test
  public void testPrintReport_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class, () -> pdfGeneratorService.printReport(app.getPublicId(), "scanId"));
  }

  @Test
  public void testPrintReport_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class, () -> pdfGeneratorService.printReport(app.getPublicId(), "scanId"));
  }

  @Test
  public void testPrintReport_Authorized() {
    grantReadPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> pdfGeneratorService.printReport(app.getPublicId(), "scanId"))
        .withMessage("Could not find a report with ID scanId");
  }

  @Test
  public void testPrintSbomReport_withAppId_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class,
        () -> pdfGeneratorService.printSbomReport(app.getPublicId(), "sbomVersion"));
  }

  @Test
  public void testPrintSbomReport_withAppId_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class,
        () -> pdfGeneratorService.printSbomReport(app.getPublicId(), "sbomVersion"));
  }

  @Test
  public void testPrintSbomReport_withAppId_Authorized() {
    grantReadPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> pdfGeneratorService.printSbomReport(app.getPublicId(), "sbomVersion"))
        .withMessage("SBOM version 'sbomVersion' not found for owner '" + app.getPublicId() + "'.");
  }

  @Test
  public void testPrintSbomReport_withApp_Unauthenticated() throws Exception {
    assertThrows(UnauthenticatedException.class, () -> pdfGeneratorService.printSbomReport(app, "sbomVersion"));
  }

  @Test
  public void testPrintSbomReport_withApp_Unauthorized() throws Exception {
    login();
    assertThrows(UnauthorizedException.class, () -> pdfGeneratorService.printSbomReport(app, "sbomVersion"));
  }

  @Test
  public void testPrintSbomReport_withApp_Authorized() {
    grantReadPermission(app.getId());
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> pdfGeneratorService.printSbomReport(app, "sbomVersion"))
        .withMessage("SBOM version 'sbomVersion' not found for owner '" + app.getPublicId() + "'.");
  }
}
