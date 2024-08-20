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
import com.sonatype.clm.testing.functional.pages.ComponentCopyrightDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentCopyrightDetailsPage.CopyrightEditor;
import com.sonatype.clm.testing.functional.pages.ComponentCopyrightDetailsPage.CopyrightFilePaths;
import com.sonatype.clm.testing.functional.pages.ComponentCopyrightDetailsPage.CopyrightList;
import com.sonatype.clm.testing.functional.pages.ComponentCopyrightDetailsPage.CopyrightOverview;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.textsInAnyOrder;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;

@Ignore
public class CopyrightDetailsTest
    extends AbstractFunctionalTest
{
  private Application app;

  private ComponentIdentifier componentId;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    init("033e7a20b23ea284d474", ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar"), "");
  }

  private void init(String hash, ComponentIdentifier componentIdentifier, String testFileSuffix) throws IOException {
    componentId = componentIdentifier;
    app = tempEntity.newApplicationWithParent(CopyrightDetailsTest.class.getSimpleName() + testFileSuffix,
        "app" + testFileSuffix, "org" + testFileSuffix);
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, hash, componentId);

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse" + testFileSuffix + ".json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCopyrightFilePaths" + testFileSuffix + ".json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment/filepaths");
  }

  @Test
  public void testCopyrightOverviewByHash() {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestCopyrightOverviewByComponentIdentifier();
    eyesWatcher.eyesCheck("Copyright Details Overview section");
  }

  @Test
  public void testCopyrightOverviewByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestCopyrightOverviewByComponentIdentifier();
  }

  private void doTestCopyrightOverviewByComponentIdentifier() {
    CopyrightOverview copyrightOverview = ComponentCopyrightDetailsPage.copyrightOverview();

    copyrightOverview.getAttributionReportStatus().shouldHave(text("Included"));
    copyrightOverview.getScope().shouldHave(text("Root Organization"));
    copyrightOverview.getSource().shouldHave(text("Sonatype Scan"));

    copyrightOverview.getCopyrightText().shouldHave(text("Copyright SomeDeveloper 2017"));
  }

  @Test
  public void testCopyrightPathClickByHash() {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestCopyrightPathClick();
  }

  @Test
  public void testCopyrightPathClickByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestCopyrightPathClick();
  }

  private void doTestCopyrightPathClick() {
    CopyrightFilePaths copyrightFilePaths = ComponentCopyrightDetailsPage.copyrightFilePaths();
    copyrightFilePaths.pathAt(1).shouldHave(cssClass("nx-collapsible-items--expanded"));
    copyrightFilePaths.getFilePath(1).shouldHave(text("path1"));
    copyrightFilePaths.getCopyrightContextText(1).shouldHave(
        text("Copyright SomeDeveloper 2019-2020 All Right reserved"));
    copyrightFilePaths.pathAt(1).$("button.nx-collapsible-items__trigger").click();
    copyrightFilePaths.pathAt(1).shouldNotHave(cssClass("nx-collapsible-items--expanded"));
    copyrightFilePaths.getCopyrightContextText(1).shouldBe(empty);
  }

  @Test
  public void testSelectDifferentCopyrightByHash() {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestSelectDifferentCopyright();
  }

  @Test
  public void testSelectDifferentCopyrightByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestSelectDifferentCopyright();
  }

  private void doTestSelectDifferentCopyright() {
    CopyrightList copyrightList = ComponentCopyrightDetailsPage.copyrightList();

    copyrightList.attributionInclusion(1).shouldHave(text("Included in attribution report"));
    copyrightList.getItemFileCount(1).shouldHave(text("Found in 2 file"));

    copyrightList.attributionInclusion(3).shouldHave(text("Included in attribution report"));
    copyrightList.getItemFileCount(3).shouldHave(text("Found in 2 files"));
  }

  @Test
  public void testSelectDifferentCopyrightAndVerifyFirstPathIsOpenByHash() {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestSelectDifferentCopyrightAndVerifyFirstPathIsOpen();
  }

  @Test
  public void testSelectDifferentCopyrightAndVerifyFirstPathIsOpenByComponentIdentifier()
      throws UnsupportedEncodingException
  {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestSelectDifferentCopyrightAndVerifyFirstPathIsOpen();
  }

  private void doTestSelectDifferentCopyrightAndVerifyFirstPathIsOpen() {
    CopyrightList copyrightList = ComponentCopyrightDetailsPage.copyrightList();
    copyrightList.getItemFileCount(3).click();
    CopyrightFilePaths copyrightFilePaths = ComponentCopyrightDetailsPage.copyrightFilePaths();
    copyrightFilePaths.pathAt(1).shouldHave(cssClass("nx-collapsible-items--expanded"));
  }

  @Test
  public void testExpandMultiplePathsByHash() {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestExpandMultiplePaths();
  }

  @Test
  public void testExpandMultiplePathsByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    doTestExpandMultiplePaths();
  }

  public void doTestExpandMultiplePaths() {
    CopyrightList copyrightList = ComponentCopyrightDetailsPage.copyrightList();
    copyrightList.getItemFileCount(3).click();
    CopyrightFilePaths copyrightFilePaths = ComponentCopyrightDetailsPage.copyrightFilePaths();
    copyrightFilePaths.pathAt(2).click();
    copyrightFilePaths.pathAt(1).shouldHave(cssClass("nx-collapsible-items--expanded"));
    copyrightFilePaths.pathAt(2).shouldHave(cssClass("nx-collapsible-items--expanded"));
  }

  @Test
  public void testEditCopyrightByHash() {
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByHash(app.getPublicId(), "033e7a20b23ea284d474", 0));
    doTestEditCopyright();
  }

  @Test
  public void testEditCopyrightByComponentIdentifier() throws IOException {
    init("02744a3ac66344569f0b", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2", "", "jar"), "2");
    refreshOrOpen(
        ComponentCopyrightDetailsPage.urlToApplicationScopeByComponentIdentifier(app.getPublicId(), componentId, 0));
    refresh();
    doTestEditCopyright();
  }

  private void doTestEditCopyright() {
    SelenideElement copyrightEditButton = ComponentCopyrightDetailsPage.copyrightEditButton();
    copyrightEditButton.click();

    CopyrightEditor editorModal = ComponentCopyrightDetailsPage.copyrightEditor();
    editorModal.copyrightText(1).setValue("Copyright SomeDeveloper 2017  Test test test");
    editorModal.saveButton().click();

    CopyrightList copyrightList = ComponentCopyrightDetailsPage.copyrightList();
    copyrightList.texts().shouldHave(textsInAnyOrder(
        "Copyright SomeDeveloper 2018-2019 All Right reserved",
        "Copyright SomeDeveloper 2019-2020",
        "Copyright SomeDeveloper 2017 Test test test"));

    CopyrightOverview copyrightOverview = ComponentCopyrightDetailsPage.copyrightOverview();
    copyrightOverview.getCopyrightText().shouldHave(text("Copyright SomeDeveloper 2017 Test test test"));
  }
}
