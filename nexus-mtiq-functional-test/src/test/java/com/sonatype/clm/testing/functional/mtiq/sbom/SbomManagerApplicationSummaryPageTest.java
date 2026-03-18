/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.elements.sbom.SbomsTile;
import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerApplicationSummaryPage;
import com.sonatype.clm.testing.functional.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.*;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.sbom.SbomTestHelper.mockOriginalSbom;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SbomManagerApplicationSummaryPageTest
    extends AbstractMtiqFunctionalTest
{
  private final SbomManagerApplicationSummaryPage sbomSummaryPage = new SbomManagerApplicationSummaryPage();

  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private Organization organization;

  private Application application;

  private ThirdPartySbomMetadata sbomMetadata;

  private final InsightWork insightWork = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  @Before
  public void init() throws Exception {
    thirdPartySbomMetadataDAO = lookup(ThirdPartySbomMetadataDAO.class);
    organization = tempEntity.newOrganization("test-organization");
    application = tempEntity.newApplication("Test Application", "test-application", organization.getId());

    createSbomMetadata("test-version", true);
  }

  private void setLicenseAndAdminLogin() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER_SAAS);
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
  }

  @Test
  public void testSbomsTile_RenderSuccessful() {
    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.shouldBe(visible);
    sbomsTile.importButton().shouldBe(visible);
    sbomsTile.header().shouldHave(text("SBOMS"));
    sbomsTile.tableHeaders()
        .shouldHave(CollectionCondition
            .size(6));
    sbomsTile.columnHeader(0)
        .shouldHave(
            text("VERSIONS"));
    sbomsTile.columnHeader(1)
        .shouldHave(
            text("VULNERABILITIES"));
    sbomsTile.columnHeader(2)
        .shouldHave(
            text("RELEASE STATUS"));
    sbomsTile.columnHeader(3)
        .shouldHave(
            text("BOM FORMAT"));
    sbomsTile.columnHeader(4)
        .shouldHave(
            text("IMPORT DATE"));
    sbomsTile.columnHeader(5)
        .shouldHave(
            text("ACTIONS"));
    ElementsCollection tableRows = sbomsTile.tableRows();
    tableRows.first().shouldBe(visible);
    tableRows.shouldHave(size(2));
    sbomsTile.footer().shouldBe(visible);
  }

  @Test
  public void testSbomsTile_RenderInvalidSbomIndicator() throws Exception {
    createSbomMetadata("test-version-2", false);

    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.shouldBe(visible);
    sbomsTile.getRowInvalidSbomIndicatorFromRow(1).shouldNotBe(visible);
    sbomsTile.getRowInvalidSbomIndicatorFromRow(2).shouldBe(visible);
  }

  @Test
  public void testSbomsTile_DeleteSbomReport() {
    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));
    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();

    sbomsTile.actions(1).click();
    sbomsTile.actionsSbomOptions().last().shouldHave(text("Delete SBOM")).click();
    sbomsTile.deleteSbomModal().shouldBe(visible);
    sbomsTile.deleteSbomModalPrimaryButton().shouldBe(text("Delete")).click();
    sbomsTile.deleteSbomModal().shouldNotBe(visible);
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("No SBOMs found"));
  }

  @Test
  public void testSbomsTile_BOMNavigation() {
    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));
    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    SelenideElement firstRowLink = sbomsTile.billOfMaterialsLink(1);
    firstRowLink.click();
    SbomManagerBillOfMaterialsPage sbomPage = new SbomManagerBillOfMaterialsPage();
    sbomPage.container().shouldBe(visible);
    sbomPage.title().shouldHave(text("Test Application")).shouldBe(visible);
    String currentUrl = WebDriverRunner.getWebDriver().getCurrentUrl();
    assertThat(currentUrl).contains(application.getPublicId());
    assertThat(currentUrl).contains(sbomMetadata.getSbomVersion());
  }

  @Test
  public void testSbomsTile_Pagination() throws Exception {
    setSbomsTileTableData();
    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));
    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.footer().shouldBe(visible);
    ElementsCollection paginationButtons = sbomsTile.paginationButtons();
    paginationButtons.get(0).shouldHave(text("1"));
    paginationButtons.get(1).shouldHave(text("2"));
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("test-version 0"));
    paginationButtons.get(1).shouldHave(text("2")).click();
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("test-version"));
    paginationButtons.get(0).shouldHave(text("1")).click();
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("test-version 0"));
  }

  @Test
  public void testSbomsTile_ChangeToAnotherApplication() {
    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));
    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.shouldBe(visible);
    sbomsTile.importButton().shouldBe(visible);
    sbomSummaryPage.title().shouldHave(text("Test Application")).shouldBe(visible);

    ElementsCollection tableRows = sbomsTile.tableRows();
    tableRows.first().shouldBe(visible);
    tableRows.shouldHave(size(2));
    sbomsTile.footer().shouldBe(visible);
    sbomsTile.tableBodyRows().shouldHave(size(1));
    sbomsTile.tableBodyRowsColumns(0).shouldHave(CollectionCondition.size(6));
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text(sbomMetadata.getSbomVersion()));
    Application newApplication = tempEntity.newApplication("New Application", "new-application", organization.getId());
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(newApplication.getPublicId()));
    sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomSummaryPage.title().shouldHave(text("New Application")).shouldBe(visible);
    sbomsTile.footer().shouldNotBe(visible);
    sbomsTile.tableBodyRows().shouldHave(size(1));
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldNotHave(text(sbomMetadata.getSbomVersion()));
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("No SBOMs found"));
  }

  @Test
  public void testSbomsTile_DownloadDropdownOptions() throws Exception {
    createSbomMetadata("test-version-2", false);

    setLicenseAndAdminLogin();

    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));
    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();

    // Valid SBOM options
    sbomsTile.actions(1).click();
    sbomsTile.actionsSbomOptions().get(0).shouldBe(enabled).shouldHave(text("Export Original SBOM"));
    sbomsTile.actionsSbomOptions().get(1).shouldBe(enabled).shouldHave(text("Additional Export Options"));
    sbomsTile.actionsSbomOptions().get(2).shouldHave(text("Export PDF"));
    sbomsTile.actionsSbomOptions().last().shouldBe(enabled).shouldHave(text("Delete SBOM"));

    // Click to close the dropdown
    SbomManagerApplicationSummaryPage.sbomsTile().click();

    // Invalid SBOM options
    sbomsTile.actions(2).click();
    sbomsTile.actionsSbomOptions().get(0).shouldBe(enabled).shouldHave(text("Export Original SBOM"));
    sbomsTile.actionsSbomOptions().get(1).shouldBe(disabled).shouldHave(text("Additional Export Options"));
    sbomsTile.actionsSbomOptions().get(2).shouldHave(text("Export PDF"));
    sbomsTile.actionsSbomOptions().last().shouldBe(enabled).shouldHave(text("Delete SBOM"));

    File downloadedSbom = sbomsTile.actionsSbomOptions()
        .first()
        .shouldHave(text("Export Original SBOM"))
        .download();

    byte[] fileBeginning = new byte[5];
    try (FileInputStream stream = new FileInputStream(downloadedSbom)) {
      stream.read(fileBeginning);
    }
    assertThat(new String(fileBeginning)).isEqualTo("<?xml");
  }

  private void createSbomMetadata(String sbomVersion, boolean isValid) throws Exception {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);
    Path zippedBom = mockOriginalSbom(SbomManagerApplicationSummaryPageTest.class, "simple-bom.xml",
        insightWork.getSbomDir(application.getId()).toPath());

    createSbomMetadata(scannedFile.getId(), zippedBom.getFileName().toString(), new Date(0), sbomVersion, isValid);
  }

  private void createSbomMetadata(
      String thirdPartyFileId,
      String fileName,
      Date createdAt,
      String sbomVersion,
      boolean isValid)
  {
    sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFileId, application.getId(), sbomVersion, ACTIVE, fileName,
            SbomSpecification.CYCLONEDX.name(), SbomFormat.XML.name(), "0.0");
    sbomMetadata.setCreatedAt(createdAt);
    sbomMetadata.setIsValid(isValid);

    thirdPartySbomMetadataDAO.update(sbomMetadata);
  }

  @Test
  public void testSbomsTile__sortByImportDate() throws Exception {
    setSbomsTileTableData();
    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.table().shouldBe(visible);
    // sort desc by default -> newest first
    ElementsCollection tableRows = sbomsTile.tableBodyRows();
    ElementsCollection paginationButtons = sbomsTile.paginationButtons();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    List<LocalDateTime> dates = tableRows.stream()
        .map(row -> row.findAll("td").get(4).text())
        .map(text -> LocalDateTime.parse(text, formatter))
        .toList();

    validateSortByDates(false, dates);

    // sort asc
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));
    paginationButtons.get(0).shouldHave(text("1")).click();
    sbomsTile.columnHeader(4).shouldHave(text("IMPORT DATE")).click();
    Selenide.sleep(1000L);
    dates = tableRows.stream()
        .map(row -> row.findAll("td").get(4).text())
        .map(text -> LocalDateTime.parse(text, formatter))
        .toList();

    validateSortByDates(true, dates);

    // sort desc

    paginationButtons.get(0).shouldHave(text("1")).click();
    sbomsTile.columnHeader(4).shouldHave(text("IMPORT DATE")).click();
    Selenide.sleep(1000L);
    dates = tableRows.stream()
        .map(row -> row.findAll("td").get(4).text())
        .map(text -> LocalDateTime.parse(text, formatter))
        .toList();

    validateSortByDates(false, dates);
  }

  private void validateSortByDates(boolean ascending, List<LocalDateTime> dates) {
    if (ascending) {
      for (int i = 0; i < dates.size() - 1; i++) {
        assertThat(dates.get(i).isBefore(dates.get(i + 1))).isTrue();
      }
    }
    else {
      for (int i = 0; i < dates.size() - 1; i++) {
        assertThat(dates.get(i).isAfter(dates.get(i + 1))).isTrue();
      }
    }
  }

  @Test
  public void testSbomsTile__sortByReleaseStatus() throws Exception {
    setSbomsTileTableData();
    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));
    waitUntilUrl(SbomManagerApplicationSummaryPage.url(application.getPublicId()));

    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.table().shouldBe(visible);
    sbomsTile.columnHeader(2).shouldHave(text("RELEASE STATUS")).click();
    verifySortOrderReleaseStatus(true, sbomsTile); // verify asc
    sbomsTile.columnHeader(2).shouldHave(text("RELEASE STATUS")).click();
    verifySortOrderReleaseStatus(false, sbomsTile);
  }

  private void verifySortOrderReleaseStatus(boolean ascending, SbomsTile sbomsTile) {
    String expectedValue;
    int rowsHighOrCriticalVulnerabilities = 5;
    for (int i = 0; i < 10; i++) {
      if (ascending) {
        expectedValue = (i < rowsHighOrCriticalVulnerabilities) ? "0%" : "44.4%";
      }
      else if (i == 0) {
        expectedValue = "100%";
      }
      else {
        expectedValue = (i <= rowsHighOrCriticalVulnerabilities) ? "44.4%" : "0%";
      }
      sbomsTile.releaseStatusColumn(i).shouldHave(text(expectedValue));
    }
  }

  private void setSbomsTileTableData() throws Exception {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();

    Calendar calendar = Calendar.getInstance();
    Date today;
    for (int i = 0; i < 10; i++) {
      Path zippedBom = mockOriginalSbom(SbomManagerApplicationSummaryPageTest.class, "simple-bom.xml",
          insightWork.getSbomDir(application.getId()).toPath());
      calendar.add(Calendar.DAY_OF_MONTH, -1);
      today = calendar.getTime();
      sbomMetadata = tempEntity.newThirdPartySbomMetadata(
          scannedFile.getId(),
          application.getId(),
          "test-version " + i,
          ACTIVE,
          zippedBom.getFileName().toString(),
          SbomSpecification.CYCLONEDX.name(),
          SbomFormat.XML.name(),
          "0.0", today);

      ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
      PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
      ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
          "s2", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(),
          packageUrlIdentifier1.getVersion(), "h2", packageUrlIdentifier1.getPackageUrl());

      tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
          "cve-1", sbomMetadata.getId(), "description1", "link1", CvssV3Severity.HIGH.getStartScoreRange(),
          CvssV3Severity.HIGH.getDisplayName(), "fix1");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-2", sbomMetadata.getId(), "description2", "link2",
          CvssV3Severity.HIGH.getStartScoreRange() + 0.2f, CvssV3Severity.HIGH.getDisplayName(), "fix2");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-3", sbomMetadata.getId(), "description3", "link3",
          CvssV3Severity.LOW.getStartScoreRange() + 1f, CvssV3Severity.LOW.getDisplayName(), "fix3");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-4", sbomMetadata.getId(), "description4", "link4",
          CvssV3Severity.LOW.getEndScoreRange(), CvssV3Severity.LOW.getDisplayName(), "fix4");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-5", sbomMetadata.getId(), "description5", "link5",
          CvssV3Severity.LOW.getEndScoreRange() - 0.1f, CvssV3Severity.LOW.getDisplayName(), "fix5");
      tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "cve-6", sbomMetadata.getId(), "description6", "link6",
          CvssV3Severity.LOW.getEndScoreRange(), CvssV3Severity.LOW.getDisplayName(), "fix6");

      if (i < 5) {
        for (int j = 4; j <= 10; j++) {
          ThirdPartyCoordinateSecurity coordinateSecurity =
              tempEntity.newThirdPartyCoordinateSecurity(
                  coordinate1,
                  "r-" + i + j,
                  sbomMetadata.getId(),
                  "description7",
                  "link7",
                  10,
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
