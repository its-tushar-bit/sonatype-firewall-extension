/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardViolations;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.RequestWaiverPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class RequestWaiverTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "scan1";

  private Organization organization;

  private Application application;

  private PolicyViolation policyViolation;

  private User developerUser;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
    logout();
  }

  @Before
  public void init() throws IOException {
    developerUser = tempEntity.newUser();
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(application, SCAN_ID, zippedReport, Configuration.baseUrl, work);
    evaluator.evaluatePolicy();

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), SCAN_ID, false, false, Date.from(twoDaysAgo));

    policyViolation = tempEntity.newPolicyViolation(policyEvaluation1, securityPolicy1, "Group1",
        "Artifact1", "Version1", "hash1", "sonatype-2017-0507");
    refreshOrOpen(DashboardPage.url());
  }

  @After
  public void cleanUp() {
    logout();
  }

  @Test
  public void testPageLayout() {
    loginAsLimitedUser();
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.root().shouldBe(visible);
    requestWaiverPage.requestWaiverHeader().shouldHave(text("Request Waiver"));
    requestWaiverPage.root().shouldHave(text(
        "A waiver request will be sent to the designated approver upon submit, if a webhook event for waiver" +
            " requests is configured. If you are unsure about the webhook configuration, share the policy violation" +
            " ID and the curl command with the designated approver."));
    requestWaiverPage.requestWaiverReadOnlyData().shouldHave(text("Group1 : Artifact1 : Version1"));
    requestWaiverPage.requestWaiverReadOnlyData().shouldHave(text("Policy 1"));
    requestWaiverPage.requestWaiverReadOnlyData().shouldHave(text("Test Constraint"));
    requestWaiverPage.requestWaiverReadOnlyData().shouldHave(text("sonatype-2017-0507"));
    requestWaiverPage.requestWaiverPolicyViolationId().shouldHave(text(policyViolation.getId()));
    requestWaiverPage.comments().shouldBe(empty);
    requestWaiverPage.saveButton().shouldBe(visible);
    requestWaiverPage.cancelButton().shouldBe(visible);

    eyesWatcher.eyesCheck("Request waivers page");
  }

  @Test
  public void testBackButton() {
    loginAsLimitedUser();
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.backButton().shouldHave(text("Back to Violation Details")).click();
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
  }

  @Test
  public void testCancelButton() {
    loginAsLimitedUser();
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.cancelButton().click();
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
  }

  @Test
  public void testBackButtonWhenNavigatedFromViolationDetails() {
    loginAsLimitedUser();
    refreshOrOpen(DashboardPage.url());
    DashboardPage.violationsTab().click();
    DashboardViolations.ViolationsResults table = DashboardPage.violationsView().results();
    table.firstViolation().click();
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();
    tile.requestWaiverButton().shouldHave(cssClass("nx-btn--primary")).click();
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.backButton().shouldHave(text("Back to Violation Details")).click();
    tile.shouldBe(visible);
  }

  @Test
  public void testCancelButtonWhenNavigatedFromViolationDetails() {
    loginAsLimitedUser();
    refreshOrOpen(DashboardPage.url());
    DashboardPage.violationsTab().click();
    DashboardViolations.ViolationsResults table = DashboardPage.violationsView().results();
    table.firstViolation().click();
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();
    tile.requestWaiverButton().click();
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.cancelButton().click();
    tile.shouldBe(visible);
  }

  @Test
  public void testBackButtonWhenNavFromComponentDetails() {
    String hash = "dc810b3d25f9e8c930f5";

    loginAsLimitedUser();
    refreshOrOpen(ApplicationReportPage.url(application, SCAN_ID));
    ApplicationReportPage reportPage = new ApplicationReportPage();
    reportPage.shouldBe(visible);
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.first();
    firstViolation.click();
    waitUntilUrl(ComponentDetailsPage.url(application, SCAN_ID, hash));
    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();

    componentDetailsPage.violationsTab().click();
    waitUntilUrl(ComponentDetailsPage.urlToViolations(application, SCAN_ID, hash));
    componentDetailsPage.violationsTabContent().shouldBe(visible);

    PolicyViolationsTable policyViolationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    policyViolationsTable.shouldBe(visible);
    SelenideElement firstRow = policyViolationsTable.getRow(1);
    firstRow.shouldBe(visible).click();

    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    violationDetailPopover.shouldBe(visible);
    violationDetailPopover.getRequestWaiversButton().shouldBe(visible).click();

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.requestWaiverHeader().shouldBe(visible);
    requestWaiverPage.backButton().click();

    waitUntilUrl(ComponentDetailsPage.urlToViolations(application, SCAN_ID, hash));
  }

  @Test
  public void testSubmitButtonAndDisabledBehavior() {
    loginAsLimitedUser();
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.waiverRequestWebhookWarning().shouldBe(visible).shouldHave(text(
        "Webhook event for Automatic Waiver Request is not configured." +
            " Contact your admin or request the waiver manually."));
    requestWaiverPage.saveButton().shouldHave(cssClass("disabled"));

    tempEntity.newWebhookWithSecret("http://localhost/webhook",
        Sets.newSet(WebhookEventType.WAIVER_REQUEST), "");
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    requestWaiverPage.waiverRequestWebhookWarning().shouldNotBe(visible);
    requestWaiverPage.comments().setValue("Some comments");
    requestWaiverPage.saveButton().shouldNotHave(cssClass("disabled")).click();
    NxSubmitMask.seeAndWaitForDismissal();
    requestWaiverPage.submitError().shouldNotBe(visible);
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
  }

  @Test
  public void testButtonStylingWithWaiverApplied() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
    DashboardPage.violationsTab().click();
    DashboardViolations.ViolationsResults table = DashboardPage.violationsView().results();
    table.firstViolation().click();
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();
    tile.addWaiverButton().click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Application - App 1"));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    violationDetailsPage.detailsTile().shouldBe(visible);
    logout();
    refreshOrOpen(DashboardPage.url());
    loginAsLimitedUser();
    DashboardPage.violationsTab().click();
    table = DashboardPage.violationsView().results();
    table.firstViolation().click();
    violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
    tile = new ViolationDetailsPage().detailsTile();
    tile.requestWaiverButton().shouldHave(cssClass("nx-btn--secondary")).click();
  }

  private void loginAsLimitedUser() {
    developerUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername()
    );
    login(developerUser.getUsername(), developerUser.getPassword());
  }
}
