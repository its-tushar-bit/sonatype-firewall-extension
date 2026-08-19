/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class AttributionReportServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  private static final String ATTRIBUTION_REPORT_TEMPLATE_ID = "attributionReportTemplateId";

  @Inject
  private AttributionReportService attributionReportService;

  @Test
  public void testSaveAttributionReportTemplate_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> attributionReportService.saveAttributionReportTemplate(null));
  }

  @Test
  public void testSaveAttributionReportTemplate_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> attributionReportService.saveAttributionReportTemplate(null));
  }

  @Test
  public void testSaveAttributionReportTemplate_Authorized() {
    grantLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    AttributionReportTemplateDTO reportTemplateDTO = new AttributionReportTemplateDTO();
    // Setters used to pass validations
    reportTemplateDTO.setTemplateName("Template Name");
    reportTemplateDTO.setDocumentTitle("Test Report");
    reportTemplateDTO.setHeader("Test header");
    reportTemplateDTO.setFooter("Test footer");
    reportTemplateDTO.setIncludeTableOfContents(true);
    reportTemplateDTO.setIncludeAppendix(true);
    reportTemplateDTO.setIncludeStandardLicenseTexts(true);

    attributionReportService.saveAttributionReportTemplate(reportTemplateDTO);
  }

  @Test
  public void testGetAttributionReportTemplateById_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> attributionReportService.getAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID));
  }

  @Test
  public void testGetAttributionReportTemplateById_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> attributionReportService.getAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID));
  }

  @Test
  public void testGetAttributionReportTemplateById_Authorized() {
    grantLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    attributionReportService.getAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID);
  }

  @Test
  public void testGetAllAttributionReportTemplates_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> attributionReportService.getAllAttributionReportTemplates());
  }

  @Test
  public void testGetAllAttributionReportTemplates_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> attributionReportService.getAllAttributionReportTemplates());
  }

  @Test
  public void testGetAllAttributionReportTemplates_Authorized() {
    grantLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    attributionReportService.getAllAttributionReportTemplates();
  }

  @Test
  public void testDeleteAttributionReportTemplateById_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> attributionReportService.deleteAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID));
  }

  @Test
  public void testDeleteAttributionReportTemplateById_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> attributionReportService.deleteAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID));
  }

  @Test
  public void testDeleteAttributionReportTemplateById_Authorized() {
    grantLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    attributionReportService.deleteAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID);
  }
}
