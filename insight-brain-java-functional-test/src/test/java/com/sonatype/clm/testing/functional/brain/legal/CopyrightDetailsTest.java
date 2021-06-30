/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
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
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.textsInAnyOrder;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;

public class CopyrightDetailsTest
    extends AbstractFunctionalTest
{
  private Application app;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    app = tempEntity.newApplicationWithParent(CopyrightDetailsTest.class.getSimpleName(), "app", "org");
    final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "033e7a20b23ea284d474",
        componentId);

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCopyrightFilePaths.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment/filepaths");

    refreshOrOpen(ComponentCopyrightDetailsPage.urlToApplicationScope(
        app.getPublicId(), "033e7a20b23ea284d474", 0));
  }

  @Test
  public void testCopyrightOverview() {
    refreshOrOpen(ComponentCopyrightDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));
    final CopyrightOverview copyrightOverview = ComponentCopyrightDetailsPage.copyrightOverview();

    copyrightOverview.getAttributionReportStatus().shouldHave(text("Included"));
    copyrightOverview.getScope().shouldHave(text("Root Organization"));
    copyrightOverview.getSource().shouldHave(text("Sonatype Scan"));

    copyrightOverview.getCopyrightText().shouldHave(text("Copyright SomeDeveloper 2017"));
    eyesWatcher.eyesCheck("Copyright Details Overview section");
  }

  @Test
  public void testCopyrightPathClick() {
    refreshOrOpen(ComponentCopyrightDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));
    final CopyrightFilePaths copyrightFilePaths = ComponentCopyrightDetailsPage.copyrightFilePaths();
    copyrightFilePaths.pathAt(1).shouldHave(cssClass("nx-tree-view--expanded"));
    copyrightFilePaths.getFilePath(1).shouldHave(text("path1"));
    copyrightFilePaths.getCopyrightContextText(1).shouldHave(
        text("Copyright SomeDeveloper 2019-2020 All Right reserved"));
    copyrightFilePaths.pathAt(1).$("button.nx-tree-view__trigger").click();
    copyrightFilePaths.pathAt(1).shouldNotHave(cssClass("nx-tree-view--expanded"));
    copyrightFilePaths.getCopyrightContextText(1).shouldHave(text(""));
  }

  @Test
  public void testSelectDifferentCopyright() {
    refreshOrOpen(ComponentCopyrightDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));

    final CopyrightList copyrightList = ComponentCopyrightDetailsPage.copyrightList();

    copyrightList.attributionInclusion(1).shouldHave(text("Included in attribution report"));
    copyrightList.getItemFileCount(1).shouldHave(text("Found in 2 file"));

    copyrightList.attributionInclusion(3).shouldHave(text("Included in attribution report"));
    copyrightList.getItemFileCount(3).shouldHave(text("Found in 2 files"));
  }

  @Test
  public void testSelectDifferentCopyrightAndVerifyFirstPathIsOpen() {
    refreshOrOpen(ComponentCopyrightDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));
    final CopyrightList copyrightList = ComponentCopyrightDetailsPage.copyrightList();
    copyrightList.getItemFileCount(3).click();
    final CopyrightFilePaths copyrightFilePaths = ComponentCopyrightDetailsPage.copyrightFilePaths();
    copyrightFilePaths.pathAt(1).shouldHave(cssClass("nx-tree-view--expanded"));
  }

  @Test
  public void testExpandMultiplePaths() {
    refreshOrOpen(ComponentCopyrightDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));
    final CopyrightList copyrightList = ComponentCopyrightDetailsPage.copyrightList();
    copyrightList.getItemFileCount(3).click();
    final CopyrightFilePaths copyrightFilePaths = ComponentCopyrightDetailsPage.copyrightFilePaths();
    copyrightFilePaths.pathAt(2).click();
    copyrightFilePaths.pathAt(1).shouldHave(cssClass("nx-tree-view--expanded"));
    copyrightFilePaths.pathAt(2).shouldHave(cssClass("nx-tree-view--expanded"));
  }

  @Test
  public void testEditCopyright() {
    refreshOrOpen(ComponentCopyrightDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));

    final SelenideElement copyrightEditButton = ComponentCopyrightDetailsPage.copyrightEditButton();
    copyrightEditButton.click();

    final CopyrightEditor editorModal = ComponentCopyrightDetailsPage.copyrightEditor();
    editorModal.copyrightText(1).setValue("Copyright SomeDeveloper 2017  Test test test");
    editorModal.saveButton().click();

    final CopyrightList copyrightList = ComponentCopyrightDetailsPage.copyrightList();
    copyrightList.texts().shouldHave(textsInAnyOrder(
        "Copyright SomeDeveloper 2018-2019 All Right reserved",
        "Copyright SomeDeveloper 2019-2020",
        "Copyright SomeDeveloper 2017 Test test test"));

    final CopyrightOverview copyrightOverview = ComponentCopyrightDetailsPage.copyrightOverview();
    copyrightOverview.getCopyrightText().shouldHave(text("Copyright SomeDeveloper 2017 Test test test"));
  }
}
