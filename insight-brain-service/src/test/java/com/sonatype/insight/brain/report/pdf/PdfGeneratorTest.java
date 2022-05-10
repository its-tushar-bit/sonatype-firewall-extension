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
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationBaseDTO;
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
import com.sonatype.insight.brain.report.pdf.PdfGenerator.WordBreaker;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.vandeseer.easytable.structure.Row;
import org.vandeseer.easytable.structure.Table;
import org.vandeseer.easytable.structure.cell.TextCell;
import org.vandeseer.easytable.structure.cell.paragraph.ParagraphCell;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.text.FontDescriptor;
import rst.pdfbox.layout.util.Pair;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfGeneratorTest
    extends AbstractComponentTest
{
  @Inject
  private ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  private InsightWork insightWork;

  @Inject
  InsightConfig config;

  @Before
  public void setup() {
    config.setBaseUrl("http://localhost:8070/");
  }

  private PdfData newPdfData() {
    PdfData pdfData = new PdfData();
    pdfData.productVersion = "98";
    pdfData.policyData = new ApiReportPolicyDataDTOV2();
    pdfData.rawData = new ApiReportRawDataDTOV2();
    return pdfData;
  }

  @Test
  public void testGenerate() throws Exception {
    Application app = tempEntity.newApplicationWithParent("appPublicId", "appName");
    String scanId = "scanId";
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scanId);
    File reportFile = insightWork.getReportFile(app.getId(), scanId);
    FileUtils.copyURLToFile(ReportHelper.zipReport("/PdfGeneratorTest/report", tempDir), reportFile);

    File pdfFile = PdfGenerator.getPdfFile(reportFile);
    PdfData pdfData = newPdfData();
    pdfData.policyData = apiReportDataServiceV2.getPolicyViolationsData(app.getPublicId(), scanId);
    pdfData.rawData = apiReportDataServiceV2.getRawData(app.getPublicId(), scanId);

    PdfGenerator.generate(pdfFile, pdfData);
    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_EmptyData() throws Exception {
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());
    PdfData pdfData = newPdfData();
    pdfData.policyData = generateMinimalPolicyData();

    PdfGenerator.generate(pdfFile, pdfData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_PolicyDataWithEmptyComponent() throws Exception {
    ApiReportPolicyDataDTOV2 policyData = generateMinimalPolicyData();
    policyData.components.add(new ApiReportComponentPolicyViolationsDTOV2());
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    PdfGenerator.generate(pdfFile, pdfData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_PolicyDataWithEmptyViolation() throws Exception {
    ApiReportPolicyDataDTOV2 policyData = generateMinimalPolicyData();
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
    component.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component);
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    PdfGenerator.generate(pdfFile, pdfData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_RawDataWithEmptyComponent() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    rawData.components.add(new ApiReportComponentDTOV2());
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());
    PdfData pdfData = newPdfData();
    pdfData.policyData = generateMinimalPolicyData();
    pdfData.rawData = rawData;

    PdfGenerator.generate(pdfFile, pdfData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_RawDataWithEmptySecurityData() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.securityData = new ApiSecurityDataDTO();
    rawData.components.add(component);
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());
    PdfData pdfData = newPdfData();
    pdfData.policyData = generateMinimalPolicyData();
    pdfData.rawData = rawData;

    PdfGenerator.generate(pdfFile, pdfData);

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
    PdfData pdfData = newPdfData();
    pdfData.policyData = generateMinimalPolicyData();
    pdfData.rawData = rawData;

    PdfGenerator.generate(pdfFile, pdfData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_RawDataWithEmptyLicenseData() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.licenseData = new ApiLicenseDataDTOV2();
    rawData.components.add(component);
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());
    PdfData pdfData = newPdfData();
    pdfData.policyData = generateMinimalPolicyData();
    pdfData.rawData = rawData;

    PdfGenerator.generate(pdfFile, pdfData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGenerate_NonLatinCharactersSupportedByFonts() throws Exception {
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
    component.displayName = "星義义こ여�-test.zip";
    component.violations.add(new ApiReportPolicyViolationDTOV2());
    ApiReportPolicyDataDTOV2 policyData = generateMinimalPolicyData();
    policyData.application.name += "星義义こ여";
    policyData.components.add(component);
    File pdfFile = PdfGenerator.getPdfFile(generateReportFile());
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    PdfGenerator.generate(pdfFile, pdfData);

    assertThat(pdfFile).isFile();
  }

  @Test
  public void testGetTitle_EmptyPolicyData() {
    String sectionName = "sectionName";
    assertThat(new PdfGenerator(null, newPdfData()).getTitle(sectionName)).isEqualTo(sectionName);
  }

  @Test
  public void testGetTitle_EmptyApplication() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    assertThat(new PdfGenerator(null, pdfData).getTitle(sectionName)).isEqualTo(sectionName);
  }

  @Test
  public void testGetTitle_OnlyApplicationName() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.application = new ApiApplicationBaseDTO();
    policyData.application.name = "someApplication";
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    assertThat(new PdfGenerator(null, pdfData).getTitle(sectionName))
        .isEqualTo(sectionName + " for " + policyData.application.name);
  }

  @Test
  public void testGetTitle_OnlyReportTitle() {
    String sectionName = "sectionName";
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.reportTitle = "reportTitle";
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

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
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    assertThat(new PdfGenerator(null, pdfData).getTitle(sectionName))
        .isEqualTo(sectionName + " for " + policyData.application.name + " " + policyData.reportTitle);
  }

  @Test
  public void testCountPolicyViolations_EmptyPolicyData() {
    assertThat(new PdfGenerator(null, newPdfData()).countPolicyViolations(0, 10)).isZero();
  }

  @Test
  public void testCountPolicyViolations_EmptyComponent() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    policyData.components.add(new ApiReportComponentPolicyViolationsDTOV2());
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    assertThat(new PdfGenerator(null, pdfData).countPolicyViolations(0, 10)).isZero();
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
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

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
    policyData.components.add(new ApiReportComponentPolicyViolationsDTOV2());
    ApiReportComponentPolicyViolationsDTOV2 component1 = new ApiReportComponentPolicyViolationsDTOV2();
    component1.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component1);
    ApiReportComponentPolicyViolationsDTOV2 component2 = new ApiReportComponentPolicyViolationsDTOV2();
    component2.violations.add(new ApiReportPolicyViolationDTOV2());
    policyData.components.add(component2);
    policyData.components.add(new ApiReportComponentPolicyViolationsDTOV2());
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    assertThat(new PdfGenerator(null, pdfData).countPolicyViolations(0, 10)).isEqualTo(2);
  }

  @Test
  public void testCountPolicyViolations_ExcludesWaivedAndGrandfathered() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    component.violations.add(violation);
    ApiReportPolicyViolationDTOV2 waivedViolation = new ApiReportPolicyViolationDTOV2();
    waivedViolation.waived = true;
    component.violations.add(waivedViolation);
    ApiReportPolicyViolationDTOV2 grandfatheredViolation = new ApiReportPolicyViolationDTOV2();
    grandfatheredViolation.grandfathered = true;
    component.violations.add(grandfatheredViolation);
    policyData.components.add(component);
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    assertThat(new PdfGenerator(null, pdfData).countPolicyViolations(0, 10)).isEqualTo(1);
  }

  @Test
  public void testCountAffectedComponents() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component1 = new ApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyThreatLevel = 2;
    component1.violations.add(violation);
    ApiReportComponentPolicyViolationsDTOV2 component2 = new ApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation2 = new ApiReportPolicyViolationDTOV2();
    violation2.policyThreatLevel = 10;
    component2.violations.add(violation2);
    policyData.components.add(component1);
    policyData.components.add(component2);
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    assertThat(new PdfGenerator(null, pdfData).countAffectedComponents()).isEqualTo(2);
  }

  @Test
  public void testCountAffectedComponents_ExcludesComponentsWithOnlyLowThreatOrWaivedOrGrandfatheredViolations() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyThreatLevel = 1;
    component.violations.add(violation);
    ApiReportPolicyViolationDTOV2 waivedViolation = new ApiReportPolicyViolationDTOV2();
    waivedViolation.policyThreatLevel = 9;
    waivedViolation.waived = true;
    component.violations.add(waivedViolation);
    ApiReportPolicyViolationDTOV2 grandfatheredViolation = new ApiReportPolicyViolationDTOV2();
    grandfatheredViolation.policyThreatLevel = 9;
    grandfatheredViolation.grandfathered = true;
    component.violations.add(grandfatheredViolation);
    policyData.components.add(component);
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    assertThat(new PdfGenerator(null, pdfData).countAffectedComponents()).isEqualTo(0);
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
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table policyViolationsTable = pdfGenerator.createPolicyViolationsTable(PdfGenerator.newPage());

    assertThat(policyViolationsTable).isNotNull();
    List<Row> rows = policyViolationsTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(4)).getText())
        .containsExactly("comp111", "comp112", "comp121", "comp122", "comp211", "comp212", "comp221", "comp222");
  }

  @Test
  public void testCreatePolicyViolationsTableData_ExcludesWaivedAndGrandfathered() {
    ApiReportPolicyDataDTOV2 policyData = new ApiReportPolicyDataDTOV2();
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
    ApiReportPolicyViolationDTOV2 violation = new ApiReportPolicyViolationDTOV2();
    violation.policyName = "policyName1";
    component.violations.add(violation);
    ApiReportPolicyViolationDTOV2 waivedViolation = new ApiReportPolicyViolationDTOV2();
    waivedViolation.policyName = "policyName2";
    waivedViolation.waived = true;
    component.violations.add(waivedViolation);
    ApiReportPolicyViolationDTOV2 grandfatheredViolation = new ApiReportPolicyViolationDTOV2();
    grandfatheredViolation.policyName = "policyName3";
    grandfatheredViolation.grandfathered = true;
    component.violations.add(grandfatheredViolation);
    policyData.components.add(component);
    PdfData pdfData = newPdfData();
    pdfData.policyData = policyData;

    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);

    assertThat(pdfGenerator.createPolicyViolationsTableData()).extracting(row -> row.policyName)
        .containsExactly(violation.policyName);
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
    PdfData pdfData = newPdfData();
    pdfData.rawData = rawData;
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
  public void testCreateLicensesTable_RowOrdering() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 c1 = generateComponentWithLicenses("v1", "v1", "v1", "v1");

    ApiReportComponentDTOV2 c2 = generateComponentWithLicenses("v2", "v1", "v1", "v1");
    ApiReportComponentDTOV2 c3 = generateComponentWithLicenses("v1", "v2", "v1", "v1");
    ApiReportComponentDTOV2 c4 = generateComponentWithLicenses("v1", "v1", "v2", "v1");
    ApiReportComponentDTOV2 c5 = generateComponentWithLicenses("v1", "v1", "v1", "v2");

    ApiReportComponentDTOV2 c6 = generateComponentWithLicenses("v2", "v2", "v1", "v1");
    ApiReportComponentDTOV2 c7 = generateComponentWithLicenses("v1", "v2", "v2", "v1");
    ApiReportComponentDTOV2 c8 = generateComponentWithLicenses("v1", "v1", "v2", "v2");
    ApiReportComponentDTOV2 c9 = generateComponentWithLicenses("v2", "v1", "v1", "v2");
    ApiReportComponentDTOV2 c10 = generateComponentWithLicenses("v1", "v2", "v1", "v2");
    ApiReportComponentDTOV2 c11 = generateComponentWithLicenses("v2", "v1", "v2", "v1");

    ApiReportComponentDTOV2 c12 = generateComponentWithLicenses("v1", "v2", "v2", "v2");
    ApiReportComponentDTOV2 c13 = generateComponentWithLicenses("v2", "v1", "v2", "v2");
    ApiReportComponentDTOV2 c14 = generateComponentWithLicenses("v2", "v2", "v1", "v2");
    ApiReportComponentDTOV2 c15 = generateComponentWithLicenses("v2", "v2", "v2", "v1");

    ApiReportComponentDTOV2 c16 = generateComponentWithLicenses("v2", "v2", "v2", "v2");

    rawData.components.addAll(Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16));
    PdfData pdfData = newPdfData();
    pdfData.rawData = rawData;
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table licensesTable = pdfGenerator.createLicensesTable(PdfGenerator.newPage());

    assertThat(licensesTable).isNotNull();
    List<Row> rows = licensesTable.getRows();
    assertThat(rows).extracting(r -> r.getCells().stream().map(c -> {
      if (c instanceof ParagraphCell) {
        return ((ParagraphCell) c).getParagraph().getWrappedParagraph().iterator().next().getText();
      }
      if (c instanceof TextCell) {
        return ((TextCell) c).getText();
      }
      return null;
    }).collect(Collectors.joining(","))).containsExactly("EFFECTIVE,DECLARED,OBSERVED,COMPONENT",
        "v1,v1,v1,v1",
        "v1,v1,v1,v2",
        "v1,v1,v2,v1",
        "v1,v1,v2,v2",
        "v1,v2,v1,v1",
        "v1,v2,v1,v2",
        "v1,v2,v2,v1",
        "v1,v2,v2,v2",
        "v2,v1,v1,v1",
        "v2,v1,v1,v2",
        "v2,v1,v2,v1",
        "v2,v1,v2,v2",
        "v2,v2,v1,v1",
        "v2,v2,v1,v2",
        "v2,v2,v2,v1",
        "v2,v2,v2,v2");
  }

  @Test
  public void testCreateBomTable_RowOrdering() throws Exception {
    ApiReportRawDataDTOV2 rawData = new ApiReportRawDataDTOV2();
    ApiReportComponentDTOV2 component1 = generateComponent("component1");
    ApiReportComponentDTOV2 component2 = generateComponent("component2");
    rawData.components.add(component2);
    rawData.components.add(component1);
    PdfData pdfData = newPdfData();
    pdfData.rawData = rawData;
    PdfGenerator pdfGenerator = new PdfGenerator(null, pdfData);
    pdfGenerator.initFontStyles(new PDDocument());

    Table bomTable = pdfGenerator.createBomTable(PdfGenerator.newPage());

    assertThat(bomTable).isNotNull();
    List<Row> rows = bomTable.getRows();
    assertThat(rows.subList(1, rows.size())).extracting(row -> ((TextCell) row.getCells().get(0)).getText())
        .containsExactly("component1", "component2");
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
    ApiReportComponentPolicyViolationsDTOV2 component = new ApiReportComponentPolicyViolationsDTOV2();
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
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
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
      String componentName)
  {
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.displayName = componentName;
    component.licenseData = new ApiLicenseDataDTOV2();
    ApiLicenseDTO license = new ApiLicenseDTO();
    license.licenseName = effectiveLicense;
    component.licenseData.effectiveLicenses.add(license);
    license = new ApiLicenseDTO();
    license.licenseName = declaredLicense;
    component.licenseData.declaredLicenses.add(license);
    license = new ApiLicenseDTO();
    license.licenseName = observedLicense;
    component.licenseData.observedLicenses.add(license);
    return component;
  }

  private ApiReportComponentDTOV2 generateComponent(String componentName) {
    ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
    component.displayName = componentName;
    return component;
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

  private String getLicenseText(List<ApiLicenseDTO> licenses) throws Exception {
    PdfGenerator pdfGenerator = new PdfGenerator(null, newPdfData());
    pdfGenerator.initFontStyles(new PDDocument());
    ParagraphCell paragraphCell =
        (ParagraphCell) pdfGenerator.buildLicensesCell(PdfGenerator.licensesToString(licenses), false).build();
    paragraphCell.setWidth(1000);
    StringBuilder stringBuilder = new StringBuilder();
    Paragraph paragraph = paragraphCell.getParagraph().getWrappedParagraph();
    paragraph.iterator().forEachRemaining(textFragment -> stringBuilder.append(textFragment.getText()));
    return stringBuilder.toString();
  }
}
