
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.componentdetails.EditLicensesPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.LicenseDetectionsTile;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class ComponentDetailsEditLicensesTest
    extends AbstractFunctionalTest
{
  private static final ComponentIdentifier JAVANCSS_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("javancss",
      "javancss", "29.50", "", "jar");

  private static final String JAVANCSS_HASH = "9aba4af169a1a3baa67f";

  private static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private Application app;

  private TestReportEvaluator evaluator;

  @Before
  public void start() throws IOException {
    Organization org = tempEntity.newOrganization("ApplicationReportTest");
    app = tempEntity.newApplication("ApplicationReportTest",
        "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testLegalTab_editLicensesPopover() {
    mockHdsResponseForFirstComponent();
    refreshOrOpen(ComponentDetailsPage.urlToLegal(app, SCAN_ID, JAVANCSS_HASH));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.legalTabContent().shouldBe(visible);

    LicenseDetectionsTile licenseDetectionsTile = componentDetailsPage.legalTabContent().licenseDetectionsTile();
    licenseDetectionsTile.shouldBe(visible);
    licenseDetectionsTile.editLicenseButton().click();

    // License Info Section
    EditLicensesPopover editLicensesPopover = new EditLicensesPopover();
    editLicensesPopover.shouldBe(visible);
    editLicensesPopover.popoverTitle().shouldHave(text("Edit Licenses"));

    ElementsCollection declaredLicenses = editLicensesPopover.getItems(editLicensesPopover.declaredLicenses());
    declaredLicenses.shouldHaveSize(1);
    declaredLicenses.first().shouldHave(text("Apache-2.0"));

    ElementsCollection observedLicenses = editLicensesPopover.getItems(editLicensesPopover.observedLicenses());
    observedLicenses.shouldHaveSize(1);
    observedLicenses.first().shouldHave(text("GPL-2.0"));

    ElementsCollection effectiveLicenses = editLicensesPopover.getItems(editLicensesPopover.effectiveLicenses());
    effectiveLicenses.shouldHaveSize(2);
    effectiveLicenses.first().shouldHave(text("Apache-2.0"));
    effectiveLicenses.last().shouldHave(text("GPL-2.0"));

    NxRadio firstScope = editLicensesPopover.scope(0);
    NxRadio thirdScope = editLicensesPopover.scope(2);
    SelenideElement statusSelect = editLicensesPopover.status();
    Button saveButton = editLicensesPopover.saveButton();

    // Default states
    editLicensesPopover.availableScopes()
        .shouldHave(texts("Application - ApplicationReportTest", "Organization - ApplicationReportTest",
            "Organization - Root Organization"));
    thirdScope.label().shouldHave(text("Organization - Root Organization"));
    editLicensesPopover.statuses().shouldHave(
        texts("Open", "Acknowledged", "Confirmed", "Inherit Status (Open)"));
    statusSelect.getSelectedOption().shouldHave(value("Open"));
    saveButton.shouldBe(CLM.DISABLED);

    // Update to 'Acknowledged' status for Root Organization
    thirdScope.click();
    statusSelect.selectOptionContainingText("Acknowledged");
    editLicensesPopover.comment().setValue("Some comments");
    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Update to 'Confirmed' status for Application
    firstScope.click();
    statusSelect.selectOptionContainingText("Confirmed");
    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Check UI for Application Override
    firstScope.shouldBe(selected);
    statusSelect.getSelectedOption().shouldHave(value("Confirmed"));

    // Check backend for Application override
    final LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    LicenseOverride override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), JAVANCSS_IDENTIFIER);
    assertThat(override.getStatus()).isEqualTo(LicenseOverrideStatus.CONFIRMED);

    // Remove Override from Application
    firstScope.click();
    statusSelect.selectOptionContainingText("Inherit Status (Acknowledged)");
    saveButton.shouldBe(enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();

    // Verify Application override has been removed on backend
    override =
        licenseOverrideDAO.getByOwnerIdAndComponentIdentifier(app.getId(), JAVANCSS_IDENTIFIER);
    assertThat(override).isNull();

    eyesWatcher.eyesCheck("component details legal tab edit licenses popover");

    editLicensesPopover.getCloseButton().click();
    editLicensesPopover.shouldNotBe(visible);
  }

  private void mockHdsResponseForFirstComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }
}
