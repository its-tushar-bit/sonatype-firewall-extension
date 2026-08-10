/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentPolicyViolationsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.report.ReportPdfEntity;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentLicense;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentPolicyViolation;
import com.sonatype.insight.brain.report.pdf.PdfData.PdfComponent.PdfComponentSecurityIssue;
import com.sonatype.insight.brain.report.pdf.PdfGenerator.Context;
import com.sonatype.insight.brain.report.pdf.PdfGenerator.WordBreaker;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.components.BomPageMetadataDTO;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vandeseer.easytable.structure.Row;
import org.vandeseer.easytable.structure.Table;
import org.vandeseer.easytable.structure.cell.TextCell;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.text.FontDescriptor;
import rst.pdfbox.layout.text.annotations.AnnotatedStyledText;
import rst.pdfbox.layout.text.annotations.Annotations.HyperlinkAnnotation;
import rst.pdfbox.layout.util.Pair;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class PdfGeneratorTest
    extends AbstractComponentH2Test
{
  private static final List<String> SBOM_SPECIFIC_CONTENT =
      List.of("Bill of Materials Report", "SBOM Metadata", "Spec Version: specVersion", "File Format: fileFormat");

  private static final List<String> SBOM_SPECIFIC_CONTENT_SPDX =
      List.of("Person: person", "Organization: organization", "Specification: SPDX");

  private static final List<String> SBOM_SPECIFIC_CONTENT_CDX =
      List.of("Author: author", "Manufacturer: manufacturer", "Supplier: supplier", "Specification: CycloneDx",
          "Original File: originalFile");

  private static final List<String> LIFECYCLE_SPECIFIC_CONTENT =
      List.of("Sonatype Application Composition Report", "IQ Server release:", "Commit:",
          "b141d3806df77594e4744bcf24b4cc95", "LEGACY VIOLATIONS");

  public static final String SCAN_ID = "scanId";

  public static final String APP_PUBLIC_ID = "appPublicId";

  public static final String APP_NAME = "appName";

  @Inject
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  private InsightWork insightWork;

  @Inject
  private ReportService reportService;

  private Application application;

  @BeforeEach
  public void setup() {
    setBaseUrl("http://localhost:8070/");
    application = tempEntity.newApplicationWithParent(APP_PUBLIC_ID, APP_NAME);
  }

  @Test
  public void testGenerate_SBOM_CDX() throws Exception {
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, SCAN_ID);
    File reportFile = insightWork.getReportFile(application.getId(), SCAN_ID);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorTest/report", tempDir), reportFile);

    ApiReportPolicyDataDTOV2 policyViolationsData =
        apiReportDataServiceV2.getPolicyViolationsData(application.getPublicId(), SCAN_ID, false);
    policyViolationsData.commitHash = "b141d3806df77594e4744bcf24b4cc95";
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);

    BomPageMetadataDTO bomPageMetadataDTO = new BomPageMetadataDTO(
        List.of("author"),
        List.of("manufacturer"),
        List.of("supplier"),
        Collections.emptyList(),
        Collections.emptyList(),
        SbomSpecification.CYCLONEDX.toString(),
        "specVersion",
        "fileFormat",
        new Date(),
        SCAN_ID,
        false,
        "originalFile",
        true);
    PdfData pdfData = PdfData.createSbomPdfData(
        null,
        "98",
        policyViolationsData,
        apiReportDataServiceV2.getRawData(application.getPublicId(), SCAN_ID),
        bomPageMetadataDTO);

    String pdfContent = generatePdfAndStripText(reportPdf, pdfData, Context.SBOM, 1, 14);
    assertThat(reportPdf.exists()).isTrue();

    assertSbomPdfCommonSections(pdfContent);
    assertThat(pdfContent)
        .contains(SBOM_SPECIFIC_CONTENT)
        .contains(SBOM_SPECIFIC_CONTENT_CDX)
        .doesNotContain(LIFECYCLE_SPECIFIC_CONTENT);
  }

  @Test
  public void testGenerate_SBOM_SPDX() throws Exception {
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, SCAN_ID);
    File reportFile = insightWork.getReportFile(application.getId(), SCAN_ID);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorTest/report", tempDir), reportFile);

    ApiReportPolicyDataDTOV2 policyViolationsData =
        apiReportDataServiceV2.getPolicyViolationsData(application.getPublicId(), SCAN_ID, false);
    policyViolationsData.commitHash = "b141d3806df77594e4744bcf24b4cc95";
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);

    BomPageMetadataDTO bomPageMetadataDTO = new BomPageMetadataDTO(
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        List.of("person"),
        List.of("organization"),
        SbomSpecification.SPDX.toString(),
        "specVersion",
        "fileFormat",
        new Date(),
        SCAN_ID,
        false,
        null,
        true);
    PdfData pdfData = PdfData.createSbomPdfData(
        null,
        "98",
        policyViolationsData,
        apiReportDataServiceV2.getRawData(application.getPublicId(), SCAN_ID),
        bomPageMetadataDTO);

    String pdfContent = generatePdfAndStripText(reportPdf, pdfData, Context.SBOM, 1, 14);
    assertThat(reportPdf.exists()).isTrue();

    assertSbomPdfCommonSections(pdfContent);
    assertThat(pdfContent)
        .contains(SBOM_SPECIFIC_CONTENT)
        .contains(SBOM_SPECIFIC_CONTENT_SPDX)
        .doesNotContain(LIFECYCLE_SPECIFIC_CONTENT);
  }

  private static void assertSbomPdfCommonSections(final String pdfContent) {
    List<String> headerSection = List.of("Policy Violations for appName Build Report", "Created on:",
        "Analyzed on:");
    List<String> pageCount = List.of("Page 1 of 14", "Page 2 of 14", "Page 3 of 14", "Page 4 of 14",
        "Page 5 of 14", "Page 6 of 14", "Page 7 of 14", "Page 8 of 14", "Page 9 of 14", "Page 10 of 14",
        "Page 11 of 14", "Page 12 of 14", "Page 13 of 14", "Page 14 of 14");
    List<String> violationsSection = List.of("26 43 8 77 VIOLATIONS",
        "Affecting 26 components",
        "THREAT POLICY NAME POLICY TYPE WAIVED COMPONENT",
        "10 Security-Critical Security No apache-collections : commons-collections : 3.1",
        "10 Security-Critical Security No com.fasterxml.jackson.core : jackson-databind : 2.0.4",
        "9 Security-High Security No apache-taglibs : standard : 1.1.2",
        "9 Security-High Security No axis : axis : 1.2",
        "7 Security-Medium Security No axis : axis : 1.2",
        "7 Security-Medium Security No axis : axis : 1.2",
        "3 Security-Low Security No commons-fileupload : commons-fileupload : 1.2.2",
        "3 Security-Low Security No org.springframework : spring-core : 3.2.4.RELEASE",
        "2 Component-Unknown Other No RegexMatch.dll",
        "2 Component-Unknown Other No WebGoat-6.0.1.war",
        "1 Architecture-Cleanup Other No junit : junit : 4.8.1",
        "1 Architecture-Quality Quality No aopalliance : aopalliance : 1.0");
    List<String> vulnerabilitiesSection = List.of("Vulnerabilities for appName Build Report",
        "VULNERABILITY CVSS SCORE COMPONENT", "CVE-2016-1000027 9.8 org.springframework : spring-web : 3.2.4.RELEASE",
        "CVE-2016-1000031 9.8 commons-fileupload : commons-fileupload : 1.2.2",
        "CVE-2017-7525 9.8 com.fasterxml.jackson.core : jackson-databind : 2.0.4",
        "sonatype-2019-0115 9.8 org.webjars jquery 1.10.2",
        "sonatype-2015-0327 3.7 org.springframework : spring-core : 3.2.4.RELEASE",
        "sonatype-2019-0341 3.7 org.springframework.security : spring-security-web : 3.2.4.RELEASE",
        "sonatype-2014-0058 3.6 org.webjars angularjs 1.2.16");
    List<String> licensesSection = List.of("Licenses for appName Build Report",
        "9 GPL-2.0 org.owasp.webgoat webgoat-container 7.0",
        "6 BSD-3-Clause, Non-Standard, BSD hsqldb : hsqldb : 1.8.0.7",
        "5 Apache-1.1 commons-digester : commons-digester : 1.4.1",
        "5 Apache-1.1 commons-discovery : commons-discovery : 0.2",
        "5 CDDL-1.0 javax.servlet : jstl : 1.2",
        "5 CPL-1.0 junit : junit : 4.8.1");
    List<String> bomSection = List.of("Component BOM for appName Build Report",
        "62 COMPONENTS", "95% of all components identified", "COMPONENT", "aopalliance : aopalliance : 1.0",
        "apache-collections : commons-collections : 3.1", "apache-taglibs : standard : 1.1.2",
        "axis : axis : 1.2", "axis : axis-ant : 1.2", "axis : axis-jaxrpc : 1.2",
        "javax.mail : mail : 1.4.2", "javax.mail : mailapi : 1.4.2", "pywebtest-gitbook 0.0.1",
        "RegexMatch.dll");

    assertThat(pdfContent)
        .contains(headerSection)
        .contains(pageCount)
        .contains(violationsSection)
        .contains(vulnerabilitiesSection)
        .contains(licensesSection)
        .contains(bomSection);
  }

  @Test
  public void testGenerate_LIFECYCLE() throws Exception {
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, SCAN_ID);
    File reportFile = insightWork.getReportFile(application.getId(), SCAN_ID);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorTest/report", tempDir), reportFile);

    ApiReportPolicyDataDTOV2 policyViolationsData =
        apiReportDataServiceV2.getPolicyViolationsData(application.getPublicId(), SCAN_ID, false);
    policyViolationsData.commitHash = "b141d3806df77594e4744bcf24b4cc95";
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyViolationsData,
        apiReportDataServiceV2.getRawData(application.getPublicId(), SCAN_ID));

    String pdfContent = generatePdfAndStripText(reportPdf, pdfData, Context.LIFECYCLE, 1, 14);
    assertThat(reportPdf.exists()).isTrue();

    assertCommonSections(pdfContent);
    assertThat(pdfContent)
        .contains(LIFECYCLE_SPECIFIC_CONTENT)
        .doesNotContain(SBOM_SPECIFIC_CONTENT)
        .doesNotContain(SBOM_SPECIFIC_CONTENT_CDX)
        .doesNotContain(SBOM_SPECIFIC_CONTENT_SPDX);
  }

  private void assertCommonSections(String pdfContent) {
    DateTimeFormatter timeZoneFormatter = DateTimeFormatter.ofPattern("Z");
    String timeZoneOffset = ZonedDateTime.now().format(timeZoneFormatter);
    List<String> headerSection = List.of("Policy Violations for appName Build Report", "Created on:",
        "Analyzed on:", "UTC " + timeZoneOffset);
    List<String> pageCount = List.of("Page 1 of 14", "Page 2 of 14", "Page 3 of 14", "Page 4 of 14",
        "Page 5 of 14", "Page 6 of 14", "Page 7 of 14", "Page 8 of 14", "Page 9 of 14", "Page 10 of 14",
        "Page 11 of 14", "Page 12 of 14", "Page 13 of 14", "Page 14 of 14");
    List<String> violationsSection = List.of("26 43 8 77 VIOLATIONS",
        "Affecting 26 components",
        "THREAT POLICY NAME POLICY TYPE WAIVED COMPONENT",
        "10 Security-Critical Security No apache-collections : commons-collections : 3.1",
        "10 Security-Critical Security No com.fasterxml.jackson.core : jackson-databind : 2.0.4",
        "9 Security-High Security No apache-taglibs : standard : 1.1.2",
        "9 Security-High Security No axis : axis : 1.2",
        "7 Security-Medium Security No axis : axis : 1.2",
        "7 Security-Medium Security No axis : axis : 1.2",
        "3 Security-Low Security No commons-fileupload : commons-fileupload : 1.2.2",
        "3 Security-Low Security No org.springframework : spring-core : 3.2.4.RELEASE",
        "2 Component-Unknown Other No RegexMatch.dll",
        "2 Component-Unknown Other No WebGoat-6.0.1.war",
        "1 Architecture-Cleanup Other No junit : junit : 4.8.1",
        "1 Architecture-Quality Quality No aopalliance : aopalliance : 1.0");
    List<String> vulnerabilitiesSection = List.of("Vulnerabilities for appName Build Report",
        "VULNERABILITY CVSS SCORE COMPONENT", "CVE-2016-1000027 9.8 org.springframework : spring-web : 3.2.4.RELEASE",
        "CVE-2016-1000031 9.8 commons-fileupload : commons-fileupload : 1.2.2",
        "CVE-2017-7525 9.8 com.fasterxml.jackson.core : jackson-databind : 2.0.4",
        "sonatype-2019-0115 9.8 org.webjars jquery 1.10.2",
        "sonatype-2015-0327 3.7 org.springframework : spring-core : 3.2.4.RELEASE",
        "sonatype-2019-0341 3.7 org.springframework.security : spring-security-web : 3.2.4.RELEASE",
        "sonatype-2014-0058 3.6 org.webjars angularjs 1.2.16");
    List<String> licensesSection = List.of("Licenses for appName Build Report",
        "9 GPL-2.0 GPL-2.0 Not Supported org.owasp.webgoat webgoat-container 7.0",
        "5 Apache-1.1 Apache-1.1 No Sources ecs : ecs : 1.4.2",
        "5 Apache-1.1 Not Declared Apache-1.1 commons-digester : commons-digester : 1.4.1",
        "2 LGPL-3.0 or MIT LGPL-3.0 or MIT Not Supported jquery-form 4.2.0",
        "0 Apache-2.0 Apache-2.0 Apache-2.0 axis : axis-jaxrpc : 1.2",
        "0 Apache-2.0 Apache-2.0 Apache-2.0 commons-fileupload : commons-fileupload : 1.2.2");
    List<String> bomSection = List.of("Component BOM for appName Build Report",
        "62 COMPONENTS", "95% of all components identified", "COMPONENT", "aopalliance : aopalliance : 1.0",
        "apache-collections : commons-collections : 3.1", "apache-taglibs : standard : 1.1.2",
        "axis : axis : 1.2", "axis : axis-ant : 1.2", "axis : axis-jaxrpc : 1.2",
        "javax.mail : mail : 1.4.2", "javax.mail : mailapi : 1.4.2", "pywebtest-gitbook 0.0.1",
        "RegexMatch.dll");

    assertThat(pdfContent)
        .contains(headerSection)
        .contains(pageCount)
        .contains(violationsSection)
        .contains(vulnerabilitiesSection)
        .contains(licensesSection)
        .contains(bomSection);
  }

  @Test
  public void testGenerate_EmptyData() throws Exception {
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        generateMinimalPolicyData(),
        new ApiReportRawDataDTOV2());
    generateReportFile();
    PdfGenerator.generate(reportPdf, pdfData);

    assertThat(reportPdf.exists()).isTrue();
  }

  @Test
  public void testGenerate_PolicyDataWithEmptyComponent() throws Exception {
    ApiReportPolicyDataDTOV2 policyData = generateMinimalPolicyData();
    policyData.components.add(newApiReportComponentPolicyViolationsDTOV2());
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());
    generateReportFile();
    PdfGenerator.generate(reportPdf, pdfData);

    assertThat(reportPdf.exists()).isTrue();
  }

  @Test
  public void testGenerate_PolicyDataWithEmptyViolation() throws Exception {
    ApiReportPolicyDataDTOV2 policyData = generateMinimalPolicyData();
    ApiReportComponentPolicyViolationsDTOV2 component = newApiReportComponentPolicyViolationsDTOV2();
    component.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component);
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());
    generateReportFile();
    PdfGenerator.generate(reportPdf, pdfData);

    assertThat(reportPdf.exists()).isTrue();
  }

  @Test
  public void testGenerate_RawDataWithEmptyComponent() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    rawData.components.add(newApiReportComponentDTOV2());
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        generateMinimalPolicyData(),
        rawData);
    generateReportFile();
    PdfGenerator.generate(reportPdf, pdfData);

    assertThat(reportPdf.exists()).isTrue();
  }

  @Test
  public void testGenerate_RawDataWithEmptySecurityData() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = newApiReportComponentDTOV2();
    component.securityData = new ApiSecurityDataDTO();
    rawData.components.add(component);
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        generateMinimalPolicyData(),
        rawData);
    generateReportFile();
    PdfGenerator.generate(reportPdf, pdfData);

    assertThat(reportPdf.exists()).isTrue();
  }

  @Test
  public void testGenerate_RawDataWithEmptySecurityIssue() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = newApiReportComponentDTOV2();
    component.securityData = new ApiSecurityDataDTO();
    component.securityData.securityIssues.add(new ApiSecurityIssueDTO());
    rawData.components.add(component);
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        generateMinimalPolicyData(),
        rawData);
    generateReportFile();
    PdfGenerator.generate(reportPdf, pdfData);

    assertThat(reportPdf.exists()).isTrue();
  }

  @Test
  public void testGenerate_RawDataWithEmptyLicenseData() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = newApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    rawData.components.add(component);
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        generateMinimalPolicyData(),
        rawData);
    generateReportFile();
    PdfGenerator.generate(reportPdf, pdfData);

    assertThat(reportPdf.exists()).isTrue();
  }

  @Test
  public void testGenerate_NonLatinCharactersSupportedByFonts() throws Exception {
    ApiReportComponentPolicyViolationsDTOV2 component = newApiReportComponentPolicyViolationsDTOV2();
    component.displayName = "星義义こ여�-test.zip";
    component.violations.add(new ApiReportPolicyViolationDTOV2());
    ApiReportPolicyDataDTOV2 policyData = generateMinimalPolicyData();
    policyData.application.name += "星義义こ여";
    policyData.components.add(component);
    generateReportFile();
    ReportPdfEntity reportPdf = reportService.getPdfReport(application.getId(), SCAN_ID);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        generateMinimalPolicyData(),
        new ApiReportRawDataDTOV2());

    PdfGenerator.generate(reportPdf, pdfData);

    assertThat(reportPdf.exists()).isTrue();
  }

  @Test
  public void testGetTitle_EmptyPolicyData() {
    String sectionName = "sectionName";
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        new ApiReportPolicyDataDTOV2(),
        new ApiReportRawDataDTOV2());
    assertThat(new PdfGenerator(null, pdfData).getTitle(sectionName)).isEqualTo(sectionName);
  }

  @Test
  public void testGetTitle_EmptyApplication() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).getTitle(sectionName)).isEqualTo(sectionName);
  }

  @Test
  public void testGetTitle_OnlyApplicationName() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();
    policyData.application.name = "someApplication";
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).getTitle(sectionName))
        .isEqualTo(sectionName + " for " + policyData.application.name);
  }

  @Test
  public void testGetTitle_OnlyReportTitle() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.reportTitle = "reportTitle";
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).getTitle(sectionName))
        .isEqualTo(sectionName + " for " + policyData.reportTitle);
  }

  @Test
  public void testGetTitle_ApplicationNameAndReportTitle() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();
    policyData.application.name = "someApplication";
    policyData.reportTitle = "reportTitle";
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).getTitle(sectionName))
        .isEqualTo(sectionName + " for " + policyData.application.name + " " + policyData.reportTitle);
  }

  @Test
  public void testCountPolicyViolations_EmptyPolicyData() {
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        new ApiReportPolicyDataDTOV2(),
        new ApiReportRawDataDTOV2());
    assertThat(new PdfGenerator(null, pdfData).countPolicyViolations(0, 10)).isZero();
  }

  @Test
  public void testCountPolicyViolations_EmptyComponent() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.components.add(newApiReportComponentPolicyViolationsDTOV2());
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).countPolicyViolations(0, 10)).isZero();
  }

  @Test
  public void testCountPolicyViolations_OneComponent() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = newApiReportComponentPolicyViolationsDTOV2();
    component.violations.add(new ApiReportPolicyViolationDTOV2());
    ApiReportPolicyViolationDTOV2 violation1 = new ApiReportPolicyViolationDTOV2();
    violation1.policyThreatLevel = 1;
    component.violations.add(violation1);
    ApiReportPolicyViolationDTOV2 violation2 = new ApiReportPolicyViolationDTOV2();
    violation2.policyThreatLevel = 2;
    component.violations.add(violation2);
    ApiReportPolicyViolationDTOV2 violation3 = new ApiReportPolicyViolationDTOV2();
    violation3.policyThreatLevel = 2;
    component.violations.add(violation3);
    ApiReportPolicyViolationDTOV2 violation4 = new ApiReportPolicyViolationDTOV2();
    violation4.policyThreatLevel = 10;
    component.violations.add(violation4);
    policyData.components.add(component);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    assertThat(pdfGenerator.countPolicyViolations(0, 0)).isEqualTo(1);
    assertThat(pdfGenerator.countPolicyViolations(2, 2)).isEqualTo(2);
    assertThat(pdfGenerator.countPolicyViolations(0, 2)).isEqualTo(4);
    assertThat(pdfGenerator.countPolicyViolations(10, 10)).isEqualTo(1);
    assertThat(pdfGenerator.countPolicyViolations(0, 10)).isEqualTo(5);
  }

  @Test
  public void testCountPolicyViolations_MultipleComponents() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.components.add(newApiReportComponentPolicyViolationsDTOV2());
    ApiReportComponentPolicyViolationsDTOV2 component1 = newApiReportComponentPolicyViolationsDTOV2();
    component1.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component1);
    ApiReportComponentPolicyViolationsDTOV2 component2 = newApiReportComponentPolicyViolationsDTOV2();
    component2.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component2);
    policyData.components.add(newApiReportComponentPolicyViolationsDTOV2());
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).countPolicyViolations(0, 10)).isEqualTo(2);
  }

  @Test
  public void testCountPolicyViolations_ExcludesLegacyStatus() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = newApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    component.violations.add(violation);
    ApiReportPolicyViolationDTOV2 waivedViolation = new ApiReportPolicyViolationDTOV2();
    waivedViolation.waived = true;
    component.violations.add(waivedViolation);
    ApiReportPolicyViolationDTOV2 legacyViolation = new ApiReportPolicyViolationDTOV2();
    legacyViolation.legacyViolation = true;
    component.violations.add(legacyViolation);
    policyData.components.add(component);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).countPolicyViolations(0, 10)).isEqualTo(2);
  }

  @Test
  public void testCountAffectedComponents() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component1 = newApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyThreatLevel = 2;
    component1.violations.add(violation);
    ApiReportComponentPolicyViolationsDTOV2 component2 = newApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation2 = new ApiReportPolicyViolationDTOV2();
    violation2.policyThreatLevel = 10;
    component2.violations.add(violation2);
    policyData.components.add(component1);
    policyData.components.add(component2);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).countAffectedComponents()).isEqualTo(2);
  }

  @Test
  public void testCountAffectedComponents_ExcludesComponentsWithOnlyLowThreatOrLegacyViolations() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = newApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyThreatLevel = 1;
    component.violations.add(violation);
    ApiReportPolicyViolationDTOV2 waivedViolation = new ApiReportPolicyViolationDTOV2();
    waivedViolation.policyThreatLevel = 9;
    waivedViolation.waived = true;
    component.violations.add(waivedViolation);
    ApiReportPolicyViolationDTOV2 legacyViolation = new ApiReportPolicyViolationDTOV2();
    legacyViolation.policyThreatLevel = 9;
    legacyViolation.legacyViolation = true;
    component.violations.add(legacyViolation);
    policyData.components.add(component);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    assertThat(new PdfGenerator(null, pdfData).countAffectedComponents()).isEqualTo(1);
  }

  @Test
  public void testGetLicensesString_Empty() throws Exception {
    assertThat(getLicenseText(Collections.emptyList())).isEmpty();
  }

  @Test
  public void testGetLicensesString_OneLicense() throws Exception {
    ApiLicenseDTO license = new ApiLicenseDTO();
    license.licenseName = "license";

    assertThat(getLicenseText(Collections.singletonList(license))).isEqualTo("license");
  }

  @Test
  public void testGetLicensesString_MultipleLicenses() throws Exception {
    ApiLicenseDTO license1 = new ApiLicenseDTO();
    license1.licenseName = "license1";
    ApiLicenseDTO license2 = new ApiLicenseDTO();
    license2.licenseName = "license2";
    ApiLicenseDTO license3 = new ApiLicenseDTO();
    license3.licenseName = "license3";

    assertThat(getLicenseText(Arrays.asList(license1, license2, license3))).isEqualTo("license1, license2, license3");
  }

  @Test
  public void testCreatePolicyViolationsTable_RowOrdering() throws Exception {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component111 = generateComponentWithPolicyThreat(2, "policy1", "comp111");
    ApiReportComponentPolicyViolationsDTOV2 component112 = generateComponentWithPolicyThreat(2, "policy1", "comp112");
    ApiReportComponentPolicyViolationsDTOV2 component121 = generateComponentWithPolicyThreat(2, "policy2", "comp121");
    ApiReportComponentPolicyViolationsDTOV2 component122 = generateComponentWithPolicyThreat(2, "policy2", "comp122");
    ApiReportComponentPolicyViolationsDTOV2 component211 = generateComponentWithPolicyThreat(1, "policy1", "comp211");
    ApiReportComponentPolicyViolationsDTOV2 component212 = generateComponentWithPolicyThreat(1, "policy1", "comp212");
    ApiReportComponentPolicyViolationsDTOV2 component221 = generateComponentWithPolicyThreat(1, "policy2", "comp221");
    ApiReportComponentPolicyViolationsDTOV2 component222 = generateComponentWithPolicyThreat(1, "policy2", "comp222");
    policyData.components.add(component112);
    policyData.components.add(component121);
    policyData.components.add(component122);
    policyData.components.add(component211);
    policyData.components.add(component212);
    policyData.components.add(component221);
    policyData.components.add(component222);
    policyData.components.add(component111);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table policyViolationsTable = pdfGenerator.createPolicyViolationsTable(PdfGenerator.newPage());

    assertThat(policyViolationsTable).isNotNull();
    List<Row> rows = policyViolationsTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(5)).getText())
        .containsExactly("comp111", "comp112", "comp121", "comp122", "comp211", "comp212", "comp221", "comp222");
  }

  @Test
  public void testCreatePolicyViolationsTableData_ExcludesLegacyStatus() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = newApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyName = "policyName1";
    component.violations.add(violation);
    ApiReportPolicyViolationDTOV2 waivedViolation = new ApiReportPolicyViolationDTOV2();
    waivedViolation.policyName = "policyName2";
    waivedViolation.waived = true;
    component.violations.add(waivedViolation);
    ApiReportPolicyViolationDTOV2 legacyViolation = new ApiReportPolicyViolationDTOV2();
    legacyViolation.policyName = "policyName3";
    legacyViolation.legacyViolation = true;
    component.violations.add(legacyViolation);
    policyData.components.add(component);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);

    assertThat(pdfGenerator.createPolicyViolationsTableData()).extracting(row -> row.policyName)
        .containsExactly(violation.policyName, waivedViolation.policyName);
  }

  @Test
  public void testCreatePolicyViolationsTableData_IncludesWaivedStatus() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = newApiReportComponentPolicyViolationsDTOV2();

    ApiReportPolicyViolationDTOV2 activeViolation = new ApiReportPolicyViolationDTOV2();
    activeViolation.policyName = "policyName1";
    activeViolation.waived = false;
    activeViolation.legacyViolation = false;
    component.violations.add(activeViolation);

    ApiReportPolicyViolationDTOV2 waivedViolation = new ApiReportPolicyViolationDTOV2();
    waivedViolation.policyName = "policyName2";
    waivedViolation.waived = true;
    waivedViolation.legacyViolation = false;
    component.violations.add(waivedViolation);

    policyData.components.add(component);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        policyData,
        new ApiReportRawDataDTOV2());

    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    List<PolicyViolationsTableRow> tableData = pdfGenerator.createPolicyViolationsTableData();

    assertThat(tableData).hasSize(2);
    assertThat(tableData.get(0).waived).isFalse();
    assertThat(tableData.get(1).waived).isTrue();
  }

  @Test
  public void testCreateSecurityIssuesTable_RowOrdering() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component111 = generateComponentWithSecurityIssue("securityIssue1", 2, "component111");
    ApiReportComponentDTOV2 component112 = generateComponentWithSecurityIssue("securityIssue1", 2, "component112");
    ApiReportComponentDTOV2 component121 = generateComponentWithSecurityIssue("securityIssue2", 2, "component121");
    ApiReportComponentDTOV2 component122 = generateComponentWithSecurityIssue("securityIssue2", 2, "component122");
    ApiReportComponentDTOV2 component211 = generateComponentWithSecurityIssue("securityIssue1", 1, "component211");
    ApiReportComponentDTOV2 component212 = generateComponentWithSecurityIssue("securityIssue1", 1, "component212");
    ApiReportComponentDTOV2 component221 = generateComponentWithSecurityIssue("securityIssue2", 1, "component221");
    ApiReportComponentDTOV2 component222 = generateComponentWithSecurityIssue("securityIssue2", 1, "component222");
    rawData.components.add(component112);
    rawData.components.add(component121);
    rawData.components.add(component122);
    rawData.components.add(component211);
    rawData.components.add(component212);
    rawData.components.add(component221);
    rawData.components.add(component222);
    rawData.components.add(component111);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        new ApiReportPolicyDataDTOV2(),
        rawData);
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table securityIssuesTable = pdfGenerator.createSecurityIssuesTable(PdfGenerator.newPage());

    assertThat(securityIssuesTable).isNotNull();
    List<Row> rows = securityIssuesTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(2)).getText())
        .containsExactly("component111", "component112", "component121", "component122", "component211", "component212",
            "component221", "component222");
  }

  @Test
  public void testCreateSecurityIssuesTable_withBaseUrlForVulnerabilityIdCell() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component111 = generateComponentWithSecurityIssue("securityIssue1", 2, "component111");
    ApiReportComponentDTOV2 component112 = generateComponentWithSecurityIssue("securityIssue1", 2, "component112");
    ApiReportComponentDTOV2 component121 = generateComponentWithSecurityIssue("securityIssue2", 2, "component121");
    rawData.components.add(component112);
    rawData.components.add(component121);
    rawData.components.add(component111);
    PdfData pdfData = PdfData.createPdfData(
        "https://somebaseurl.com/",
        "98",
        new ApiReportPolicyDataDTOV2(),
        rawData);
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table securityIssuesTable = pdfGenerator.createSecurityIssuesTable(PdfGenerator.newPage());

    assertThat(securityIssuesTable).isNotNull();
    List<Row> rows = securityIssuesTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(2)).getText())
        .containsExactly("component111", "component112", "component121");
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((ParagraphCell) row.getCells().get(0)).getParagraph()
        .getWrappedParagraph()
        .iterator()
        .next()
        .getText())
        .containsExactly("securityIssue1", "securityIssue1", "securityIssue2");
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((AnnotatedStyledText) ((ParagraphCell) row
        .getCells()
        .get(0)).getParagraph().getWrappedParagraph().iterator().next()).getAnnotationsOfType(
            HyperlinkAnnotation.class).iterator().next().getHyperlinkURI())
        .containsExactly("https://somebaseurl.com/ui/links/vln/securityIssue1",
            "https://somebaseurl.com/ui/links/vln/securityIssue1",
            "https://somebaseurl.com/ui/links/vln/securityIssue2");
  }

  @Test
  public void testCreateSecurityIssuesTable_withAnalysisStateInfo() throws Exception {
    BomPageMetadataDTO bomPageMetadataDTO = new BomPageMetadataDTO(
        List.of("author"),
        List.of("manufacturer"),
        List.of("supplier"),
        Collections.emptyList(),
        Collections.emptyList(),
        SbomSpecification.CYCLONEDX.toString(),
        "specVersion",
        "fileFormat",
        new Date(),
        "scanId",
        false,
        null,
        true);
    PdfData pdfData = mockPdfDataForSbomManager(bomPageMetadataDTO);
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData, Context.SBOM);
    pdfGenerator.initFontStyles(new PDDocument());

    Table securityIssuesTable = pdfGenerator.createSecurityIssuesTable(PdfGenerator.newPage());

    assertThat(securityIssuesTable).isNotNull();
    List<Row> rows = securityIssuesTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(2)).getText())
        .containsExactly("component 0", "component 1", "component 0", "component 1");
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((ParagraphCell) row.getCells().get(0)).getParagraph()
        .getWrappedParagraph()
        .iterator()
        .next()
        .getText())
        .containsExactly("reference0", "reference0", "reference1", "reference1");
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(3)).getText())
        .containsExactly("analysisState", "analysisState", "analysisState", "analysisState");
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((AnnotatedStyledText) ((ParagraphCell) row
        .getCells()
        .get(0)).getParagraph().getWrappedParagraph().iterator().next()).getAnnotationsOfType(
            HyperlinkAnnotation.class).iterator().next().getHyperlinkURI())
        .containsExactly("https://somebaseurl.com/ui/links/vln/reference0",
            "https://somebaseurl.com/ui/links/vln/reference0",
            "https://somebaseurl.com/ui/links/vln/reference1",
            "https://somebaseurl.com/ui/links/vln/reference1");
  }

  @Test
  public void testCreateLicensesTable_RowOrdering() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 c1 = generateComponentWithLicenses("v1", "v1", "v1", 9, "v1");

    ApiReportComponentDTOV2 c2 = generateComponentWithLicenses("v2", "v1", "v1", 1, "v1");
    ApiReportComponentDTOV2 c3 = generateComponentWithLicenses("v1", "v2", "v1", 1, "v1");
    ApiReportComponentDTOV2 c4 = generateComponentWithLicenses("v1", "v1", "v2", 6, "v1");
    ApiReportComponentDTOV2 c5 = generateComponentWithLicenses("v1", "v1", "v1", 9, "v2");

    ApiReportComponentDTOV2 c6 = generateComponentWithLicenses("v2", "v2", "v1", 9, "v1");
    ApiReportComponentDTOV2 c7 = generateComponentWithLicenses("v1", "v2", "v2", 9, "v1");
    ApiReportComponentDTOV2 c8 = generateComponentWithLicenses("v1", "v1", "v2", 9, "v2");
    ApiReportComponentDTOV2 c9 = generateComponentWithLicenses("v2", "v1", "v1", 6, "v2");
    ApiReportComponentDTOV2 c10 = generateComponentWithLicenses("v1", "v2", "v1", 1, "v2");
    ApiReportComponentDTOV2 c11 = generateComponentWithLicenses("v2", "v1", "v2", 4, "v1");

    ApiReportComponentDTOV2 c12 = generateComponentWithLicenses("v1", "v2", "v2", 4, "v2");
    ApiReportComponentDTOV2 c13 = generateComponentWithLicenses("v2", "v1", "v2", 1, "v2");
    ApiReportComponentDTOV2 c14 = generateComponentWithLicenses("v2", "v2", "v1", 6, "v2");
    ApiReportComponentDTOV2 c15 = generateComponentWithLicenses("v2", "v2", "v2", 9, "v1");
    ApiReportComponentDTOV2 c16 = generateComponentWithLicenses("v2", "v2", "v2", 1, "v2");

    ApiReportComponentDTOV2 c17 = generateComponentWithLicenses(null, null, null, null, "unknownComponent");

    rawData.components.addAll(
        Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17));
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        new ApiReportPolicyDataDTOV2(),
        rawData);
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table licensesTable = pdfGenerator.createLicensesTable(PdfGenerator.newPage());

    assertThat(licensesTable).isNotNull();
    List<Row> rows = licensesTable.getRows();
    assertThat(rows)
        .hasSize(rawData.components.size())
        .extracting(r -> r.getCells().stream().map(c -> {
          if (c instanceof ParagraphCell) {
            return ((ParagraphCell) c).getParagraph().getWrappedParagraph().iterator().next().getText();
          }
          if (c instanceof TextCell) {
            return ((TextCell) c).getText();
          }
          return null;
        }).collect(Collectors.joining(",")))
        .containsExactly(
            "THREAT,EFFECTIVE,DECLARED,OBSERVED,COMPONENT",
            ",9,v1,v1,v1,v1",
            ",9,v1,v1,v1,v2",
            ",9,v1,v1,v2,v2",
            ",9,v1,v2,v2,v1",
            ",9,v2,v2,v1,v1",
            ",9,v2,v2,v2,v1",
            ",6,v1,v1,v2,v1",
            ",6,v2,v1,v1,v2",
            ",6,v2,v2,v1,v2",
            ",4,v1,v2,v2,v2",
            ",4,v2,v1,v2,v1",
            ",1,v1,v2,v1,v1",
            ",1,v1,v2,v1,v2",
            ",1,v2,v1,v1,v1",
            ",1,v2,v1,v2,v2",
            ",1,v2,v2,v2,v2");
  }

  @Test
  public void testCreateBomTable_RowOrdering() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component1 = generateComponent("component1");
    ApiReportComponentDTOV2 component2 = generateComponent("component2");
    rawData.components.add(component2);
    rawData.components.add(component1);
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        new ApiReportPolicyDataDTOV2(),
        rawData);
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table bomTable = pdfGenerator.createBomTable(PdfGenerator.newPage());

    assertThat(bomTable).isNotNull();
    List<Row> rows = bomTable.getRows();
    assertThat(rows).hasSize(3)
        .extracting(row -> ((TextCell) row.getCells().get(0)).getText())
        .containsExactly("COMPONENT", "component1", "component2");
  }

  @Test
  public void testMediaBoxSize() {
    assertThat(PdfGenerator.MEDIA_BOX_SIZE.getWidth())
        .isEqualTo(Math.min(PDRectangle.A4.getWidth(), PDRectangle.LETTER.getWidth()));
    assertThat(PdfGenerator.MEDIA_BOX_SIZE.getHeight())
        .isEqualTo(Math.min(PDRectangle.A4.getHeight(), PDRectangle.LETTER.getHeight()));
  }

  @Test
  public void testCropBoxSize() {
    assertThat(PdfGenerator.CROP_BOX_SIZE.getWidth())
        .isEqualTo(PdfGenerator.MEDIA_BOX_SIZE.getWidth() - PdfGenerator.USER_SPACE_UNITS_PER_INCH);
    assertThat(PdfGenerator.CROP_BOX_SIZE.getHeight())
        .isEqualTo(PdfGenerator.MEDIA_BOX_SIZE.getHeight() - PdfGenerator.USER_SPACE_UNITS_PER_INCH);
  }

  @Test
  public void testBreakWordHard_IncludesAllLetters() throws Exception {
    WordBreaker wordBreaker = new WordBreaker();
    PDFont openSansRegular = PdfGeneratorUtils.loadPDType0Font(new PDDocument(), "OpenSans-Regular.ttf");
    FontDescriptor fontDescriptor = new FontDescriptor(openSansRegular, 8);

    Pair<String> result = wordBreaker.breakWordHard("sonatype-2015-0002", fontDescriptor, 92.65513f);

    assertThat(result).isEqualTo(new Pair<>("sonatype-2015-0002", ""));
  }

  @Test
  public void testBreakWordHard_IncludesNoLetters() throws Exception {
    WordBreaker wordBreaker = new WordBreaker();
    PDFont openSansRegular = PdfGeneratorUtils.loadPDType0Font(new PDDocument(), "OpenSans-Regular.ttf");
    FontDescriptor fontDescriptor = new FontDescriptor(openSansRegular, 8);

    Pair<String> result = wordBreaker.breakWordHard("sonatype-2015-0002", fontDescriptor, 0f);

    assertThat(result).isEqualTo(new Pair<>("", "sonatype-2015-0002"));
  }

  private ApiReportComponentPolicyViolationsDTOV2 generateComponentWithPolicyThreat(
      int threatLevel,
      String policyName,
      String componentName)
  {
    ApiReportComponentPolicyViolationsDTOV2 component = newApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyThreatLevel = threatLevel;
    violation.policyName = policyName;
    component.violations.add(violation);
    component.displayName = componentName;
    return component;
  }

  private ApiReportComponentDTOV2 generateComponentWithSecurityIssue(
      String reference,
      float severity,
      String componentName)
  {
    ApiReportComponentDTOV2 component = newApiReportComponentDTOV2();
    component.displayName = componentName;
    component.securityData = new ApiSecurityDataDTO();
    ApiSecurityIssueDTO securityIssue = new ApiSecurityIssueDTO();
    securityIssue.reference = reference;
    securityIssue.severity = severity;
    component.securityData.securityIssues.add(securityIssue);
    return component;
  }

  private ApiReportComponentDTOV2 generateComponentWithLicenses(
      String effectiveLicense,
      String declaredLicense,
      String observedLicense,
      Integer threatLevel,
      String componentName)
  {
    ApiReportComponentDTOV2 component = newApiReportComponentDTOV2();
    component.displayName = componentName;
    component.licenseData = new ApiLicenseDataDTOV2();
    if (StringUtils.isNotEmpty(effectiveLicense)) {
      ApiLicenseDTO license = new ApiLicenseDTO();
      license.licenseName = effectiveLicense;
      component.licenseData.effectiveLicenses.add(license);
    }
    if (StringUtils.isNotEmpty(declaredLicense)) {
      ApiLicenseDTO license = new ApiLicenseDTO();
      license.licenseName = declaredLicense;
      component.licenseData.declaredLicenses.add(license);
    }
    if (StringUtils.isNotEmpty(observedLicense)) {
      ApiLicenseDTO license = new ApiLicenseDTO();
      license.licenseName = observedLicense;
      component.licenseData.observedLicenses.add(license);
    }
    if (threatLevel != null) {
      ApiLicenseThreatDTOV2 licenseThreat = new ApiLicenseThreatDTOV2();
      licenseThreat.licenseThreatGroupLevel = threatLevel;
      component.licenseData.effectiveLicenseThreats.add(licenseThreat);
    }
    return component;
  }

  private ApiReportComponentDTOV2 generateComponent(String componentName) {
    ApiReportComponentDTOV2 component = newApiReportComponentDTOV2();
    component.displayName = componentName;
    return component;
  }

  private void generateReportFile() {
    File reportFile =
        insightWork.getReportFile(application.getId(), SCAN_ID);
    reportFile.getParentFile().mkdirs();
  }

  private ApiReportPolicyDataDTOV2 generateMinimalPolicyData() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();
    policyData.application.id = "appId";
    policyData.application.publicId = "appPublicId";
    policyData.application.name = "appName";
    policyData.reportTime = new Date();
    policyData.reportTitle = "Build Report";
    return policyData;
  }

  private String getLicenseText(List<ApiLicenseDTO> licenses) throws Exception {
    PdfData pdfData = PdfData.createPdfData(
        null,
        "98",
        new ApiReportPolicyDataDTOV2(),
        new ApiReportRawDataDTOV2());
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());
    ParagraphCell paragraphCell =
        (ParagraphCell) pdfGenerator.buildLicensesCell(
            licenses.stream().map(license -> license.licenseName).collect(Collectors.joining(", ")), false).build();
    paragraphCell.setWidth(1000);
    StringBuilder stringBuilder = new StringBuilder();
    Paragraph paragraph = paragraphCell.getParagraph().getWrappedParagraph();
    paragraph.iterator().forEachRemaining(textFragment -> stringBuilder.append(textFragment.getText()));
    return stringBuilder.toString();
  }

  private ApiReportComponentPolicyViolationsDTOV2 newApiReportComponentPolicyViolationsDTOV2() {
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
    component.hash = tempEntity.newRandomHash();
    return component;
  }

  private ApiReportComponentDTOV2 newApiReportComponentDTOV2() {
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.hash = tempEntity.newRandomHash();
    return component;
  }

  private String generatePdfAndStripText(
      final ReportPdfEntity reportPdf,
      final PdfData pdfData,
      final Context context,
      int startPage,
      int endPage) throws IOException
  {
    try (PDDocument pdDocument = new PDDocument()) {
      PdfGenerator pdfGenerator = new PdfGenerator(reportPdf, pdfData, context);
      pdfGenerator.doGenerate(pdDocument);
      PDFTextStripper textStripper = new PDFTextStripper();
      textStripper.setStartPage(startPage);
      textStripper.setEndPage(endPage);
      textStripper.setAddMoreFormatting(false);
      String pdfContent = textStripper.getText(pdfGenerator.getPdf());

      return pdfContent;
    }
  }

  private PdfData mockPdfDataForSbomManager(BomPageMetadataDTO bomPageMetadataDTO) {
    PdfData pdfData = new PdfData();
    pdfData.baseUrl = "https://somebaseurl.com/";
    pdfData.title = "Policy Violations for appName Build Report";
    pdfData.sbomMetadata = bomPageMetadataDTO;
    pdfData.createdDate = new Date();
    pdfData.analyzedDate = new Date();
    pdfData.productVersion = "productVersion";
    List<PdfComponent> components = Arrays.asList(
        new PdfComponent(),
        new PdfComponent());
    for (PdfComponent component : components) {
      component.displayName = "component " + components.indexOf(component);
      component.matchState = "matchState";
      component.policyViolations = Arrays.asList(
          new PdfComponentPolicyViolation(),
          new PdfComponentPolicyViolation());
      for (PdfComponentPolicyViolation violation : component.policyViolations) {
        violation.policyThreatLevel = 1;
        violation.policyName = "policyName" + component.policyViolations.indexOf(violation);
        violation.policyThreatCategory = "policyThreatCategory";
        violation.legacyViolation = true;
        violation.waived = true;
      }
      component.securityIssues = Arrays.asList(
          new PdfComponentSecurityIssue(),
          new PdfComponentSecurityIssue());
      for (PdfComponentSecurityIssue issue : component.securityIssues) {
        issue.reference = "reference" + component.securityIssues.indexOf(issue);
        issue.severity = 1.0f;
        issue.analysisState = "analysisState";
      }
      component.effectiveLicenses = Arrays.asList(
          new PdfComponentLicense(),
          new PdfComponentLicense());
      for (PdfComponentLicense license : component.effectiveLicenses) {
        license.name = "name";
      }

    }
    pdfData.components = components;

    return pdfData;
  }
}
