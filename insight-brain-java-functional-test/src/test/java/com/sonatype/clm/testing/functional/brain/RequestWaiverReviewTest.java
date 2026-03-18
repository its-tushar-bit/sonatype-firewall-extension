/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestsResults;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversResults;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.RequestWaiverReviewPage;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

public class RequestWaiverReviewTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "scan1";

  private Organization organization;

  private Application application;

  private Policy securityPolicy;

  private ComponentIdentifier componentIdentifier;

  private String purl;

  private PolicyViolation policyViolation;

  private String waiverReason;

  private PolicyWaiverRequest policyWaiverRequest;

  private PolicyWaiverDAO policyWaiverDAO;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private final Date now = new Date();

  private final Date twoDaysAgo = DateUtils.addDays(now, -2);

  private final Date threeDaysFromNow = DateUtils.addDays(now, 3);

  private static final WaiverRequestsResults dashboardWaiverRequestTable = DashboardPage.waiverRequestsView().results();

  private static final WaiversResults dashboardWaiverTable = DashboardPage.waiversView().results();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);
    policyWaiverRequestDAO = lookup(PolicyWaiverRequestDAO.class);

    organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    securityPolicy = tempEntity.newPolicy(ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), SCAN_ID, false, false, twoDaysAgo);
    componentIdentifier = ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1", "", "jar");
    purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, securityPolicy, componentIdentifier, "hash1",
        "sonatype-2017-0507");

    waiverReason = "9b704ef5bc064fc29d7fe08a251ee9a6"; // Acknowledged violation

    policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash1")
        .setPolicyId(securityPolicy.getId())
        .setPolicyViolationId(policyViolation.getId())
        .setOwnerId(organization.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setWaiverReasonId(waiverReason) // Acknowledged violation
        .setComment("Comment 1")
        .setNoteToReviewer("Note to Reviewer 1")
        .setRequestTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setRequesterId("testuser1")
        .setRequesterName("Test User 1")
        .setComponentUpgradeAvailable(false));

    refreshOrOpen(RequestWaiverReviewPage.url(organization.getType().toString(), organization.getId(),
        policyWaiverRequest.getId()));
    RequestWaiverReviewPage.waitUntilSpinnersGone();
  }

  @Test
  public void testPageLayout() {
    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    // Verify that the elements are loaded with the Waiver Request details
    verifyRequestWaiverReviewPage(requestWaiverReviewPage);
  }

  @Test
  public void testPageLayout_FromDashboard() {
    refreshOrOpen(DashboardPage.urlToWaiverRequests());
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);
    dashboardWaiverRequestTable.firstWaiverRequest().click();

    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    // Verify that the elements are loaded with the Waiver Request details
    verifyRequestWaiverReviewPage(requestWaiverReviewPage);
  }

  @Test
  public void testPageLayout_404() {
    String invalidId = "invalid-id";
    refreshOrOpen(RequestWaiverReviewPage.url(organization.getType().toString(), organization.getId(), invalidId));
    RequestWaiverReviewPage.waitUntilSpinnersGone();

    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    requestWaiverReviewPage.root().shouldBe(visible);
    requestWaiverReviewPage.requestWaiverReviewAlert()
        .shouldHave(text("An error occurred loading data. " +
            "Cannot find a policy waiver request with ID " + invalidId +
            " for owner " + policyWaiverRequest.getOwnerId() + "."));
  }

  @Test
  public void testBackButton() {
    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    requestWaiverReviewPage.backButton().shouldHave(text("Back to Waiver Requests")).click();

    WaiverRequestTile waiverRequestTile = verifyDashboardWaiverRequest(0, policyWaiverRequest);
    waiverRequestTile.scope().shouldHave(text("Organization - Org 1"));
    waiverRequestTile.status().shouldHave(text("Requested"));
  }

  @Test
  public void testCancelButton() {
    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    requestWaiverReviewPage.cancelButton().click();

    WaiverRequestTile waiverRequestTile = verifyDashboardWaiverRequest(0, policyWaiverRequest);
    waiverRequestTile.scope().shouldHave(text("Organization - Org 1"));
    waiverRequestTile.status().shouldHave(text("Requested"));
  }

  @Test
  public void testApproveButton_ApproveWaiverRequestAndCreatesWaiverWithNoUpdatedValues() {
    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    requestWaiverReviewPage.approveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // The waiver request values don't change and the status is updated to "Approved"
    PolicyWaiverRequest waiverRequest = verifyWaiverRequestInDB(policyWaiverRequest);
    assertThat(waiverRequest.getStatus()).isEqualTo(PolicyWaiverRequestStatus.APPROVED);

    WaiverRequestTile waiverRequestTile = verifyDashboardWaiverRequest(0, policyWaiverRequest);
    waiverRequestTile.scope().shouldHave(text("Organization - Org 1"));
    waiverRequestTile.status().shouldHave(text("Approved"));

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);
    assertThat(formatDate(waivers.get(0).getExpiryTime())).isEqualTo(formatDate(policyWaiverRequest.getExpiryTime()));
    assertThat(waivers.get(0).getWaiverReasonId()).isEqualTo(waiverReason);
    assertThat(waivers.get(0).getComment()).isEqualTo(policyWaiverRequest.getComment());

    WaiverTile waiver = verifyDashboardWaivers();
    waiver.scope().shouldHave(text("Organization - Org 1"));
    waiver.component().shouldHave(text("Group1 : Artifact1 : Version1"));
  }

  @Test
  public void testApproveButton_ApproveWaiverRequestAndCreatesWaiverWithRootOrgScopeAndNoUpdatedValues() {
    PolicyWaiverRequest policyWaiverRequest2 = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash2")
        .setPolicyId(securityPolicy.getId())
        .setPolicyViolationId(policyViolation.getId())
        .setOwnerId(ROOT_ORGANIZATION_ID)
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setWaiverReasonId(waiverReason) // Acknowledged violation
        .setComment("Comment 2")
        .setNoteToReviewer("Note to Reviewer 2")
        .setRequestTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setRequesterId("admin")
        .setRequesterName("Admin User")
        .setComponentUpgradeAvailable(false));

    refreshOrOpen(RequestWaiverReviewPage.url("organization", ROOT_ORGANIZATION_ID,
        policyWaiverRequest2.getId()));
    RequestWaiverReviewPage.waitUntilSpinnersGone();

    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    requestWaiverReviewPage.approveButton().shouldBe(visible);
    requestWaiverReviewPage.approveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // The waiver request values don't change and the status is updated to "Approved"
    PolicyWaiverRequest waiverRequest = verifyWaiverRequestInDB(policyWaiverRequest2);
    assertThat(waiverRequest.getStatus()).isEqualTo(PolicyWaiverRequestStatus.APPROVED);

    WaiverRequestTile waiverRequestTile = verifyDashboardWaiverRequest(1, policyWaiverRequest2);
    waiverRequestTile.scope().shouldHave(text("Root Organization"));
    waiverRequestTile.status().shouldHave(text("Approved"));

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(ROOT_ORGANIZATION_ID);
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getComponentMatchStrategy()).isEqualTo(EXACT_COMPONENT);
    assertThat(formatDate(waivers.get(0).getExpiryTime())).isEqualTo(formatDate(policyWaiverRequest2.getExpiryTime()));
    assertThat(waivers.get(0).getWaiverReasonId()).isEqualTo(waiverReason);
    assertThat(waivers.get(0).getComment()).isEqualTo(policyWaiverRequest2.getComment());

    WaiverTile waiver = verifyDashboardWaivers();
    waiver.scope().shouldHave(text("Root Organization"));
    waiver.component().shouldHave(text("Group1 : Artifact1 : Version1"));
  }

  @Test
  public void testApproveButton_ApproveWaiverRequestAndCreatesWaiverWithUpdatedValues() {
    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();

    // Updated scope
    requestWaiverReviewPage.requestWaiverScopeSelect().selectOptionContainingText("Organization - Org 1");

    // Update matcher strategy (all versions)
    NxRadio chosenComponent = requestWaiverReviewPage.requestWaiverComponent(1);
    chosenComponent.click();

    // Update expiry time
    requestWaiverReviewPage.requestWaiverExpiryTimesSelect().selectOptionContainingText("Never");

    // Update reason
    requestWaiverReviewPage.requestWaiverReasonSelect().selectOptionContainingText("Other");

    // Update comment
    requestWaiverReviewPage.requestWaiverComments().setValue("Updated Comments from review");

    requestWaiverReviewPage.approveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // The waiver request values don't change and the status is updated to "Approved"
    PolicyWaiverRequest waiverRequest = verifyWaiverRequestInDB(policyWaiverRequest);
    assertThat(waiverRequest.getStatus()).isEqualTo(PolicyWaiverRequestStatus.APPROVED);

    WaiverRequestTile waiverRequestTile = verifyDashboardWaiverRequest(0, policyWaiverRequest);
    waiverRequestTile.scope().shouldHave(text("Organization - Org 1"));
    waiverRequestTile.status().shouldHave(text("Approved"));

    List<PolicyWaiver> waivers = policyWaiverDAO.getActiveByOwnerId(organization.getId());
    assertThat(waivers).hasSize(1);
    assertThat(waivers.get(0).getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(waivers.get(0).getComponentMatchStrategy()).isEqualTo(ALL_VERSIONS);
    assertThat(waivers.get(0).getExpiryTime()).isNull();
    assertThat(waivers.get(0).getWaiverReasonId()).isNotNull();
    assertThat(waivers.get(0).getComment()).isEqualTo("Updated Comments from review");

    WaiverTile waiver = verifyDashboardWaivers();
    waiver.scope().shouldHave(text("Organization - Org 1"));
    waiver.component().shouldHave(text("Group1 : Artifact1 (all versions)"));
  }

  @Test
  public void testReadOnlyElements_ApprovedWaiverRequest() {
    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    verifyRequestWaiverReviewPage(requestWaiverReviewPage);

    requestWaiverReviewPage.approveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // re-open the requested waiver which is already approved
    refreshOrOpen(RequestWaiverReviewPage.url(organization.getType().toString(), organization.getId(),
        policyWaiverRequest.getId()));
    RequestWaiverReviewPage.waitUntilSpinnersGone();

    verifyDisabledElements(requestWaiverReviewPage);
  }

  @Test
  public void testReadOnlyElements_UserWithoutPermission() {
    logout();
    loginAsLimitedUser();
    try {
      refreshOrOpen(RequestWaiverReviewPage.url(organization.getType().toString(), organization.getId(),
          policyWaiverRequest.getId()));
      RequestWaiverReviewPage.waitUntilSpinnersGone();

      RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
      verifyRequestWaiverReviewPage(requestWaiverReviewPage);

      verifyDisabledElements(requestWaiverReviewPage);
    }
    finally {
      // For the next test
      logout();
      loginAsAdmin();
    }
  }

  @Test
  public void testRejectButton_OpensModalThenCancel() {
    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();
    requestWaiverReviewPage.rejectButton().click();
    verifyWaiverRequestRejectModal(requestWaiverReviewPage);

    requestWaiverReviewPage.cancelRejectionButton().click();

    // The waiver request field values should remain unchanged
    PolicyWaiverRequest waiverRequest = policyWaiverRequestDAO.getById(policyWaiverRequest.getId());
    JPA.assertEntityEquals(waiverRequest, policyWaiverRequest);
    verifyRequestWaiverReviewPage(requestWaiverReviewPage);
  }

  @Test
  public void testRejectButton_OpensModalThenRejectsWaiverRequestAndDisplayInDashboard() {
    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();

    requestWaiverReviewPage.rejectButton().click();

    verifyWaiverRequestRejectModal(requestWaiverReviewPage);

    requestWaiverReviewPage.requestWaiverRejectReason().setValue("Test rejection reason");
    requestWaiverReviewPage.sendRejectionButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    // The waiver request values don't change and the status is updated to "Rejected" with the rejection reason
    PolicyWaiverRequest waiverRequest = verifyWaiverRequestInDB(policyWaiverRequest);
    assertThat(waiverRequest.getStatus()).isEqualTo(PolicyWaiverRequestStatus.REJECTED);
    assertThat(waiverRequest.getRejectionReason()).isEqualTo("Test rejection reason");

    // The waiver request should be displayed in the dashboard
    WaiverRequestTile waiverRequestTile = verifyDashboardWaiverRequest(0, policyWaiverRequest);
    waiverRequestTile.status().shouldHave(text("Rejected"));
  }

  @Test
  public void testErrorAlertWhenWaiverRequestIsRejected() {
    String rejectionReason = "This component violates security policies";
    String reviewerName = "Admin Reviewer";

    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.REJECTED);
    policyWaiverRequest.setRejectionReason(rejectionReason);
    policyWaiverRequest.setReviewerId("admin");
    policyWaiverRequest.setReviewerName(reviewerName);
    policyWaiverRequestDAO.update(policyWaiverRequest);

    refreshOrOpen(RequestWaiverReviewPage.url(organization.getType().toString(), organization.getId(),
        policyWaiverRequest.getId()));
    RequestWaiverReviewPage.waitUntilSpinnersGone();

    RequestWaiverReviewPage requestWaiverReviewPage = new RequestWaiverReviewPage();

    // Verify the usual page elements are visible
    verifyRequestWaiverReviewPage(requestWaiverReviewPage);

    // Verify the rejection error alert is visible and contains the correct information
    requestWaiverReviewPage.rejectionErrorAlert().shouldBe(visible);

    String expectedText = "This Waiver Request was rejected by " + reviewerName + " for the following reason:";
    requestWaiverReviewPage.rejectionErrorAlert().shouldHave(text(expectedText));
    requestWaiverReviewPage.rejectionErrorAlert().shouldHave(text(rejectionReason));
  }

  private void loginAsLimitedUser() {
    User developerUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        ROOT_ORGANIZATION_ID,
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());
    login(developerUser.getUsername(), developerUser.getPassword());
  }

  private void verifyRequestWaiverReviewPage(RequestWaiverReviewPage requestWaiverReviewPage) {
    requestWaiverReviewPage.root().shouldBe(visible);
    requestWaiverReviewPage.requestWaiverReviewHeader().shouldHave(text("Review Requested Waiver"));

    requestWaiverReviewPage.requestWaiverReviewInfoTitle()
        .shouldHave(text("Requested Waiver Information"));

    requestWaiverReviewPage.requestWaiverInfoRequestedBy()
        .shouldHave(text("Requested By"))
        .shouldHave(text(policyWaiverRequest.getRequesterName()));

    requestWaiverReviewPage.requestWaiverInfoDateRequested()
        .shouldHave(text("Date Requested"))
        .shouldHave(text(formatDate(policyWaiverRequest.getRequestTime())));

    requestWaiverReviewPage.requestWaiverInfoNoteToReviewer()
        .shouldHave(text("Note to Reviewer"))
        .shouldHave(text("This note will only be visible on the waiver request. " +
            "It will not be visible on the waiver if it is approved."))
        .shouldHave(text(policyWaiverRequest.getNoteToReviewer()));

    requestWaiverReviewPage.waiverConfigurationTitle().shouldHave(text("Waiver Configuration"));

    requestWaiverReviewPage.requestWaiverComponentName().shouldHave(text("Group1 : Artifact1 : Version1"));

    requestWaiverReviewPage.requestWaiverPolicy().shouldHave(text("Policy"));
    requestWaiverReviewPage.requestWaiverPolicy().shouldHave(text("Policy 1"));

    requestWaiverReviewPage.requestWaiverConstraint().shouldHave(text("Constraint Name"));
    requestWaiverReviewPage.requestWaiverConstraint().shouldHave(text("Test Constraint"));

    requestWaiverReviewPage.requestWaiverConditions().shouldHave(text("Conditions"));
    requestWaiverReviewPage.requestWaiverConditions().shouldHave(text("sonatype-2017-0507"));

    requestWaiverReviewPage.requestWaiverScope().shouldHave(text("Scope"));
    requestWaiverReviewPage.requestWaiverScopeOptions()
        .findBy(text("Organization - Org 1"))
        .shouldBe(selected);

    requestWaiverReviewPage.requestWaiverComponents().shouldHave(text("Components"));
    requestWaiverReviewPage.requestWaiverComponentsOptions()
        .shouldHave(
            exactTexts("Group1 : Artifact1 : Version1", "Group1 : Artifact1 (all versions)", "All Components"));
    // exact component is checked:
    requestWaiverReviewPage.requestWaiverComponentsInputs().get(0).shouldBe(checked);

    requestWaiverReviewPage.requestWaiverExpiryTime().shouldHave(text("Waiver Expiration"));
    requestWaiverReviewPage.requestWaiverExpiryTimeOptions().findBy(text("Custom")).shouldBe(selected);

    requestWaiverReviewPage.requestWaiverReason().shouldHave(text("Reason"));
    requestWaiverReviewPage.requestWaiverReasonOptions().findBy(text("Acknowledged violation")).shouldBe(selected);

    requestWaiverReviewPage.requestWaiverComments().shouldHave(text("Comment 1"));

    requestWaiverReviewPage.approveButton().shouldBe(visible);
    requestWaiverReviewPage.cancelButton().shouldBe(visible);
  }

  private void verifyWaiverRequestRejectModal(RequestWaiverReviewPage requestWaiverReviewPage) {
    requestWaiverReviewPage.requestWaiverRejectTitle().shouldHave(text("Reject Waiver Request"));
    requestWaiverReviewPage.requestWaiverRejectLegend().shouldHave(text("Rejection Reason"));
    requestWaiverReviewPage.requestWaiverRejectReason().shouldBe(empty);
    requestWaiverReviewPage.requestWaiverRejectReason()
        .shouldHave(attribute("placeholder", "Enter the reason the waiver request was rejected here"));
  }

  private void verifyDisabledElements(RequestWaiverReviewPage requestWaiverReviewPage) {
    requestWaiverReviewPage.requestWaiverScopeOptions().forEach(option -> option.shouldBe(disabled));
    requestWaiverReviewPage.requestWaiverComponents().shouldHave(attribute("disabled"));
    requestWaiverReviewPage.requestWaiverExpiryTimeOptions().forEach(option -> option.shouldBe(disabled));
    requestWaiverReviewPage.requestWaiverReasonOptions().forEach(option -> option.shouldBe(disabled));
    requestWaiverReviewPage.requestWaiverComments().shouldBe(disabled);
    requestWaiverReviewPage.approveButton().shouldHave(cssClass("disabled"));
    requestWaiverReviewPage.rejectButton().shouldBe(disabled);
  }

  private WaiverRequestTile verifyDashboardWaiverRequest(int index, PolicyWaiverRequest policyWaiverRequest) {
    DashboardPage.dashboardContainer().shouldBe(visible);
    WaiverRequestTile waiverRequestTile = dashboardWaiverRequestTable.waiverRequest(index);
    waiverRequestTile.threatIndicator().shouldHave(SEVERE);
    waiverRequestTile.threatNumber().shouldHave(text(String.valueOf(securityPolicy.getThreatLevel())));
    waiverRequestTile.createTime().shouldHave(text(formatDate(policyWaiverRequest.getRequestTime())));
    waiverRequestTile.requester().shouldHave(text(policyWaiverRequest.getRequesterName()));
    waiverRequestTile.policy().shouldHave(text(securityPolicy.getName()));
    waiverRequestTile.component().shouldHave(text("Group1 : Artifact1 : Version1"));

    return waiverRequestTile;
  }

  private WaiverTile verifyDashboardWaivers() {
    DashboardPage.dashboardContainer().shouldBe(visible);
    DashboardPage.waiversTab().click();
    WaiverTile waiver = dashboardWaiverTable.waiver(0);
    waiver.threatIndicator().shouldHave(SEVERE);
    waiver.threatNumber().shouldHave(text("7"));
    waiver.createTime().shouldHave(text(formatDate(now)));
    waiver.policy().shouldHave(text("Policy 1"));

    return waiver;
  }

  private PolicyWaiverRequest verifyWaiverRequestInDB(PolicyWaiverRequest policyWaiverRequest) {
    PolicyWaiverRequest waiverRequest = policyWaiverRequestDAO.getById(policyWaiverRequest.getId());

    assertThat(waiverRequest.getPolicyId()).isEqualTo(policyWaiverRequest.getPolicyId());
    assertThat(waiverRequest.getOwnerId()).isEqualTo(policyWaiverRequest.getOwnerId());
    assertThat(waiverRequest.getComponentMatchStrategy()).isEqualTo(
        policyWaiverRequest.getComponentMatchStrategy());
    assertThat(waiverRequest.getExpiryTime()).isEqualTo(policyWaiverRequest.getExpiryTime());
    assertThat(waiverRequest.getWaiverReasonId()).isEqualTo(policyWaiverRequest.getWaiverReasonId());
    assertThat(waiverRequest.getComment()).isEqualTo(policyWaiverRequest.getComment());
    assertThat(waiverRequest.getNoteToReviewer()).isEqualTo(policyWaiverRequest.getNoteToReviewer());

    return waiverRequest;
  }

  private String formatDate(Date date) {
    return DateFormatUtils.format(date, "yyyy-MM-dd", TimeZone.getTimeZone("UTC"));
  }
}
