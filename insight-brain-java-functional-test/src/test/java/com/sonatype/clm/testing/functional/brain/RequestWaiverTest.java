/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.RequestWaiverPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.selected;
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

  @Before
  public void init() {
    developerUser = tempEntity.newUser();
    Date twoDaysAgo = DateUtils.addDays(new Date(), -2);

    organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), SCAN_ID, false, false, twoDaysAgo);

    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, securityPolicy, "Group1",
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
    verifyRequestWaiverPage(requestWaiverPage);

    eyesWatcher.eyesCheck("Request waivers page");
  }

  @Test
  public void testSubmitButton() {
    loginAsLimitedUser();
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    verifyRequestWaiverPage(requestWaiverPage);

    requestWaiverPage.waiverReasonSelect().selectOptionContainingText("Acknowledged violation");
    requestWaiverPage.requestWaiverComments().setValue("Some comments");
    requestWaiverPage.requestWaiverNoteToReviewer().setValue("Some note to reviewer");
    requestWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    requestWaiverPage.submitError().shouldNotBe(visible);
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.shouldBe(visible);
  }

  @Test
  public void testSubmitError() {
    loginAsLimitedUser();
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    verifyRequestWaiverPage(requestWaiverPage);

    // save waiver request first time
    requestWaiverPage.requestWaiverComments().setValue("Some comments");
    requestWaiverPage.requestWaiverNoteToReviewer().setValue("Some note to reviewer");
    requestWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    requestWaiverPage.submitError().shouldNotBe(visible);

    // save waiver request second time
    refreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    requestWaiverPage.requestWaiverComments().setValue("Other comments");
    requestWaiverPage.requestWaiverNoteToReviewer().setValue("Other note to reviewer");
    requestWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    requestWaiverPage.submitError().shouldBe(visible);
    requestWaiverPage.submitError()
        .shouldHave(text("An error occurred saving data. This policy waiver request already exists."));
  }

  private void loginAsLimitedUser() {
    developerUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());
    login(developerUser.getUsername(), developerUser.getPassword());
  }

  private void verifyRequestWaiverPage(RequestWaiverPage requestWaiverPage) {
    requestWaiverPage.root().shouldBe(visible);
    requestWaiverPage.requestWaiverHeader().shouldHave(text("Request Waiver"));
    requestWaiverPage.requestWaiverTitle().shouldHave(text("Waiver Configuration"));

    requestWaiverPage.requestWaiverComponentName().shouldHave(text("Group1 : Artifact1 : Version1"));

    requestWaiverPage.requestWaiverPolicy().shouldHave(text("Policy"));
    requestWaiverPage.requestWaiverPolicy().shouldHave(text("Policy 1"));

    requestWaiverPage.requestWaiverConstraint().shouldHave(text("Constraint Name"));
    requestWaiverPage.requestWaiverConstraint().shouldHave(text("Test Constraint"));

    requestWaiverPage.requestWaiverConditions().shouldHave(text("Conditions"));
    requestWaiverPage.requestWaiverConditions().shouldHave(text("sonatype-2017-0507"));

    requestWaiverPage.requestWaiverScope().shouldHave(text("Scope"));
    requestWaiverPage.requestWaiverScopeOptions().shouldHave(size(3));
    requestWaiverPage.requestWaiverScopeOptions()
        .shouldHave(
            exactTexts("Application - App 1", "Organization - Org 1", "Organization - Root Organization"));
    requestWaiverPage.requestWaiverScopeOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverComponents().shouldHave(text("Components"));
    requestWaiverPage.requestWaiverComponentsOptions().shouldHave(size(3));
    requestWaiverPage.requestWaiverComponentsOptions()
        .shouldHave(
            exactTexts("Group1 : Artifact1 : Version1", "Group1 : Artifact1 (all versions)", "All Components"));
    requestWaiverPage.requestWaiverComponentsRadios().get(0).shouldBe(checked);

    requestWaiverPage.requestWaiverExpiryTime().shouldHave(text("Waiver Expiration"));
    requestWaiverPage.requestWaiverExpiryTimeOptions().shouldHave(size(8));
    requestWaiverPage.requestWaiverExpiryTimeOptions()
        .shouldHave(exactTexts("Never", "7 Days", "14 Days", "30 Days",
            "60 Days", "90 Days", "120 Days", "Custom"));
    requestWaiverPage.requestWaiverExpiryTimeOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverReason().shouldHave(text("Reason"));
    requestWaiverPage.requestWaiverReasonOptions()
        .shouldHave(
            exactTexts("Select a reason", "Acknowledged violation", "Evaluating component", "Mitigated externally",
                "No upgrade path", "Not exploitable", "Not reachable", "Researching", "Other"));
    requestWaiverPage.requestWaiverReasonOptions().get(0).shouldBe(selected);

    requestWaiverPage.requestWaiverComments().shouldBe(empty);
    requestWaiverPage.requestWaiverNoteToReviewer().shouldBe(empty);

    requestWaiverPage.saveButton().shouldBe(visible);
    requestWaiverPage.cancelButton().shouldBe(visible);
  }
}
