/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Objects;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationAttributionReportPage;
import com.sonatype.clm.testing.functional.pages.AttributionReportFormPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Condition;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.switchTo;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class ApplicationAttributionReportTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  public static final String APACHE_TILES_PURL =
      "pkg\\:maven\\/org\\.apache\\.tiles\\/tiles-core\\@2\\.2\\.2\\?type\\=jar";

  public static final String SPRING_SECURITY_PURL =
      "pkg\\:maven\\/org\\.springframework\\.security\\/spring-security-web\\@3\\.2\\.4\\.RELEASE\\?type\\=jar";

  // The box.json contains 2 invalid component identifiers, so there are 62 valid only.
  public static final int EXPECTED_COUNT_OF_COMPONENTS = 63;

  private final ApplicationAttributionReportPage reportPage = new ApplicationAttributionReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(Objects.requireNonNull(
                this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(
                Objects.requireNonNull(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(Objects.requireNonNull(this.getClass()
                .getResourceAsStream("/legal/ApplicationAttributionReportTest-legalFileHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(Objects.requireNonNull(this.getClass()
                .getResourceAsStream("/legal/legalSourceLinkHdsResponse.json")),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/source-link");

    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ApplicationReportTest", "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
  }

  @Test
  public void testReportLoadedWithAllDefaults() {
    refreshOrOpen(ApplicationAttributionReportPage.url(app, Stage.ID_BUILD));
    reportPage.reportTitle().shouldHave(exactText("Attribution Report for " + app.getName()));
  }

  @Test
  public void testReportLoadedFormDefaults() {
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();

    attrReportFormPage.getFormSubmitBtn().click();

    waitForReportToGenerate();

    reportPage.reportTitle().shouldHave(exactText("Attribution Report for " + app.getName()));
    reportPage.tableOfContents().shouldBe(Condition.visible);
    reportPage.appendix().shouldBe(Condition.visible);
    reportPage.header().shouldNotBe(Condition.visible);
    reportPage.footer().shouldNotBe(Condition.visible);
    reportPage.additionalNotices().shouldNotBe(Condition.visible);
    reportPage.componentElements().shouldHave(size(EXPECTED_COUNT_OF_COMPONENTS));

    reportPage.findComponentFor(APACHE_TILES_PURL).should(Condition.text("Notice content"));
    reportPage.findComponentFor(APACHE_TILES_PURL).should(Condition.text("License content"));

    reportPage.findComponentFor(SPRING_SECURITY_PURL)
        .should(Condition.not(Condition.text("Standard License Text")));
    reportPage.appendixStandardLicenseText("Apache-2\\.0").should(Condition.visible);
  }

  @Test
  public void testReport_additionalNotices() throws IOException {
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();

    File file1 = Files.createTempFile("file1", ".txt").toFile();
    file1.deleteOnExit();

    File file2 = Files.createTempFile("file2", ".txt").toFile();
    file2.deleteOnExit();

    final String firstFileContent = "First file content";
    final String secondFileContent = "Some other notice";

    FileUtils.writeStringToFile(file1, firstFileContent, StandardCharsets.UTF_8);
    FileUtils.writeStringToFile(file2, secondFileContent, StandardCharsets.UTF_8);

    attrReportFormPage.getFileInputs().get(0).uploadFile(file1);
    attrReportFormPage.getFileInputs().get(1).uploadFile(file2);

    attrReportFormPage.getFormSubmitBtn().click();

    waitForReportToGenerate();

    reportPage.reportTitle().shouldHave(exactText("Attribution Report for " + app.getName()));
    reportPage.tableOfContents().shouldBe(Condition.visible);
    reportPage.appendix().shouldBe(Condition.visible);
    reportPage.header().shouldNotBe(Condition.visible);
    reportPage.footer().shouldNotBe(Condition.visible);
    reportPage.componentElements().shouldHave(size(EXPECTED_COUNT_OF_COMPONENTS));

    reportPage.additionalNotices().shouldBe(Condition.visible);
    reportPage.additionalNotices().shouldHave(Condition.text(firstFileContent));
    reportPage.additionalNotices().shouldHave(Condition.text(secondFileContent));
  }

  @Test
  public void testReportLoaded_NoTableOfContent() {
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();

    attrReportFormPage.getTableOfContentsCheck().click();
    attrReportFormPage.getTitleInput().setValue("My Custom report Title");
    attrReportFormPage.getHeaderInput().setValue("My Header");
    attrReportFormPage.getFooterInput().setValue("My Footer");

    attrReportFormPage.getFormSubmitBtn().click();

    waitForReportToGenerate();

    eyesWatcher.eyesCheck("Legal Attribution Report - No table of contents");

    reportPage.reportTitle().shouldHave(exactText("My Custom report Title"));
    reportPage.tableOfContents().shouldNotBe(Condition.visible);
    reportPage.appendix().shouldBe(Condition.visible);
    reportPage.header().shouldBe(Condition.visible);
    reportPage.header().should(Condition.text("My Header"));
    reportPage.footer().shouldBe(Condition.visible);
    reportPage.footer().should(Condition.text("My Footer"));

    reportPage.componentElements().shouldHave(size(EXPECTED_COUNT_OF_COMPONENTS));

    reportPage.findComponentFor(APACHE_TILES_PURL).should(Condition.text("Notice content"));
    reportPage.findComponentFor(APACHE_TILES_PURL).should(Condition.text("License content"));

    reportPage.findComponentFor(SPRING_SECURITY_PURL).shouldNot(Condition.text("Standard License Text"));
    reportPage.appendixStandardLicenseText("Apache-2\\.0").should(Condition.visible);
  }

  @Test
  public void testReport_noStandardLicense() {
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();

    attrReportFormPage.getIncludeStandardLicenseTextsCheck().click();
    attrReportFormPage.getFormSubmitBtn().click();

    waitForReportToGenerate();

    eyesWatcher.eyesCheck("Legal Attribution Report - No standard license");

    reportPage.reportTitle().shouldHave(exactText("Attribution Report for " + app.getName()));
    reportPage.tableOfContents().shouldBe(Condition.visible);
    reportPage.appendix().shouldNotBe(Condition.visible);
    reportPage.header().shouldNotBe(Condition.visible);
    reportPage.footer().shouldNotBe(Condition.visible);
    reportPage.componentElements().shouldHave(size(EXPECTED_COUNT_OF_COMPONENTS));

    reportPage.findComponentFor(APACHE_TILES_PURL).should(Condition.text("Notice content"));
    reportPage.findComponentFor(APACHE_TILES_PURL).should(Condition.text("License content"));

    reportPage.findComponentFor(SPRING_SECURITY_PURL).shouldNot(Condition.text("Standard License Text"));
    reportPage.appendixStandardLicenseText("Apache-2\\.0").shouldNot(Condition.visible);
  }

  @Test
  public void testReport_noAppendix() {
    refreshOrOpen(AttributionReportFormPage.url(app.getPublicId(), BuildStageType.ID));
    AttributionReportFormPage attrReportFormPage = new AttributionReportFormPage();

    attrReportFormPage.getAppendixCheck().click();
    attrReportFormPage.getFormSubmitBtn().click();

    waitForReportToGenerate();

    reportPage.reportTitle().shouldHave(exactText("Attribution Report for " + app.getName()));
    reportPage.tableOfContents().shouldBe(Condition.visible);
    reportPage.appendix().shouldNotBe(Condition.visible);
    reportPage.header().shouldNotBe(Condition.visible);
    reportPage.footer().shouldNotBe(Condition.visible);
    reportPage.componentElements().shouldHave(size(EXPECTED_COUNT_OF_COMPONENTS));

    reportPage.findComponentFor(APACHE_TILES_PURL).should(Condition.text("Notice content"));
    reportPage.findComponentFor(APACHE_TILES_PURL).should(Condition.text("License content"));

    reportPage.findComponentFor(SPRING_SECURITY_PURL).should(Condition.text("Standard License Text"));
    reportPage.appendixStandardLicenseText("Apache-2\\.0").shouldNot(Condition.visible);
  }

  private void waitForReportToGenerate() {
    Wait<WebDriver> wait = new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(5))
        .ignoring(NoSuchElementException.class);
    wait.until(ExpectedConditions.numberOfWindowsToBe(2));
    switchTo().window(1);
    Assert.assertTrue(getWebDriver().getCurrentUrl()
        .matches(".*licenseLegalMetadata/application/ApplicationReportTest/stage/build/report"));
  }
}
