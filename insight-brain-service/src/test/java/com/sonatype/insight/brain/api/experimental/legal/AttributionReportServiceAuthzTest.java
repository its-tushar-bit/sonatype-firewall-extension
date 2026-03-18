/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

public class AttributionReportServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  private static final String ATTRIBUTION_REPORT_TEMPLATE_ID = "attributionReportTemplateId";

  @Inject
  private AttributionReportService attributionReportService;

  @Test(expected = UnauthenticatedException.class)
  public void testSaveAttributionReportTemplate_Unauthenticated() {
    attributionReportService.saveAttributionReportTemplate(null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testSaveAttributionReportTemplate_Unauthorized() {
    login();
    attributionReportService.saveAttributionReportTemplate(null);
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

  @Test(expected = UnauthenticatedException.class)
  public void testGetAttributionReportTemplateById_Unauthenticated() {
    attributionReportService.getAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAttributionReportTemplateById_Unauthorized() {
    login();
    attributionReportService.getAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID);
  }

  @Test
  public void testGetAttributionReportTemplateById_Authorized() {
    grantLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    attributionReportService.getAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAllAttributionReportTemplates_Unauthenticated() {
    attributionReportService.getAllAttributionReportTemplates();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAllAttributionReportTemplates_Unauthorized() {
    login();
    attributionReportService.getAllAttributionReportTemplates();
  }

  @Test
  public void testGetAllAttributionReportTemplates_Authorized() {
    grantLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    attributionReportService.getAllAttributionReportTemplates();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteAttributionReportTemplateById_Unauthenticated() {
    attributionReportService.deleteAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID);
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteAttributionReportTemplateById_Unauthorized() {
    login();
    attributionReportService.deleteAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID);
  }

  @Test
  public void testDeleteAttributionReportTemplateById_Authorized() {
    grantLegalReviewerPermission(ROOT_ORGANIZATION_ID);
    attributionReportService.deleteAttributionReportTemplateById(ATTRIBUTION_REPORT_TEMPLATE_ID);
  }
}
