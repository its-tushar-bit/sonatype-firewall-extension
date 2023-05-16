/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardViolations;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.RequestWaiverPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class RequestWaiverTest
    extends AbstractFunctionalTest
{
  private Organization organization;

  private Application application;

  private PolicyViolation policyViolation;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void startup() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    policyViolation = tempEntity.newPolicyViolation(policyEvaluation1, securityPolicy1, "Group1",
        "Artifact1", "Version1", "hash1", "sonatype-2017-0507");
  }

  @Test
  public void testPageLayout() {
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.root().shouldBe(visible);
    requestWaiverPage.requestWaiverHeader().shouldHave(text("Request Waiver"));
    requestWaiverPage.root().shouldHave(text("To request a waiver, please share the Policy Violation ID and" +
            " sample curl command (found below) with the approver. Learn about automating waiver requests."));
    requestWaiverPage.requestWaiverReadOnlyData().shouldHave(text("Group1 : Artifact1 : Version1"));
    requestWaiverPage.requestWaiverReadOnlyData().shouldHave(text("Policy 1"));
    requestWaiverPage.requestWaiverReadOnlyData().shouldHave(text("Test Constraint"));
    requestWaiverPage.requestWaiverReadOnlyData().shouldHave(text("sonatype-2017-0507"));
    requestWaiverPage.requestWaiverPolicyViolationId().shouldHave(text(policyViolation.getId()));

    eyesWatcher.eyesCheck("Request waivers page");
  }

  @Test
  public void testBackButton() {
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.backButton().shouldHave(text("Back to Waivers")).click();
    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.shouldBe(visible);
  }

  @Test
  public void testBackButtonWhenNavigatedFromViolationDetails() {
    refreshOrOpen(DashboardPage.url());
    DashboardPage.violationsTab().click();
    DashboardViolations.ViolationsResults table = DashboardPage.violationsView().results();
    table.firstViolation().click();
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();
    tile.manageWaiversButton().click();
    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.shouldBe(visible);
    listWaiversPage.requestWaiverButton().click();
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.backButton().shouldHave(text("Back to Waivers")).click();
    listWaiversPage.shouldBe(visible);
  }

  @Test
  public void testBackButtonWhenNavFromComponentDetails() {
    //todo when the Add/Request Waiver segmented button gets added to the Violation Details Popover
  }
}
