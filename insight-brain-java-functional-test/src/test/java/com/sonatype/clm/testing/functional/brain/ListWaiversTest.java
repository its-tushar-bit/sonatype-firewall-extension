/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.DeleteWaiverModal;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.WaiverListRow;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.WaiverListTable;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;

public class ListWaiversTest
    extends AbstractFunctionalTest
{
  private static Organization organization;

  private static Application application;

  private static Policy securityPolicy1;

  private static PolicyViolation policyViolation;

  @BeforeClass
  public static void startup() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    organization = staticTempEntity.newOrganization("Org 1");
    application = staticTempEntity.newApplication("App 1", "app1", organization.getId());
    securityPolicy1 = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation1 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    policyViolation = staticTempEntity.newPolicyViolation(policyEvaluation1, securityPolicy1, "Group1",
        "Artifact1", "Version1", "hash1", "sonatype-2017-0507");
  }

  @Before
  public void before() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    logout();
  }

  @Test
  public void testPageLayout() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.title().shouldHave(text("Waivers for Violation"));
    listWaiversPage.waiverDetailsTitle().shouldHave(text("Violation Details"));
    listWaiversPage.policyName().shouldHave(text("Policy 1"));
    listWaiversPage.policyName().shouldHave(cssClass("iq-threat-level--severe"));
    listWaiversPage.constraintName().shouldHave(text("Test Constraint"));
    listWaiversPage.conditions().shouldHaveSize(1);
    listWaiversPage.condition(1).shouldHave(text("sonatype-2017-0507"));
    listWaiversPage.componentName().shouldHave(text("Group1 : Artifact1 : Version1"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testWaiverListAddWaiverButton() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();

    listWaiversPage.addWaiverButton().shouldNotBe(DISABLED).click();
    // that it reaches this page after click is assertion enough
    waitUntilUrl(AddWaiverPage.url(policyViolation.getId()));
  }

  @Test
  public void testWaiverListAddWaiverButton_NotAuthorised() {
    Role role = tempEntity.newRole(false, Permission.READ);
    User user = tempEntity.newUser();
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());

    logout();
    login(user.getUsername(), user.getPassword());
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.addWaiverButton().shouldBe(visible, DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Insufficient permissions to Add Waiver"));
    listWaiversPage.addWaiverButton().click();
    // should remain on the same page
    listWaiversPage.title().shouldBe(visible);
  }

  @Test
  public void testWaiverListTableEmptyMessage() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTitle().shouldHave(text("Applicable Waivers"));

    WaiverListTable waiverListTable = listWaiversPage.waiverListTable();
    waiverListTable.headerRow().dateCreated().shouldHave(text("DATE CREATED"));
    waiverListTable.headerRow().scope().shouldHave(text("SCOPE"));
    waiverListTable.headerRow().components().shouldHave(text("COMPONENTS"));
    waiverListTable.headerRow().waiverExpiration().shouldHave(text("WAIVER EXPIRATION"));
    waiverListTable.headerRow().comments().shouldHave(text("COMMENTS"));
    waiverListTable.noWaiversMessage().should(exist);
    waiverListTable.noWaiversMessage().shouldHave(
        text("You don't have any waivers: to learn more about waivers you can check our help documentation."));
  }

  @Test
  public void testWaiverListTable() {
    Instant now = Instant.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy").withZone(ZoneId.systemDefault());
    String nowStr = formatter.format(now);
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    tempEntity.newWaiver(null, securityPolicy1.getId(), organization.getId(),
        policyViolation.getConstraintFacts(), null,
        Date.from(LocalDate.parse("2020-05-05").atStartOfDay(ZoneId.of("America/New_York")).toInstant()));
    tempEntity.newWaiver("hash1", securityPolicy1.getId(), application.getId(),
        policyViolation.getConstraintFacts(), "Comment 1", Date.from(now), Date.from(fiveDaysAgo));
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();

    WaiverListTable waiverListTable = listWaiversPage.waiverListTable();
    waiverListTable.noWaiversMessage().shouldNot(exist);
    waiverListTable.rows().shouldHaveSize(2);

    WaiverListRow row1 = waiverListTable.row(1);
    row1.shouldNotHave(cssClass("list-waivers-row--expired"));
    row1.dateCreated().shouldHave(text("05/05/2020"));
    row1.scope().shouldHave(text("Organization - Org 1"));
    row1.components().shouldHave(text("All"));
    row1.waiverExpiration().shouldHave(text("Does not expire"));
    row1.comments().shouldHave(text("- -"));

    WaiverListRow row2 = waiverListTable.row(2);
    row2.shouldHave(cssClass("list-waivers-row--expired"));
    row2.dateCreated().shouldHave(text(nowStr));
    row2.scope().shouldHave(text("Application - App 1"));
    row2.components().shouldHave(text("Group1 : Artifact1 : Version1"));
    row2.waiverExpiration().shouldHave(text("5 days ago"));
    row2.comments().shouldHave(text("Comment 1"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testBackButton() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));
    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.backButton().shouldHave(text("Back to Violation Details")).click();
    waitUntilUrl(ViolationDetailsPage.url(policyViolation.getId()));
  }

  @Test
  public void testDeleteButton() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    Organization tempOrg = tempEntity.newOrganization("Org Temp");
    Application tempApp = tempEntity.newApplication("App Temp", "apptemp", tempOrg.getId());
    Policy securityPolicyTemp = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy Temp", 8);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(tempApp.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    PolicyViolation policyViolationTemp = tempEntity.newPolicyViolation(policyEvaluation1, securityPolicyTemp,
        "Group1", "Artifact1", "Version1", "hash1", "sonatype-2017-0507");

    tempEntity.newWaiver(null, securityPolicyTemp.getId(), tempOrg.getId(),
        policyViolationTemp.getConstraintFacts(), null,
        Date.from(LocalDate.parse("2020-05-05").atStartOfDay(ZoneId.of("America/New_York")).toInstant()));

    tempEntity.newWaiver("hash1", securityPolicyTemp.getId(), tempApp.getId(),
        policyViolationTemp.getConstraintFacts(), "Comment 1", Date.from(now), Date.from(fiveDaysAgo));
    refreshOrOpen(ListWaiversPage.url(policyViolationTemp.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();

    WaiverListTable waiverListTable = listWaiversPage.waiverListTable();
    waiverListTable.noWaiversMessage().shouldNot(exist);
    waiverListTable.rows().shouldHaveSize(2);

    WaiverListRow row1 = waiverListTable.row(1);
    row1.scope().shouldHave(text("Organization - Org Temp"));
    row1.deleteButton().should(exist);
    row1.deleteButton().click();

    DeleteWaiverModal modal = listWaiversPage.deleteWaiverModal();
    modal.root().shouldBe(visible);
    modal.header().shouldHave(text("Delete Waiver"));
    modal.message().shouldHave(text("Are you sure you want to delete this waiver?"));
    modal.cancelButton().shouldHave(text("Cancel")).click();
    modal.root().should(disappear);

    row1.deleteButton().click();
    modal.root().shouldBe(visible);
    eyesWatcher.eyesCheck();
    modal.yesButton().click();
    modal.root().should(disappear);

    row1 = waiverListTable.row(1);
    // Assert that row is now a different one — previous one is gone
    waiverListTable.rows().shouldHaveSize(1);
    row1.scope().shouldHave(text("Application - App Temp"));
    row1.deleteButton().click();
    modal.yesButton().click();
    modal.root().should(disappear);

    waiverListTable.noWaiversMessage().shouldBe(visible);
  }
}
