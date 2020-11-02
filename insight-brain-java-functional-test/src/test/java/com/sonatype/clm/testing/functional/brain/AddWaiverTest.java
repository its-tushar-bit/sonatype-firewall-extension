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
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.NxVulnerabilityModal;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
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
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class AddWaiverTest
    extends AbstractFunctionalTest
{
  private static Organization organization;

  private static Application application;

  private static PolicyViolation policyViolation;

  private static PolicyViolation otherViolation;

  private static PolicyWaiverDAO policyWaiverDAO;

  @BeforeClass
  public static void startup() {
    policyWaiverDAO = new PolicyWaiverDAO();
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    organization = staticTempEntity.newOrganization("Org 1");
    application = staticTempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy1 = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);
    Policy securityPolicy2 = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 8);

    PolicyEvaluation policyEvaluation1 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    policyViolation = staticTempEntity.newPolicyViolation(policyEvaluation1, securityPolicy1, "Group1",
        "Artifact1", "Version1", "hash1", "sonatype-2017-0507");

    otherViolation = staticTempEntity.newPolicyViolation(policyEvaluation1, securityPolicy2, "Group2",
        "Artifact2", "Version2", "hash2", "sonatype-2018-0777");

    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testPageLayout() {
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.artifactName().shouldHave(text("Artifact1"));
    addWaiverPage.componentName().shouldHave(text("Group1 : Artifact1 : Version1"));
    addWaiverPage.policyName().shouldHave(text("Policy 1"));
    addWaiverPage.constraintName().shouldHave(text("Test Constraint"));
    addWaiverPage.conditions().shouldHaveSize(1);
    addWaiverPage.condition(1).shouldHave(text("sonatype-2017-0507"));
    addWaiverPage.vulnerabilityDetailsLink().shouldHave(text("See Security Vulnerability Details"));
    addWaiverPage.availableScopes().shouldHaveSize(3);
    addWaiverPage.scope(0).label().shouldHave(text("Application - App 1"));
    addWaiverPage.scope(1).label().shouldHave(text("Organization - Org 1"));
    addWaiverPage.scope(2).label().shouldHave(text("Organization - Root Organization"));
    addWaiverPage.availableComponents().shouldHaveSize(2);
    addWaiverPage.component(0).label().shouldHave(text("Group1 : Artifact1 : Version1"));
    addWaiverPage.component(1).label().shouldHave(text("All Components"));
    addWaiverPage.comments().shouldHave(text(""));
    addWaiverPage.expiryTimesOptions().shouldHaveSize(7);
    addWaiverPage.expiryTimesOptions().get(0).shouldHave(text("Never"));
    addWaiverPage.expiryTimesOptions().get(1).shouldHave(text("7 Days"));
    addWaiverPage.expiryTimesOptions().get(2).shouldHave(text("14 Days"));
    addWaiverPage.expiryTimesOptions().get(3).shouldHave(text("30 Days"));
    addWaiverPage.expiryTimesOptions().get(4).shouldHave(text("60 Days"));
    addWaiverPage.expiryTimesOptions().get(5).shouldHave(text("90 Days"));
    addWaiverPage.expiryTimesOptions().get(6).shouldHave(text("120 Days"));
    addWaiverPage.expiryTimesSelect().getSelectedOption().shouldHave(text("Never"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testVulnerabilityDetailsModal() {
    testCLMServer.getHdsServer().respondWith(getClass().getResource("/vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri("rest/vulnerability/details/json/sonatype-2017-0507");
    refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.vulnerabilityDetailsLink().shouldHave(text("See Security Vulnerability Details"));
    addWaiverPage.vulnerabilityDetailsLink().click();

    NxVulnerabilityModal vulnerabilityModal = addWaiverPage.vulnerabilityModal();
    vulnerabilityModal.shouldBe(visible);
    SelenideElement vulnerabilityDetails = vulnerabilityModal.vulnerabilityDetails();
    vulnerabilityDetails.shouldHave(text("sonatype-2017-0507"));
    vulnerabilityDetails.shouldHave(text("Sonatype CVSS 3:5.4"));
    vulnerabilityDetails.shouldHave(text("Sonatype Data Research"));
    vulnerabilityDetails.shouldHave(text("There is no non vulnerable version of this package. We recommend " +
        "investigating alternative components or a potential mitigating control."));
    vulnerabilityDetails.shouldHave(text("Root Cause " +
        "org.webjars:bootstrap:3.1.1META-INF/resources/webjars/bootstrap/3.1.1/js/bootstrap.js[3.1.1-1,3.1.1-2]"));
    eyesWatcher.eyesCheck();
    vulnerabilityModal.closeButton().shouldHave(text("Close")).click();
    vulnerabilityModal.shouldNot(exist);
  }

  @Test
  public void testSubmit_ApplicationWaiver_SingleComponent() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(0);
      chosenScope.label().shouldHave(text("Application - App 1"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(0);
      chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
      chosenComponent.click();
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getApplicableToComponent(application.getId(), "hash1");
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getExpiryTime()).isNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_ApplicationWaiver_AllComponents() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(0);
      chosenScope.label().shouldHave(text("Application - App 1"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(1);
      chosenComponent.label().shouldHave(text("All Components"));
      chosenComponent.click();
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getHash()).isNull();
      assertThat(waivers.get(0).getExpiryTime()).isNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_ApplicationWaiver_ExpiringWaiver() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(0);
      chosenScope.label().shouldHave(text("Application - App 1"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(1);
      chosenComponent.label().shouldHave(text("All Components"));
      chosenComponent.click();
      addWaiverPage.expiryTimesSelect().selectOptionContainingText("7 Days");
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getHash()).isNull();
      assertThat(waivers.get(0).getExpiryTime()).isNotNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_OrgWaiver_SingleComponent() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(1);
      chosenScope.label().shouldHave(text("Organization - Org 1"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(0);
      chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
      chosenComponent.click();
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getApplicableToComponent(organization.getId(), "hash1");
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getExpiryTime()).isNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_OrgWaiver_AllComponents() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(1);
      chosenScope.label().shouldHave(text("Organization - Org 1"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(1);
      chosenComponent.label().shouldHave(text("All Components"));
      chosenComponent.click();
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getHash()).isNull();
      assertThat(waivers.get(0).getExpiryTime()).isNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_OrgWaiver_ExpiringWaiver() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(1);
      chosenScope.label().shouldHave(text("Organization - Org 1"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(0);
      chosenComponent.label().shouldHave(text("Group1 : Artifact1 : Version1"));
      chosenComponent.click();

      addWaiverPage.expiryTimesSelect().selectOptionContainingText("14 Days");

      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getApplicableToComponent(organization.getId(), "hash1");
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
      assertThat(waivers.get(0).getExpiryTime()).isNotNull();
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmit_RootOrgWaiver_SingleComponent() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(2);
      chosenScope.label().shouldHave(text("Organization - Root Organization"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
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
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(2);
      chosenScope.label().shouldHave(text("Organization - Root Organization"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(1);
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
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testSubmitError() {
    try {
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
    finally {
      cleanupCreatedWaivers(policyWaiverDAO.getActiveByOwnerId(application.getId()));
    }
  }

  @Test
  public void testApplicationPolicyCanOnlyBeScopedToApplication() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      Instant now = Instant.now();
      Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

      Policy appPolicy = staticTempEntity.newPolicy(application.getId(), "Application Policy", 8);
      PolicyEvaluation policyEval = staticTempEntity.newPolicyEvaluation(application.getId(),
          StageTypes.BUILD.getId(), "scan3", false, false, Date.from(twoDaysAgo));
      PolicyViolation appLevelPolicyViolation = staticTempEntity.newPolicyViolation(policyEval, appPolicy, "Group3",
          "Artifact3", "Version3", "hash3", "sonatype-2019-0666");

      refreshOrOpen(AddWaiverPage.url(appLevelPolicyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      // there's only one possible scope for an application-policy: application
      addWaiverPage.availableScopes().shouldHaveSize(1);
      NxRadio chosenScope = addWaiverPage.scope(0);
      chosenScope.label().shouldHave(text("Application - App 1"));
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getActiveByOwnerId(application.getId());
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(appPolicy.getId());
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testOrganizationPolicyCanBeScopedToOrganization() {
    List<PolicyWaiver> waivers = Collections.emptyList();
    try {
      Instant now = Instant.now();
      Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

      Policy orgPolicy = staticTempEntity.newPolicy(organization.getId(), "Org Policy", 8);
      PolicyEvaluation policyEval = staticTempEntity.newPolicyEvaluation(application.getId(),
          StageTypes.BUILD.getId(), "scan4", false, false, Date.from(twoDaysAgo));
      PolicyViolation orgLevelPolicyViolation = staticTempEntity.newPolicyViolation(policyEval, orgPolicy, "Group4",
          "Artifact4", "Version4", "hash4", "sonatype-2020-0666");

      refreshOrOpen(AddWaiverPage.url(orgLevelPolicyViolation.getId()));

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      // root-org shouldn't be available for org-level policies
      addWaiverPage.availableScopes().shouldHaveSize(2);
      NxRadio appScope = addWaiverPage.scope(0);
      appScope.label().shouldHave(text("Application - App 1"));
      NxRadio orgScope = addWaiverPage.scope(1);
      orgScope.label().shouldHave(text("Organization - Org 1"));
      orgScope.click();
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
      assertThat(waivers.size()).isEqualTo(1);
      assertThat(waivers.get(0).getPolicyId()).isEqualTo(orgPolicy.getId());
    }
    finally {
      cleanupCreatedWaivers(waivers);
    }
  }

  @Test
  public void testOpenPageDirectly_cancelReturnsToListWaiversThenBackReturnsToViolationDetails() {
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));
      refresh(); // refresh to ensure there is no previous page/routing information

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);

      addWaiverPage.cancelButton().click();
      NxSubmitMask.seeAndWaitForDismissal();

      waitUntilUrl(ListWaiversPage.url(policyViolation.getId()));
      ListWaiversPage listWaiversPage = new ListWaiversPage();
      listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
      listWaiversPage.backButton().shouldBe(visible).click();

      waitUntilUrl(ViolationDetailsPage.url(policyViolation.getId()));
      ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
      violationDetailsPage.detailsTile().shouldBe(visible);
      violationDetailsPage.detailsTile().manageWaiversButton().shouldBe(visible);
      violationDetailsPage.detailsTile().waiversIndicator().shouldBe(visible).shouldHave(text("0 Active Waivers"));
      violationDetailsPage.sidebarNav().sidebarNavItems().shouldHaveSize(1);
    }
    finally {
      cleanupCreatedWaivers(policyWaiverDAO.getActiveByOwnerId(application.getId()));
    }
  }

  @Test
  public void testOpenPageDirectly_submitReturnsToListWaiversThenBackReturnsToViolationDetails() {
    try {
      refreshOrOpen(AddWaiverPage.url(policyViolation.getId()));
      refresh(); // refresh to ensure there is no previous page/routing information

      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(0);
      chosenScope.label().shouldHave(text("Application - App 1"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(1);
      chosenComponent.label().shouldHave(text("All Components"));
      chosenComponent.click();
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waitUntilUrl(ListWaiversPage.url(policyViolation.getId()));
      ListWaiversPage listWaiversPage = new ListWaiversPage();
      listWaiversPage.waiverListTable().noWaiversMessage().shouldNotBe(visible);
      listWaiversPage.waiverListTable().rows().shouldHaveSize(1);
      listWaiversPage.waiverListTable().row(1).comments().shouldHave(text("Some comments"));
      listWaiversPage.backButton().shouldBe(visible).click();

      waitUntilUrl(ViolationDetailsPage.url(policyViolation.getId()));
      ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
      violationDetailsPage.detailsTile().shouldBe(visible);
      violationDetailsPage.detailsTile().manageWaiversButton().shouldBe(visible);
      violationDetailsPage.detailsTile().waiversIndicator().shouldBe(visible).shouldHave(text("1 Active Waiver"));
      violationDetailsPage.sidebarNav().sidebarNavItems().shouldHaveSize(1);
    }
    finally {
      cleanupCreatedWaivers(policyWaiverDAO.getActiveByOwnerId(application.getId()));
    }
  }

  @Test
  public void testOpenPageFromViolationDetails_cancelReturnsToListWaiversThenBackReturnsToViolationDetails() {
    try {
      refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(policyViolation.getId(), "violation", "filter"));
      refresh(); // refresh to ensure there is no previous page/routing information

      ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
      violationDetailsPage.detailsTile().manageWaiversButton().click();

      waitUntilUrl(ListWaiversPage.urlWithQueryParams(policyViolation.getId(), "violation", "filter"));
      ListWaiversPage listWaiversPage = new ListWaiversPage();
      listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
      listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();

      waitUntilUrl(AddWaiverPage.url(policyViolation.getId()));
      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      addWaiverPage.cancelButton().click();
      NxSubmitMask.seeAndWaitForDismissal();

      waitUntilUrl(ListWaiversPage.urlWithQueryParams(policyViolation.getId(), "violation", "filter"));
      listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
      listWaiversPage.backButton().shouldBe(visible).click();

      violationDetailsPage.detailsTile().shouldBe(visible);
      violationDetailsPage.detailsTile().manageWaiversButton().shouldBe(visible);
      violationDetailsPage.detailsTile().waiversIndicator().shouldBe(visible).shouldHave(text("0 Active Waivers"));
      violationDetailsPage.sidebarNav().sidebarNavItems().shouldHaveSize(3);
      violationDetailsPage.sidebarNav().navItem(2).shouldHave(cssClass("selected"));
    }
    finally {
      cleanupCreatedWaivers(policyWaiverDAO.getActiveByOwnerId(application.getId()));
    }
  }

  @Test
  public void testOpenPageFromViolationDetails_submitReturnsToListWaiversThenBackReturnsToViolationDetails() {
    try {
      refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(policyViolation.getId(), "violation", "filter"));
      refresh(); // refresh to ensure there is no previous page/routing information

      ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
      violationDetailsPage.detailsTile().manageWaiversButton().click();

      waitUntilUrl(ListWaiversPage.urlWithQueryParams(policyViolation.getId(), "violation", "filter"));
      ListWaiversPage listWaiversPage = new ListWaiversPage();
      listWaiversPage.waiverListTable().noWaiversMessage().shouldBe(visible);
      listWaiversPage.addWaiverButton().shouldBe(visible, enabled).click();

      waitUntilUrl(AddWaiverPage.url(policyViolation.getId()));
      AddWaiverPage addWaiverPage = new AddWaiverPage();
      addWaiverPage.availableScopes().shouldHaveSize(3);
      NxRadio chosenScope = addWaiverPage.scope(0);
      chosenScope.label().shouldHave(text("Application - App 1"));
      chosenScope.click();
      addWaiverPage.availableComponents().shouldHaveSize(2);
      NxRadio chosenComponent = addWaiverPage.component(1);
      chosenComponent.label().shouldHave(text("All Components"));
      chosenComponent.click();
      addWaiverPage.comments().setValue("Some comments");
      addWaiverPage.saveButton().click();
      NxSubmitMask.seeAndWaitForDismissal();
      addWaiverPage.submitError().shouldNotBe(visible);

      waitUntilUrl(ListWaiversPage.urlWithQueryParams(policyViolation.getId(), "violation", "filter"));
      listWaiversPage.waiverListTable().noWaiversMessage().shouldNotBe(visible);
      listWaiversPage.waiverListTable().rows().shouldHaveSize(1);
      listWaiversPage.waiverListTable().row(1).comments().shouldHave(text("Some comments"));
      listWaiversPage.backButton().shouldBe(visible).click();

      violationDetailsPage.detailsTile().shouldBe(visible);
      violationDetailsPage.detailsTile().manageWaiversButton().shouldBe(visible);
      violationDetailsPage.detailsTile().waiversIndicator().shouldBe(visible).shouldHave(text("1 Active Waiver"));
      violationDetailsPage.sidebarNav().sidebarNavItems().shouldHaveSize(2);
      violationDetailsPage.sidebarNav().navItem(1).shouldHave(cssClass("selected"));
    }
    finally {
      cleanupCreatedWaivers(policyWaiverDAO.getActiveByOwnerId(application.getId()));
    }
  }

  private void cleanupCreatedWaivers(List<PolicyWaiver> waivers) {
    if (waivers != null && !waivers.isEmpty()) {
      waivers.forEach(policyWaiverDAO::delete);
    }
  }
}
