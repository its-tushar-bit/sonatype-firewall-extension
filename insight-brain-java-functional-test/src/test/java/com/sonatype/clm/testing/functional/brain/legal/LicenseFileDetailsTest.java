/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ComponentLicenseFileDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentLicenseFileDetailsPage.LicenseFileEditor;
import com.sonatype.clm.testing.functional.pages.ComponentLicenseFileDetailsPage.LicenseFileList;
import com.sonatype.clm.testing.functional.pages.ComponentLicenseFileDetailsPage.LicenseFileOverview;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;

public class LicenseFileDetailsTest
    extends AbstractFunctionalTest
{
  private Application app;

  private Organization rootOrg;

  ComponentIdentifier componentId;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    rootOrg = lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
    app = tempEntity.newApplicationWithParent(LicenseFileDetailsTest.class.getSimpleName(), "app", "org");
    componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "033e7a20b23ea284d474",
        componentId);

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalFileHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");

    refreshOrOpen(ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(
        app.getPublicId(), "033e7a20b23ea284d474", 0));
  }

  @Test
  public void testLicenseOverviewByHash() {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestLicenseOverview();
    eyesWatcher.eyesCheck("License File Details Overview section");
  }

  @Test
  public void testLicenseOverviewByComponentIdenfifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestLicenseOverview();
  }

  private void doTestLicenseOverview() {
    LicenseFileOverview licenseOverview = ComponentLicenseFileDetailsPage.licenseFileOverview();

    licenseOverview.getAttributionReportStatus().shouldHave(text("Included"));
    licenseOverview.getScope().shouldHave(text("Root Organization"));
    licenseOverview.getSource().shouldHave(text("Sonatype Scan"));

    licenseOverview.getLicenseText().shouldHave(text("Apache ServiceComb Copyright 2017-2021"));
  }

  @Test
  public void testLicenseListByHash() {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestLicenseList();
  }

  @Test
  public void testLicenseListByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestLicenseList();
  }

  private void doTestLicenseList() {
    LicenseFileList noticeList = ComponentLicenseFileDetailsPage.licenseFileList();

    noticeList.shouldHave(text("license"));
    noticeList.attributionInclusion(1).shouldHave(text("Included in attribution report"));
    noticeList.attributionInclusion(2).shouldHave(text("Included in attribution report"));
  }

  @Test
  public void testChangeSelectedLicenseByHash() {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestChangeSelectedLicense();
  }

  @Test
  public void testChangeSelectedLicenseByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestChangeSelectedLicense();
  }

  private void doTestChangeSelectedLicense() {
    LicenseFileOverview licenseOverview = ComponentLicenseFileDetailsPage.licenseFileOverview();
    LicenseFileList licenesList = ComponentLicenseFileDetailsPage.licenseFileList();

    licenseOverview.shouldHave(text("Apache Servicecomb"));
    licenseOverview.shouldNotHave(text("content"));
    SelenideElement secondLicense = licenesList.itemAt(2);
    secondLicense.lastChild().click();
    licenseOverview.shouldHave(text("content"));
    licenseOverview.shouldNotHave(text("Apache Servicecomb"));
  }

  @Test
  public void testAddVerifyLicenseByHash() {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));

    String content = "added license by hash";
    LicenseFileOverview licenseFileOverview = doTestAddVerifyLicense(content);

    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    licenseFileOverview.shouldHave(text(content));
  }

  @Test
  public void testAddVerifyLicenseByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));

    String content = "added license by component identifier";
    LicenseFileOverview licenseFileOverview = doTestAddVerifyLicense(content);

    refreshOrOpen(
        ComponentLicenseFileDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    licenseFileOverview.shouldHave(text(content));
  }

  private LicenseFileOverview doTestAddVerifyLicense(String content) {
    LicenseFileOverview licenseFileOverview = ComponentLicenseFileDetailsPage.licenseFileOverview();

    licenseFileOverview.shouldHave(text("Apache Servicecomb"));
    licenseFileOverview.shouldNotHave(text("content"));

    ComponentLegalFile componentLicenseFile =
        tempEntity.newComponentLegalFile(componentId, rootOrg.getId(), LegalFileType.LICENSE, "licenseContentHash");
    tempEntity.newLegalFileOverride("ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", content,
        ComponentLegalPartStatus.ENABLED, componentLicenseFile.getId());

    return licenseFileOverview;
  }

  private void doTestEditLicenseFiles(String content) {
    SelenideElement licenseEditButton = ComponentLicenseFileDetailsPage.editButton();
    licenseEditButton.click();

    LicenseFileEditor editorModal = ComponentLicenseFileDetailsPage.licenseFileEditor();
    editorModal.licenseText(1).setValue(content);
    editorModal.saveButton().click();

    LicenseFileOverview licenseFileOverview = ComponentLicenseFileDetailsPage.licenseFileOverview();
    licenseFileOverview.getLicenseText().shouldHave(text(content));
  }
}
