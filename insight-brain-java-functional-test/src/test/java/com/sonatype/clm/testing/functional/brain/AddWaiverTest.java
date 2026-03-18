/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.IqVulnerabilityModal;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable;
import com.sonatype.clm.testing.functional.elements.ListWaiversTable.ListWaiversTableRow;
import com.sonatype.clm.testing.functional.elements.NxFormSelect.Option;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
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

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.selected;
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
  public void testVulnerabilityDetailsModal() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri("rest/vulnerability/details/json/sonatype-2017-0507");
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.vulnerabilityDetailsLink().shouldHave(text("See Security Vulnerability Details"));
    addWaiverPage.vulnerabilityDetailsLink().click();

    IqVulnerabilityModal vulnerabilityModal = addWaiverPage.vulnerabilityModal();
    vulnerabilityModal.shouldBe(visible);
    SelenideElement vulnerabilityDetails = vulnerabilityModal.vulnerabilityDetails();
    vulnerabilityDetails.shouldHave(text("sonatype-2017-0507"));
    vulnerabilityDetails.shouldHave(text("Sonatype CVSS 35.4"));
    vulnerabilityDetails.shouldHave(text("Sonatype Data Research"));
    vulnerabilityDetails.shouldHave(text("There is no non vulnerable version of this package. We recommend " +
        "investigating alternative components or a potential mitigating control."));
    vulnerabilityDetails.shouldHave(text("Root Cause " +
        "org.webjars:bootstrap:3.1.1META-INF/resources/webjars/bootstrap/3.1.1/js/bootstrap.js[3.1.1-1,3.1.1-2]"));
    vulnerabilityModal.closeButton().shouldHave(text("Close")).click();
    vulnerabilityModal.shouldNot(exist);
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
  public void testSubmit_ApplicationWaiver_AllComponents() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

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

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getHash()).isNull();
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_ApplicationWaiver_ExpiringWaiver() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Application - App 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.expiryTimesSelect().selectOptionContainingText("7 Days");
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getHash()).isNull();
    assertThat(waivers.get(0).getExpiryTime()).isNotNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_ApplicationWaiver_CustomExpiringWaiver() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Application - App 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.expiryTimesSelect().selectOptionContainingText("Custom");

    addWaiverPage.customExpiryTime().val("01-01-0001");
    addWaiverPage.customExpiryTimeErrorMessage().shouldHave(text("Date must be in the future"));

    addWaiverPage.customExpiryTime().val("01-01-9999");
    addWaiverPage.expiryTimeMessage().shouldBe(visible);
    chosenComponent.label().shouldHave(text("All Components"));
    addWaiverPage.comments().setValue("Some comments");

    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getHash()).isNull();
    assertThat(waivers.get(0).getExpiryTime()).isNotNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_ApplicationWaiver_WithReason() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Application - App 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();
    addWaiverPage.waiverReasonSelect().selectOptionContainingText("Mitigated externally");
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(application.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNotNull();
  }

  @Test
  public void testSubmit_OrgWaiver_SingleComponent() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();

    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(1, "Organization - Org 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(organization.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_OrgWaiver_AllComponents() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(1, "Organization - Org 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getHash()).isNull();
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_OrgWaiver_ExpiringWaiver() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(1, "Organization - Org 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();

    addWaiverPage.expiryTimesSelect().selectOptionContainingText("14 Days");

    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(organization.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNotNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_OrgWaiver_CustomExpiringWaiver() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(1, "Organization - Org 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();
    addWaiverPage.expiryTimesSelect().selectOptionContainingText("Custom");

    addWaiverPage.customExpiryTime().val("01-01-0001");
    addWaiverPage.customExpiryTimeErrorMessage().shouldHave(text("Date must be in the future"));

    addWaiverPage.customExpiryTime().val("01-01-9999");
    addWaiverPage.expiryTimeMessage().shouldBe(visible);
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(organization.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNotNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_OrgWaiver_WithReason() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();

    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(1, "Organization - Org 1"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();
    addWaiverPage.waiverReasonSelect().selectOptionContainingText("Acknowledged violation");
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(organization.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNotNull();
  }

  @Test
  public void testSubmit_ParentOrgWaiver_AllComponents() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(2, "Organization - Parent Org"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(2);
    chosenComponent.label().shouldHave(text("All Components"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(parentOrganization.getId());
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getHash()).isNull();
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_ParentOrgWaiver_SingleComponent() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();

    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(2, "Organization - Parent Org"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(parentOrganization.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_ParentOrgWaiver_ExpiringWaiver() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(2, "Organization - Parent Org"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();

    addWaiverPage.expiryTimesSelect().selectOptionContainingText("14 Days");

    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(parentOrganization.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNotNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_ParentOrgWaiver_CustomExpiringWaiver() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(2, "Organization - Parent Org"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();
    addWaiverPage.expiryTimesSelect().selectOptionContainingText("Custom");

    addWaiverPage.customExpiryTime().val("01-01-0001");
    addWaiverPage.customExpiryTimeErrorMessage().shouldHave(text("Date must be in the future"));

    addWaiverPage.customExpiryTime().val("01-01-9999");
    addWaiverPage.expiryTimeMessage().shouldBe(visible);
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(parentOrganization.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNotNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNull();
  }

  @Test
  public void testSubmit_ParentOrgWaiver_WithReason() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();

    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(2, "Organization - Parent Org"));
    addWaiverPage.availableComponents().shouldHave(size(3));
    NxRadio chosenComponent = addWaiverPage.component(0);
    chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
    chosenComponent.click();
    addWaiverPage.waiverReasonSelect().selectOptionContainingText("Acknowledged violation");
    addWaiverPage.comments().setValue("Some comments");
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getApplicableToComponent(parentOrganization.getId(), "hash1");
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNotNull();
  }

  @Test
  public void testSubmit_RootOrgWaiver_SingleComponent() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHave(size(4));
      addWaiverPage.availableScopesDropdown().chooseOption(new Option(3, "Organization - Root Organization"));
      addWaiverPage.availableComponents().shouldHave(size(3));
      NxRadio chosenComponent = addWaiverPage.component(0);
      chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
      chosenComponent.click();
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getApplicableToComponent(Organization.ROOT_ORGANIZATION_ID, "hash1");
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getExpiryTime()).isNull();
      assertThat(waivers.get(0).getWaiverReasonId()).isNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_RootOrgWaiver_WithReason() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHave(size(4));
      addWaiverPage.availableScopesDropdown().chooseOption(new Option(3, "Organization - Root Organization"));
      addWaiverPage.availableComponents().shouldHave(size(3));
      NxRadio chosenComponent = addWaiverPage.component(0);
      chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
      chosenComponent.click();
      addWaiverPage.waiverReasonSelect().selectOptionContainingText("Acknowledged violation");
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getApplicableToComponent(Organization.ROOT_ORGANIZATION_ID, "hash1");
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getExpiryTime()).isNull();
      assertThat(waivers.get(0).getWaiverReasonId()).isNotNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_RootOrgWaiver_AllComponents() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHave(size(4));
      addWaiverPage.availableScopesDropdown().chooseOption(new Option(3, "Organization - Root Organization"));
      addWaiverPage.availableComponents().shouldHave(size(3));
      NxRadio chosenComponent = addWaiverPage.component(2);
      chosenComponent.label().shouldHave(text("All Components"));
      chosenComponent.click();
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getActiveByOwnerId(Organization.ROOT_ORGANIZATION_ID);
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getHash()).isNull();
      assertThat(waivers.get(0).getExpiryTime()).isNull();
      assertThat(waivers.get(0).getWaiverReasonId()).isNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
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
  public void testUnsavedChangesModal_ContinueNavigation() {
    refreshOrOpen(AddWaiverPage.url(otherViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.comments().setValue("Changed comment");

    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();
    DashboardPage.dashboardContainer().shouldBe(visible);
  }

  @Test
  public void testUnsavedChangesModal_CancelNavigation() {
    refreshOrOpen(AddWaiverPage.url(otherViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.artifactName().shouldBe(visible);
    addWaiverPage.expiryTimesSelect().selectOptionContainingText("14 Days");

    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    addWaiverPage.artifactName().shouldBe(visible);
  }

  @Test
  public void testUnsavedChangesModal_RevertToPristineValues() {
    refreshOrOpen(AddWaiverPage.url(otherViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.artifactName().shouldBe(visible);
    addWaiverPage.expiryTimesSelect().selectOptionContainingText("14 Days");

    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.dashboardContainer().shouldNotBe(visible);
    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();

    addWaiverPage.expiryTimesSelect().selectOptionContainingText("Never");
    refreshOrOpen(DashboardPage.urlToViolations());
    unsavedChangesModal.shouldNotBe(visible);
    DashboardPage.dashboardContainer().shouldBe(visible);
  }

  @Test
  public void testApplicationPolicyCanOnlyBeScopedToApplication() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    Policy appPolicy = tempEntity.newPolicy(application.getId(), "Application Policy", 8);
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getId(), "scan3",
        false, false, Date.from(twoDaysAgo));
    PolicyViolation appLevelPolicyViolation = tempEntity.newPolicyViolation(policyEval, appPolicy, "Group3",
        "Artifact3", "Version3", "hash3", "sonatype-2019-0666");

    refreshOrOpen(AddWaiverPage.url(appLevelPolicyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    // there's only one possible scope for an application-policy: application
    addWaiverPage.availableScopes().shouldHave(size(1));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(0, "Application - App 1"));
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(appPolicy.getId());
  }

  @Test
  public void testOrganizationPolicyCanBeScopedToOrganization() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    Policy orgPolicy = tempEntity.newPolicy(organization.getId(), "Org Policy", 8);
    PolicyEvaluation policyEval = tempEntity.newPolicyEvaluation(application.getId(), StageTypes.BUILD.getId(), "scan4",
        false, false, Date.from(twoDaysAgo));
    PolicyViolation orgLevelPolicyViolation = tempEntity.newPolicyViolation(policyEval, orgPolicy, "Group4",
        "Artifact4", "Version4", "hash4", "sonatype-2020-0666");

    refreshOrOpen(AddWaiverPage.url(orgLevelPolicyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    // root-org shouldn't be available for org-level policies
    addWaiverPage.availableScopes().shouldHave(size(2));
    addWaiverPage.availableScopesDropdown().chooseOption(new Option(1, "Organization - Org 1"));
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.submitError().shouldNotBe(visible);

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(waivers.size()).isEqualTo(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(orgPolicy.getId());
  }

  @Test
  public void testOpenPageDirectly_cancelReturnsToViolationDetails() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));
    refresh(); // refresh to ensure there is no previous page/routing information

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));

    addWaiverPage.cancelButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    waitUntilUrl(ViolationDetailsPage.url(policyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.detailsTile().shouldBe(visible);
    violationDetailsPage.applicableWaiversTab().shouldBe(visible).shouldHave(text("Applicable Waivers"));
    violationDetailsPage.sidebarNav().sidebarNavItems().shouldHave(size(1));
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

  @Test
  public void testOpenPageFromViolationDetails_cancelReturnsToViolationDetails() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(policyViolation.getId(), "violation", "filter"));
    refresh(); // refresh to ensure there is no previous page/routing information

    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.detailsTile().addWaiverButton().shouldBe(visible);
    violationDetailsPage.detailsTile().addWaiverButton().click();

    waitUntilUrl(AddWaiverPage.url(policyViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHave(size(4));
    addWaiverPage.cancelButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    violationDetailsPage.detailsTile().shouldBe(visible);
    violationDetailsPage.detailsTile().addWaiverButton().shouldBe(visible);
    violationDetailsPage.applicableWaiversTab().shouldBe(visible).shouldHave(text("Applicable Waivers"));
    violationDetailsPage.sidebarNav().sidebarNavItems().shouldHave(size(2));
    violationDetailsPage.sidebarNav().navItem(1).shouldHave(cssClass("selected"));

    violationDetailsPage.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTable =
        violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.noWaiversMessage().shouldBe(visible);
  }

  @Test
  public void testOpenPageFromViolationDetails_submitReturnsToViolationDetails() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(policyViolation.getId(), "violation", "filter"));
    refresh(); // refresh to ensure there is no previous page/routing information

    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    violationDetailsPage.applicableWaiversTab().shouldBe(visible).shouldHave(text("Applicable Waivers"));
    violationDetailsPage.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTable =
        violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTable.noWaiversMessage().shouldBe(visible);

    violationDetailsPage.detailsTile().addWaiverSegmentedButton().shouldHave(cssClass("nx-btn--primary")).click();

    waitUntilUrl(AddWaiverPage.url(policyViolation.getId()));
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

    violationDetailsPage.detailsTile().shouldBe(visible);
    violationDetailsPage.detailsTile().addWaiverButton().shouldBe(visible);
    violationDetailsPage.applicableWaiversTab().shouldBe(visible).shouldHave(text("1 Applicable Waivers"));
    violationDetailsPage.detailsTile().addWaiverSegmentedButton().shouldHave(cssClass("nx-btn--secondary"));
    violationDetailsPage.sidebarNav().sidebarNavItems().shouldHave(size(2));
    violationDetailsPage.sidebarNav().navItem(1).shouldHave(cssClass("selected"));

    violationDetailsPage.applicableWaiversTab().click();
    ListWaiversTable applicableWaiversTableAfterWaiverSave =
        violationDetailsPage.applicableWaiversInfoTile().getApplicableWaiversTable();
    applicableWaiversTableAfterWaiverSave.rows().shouldHave(size(1));
    ListWaiversTableRow waiversTableRow = applicableWaiversTableAfterWaiverSave.row(1);
    waiversTableRow.comments().shouldHave(text("Some comments"));
    waiversTableRow.components().shouldHave(text("All"));
  }

  @Test
  public void testSubmit_WaiverWithPreloadedComment() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId(), "preloaded%20comments"));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.comments().shouldHave(text("preloaded comments"));
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getApplicableToComponent(application.getId(), "hash1");
      assertThat(waivers).hasSize(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getComment()).isEqualTo("preloaded comments");
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_WaiverWithPreloadedReason() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId(), null,
          "9b704ef5bc064fc29d7fe08a251ee9a6"));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.waiverReasonOptions().get(1).shouldBe(selected);
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getApplicableToComponent(application.getId(), "hash1");
      assertThat(waivers).hasSize(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getWaiverReasonId()).isEqualTo("9b704ef5bc064fc29d7fe08a251ee9a6");
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_WaiverWithPreloadedCommentAndReason() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId(), "preloaded%20comments",
          "9b704ef5bc064fc29d7fe08a251ee9a6"));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.comments().shouldHave(text("preloaded comments"));
      addWaiverPage.waiverReasonOptions().get(1).shouldBe(selected);
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getApplicableToComponent(application.getId(), "hash1");
      assertThat(waivers).hasSize(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getComment()).isEqualTo("preloaded comments");
      assertThat(waivers.get(0).getWaiverReasonId()).isEqualTo("9b704ef5bc064fc29d7fe08a251ee9a6");
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  private void cleanupCreatedWaivers(List<PolicyWaiver> waivers) {
    if (waivers != null && !waivers.isEmpty()) {
      waivers.forEach(policyWaiverDAO::delete);
    }
  }
}
