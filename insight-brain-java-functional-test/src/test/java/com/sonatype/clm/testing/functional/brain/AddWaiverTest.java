/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable.ListWaiversTableRow;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class AddWaiverTest
    extends AbstractFunctionalTest
{
  private Organization organization;

  private Organization parentOrganization;

  private Application application;

  private PolicyViolation policyViolation;

  private PolicyViolation otherViolation;

  private PolicyWaiverDAO policyWaiverDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    parentOrganization = tempEntity.newOrganization("Parent Org");
    organization = tempEntity.newOrganization("Org 1", parentOrganization);
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy1 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);
    Policy securityPolicy2 = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 8);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    policyViolation = tempEntity.newPolicyViolation(policyEvaluation1, securityPolicy1, "Group1",
        "Artifact1", "Version1", "hash1", "sonatype-2017-0507");

    otherViolation = tempEntity.newPolicyViolation(policyEvaluation1, securityPolicy2, "Group2",
        "Artifact2", "Version2", "hash2", "sonatype-2018-0777");
  }

  @Test
  public void testPageLayout() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.artifactName().shouldHave(text("Artifact1"));
    addWaiverPage.componentName().shouldHave(text("Group1 : Artifact1 : Version1"));
    addWaiverPage.policyName().shouldHave(text("Policy 1"));
    addWaiverPage.constraintName().shouldHave(text("Test Constraint"));
    addWaiverPage.conditions().shouldHave(size(1));
    addWaiverPage.condition(1).shouldHave(text("sonatype-2017-0507"));
    addWaiverPage.vulnerabilityDetailsLink().shouldHave(text("See Security Vulnerability Details"));
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.scope(0).shouldHave(text("Application - App 1"));
    addWaiverPage.scope(1).shouldHave(text("Organization - Org 1"));
    addWaiverPage.scope(2).shouldHave(text("Organization - Parent Org"));
    addWaiverPage.scope(3).shouldHave(text("Organization - Root Organization"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    addWaiverPage.component(0).label().shouldHave(text("Group1 : Artifact1 : Version1"));
    addWaiverPage.component(1).label().shouldHave(text("Group1 : Artifact1 (all versions)"));
    addWaiverPage.component(2).label().shouldHave(text("All Components"));
    addWaiverPage.comments().shouldBe(empty);
    addWaiverPage.expiryTimesOptions().shouldHave(size(8));
    addWaiverPage.expiryTimesOptions().get(0).shouldHave(text("Never"));
    addWaiverPage.expiryTimesOptions().get(1).shouldHave(text("7 Days"));
    addWaiverPage.expiryTimesOptions().get(2).shouldHave(text("14 Days"));
    addWaiverPage.expiryTimesOptions().get(3).shouldHave(text("30 Days"));
    addWaiverPage.expiryTimesOptions().get(4).shouldHave(text("60 Days"));
    addWaiverPage.expiryTimesOptions().get(5).shouldHave(text("90 Days"));
    addWaiverPage.expiryTimesOptions().get(6).shouldHave(text("120 Days"));
    addWaiverPage.expiryTimesOptions().get(7).shouldHave(text("Custom"));
    addWaiverPage.expiryTimesSelect().getSelectedOption().shouldHave(text("Never"));
    addWaiverPage.waiverReasonOptions().shouldHave(size(9));
    addWaiverPage.waiverReasonOptions().get(0).shouldHave(text("Select a reason"));
    addWaiverPage.waiverReasonOptions().get(1).shouldHave(text("Acknowledged violation"));
    addWaiverPage.waiverReasonOptions().get(2).shouldHave(text("Evaluating component"));
    addWaiverPage.waiverReasonOptions().get(3).shouldHave(text("Mitigated externally"));
    addWaiverPage.waiverReasonOptions().get(4).shouldHave(text("No upgrade path"));
    addWaiverPage.waiverReasonOptions().get(5).shouldHave(text("Not exploitable"));
    addWaiverPage.waiverReasonOptions().get(6).shouldHave(text("Not reachable"));
    addWaiverPage.waiverReasonOptions().get(7).shouldHave(text("Researching"));
    addWaiverPage.waiverReasonOptions().get(8).shouldHave(text("Other"));
    addWaiverPage.waiverReasonSelect().getSelectedOption().shouldHave(text("Select a reason"));

    eyesWatcher.eyesCheck();

    addWaiverPage.currentUserName().scrollIntoView(true).shouldHave(text("Admin BuiltIn"));

    eyesWatcher.eyesCheck("add waiver form: scroll to \"created by\" field");
  }

  @Test
  public void testSubmit_ApplicationWaiver_SingleComponent() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Application - App 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(application.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmitError() {
    refreshOrOpen(AddWaiverPage.url(otherViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.comments().setValue("Changed comment");
    // save waiver the first time
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    refreshOrOpen(AddWaiverPage.url(otherViolation.getId()));
    addWaiverPage.comments().setValue("Modified comment");
    // attempt to save waiver a second time
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    // Waiver already exists so submit error should be visible.
    addWaiverPage.submitError().shouldBe(visible);
  }

  @Test
  public void testOpenPageDirectly_submitReturnsToViolationDetails() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));
    refresh(); // refresh to ensure there is no previous page/routing information

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Application - App 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    waitUntilUrl(ViolationDetailsPage.url(policyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.detailsTile().shouldBe(visible);
    violationDetailsPage.applicableWaiversTab().shouldBe(visible).shouldHave(text("1 Applicable Waivers"));
    violationDetailsPage.sidebarNav().sidebarNavItems().shouldHave(size(1));

    violationDetailsPage.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTable =
        violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.rows().shouldHave(size(1));
    ListWaiversTableRow waiversTableRow = applicableWaiversTable.row(1);
    waiversTableRow.comments().shouldHave(text("Some comments"));
    waiversTableRow.components().shouldHave(text("All"));
  }

}
