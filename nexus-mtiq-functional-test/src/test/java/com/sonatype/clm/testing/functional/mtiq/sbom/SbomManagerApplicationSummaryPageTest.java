/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.sbom;

import java.nio.file.Path;
import java.util.Date;

import com.sonatype.clm.testing.functional.mtiq.AbstractMtiqFunctionalTest;
import com.sonatype.clm.testing.functional.mtiq.elements.sbom.SbomsTile;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.SbomManagerApplicationSummaryPage;
import com.sonatype.clm.testing.functional.mtiq.pages.sbom.SbomManagerBillOfMaterialsPage;
import com.sonatype.clm.testing.functional.pages.IndexPage;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.scan.file.SbomFormat;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
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

    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);
    Path zippedBom = mockOriginalSbom(SbomManagerApplicationSummaryPageTest.class, "simple-bom.xml",
        insightWork.getSbomDir(application.getId()).toPath());

    sbomMetadata = tempEntity.newThirdPartySbomMetadata(
        scannedFile.getId(),
        application.getId(),
        "test-version",
        "ACTIVE",
        zippedBom.getFileName().toString(),
        SbomSpecification.CYCLONEDX.name(),
        SbomFormat.XML.name(),
        "0.0"
    );
    sbomMetadata.setCreatedAt(new Date(0));
    thirdPartySbomMetadataDAO.update(sbomMetadata);
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
    sbomsTile.tableHeaders().shouldHave(CollectionCondition
        .size(5));
    sbomsTile.columnHeader(0).shouldHave(
        text("VERSIONS"));
    sbomsTile.columnHeader(1).shouldHave(
        text("VULNERABILITIES"));
    sbomsTile.columnHeader(2).shouldHave(
        text("BOM FORMAT"));
    sbomsTile.columnHeader(3).shouldHave(
        text("IMPORT DATE"));
    sbomsTile.columnHeader(4).shouldHave(
        text("ACTIONS"));
    ElementsCollection tableRows = sbomsTile.tableRows();
    tableRows.first().shouldBe(visible);
    tableRows.shouldHave(size(2));
    sbomsTile.footer().shouldBe(visible);
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
  @Ignore
  public void testSbomsTile_Pagination() {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(scannedFile);
    for (int i = 0; i < 50; i++) {
      tempEntity.newThirdPartySbomMetadata(
          scannedFile.getId(),
          application.getId(),
          "test-version-" + i,
          "ACTIVE",
          scannedFile.getFilename(),
          SbomSpecification.CYCLONEDX.name(),
          SbomFormat.XML.name(),
          "0.0"
      );
    }
    setLicenseAndAdminLogin();
    refreshOrOpen(SbomManagerApplicationSummaryPage.url(application.getPublicId()));
    SbomsTile sbomsTile = SbomManagerApplicationSummaryPage.sbomsTile();
    sbomsTile.footer().shouldBe(visible);
    ElementsCollection paginationButtons = sbomsTile.paginationButtons();
    paginationButtons.get(0).shouldHave(text("1"));
    paginationButtons.get(5).shouldHave(text("6"));
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("test-version-49"));
    paginationButtons.get(2).shouldHave(text("3")).click();
    sbomsTile.tableBodyRowsColumns(0).get(0).shouldHave(text("test-version-29"));
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
    sbomsTile.tableBodyRowsColumns(0).shouldHave(CollectionCondition.size(5));
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
}
