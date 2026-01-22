/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.legal.AttributionReportTemplateDTO;
import com.sonatype.insight.brain.model.legal.AttributionReportTemplate;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AttributionReportServiceTest
    extends AbstractComponentTest
{
  @Inject
  private AttributionReportService attributionReportService;

  @Inject
  private TestProductLicense testProductLicense;

  @Test
  public void testSave_validTemplate() {
    AttributionReportTemplateDTO reportTemplateDTO = new AttributionReportTemplateDTO();
    reportTemplateDTO.setTemplateName("Template Name");
    reportTemplateDTO.setDocumentTitle("Test Report");
    reportTemplateDTO.setHeader("Test header");
    reportTemplateDTO.setFooter("Test footer");
    reportTemplateDTO.setIncludeTableOfContents(true);
    reportTemplateDTO.setIncludeAppendix(true);
    reportTemplateDTO.setIncludeStandardLicenseTexts(true);
    AttributionReportTemplateDTO savedReport = attributionReportService
        .saveAttributionReportTemplate(reportTemplateDTO);

    Optional<AttributionReportTemplateDTO> retrievedReport = attributionReportService
        .getAttributionReportTemplateById(savedReport.getId());
    assertThat(retrievedReport).isPresent();

    assertThat(savedReport.getDocumentTitle()).isEqualTo(reportTemplateDTO.getDocumentTitle());
    assertThat(retrievedReport.get().getDocumentTitle()).isEqualTo(savedReport.getDocumentTitle());
    assertThat(retrievedReport).contains(savedReport);
  }

  @Test
  public void testGetAll_licensed() {
    AttributionReportTemplate reportTemplate1 = tempEntity
        .createNewAttributionReportTemplate("template one", "report 1");
    AttributionReportTemplate reportTemplate2 =
        tempEntity.createNewAttributionReportTemplate("template two", "report 2");
    List<AttributionReportTemplateDTO> allReports = attributionReportService
        .getAllAttributionReportTemplates();
    assertThat(allReports).hasSize(2);
    assertThat(allReports.stream().map(AttributionReportTemplateDTO::getDocumentTitle).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(reportTemplate1.getDocumentTitle(), reportTemplate2.getDocumentTitle());
    assertThat(allReports.stream().map(AttributionReportTemplateDTO::getTemplateName).collect(Collectors.toSet()))
        .containsExactlyInAnyOrder(reportTemplate1.getTemplateName(), reportTemplate2.getTemplateName());
  }

  @Test
  public void testSave_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> attributionReportService.saveAttributionReportTemplate(null));
  }

  @Test
  public void testGet_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> attributionReportService.getAllAttributionReportTemplates());
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> attributionReportService.getAttributionReportTemplateById(""));
  }

  @Test
  public void testDelete_unlicensed() {
    testProductLicense.setMissingFeatures(LicensedFeature.ADVANCED_LEGAL_PACK);
    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> attributionReportService.deleteAttributionReportTemplateById(""));
  }

  @Test
  public void testSave_updateExisting() {
    assertThat(attributionReportService.getAllAttributionReportTemplates()).isEmpty();
    AttributionReportTemplateDTO reportTemplateDTO = new AttributionReportTemplateDTO();
    reportTemplateDTO.setTemplateName("template name");
    reportTemplateDTO.setDocumentTitle("document title");

    AttributionReportTemplateDTO savedReport = attributionReportService
        .saveAttributionReportTemplate(reportTemplateDTO);
    savedReport.setTemplateName("Updated template name");
    savedReport.setDocumentTitle("Updated document title");
    savedReport.setHeader("Updated report header");
    savedReport.setFooter("Updated report footer");
    savedReport.setIncludeTableOfContents(true);
    savedReport.setIncludeAppendix(true);
    savedReport.setIncludeStandardLicenseTexts(true);
    savedReport.setIncludeSonatypeSpecialLicenses(true);

    attributionReportService.saveAttributionReportTemplate(savedReport);
    Optional<AttributionReportTemplateDTO> updatedReport = attributionReportService
        .getAttributionReportTemplateById(savedReport.getId());
    assertThat(updatedReport).isPresent();
    assertThat(savedReport.getTemplateName()).isEqualTo(updatedReport.get().getTemplateName());
    assertThat(savedReport.getDocumentTitle()).isEqualTo(updatedReport.get().getDocumentTitle());
    assertThat(savedReport.getHeader()).isEqualTo(updatedReport.get().getHeader());
    assertThat(savedReport.getFooter()).isEqualTo(updatedReport.get().getFooter());
    assertThat(savedReport.getId()).isEqualTo(updatedReport.get().getId());
    assertThat(savedReport.isIncludeAppendix()).isEqualTo(updatedReport.get().isIncludeAppendix());
    assertThat(savedReport.isIncludeTableOfContents()).isEqualTo(updatedReport.get().isIncludeTableOfContents());
    assertThat(savedReport.isIncludeSonatypeSpecialLicenses()).isEqualTo(updatedReport.get()
        .isIncludeSonatypeSpecialLicenses());
    assertThat(attributionReportService.getAllAttributionReportTemplates()).hasSize(1);
  }
}
