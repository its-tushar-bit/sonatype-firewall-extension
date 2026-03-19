/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import com.codeborne.selenide.ElementsCollection;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.sbom.SbomApplicationsTable;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomApplicationsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.*;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;

public class SbomApplicationsPageTest
    extends AbstractFunctionalTest
{
  private Organization organization;

  private Policy policy;

  private ThirdPartySbomMetadata sbomMetadata;

  private final InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Before
  public void beforeEachMethod() {
    organization = tempEntity.newOrganization();
    policy = tempEntity.newPolicy(organization);

    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER);

    refreshOrOpen(IndexPage.url());
  }

  @Test
  public void testApplicationsPage__tableRendered() throws Exception {
    setSbomApplicationsTableData();
    refreshOrOpen(SbomApplicationsPage.url());

    SbomApplicationsPage sbomApplicationsPage = new SbomApplicationsPage();
    sbomApplicationsPage.title().shouldHave(text("Applications"));

    SbomApplicationsTable applicationsTable = SbomApplicationsPage.sbomApplicationsTable();
    applicationsTable.table().shouldBe(visible);

    applicationsTable.tableHeaders().shouldHave(size(6));
    applicationsTable.columnHeader(0)
        .shouldHave(
            text("NAME"));
    applicationsTable.columnHeader(1)
        .shouldHave(
            text("LATEST VERSION"));
    applicationsTable.columnHeader(2)
        .shouldHave(
            text("RELEASE STATUS"));
    applicationsTable.columnHeader(3)
        .shouldHave(
            text("IMPORT DATE"));
    applicationsTable.columnHeader(4).shouldHave(text("VULNERABILITIES"));
    applicationsTable.columnHeader(5)
        .shouldHave(
            text("VIOLATIONS"));
    ElementsCollection tableRows = applicationsTable.tableBodyRows();
    tableRows.shouldHave(sizeGreaterThan(49));
    applicationsTable.footer().shouldBe(visible);
    applicationsTable.paginationStatus().shouldHave(visible);
  }

  @Test
  public void testApplicationsPage__nameFilter() throws Exception {
    setSbomApplicationsTableData();
    refreshOrOpen(SbomApplicationsPage.url());

    SbomApplicationsTable applicationsTable = SbomApplicationsPage.sbomApplicationsTable();
    ElementsCollection tableRows = applicationsTable.tableBodyRows();
    applicationsTable.applicationNameFilter().shouldBe(visible);
    applicationsTable.applicationNameFilter().sendKeys("Test App 19");
    tableRows.shouldHave(size(1));
    applicationsTable.tableBodyRows().get(0).shouldHave(text("Test App 19"));
    applicationsTable.applicationNameFilter().clear();
    applicationsTable.applicationNameFilter().sendKeys(" ");
    tableRows.shouldHave(size(50));
    applicationsTable.applicationNameFilter().sendKeys("Test App 21");
    tableRows.shouldHave(size(1));
    applicationsTable.tableBodyRows().get(0).shouldHave(text("Test App 21"));
  }

  @Test
  public void testApplicationsPage__pagination() throws Exception {
    setSbomApplicationsTableData();
    refreshOrOpen(SbomApplicationsPage.url());

    SbomApplicationsTable applicationsTable = SbomApplicationsPage.sbomApplicationsTable();
    applicationsTable.table().shouldBe(visible);
    ElementsCollection paginationButtons = applicationsTable.paginationButtons();
    paginationButtons.get(0).shouldHave(text("1"));
    paginationButtons.get(1).shouldHave(text("2"));
    applicationsTable.tableBodyRowsColumns(0).get(0).shouldHave(text("Test App 0"));
    applicationsTable.paginationStatus().shouldHave(text("Showing 50 of 75 applications"));
    paginationButtons.get(1).shouldHave(text("2")).click();
    applicationsTable.tableBodyRowsColumns(24).get(0).shouldHave(text("Test App 74"));
    applicationsTable.paginationStatus().shouldHave(text("Showing 75 of 75 applications"));
  }

  private void setSbomApplicationsTableData() throws Exception {
    organization = tempEntity.newOrganization("Test Organization");
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(scannedFile);
    for (int i = 0; i < 3; i++) {
      Application app = tempEntity.newApplication("Test Policy App " + i, "test-app-policy-" + i, organization.getId());
      File reportFile = insightWork.getReportFile(app.getId(), scan.getScanId());
      FileUtils.copyURLToFile(ReportHelper
          .zipReport("/SbomApplicationsPageTest", tempDir), reportFile);
      tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, scan.getScanId());

      PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(app.getId(),
          ComplianceStageType.ID, "scanId1App1");
      tempEntity.newPolicyViolation(policyEvaluation, policy, "g1",
          "a1", "v1", "h1", "r1");
    }
    for (int i = 0; i < 75; i++) {
      Application app = tempEntity.newApplication("Test App " + i, "test-app-" + i, organization.getId());
      Path zippedBom = mockOriginalSbom(this.getClass(), "simple-bom.xml",
          insightWork.getSbomDir(app.getId()).toPath());
      LocalDateTime today = LocalDateTime.now(ZoneId.systemDefault()).minusDays(i + 1);
      sbomMetadata = tempEntity.newThirdPartySbomMetadata(
          scannedFile.getId(),
          app.getId(),
          "test-version " + i,
          ACTIVE,
          zippedBom.getFileName().toString(),
          SbomSpecification.CYCLONEDX.name(),
          SbomFormat.XML.name(),
          "0.0", Date.from(today.atZone(ZoneId.systemDefault()).toInstant()));

      ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
      PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
      ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
          "s2", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
          "h2", packageUrlIdentifier1.getPackageUrl());

      tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
          "cve-1", sbomMetadata.getId(), "description1", "link1", CvssV3Severity.LOW.getStartScoreRange(),
          CvssV3Severity.LOW.getDisplayName(), "fix1");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-2", sbomMetadata.getId(), "description2", "link2",
          CvssV3Severity.LOW.getStartScoreRange() + 0.2f, CvssV3Severity.LOW.getDisplayName(), "fix2");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-3", sbomMetadata.getId(), "description3", "link3",
          CvssV3Severity.LOW.getStartScoreRange() + 1f, CvssV3Severity.LOW.getDisplayName(), "fix3");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-4", sbomMetadata.getId(), "description4", "link4",
          CvssV3Severity.LOW.getEndScoreRange(), CvssV3Severity.LOW.getDisplayName(), "fix4");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-5", sbomMetadata.getId(), "description5", "link5",
          CvssV3Severity.LOW.getEndScoreRange() - 0.1f, CvssV3Severity.LOW.getDisplayName(), "fix5");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-6", sbomMetadata.getId(), "description6", "link6",
          CvssV3Severity.LOW.getEndScoreRange(), CvssV3Severity.LOW.getDisplayName(), "fix6");

      if (i < 30) {
        for (int j = 4; j <= 10; j++) {
          ThirdPartyCoordinateSecurity coordinateSecurity =
              tempEntity.newThirdPartyCoordinateSecurity(
                  coordinate1,
                  "r-" + i + j,
                  sbomMetadata.getId(),
                  "description7",
                  "link7",
                  Math.min(i, 10),
                  "severity",
                  "fix7");
          if (j <= 7) {
            insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity);
          }
        }
      }
    }
  }

  private void insertVEXToThirdPartyCoordinateSecurity(ThirdPartyCoordinateSecurity coordinateSecurity) {
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
        "state", "justification", "response", "detail");
  }

}
