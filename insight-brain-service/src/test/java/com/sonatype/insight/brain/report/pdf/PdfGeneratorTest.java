/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report.pdf;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseDataDTOV2;
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
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.vandeseer.easytable.structure.Row;
import org.vandeseer.easytable.structure.Table;
import org.vandeseer.easytable.structure.cell.TextCell;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell;
import rst.pdfbox.layout.elements.Paragraph;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfGeneratorTest
    extends AbstractComponentTest
{
  @Inject
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  private InsightWork insightWork;

  @Test
  public void testGenerate() throws Exception {
    Application app = tempEntity.newApplicationWithParent("appPublicId", "appName");
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    File reportFile = insightWork.getReportFile(app.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorTest/report", tempDir), reportFile);

    File pdfFile = PdfGenerator.getPdfFile(reportFile);

    PdfGenerator.generate(pdfFile, apiReportDataServiceV2.getPolicyViolationsData(app.getPublicId(), scanId),
        apiReportDataServiceV2.getRawData(app.getPublicId(), scanId));
    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_EmptyData() throws Exception {
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());

    PdfGenerator.generate(pdfFile, generateMinimalPolicyData(), new ApiReportRawDataDTOV2());

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_PolicyDataWithEmptyComponent() throws Exception {
    ApiReportPolicyDataDTOV2 policyData = generateMinimalPolicyData();
    policyData.components.add(new ApiReportComponentPolicyViolationsDTOV2());
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());

    PdfGenerator.generate(pdfFile, policyData, new ApiReportRawDataDTOV2());

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_PolicyDataWithEmptyViolation() throws Exception {
    ApiReportPolicyDataDTOV2 policyData = generateMinimalPolicyData();
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
    component.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component);
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());

    PdfGenerator.generate(pdfFile, policyData, new ApiReportRawDataDTOV2());

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_RawDataWithEmptyComponent() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    rawData.components.add(new ApiReportComponentDTOV2());
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());

    PdfGenerator.generate(pdfFile, generateMinimalPolicyData(), rawData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_RawDataWithEmptySecurityData() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.securityData = new ApiSecurityDataDTO();
    rawData.components.add(component);
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());

    PdfGenerator.generate(pdfFile, generateMinimalPolicyData(), rawData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_RawDataWithEmptySecurityIssue() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.securityData = new ApiSecurityDataDTO();
    component.securityData.securityIssues.add(new ApiSecurityIssueDTO());
    rawData.components.add(component);
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());

    PdfGenerator.generate(pdfFile, generateMinimalPolicyData(), rawData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_RawDataWithEmptyLicenseData() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    rawData.components.add(component);
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());

    PdfGenerator.generate(pdfFile, generateMinimalPolicyData(), rawData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGetTitle_EmptyPolicyData() {
    String sectionName = "sectionName";
    assertThat(
        new PdfGenerator(null, new ApiReportPolicyDataDTOV2(), new ApiReportRawDataDTOV2()).getTitle(sectionName))
        .isEqualTo(sectionName);
  }

  @Test
  public void testGetTitle_EmptyApplication() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();

    assertThat(new PdfGenerator(null, policyData, new ApiReportRawDataDTOV2()).getTitle(sectionName))
        .isEqualTo(sectionName);
  }

  @Test
  public void testGetTitle_OnlyApplicationName() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();
    policyData.application.name = "someApplication";

    assertThat(new PdfGenerator(null, policyData, new ApiReportRawDataDTOV2()).getTitle(sectionName))
        .isEqualTo(sectionName + " for " + policyData.application.name);
  }

  @Test
  public void testGetTitle_OnlyReportTitle() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.reportTitle = "reportTitle";

    assertThat(new PdfGenerator(null, policyData, new ApiReportRawDataDTOV2()).getTitle(sectionName))
        .isEqualTo(sectionName + " for " + policyData.reportTitle);
  }

  @Test
  public void testGetTitle_ApplicationNameAndReportTitle() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();
    policyData.application.name = "someApplication";
    policyData.reportTitle = "reportTitle";

    assertThat(new PdfGenerator(null, policyData, new ApiReportRawDataDTOV2()).getTitle(sectionName))
        .isEqualTo(sectionName + " for " + policyData.application.name + " " + policyData.reportTitle);
  }

  @Test
  public void testCountPolicyViolations_EmptyPolicyData() {
    assertThat(new PdfGenerator(null, new ApiReportPolicyDataDTOV2(), new ApiReportRawDataDTOV2())
        .countPolicyViolations(0, 10)).isZero();
  }

  @Test
  public void testCountPolicyViolations_EmptyComponent() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.components.add(new ApiReportComponentPolicyViolationsDTOV2());

    assertThat(new PdfGenerator(null, policyData, new ApiReportRawDataDTOV2()).countPolicyViolations(0, 10)).isZero();
  }

  @Test
  public void testCountPolicyViolations_OneComponent() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
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

    PdfGenerator pdfGenerator = new PdfGenerator(null, policyData, new ApiReportRawDataDTOV2());
    assertThat(pdfGenerator.countPolicyViolations(0, 0)).isEqualTo(1);
    assertThat(pdfGenerator.countPolicyViolations(2, 2)).isEqualTo(2);
    assertThat(pdfGenerator.countPolicyViolations(0, 2)).isEqualTo(4);
    assertThat(pdfGenerator.countPolicyViolations(10, 10)).isEqualTo(1);
    assertThat(pdfGenerator.countPolicyViolations(0, 10)).isEqualTo(5);
  }

  @Test
  public void testCountPolicyViolations_MultipleComponents() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.components.add(new ApiReportComponentPolicyViolationsDTOV2());
    ApiReportComponentPolicyViolationsDTOV2 component1 = new ApiReportComponentPolicyViolationsDTOV2();
    component1.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component1);
    ApiReportComponentPolicyViolationsDTOV2 component2 = new ApiReportComponentPolicyViolationsDTOV2();
    component2.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component2);
    policyData.components.add(new ApiReportComponentPolicyViolationsDTOV2());

    assertThat(new PdfGenerator(null, policyData, new ApiReportRawDataDTOV2()).countPolicyViolations(0, 10))
        .isEqualTo(2);
  }

  @Test
  public void testGetComponentName_Null() {
    assertThat(PdfGenerator.getComponentName(null, null)).isNull();
  }

  @Test
  public void testGetComponentName_EmptyComponentIdentifier() {
    assertThat(PdfGenerator.getComponentName(new ApiComponentIdentifierDTOV2(), null)).isNull();
  }

  @Test
  public void testGetComponentName_ComponentIdentifierWithFormat() {
    ApiComponentIdentifierDTOV2 componentIdentifier = new ApiComponentIdentifierDTOV2();
    componentIdentifier.setFormat("format");

    assertThat(PdfGenerator.getComponentName(componentIdentifier, null)).isNull();
  }

  @Test
  public void testGetComponentName_ComponentIdentifierWithFormatAndOneCoordinate() {
    ApiComponentIdentifierDTOV2 componentIdentifier = new ApiComponentIdentifierDTOV2();
    componentIdentifier.setFormat("format");
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("coordinateName", "coordinateValue");
    componentIdentifier.setCoordinates(coordinates);

    assertThat(PdfGenerator.getComponentName(componentIdentifier, null)).isEqualTo("coordinateValue");
  }

  @Test
  public void testGetComponentName_EmptyPathnames() {
    assertThat(PdfGenerator.getComponentName(null, Collections.emptyList())).isNull();
  }

  @Test
  public void testGetComponentName_PathnameWithOnePart() {
    String pathname = "part";
    assertThat(PdfGenerator.getComponentName(null, Collections.singletonList(pathname))).isEqualTo(pathname);
  }

  @Test
  public void testGetComponentName_PathnameWithMultipleParts() {
    String pathname = "part1/part2/part3";
    assertThat(PdfGenerator.getComponentName(null, Collections.singletonList(pathname)))
        .isEqualTo(pathname.split("/")[2]);
  }

  @Test
  public void testGetComponentName_MultiplePathnames() {
    List<String> pathnames = Arrays.asList("pathname1", "pathname2", "pathname3");
    assertThat(PdfGenerator.getComponentName(null, pathnames)).isEqualTo(pathnames.get(0));
  }

  @Test
  public void testGetComponentName_ComponentIdentifierAndPathname() {
    ApiComponentIdentifierDTOV2 componentIdentifier = new ApiComponentIdentifierDTOV2();
    componentIdentifier.setFormat("format");
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put("coordinateName", "coordinateValue");
    componentIdentifier.setCoordinates(coordinates);

    assertThat(PdfGenerator.getComponentName(componentIdentifier, Collections.singletonList("pathname")))
        .isEqualTo("coordinateValue");
  }

  @Test
  public void testGetLicensesString_Empty() {
    assertThat(getLicenseText(new ApiLicenseDataDTOV2())).isEmpty();
  }

  @Test
  public void testGetLicensesString_OneObservedLicense() {
    ApiLicenseDataDTOV2 licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO observedLicense = new ApiLicenseDTO();
    observedLicense.licenseName = "observedLicense";
    licenseData.observedLicenses.add(observedLicense);

    assertThat(getLicenseText(licenseData)).isEqualTo("observedLicense");
  }

  @Test
  public void testGetLicensesString_OneDeclaredLicense() {
    ApiLicenseDataDTOV2 licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO declaredLicense = new ApiLicenseDTO();
    declaredLicense.licenseName = "declaredLicense";
    licenseData.declaredLicenses.add(declaredLicense);

    assertThat(getLicenseText(licenseData)).isEqualTo("declaredLicense");
  }

  @Test
  public void testGetLicensesString_MultipleDeclaredLicenses() {
    ApiLicenseDataDTOV2 licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO declaredLicense1 = new ApiLicenseDTO();
    declaredLicense1.licenseName = "declaredLicense1";
    licenseData.declaredLicenses.add(declaredLicense1);
    ApiLicenseDTO declaredLicense2 = new ApiLicenseDTO();
    declaredLicense2.licenseName = "declaredLicense2";
    licenseData.declaredLicenses.add(declaredLicense2);
    ApiLicenseDTO declaredLicense3 = new ApiLicenseDTO();
    declaredLicense3.licenseName = "declaredLicense3";
    licenseData.declaredLicenses.add(declaredLicense3);

    assertThat(getLicenseText(licenseData))
        .isEqualTo("declaredLicense1, declaredLicense2, declaredLicense3");
  }

  @Test
  public void testGetLicensesString_MultipleObservedLicenses() {
    ApiLicenseDataDTOV2 licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO observedLicense1 = new ApiLicenseDTO();
    observedLicense1.licenseName = "observedLicense1";
    licenseData.observedLicenses.add(observedLicense1);
    ApiLicenseDTO observedLicense2 = new ApiLicenseDTO();
    observedLicense2.licenseName = "observedLicense2";
    licenseData.observedLicenses.add(observedLicense2);
    ApiLicenseDTO observedLicense3 = new ApiLicenseDTO();
    observedLicense3.licenseName = "observedLicense3";
    licenseData.observedLicenses.add(observedLicense3);

    assertThat(getLicenseText(licenseData)).isEqualTo("observedLicense1, observedLicense2, observedLicense3");
  }

  @Test
  public void testGetLicensesString_MultipleDeclaredAndObservedLicenses() {
    ApiLicenseDataDTOV2 licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO declaredLicense1 = new ApiLicenseDTO();
    declaredLicense1.licenseName = "declaredLicense1";
    licenseData.declaredLicenses.add(declaredLicense1);
    ApiLicenseDTO declaredLicense2 = new ApiLicenseDTO();
    declaredLicense2.licenseName = "declaredLicense2";
    licenseData.declaredLicenses.add(declaredLicense2);
    ApiLicenseDTO observedLicense1 = new ApiLicenseDTO();
    observedLicense1.licenseName = "observedLicense1";
    licenseData.observedLicenses.add(observedLicense1);
    ApiLicenseDTO observedLicense2 = new ApiLicenseDTO();
    observedLicense2.licenseName = "observedLicense2";
    licenseData.observedLicenses.add(observedLicense2);

    assertThat(getLicenseText(licenseData))
        .isEqualTo("declaredLicense1, declaredLicense2, observedLicense1, observedLicense2");
  }

  @Test
  public void testCreatePolicyViolationsTable_RowOrdering() {
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
    PdfGenerator pdfGenerator = new PdfGenerator(null, policyData, new ApiReportRawDataDTOV2());
    pdfGenerator.initFontStyles(new PDDocument());

    Table policyViolationsTable = pdfGenerator.createPolicyViolationsTable(mockPage());

    assertThat(policyViolationsTable).isNotNull();
    List<Row> rows = policyViolationsTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(4)).getText())
        .containsExactly("comp111", "comp112", "comp121", "comp122", "comp211", "comp212", "comp221", "comp222");
  }

  @Test
  public void testCreateSecurityIssuesTable_RowOrdering() {
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
    PdfGenerator pdfGenerator = new PdfGenerator(null, new ApiReportPolicyDataDTOV2(), rawData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table securityIssuesTable = pdfGenerator.createSecurityIssuesTable(mockPage());

    assertThat(securityIssuesTable).isNotNull();
    List<Row> rows = securityIssuesTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(2)).getText())
        .containsExactly("component111", "component112", "component121", "component122", "component211", "component212",
            "component221", "component222");
  }

  @Test
  public void testCreateLicensesTable_RowOrdering() {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component11 = generateComponentWithLicense("license1", "component11");
    ApiReportComponentDTOV2 component12 = generateComponentWithLicense("license1", "component12");
    ApiReportComponentDTOV2 component21 = generateComponentWithLicense("license2", "component21");
    ApiReportComponentDTOV2 component22 = generateComponentWithLicense("license2", "component22");
    rawData.components.add(component22);
    rawData.components.add(component21);
    rawData.components.add(component12);
    rawData.components.add(component11);
    PdfGenerator pdfGenerator = new PdfGenerator(null, new ApiReportPolicyDataDTOV2(), rawData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table licensesTable = pdfGenerator.createLicensesTable(mockPage());

    assertThat(licensesTable).isNotNull();
    List<Row> rows = licensesTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(1)).getText())
        .containsExactly("component11", "component12", "component21", "component22");
  }

  @Test
  public void testCreateBomTable_RowOrdering() {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component1 = generateComponent("component1");
    ApiReportComponentDTOV2 component2 = generateComponent("component2");
    rawData.components.add(component2);
    rawData.components.add(component1);
    PdfGenerator pdfGenerator = new PdfGenerator(null, new ApiReportPolicyDataDTOV2(), rawData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table bomTable = pdfGenerator.createBomTable(mockPage());

    assertThat(bomTable).isNotNull();
    List<Row> rows = bomTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(0)).getText())
        .containsExactly("component1", "component2");
  }

  @Test
  public void testPageSize() {
    assertThat(PdfGenerator.PAGE_SIZE.getWidth()).isEqualTo(
        Math.min(PDRectangle.A4.getWidth(), PDRectangle.LETTER.getWidth()) - PdfGenerator.SPACE_FOR_PRINTERS);
    assertThat(PdfGenerator.PAGE_SIZE.getHeight()).isEqualTo(
        Math.min(PDRectangle.A4.getHeight(), PDRectangle.LETTER.getHeight() - PdfGenerator.SPACE_FOR_PRINTERS));
  }

  private ApiReportComponentPolicyViolationsDTOV2 generateComponentWithPolicyThreat(
      int threatLevel,
      String policyName,
      String componentName)
  {
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyThreatLevel = threatLevel;
    violation.policyName = policyName;
    component.violations.add(violation);
    component.pathnames.add(componentName);
    return component;
  }

  private ApiReportComponentDTOV2 generateComponentWithSecurityIssue(
      String reference,
      float severity,
      String componentName)
  {
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.pathnames.add(componentName);
    component.securityData = new ApiSecurityDataDTO();
    ApiSecurityIssueDTO securityIssue = new ApiSecurityIssueDTO();
    securityIssue.reference = reference;
    securityIssue.severity = severity;
    component.securityData.securityIssues.add(securityIssue);
    return component;
  }

  private ApiReportComponentDTOV2 generateComponentWithLicense(String licenseName, String componentName) {
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.pathnames.add(componentName);
    component.licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO license = new ApiLicenseDTO();
    license.licenseName = licenseName;
    component.licenseData.declaredLicenses.add(license);
    return component;
  }

  private ApiReportComponentDTOV2 generateComponent(String componentName) {
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.pathnames.add(componentName);
    return component;
  }

  private PDPage mockPage() {
    return new PDPage(PdfGenerator.PAGE_SIZE);
  }

  private File generateReportFile() {
    File reportFile =
        insightWork.getReportFile(tempEntity.newApplicationWithParent("appPublicId", "appName").getId(), "scanId");
    reportFile.getParentFile().mkdirs();
    return reportFile;
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

  private String getLicenseText(ApiLicenseDataDTOV2 licenseData) {
    PdfGenerator pdfGenerator = new PdfGenerator(null, new ApiReportPolicyDataDTOV2(), new ApiReportRawDataDTOV2());
    pdfGenerator.initFontStyles(new PDDocument());
    ParagraphCell paragraphCell = pdfGenerator.licensesCellBuilder(
        PdfGenerator.licensesToString(licenseData.declaredLicenses),
        PdfGenerator.licensesToString(licenseData.observedLicenses)).build();
    paragraphCell.setWidth(1000);
    StringBuilder stringBuilder = new StringBuilder();
    Paragraph paragraph = paragraphCell.getParagraph().getWrappedParagraph();
    paragraph.iterator().forEachRemaining(textFragment -> stringBuilder.append(textFragment.getText()));
    return stringBuilder.toString();
  }
}
