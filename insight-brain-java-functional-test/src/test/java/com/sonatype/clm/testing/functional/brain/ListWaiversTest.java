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
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.WaiverListRow;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.WaiverListTable;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;

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

    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testPageLayout() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.title().shouldHave(text("Waivers for Violation"));
    listWaiversPage.waiverDetailsTile().exists();
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
    listWaiversPage.waiverListTile().exists();

    listWaiversPage.addWaiverButton().click();
    // that it reaches this page after click is assertion enough
    waitUntilUrl(AddWaiverPage.url(policyViolation.getId()));
  }

  @Test
  public void testWaiverListTableEmptyMessage() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTile().exists();
    listWaiversPage.waiverListTitle().shouldHave(text("Applicable Waivers"));

    WaiverListTable waiverListTable = listWaiversPage.waiverListTable();
    waiverListTable.headerRow().dateCreated().shouldHave(text("DATE CREATED"));
    waiverListTable.headerRow().scope().shouldHave(text("SCOPE"));
    waiverListTable.headerRow().components().shouldHave(text("COMPONENTS"));
    waiverListTable.headerRow().waiverExpiration().shouldHave(text("WAIVER EXPIRATION"));
    waiverListTable.headerRow().comments().shouldHave(text("COMMENTS"));
    waiverListTable.noWaiversMessage().exists();
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
    listWaiversPage.waiverListTile().exists();

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
}
