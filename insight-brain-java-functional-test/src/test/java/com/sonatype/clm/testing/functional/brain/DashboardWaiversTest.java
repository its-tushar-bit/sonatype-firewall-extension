/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversHeaders;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiversResults;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.WaiverDetailsPage;
import com.sonatype.clm.testing.functional.utils.proxy.ResponseCopyHandler;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.CRITICAL;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.MODERATE;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardWaiversTest
    extends AbstractFunctionalTest
{
  private Organization rootOrg;

  private Organization organization;

  private Application application;

  private Application application2;

  private ArrayList<PolicyWaiver> policyWaivers;

  private static final String CSV_HEADERS = "Waiver Id, Threat level, Created Date, Expiration Date," +
          " Policy Id, Policy Name, Policy Constraints, Scope Type, Scope Id, Scope Name," +
          " Component Match Strategy, Component Hash, Component Name, Created by Id, Created by Name,Comment";

  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

  private Instant now = Instant.now();

  private Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

  private Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);

  private Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

  private Instant sixDaysAgo = now.minus(6, ChronoUnit.DAYS);

  private Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

  private Instant eightDaysAgo = now.minus(8, ChronoUnit.DAYS);

  private Instant fiveDaysFromNow = now.plus(5, ChronoUnit.DAYS);

  private Instant sixDaysFromNow = now.plus(6, ChronoUnit.DAYS);

  private Instant sevenDaysFromNow = now.plus(7, ChronoUnit.DAYS);

  private Instant eightDaysFromNow = now.plus(8, ChronoUnit.DAYS);

  private Instant threeDaysFromNow = now.plus(3, ChronoUnit.DAYS);

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private static final String NO_DATA_MSG =
      "No data available in the last 30 days given the applied filters and permissions.";

  private static final WaiversResults table = DashboardPage.waiversView().results();

  private static final WaiversHeaders headers = DashboardPage.waiversView().headers();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    loginAsAdmin();
  }

  @Before
  public void before() {
    rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testWaiversTable() {
    // no results
    refreshOrOpen(DashboardPage.urlToWaivers());
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    policyWaivers = createWaivers();
    refresh();

    DashboardPage.dashboardContainer().shouldBe(visible);
    table.maxResultsMessage().shouldBe(hidden);
    table.waivers().shouldHaveSize(6);

    // check the tile details
    WaiverTile waiver1 = table.firstWaiver();
    waiver1.threatIndicator().shouldHave(SEVERE);
    waiver1.threatNumber().shouldHave(text("7"));
    waiver1.createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));
    waiver1.expiryTime().shouldHave(text(dateFormat.format(Date.from(threeDaysFromNow))));
    waiver1.policy().shouldHave(text("Policy 1"));
    waiver1.scope().shouldHave(text("Organization - Org 1"));
    waiver1.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    WaiverTile waiver2 = table.waiver(1);
    waiver2.threatIndicator().shouldHave(MODERATE);
    waiver2.threatNumber().shouldHave(text("3"));
    waiver2.createTime().shouldHave(text(dateFormat.format(Date.from(threeDaysAgo))));
    waiver2.expiryTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysFromNow))));
    waiver2.policy().shouldHave(text("Policy 3"));
    waiver2.scope().shouldHave(text("Application - App 2"));
    waiver2.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    WaiverTile waiver3 = table.waiver(2);
    waiver3.threatIndicator().shouldHave(CRITICAL);
    waiver3.threatNumber().shouldHave(text("9"));
    waiver3.createTime().shouldHave(text(dateFormat.format(Date.from(sixDaysAgo))));
    waiver3.expiryTime().shouldHave(text(dateFormat.format(Date.from(sixDaysFromNow))));
    waiver3.policy().shouldHave(text("Policy 2"));
    waiver3.scope().shouldHave(text("Organization - Org 1"));
    waiver3.component().shouldHave(text("All Components"));

    WaiverTile waiver4 = table.waiver(3);
    waiver4.threatIndicator().shouldHave(CRITICAL);
    waiver4.threatNumber().shouldHave(text("9"));
    waiver4.createTime().shouldHave(text(dateFormat.format(Date.from(sevenDaysAgo))));
    waiver4.expiryTime().shouldHave(text(dateFormat.format(Date.from(sevenDaysFromNow))));
    waiver4.policy().shouldHave(text("Policy 2"));
    waiver4.scope().shouldHave(text("Application - App 1"));
    waiver4.component().shouldHave(text("Group1 : Artifact1 (all versions)"));

    WaiverTile waiver6 = table.waiver(4);
    waiver6.threatIndicator().shouldHave(SEVERE);
    waiver6.threatNumber().shouldHave(text("4"));
    waiver6.createTime().shouldHave(text(dateFormat.format(Date.from(eightDaysAgo))));
    waiver6.expiryTime().shouldHave(text(dateFormat.format(Date.from(eightDaysFromNow))));
    waiver6.policy().shouldHave(text("Policy 4"));
    waiver6.scope().shouldHave(text(rootOrg.getType().toString() + " - " + rootOrg.getName()));
    waiver6.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    WaiverTile waiver5 = table.waiver(5);
    waiver5.threatIndicator().shouldHave(MODERATE);
    waiver5.threatNumber().shouldHave(text("3"));
    waiver5.createTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysAgo))));
    waiver5.expiryTime().shouldHave(text("Never"));
    waiver5.policy().shouldHave(text("Policy 3"));
    waiver5.scope().shouldHave(text("Application - App 1"));
    waiver5.component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));

    // check the csv export default sort order
    ResponseCopyHandler responseCopyHandler = new ResponseCopyHandler("/rest/dashboard/export/policyWaivers",
            testCLMServer.getCLMServer().getPort());
    reverseProxyServer.addHandler(responseCopyHandler);
    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waivers Data")).click();
    DashboardPage.dashboardContainer().shouldBe(visible); // still on dashboard page
    String exportCsv = new String(responseCopyHandler.consumeResponse());
    DateFormat dateFormatCsv = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormatCsv.setTimeZone(TimeZone.getTimeZone("UTC"));

    //  create expected results
    String waiver1String = policyWaivers.get(0).getId() + ",7," + dateFormatCsv.format(Date.from(twoDaysAgo)) +
            "," + dateFormatCsv.format(Date.from(threeDaysFromNow)) + "," + policyWaivers.get(0).getPolicyId() +
            ",Policy 1,,organization," + policyWaivers.get(0).getOwnerId() + "," + "Org 1,EXACT_COMPONENT,hash1," +
            "Group1 : Artifact1 : 1.2.3,testuser,Test User,comment";
    String waiver2String = policyWaivers.get(1).getId() + ",3," + dateFormatCsv.format(Date.from(threeDaysAgo)) + "," +
            dateFormatCsv.format(Date.from(fiveDaysFromNow)) + "," + policyWaivers.get(1).getPolicyId() +
            ",Policy 3,,application," + policyWaivers.get(1).getOwnerId() + ",App 2,EXACT_COMPONENT,hash2," +
            "Group1 : Artifact1 : 1.2.3,testuser,Test User,comment";
    String waiver3String = policyWaivers.get(3).getId() + ",9," + dateFormatCsv.format(Date.from(sixDaysAgo)) +
            "," + dateFormatCsv.format(Date.from(sixDaysFromNow)) + "," + policyWaivers.get(3).getPolicyId() +
            ",Policy 2,,organization," + policyWaivers.get(3).getOwnerId() + ",Org 1,ALL_COMPONENTS,hash4," +
            "Group1 : Artifact1 : 1.2.3,testuser,Test User,org all components";
    String waiver4String = policyWaivers.get(4).getId() + ",9," + dateFormatCsv.format(Date.from(sevenDaysAgo)) + "," +
            dateFormatCsv.format(Date.from(sevenDaysFromNow)) + "," + policyWaivers.get(4).getPolicyId() +
            ",Policy 2,,application," + policyWaivers.get(4).getOwnerId() + ",App 1,ALL_VERSIONS,hash5," +
            "Group1 : Artifact1 : 1.2.3,testuser,Test User,app all versions";
    String waiver5String = policyWaivers.get(5).getId() + ",4," + dateFormatCsv.format(Date.from(eightDaysAgo)) + "," +
            dateFormatCsv.format(Date.from(eightDaysFromNow)) +  "," + policyWaivers.get(5).getPolicyId() +
            ",Policy 4,,organization," + policyWaivers.get(5).getOwnerId() +
            "," + rootOrg.getName() + ",EXACT_COMPONENT,hash6,Group1 : Artifact1 : 1.2.3,testuser,Test User,comment";
    String waiver6String = policyWaivers.get(2).getId() + ",3," + dateFormatCsv.format(Date.from(fiveDaysAgo)) + ",," +
            policyWaivers.get(2).getPolicyId() + ",Policy 3,,application," + policyWaivers.get(2).getOwnerId() +
            ",App 1,EXACT_COMPONENT,hash3,Group1 : Artifact1 : 1.2.3,testuser,Test User,comment";

    String[] expectedResults = new String[]{
        waiver1String,
        waiver2String,
        waiver3String,
        waiver4String,
        waiver5String,
        waiver6String
    };
    assertWaiversCsv(exportCsv, expectedResults);

    headers.threatHeader().click();
    table.firstWaiver().threatNumber().shouldHave(text("9"));
    table.lastWaiver().threatNumber().shouldHave(text("3"));

    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waivers Data")).click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        waiver3String,
        waiver4String,
        waiver1String,
        waiver5String,
        waiver2String,
        waiver6String
    };
    assertWaiversCsv(exportCsv, expectedResults);

    headers.dateHeader().click();
    table.firstWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(eightDaysAgo))));
    table.lastWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));

    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waivers Data")).click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        waiver5String,
        waiver4String,
        waiver3String,
        waiver6String,
        waiver2String,
        waiver1String
    };
    assertWaiversCsv(exportCsv, expectedResults);

    headers.expirationHeader().click();
    table.firstWaiver().expiryTime().shouldHave(text(dateFormat.format(Date.from(threeDaysFromNow))));
    table.lastWaiver().expiryTime().shouldHave(text("Never"));

    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waivers Data")).click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        waiver1String,
        waiver2String,
        waiver3String,
        waiver4String,
        waiver5String,
        waiver6String
    };
    assertWaiversCsv(exportCsv, expectedResults);

    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeUp();
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeDown();
    table.firstWaiver().policy().shouldHave(text("Policy 4"));
    table.lastWaiver().policy().shouldHave(text("Policy 1"));

    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waivers Data")).click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        waiver5String,
        waiver2String,
        waiver6String,
        waiver3String,
        waiver4String,
        waiver1String
    };
    assertWaiversCsv(exportCsv, expectedResults);

    headers.scopeHeader().click();
    table.firstWaiver().scope().shouldHave(text("Application - App 1"));
    table.lastWaiver().scope().shouldHave(text(rootOrg.getType().toString() + " - " + rootOrg.getName()));

    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waivers Data")).click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        waiver4String,
        waiver6String,
        waiver2String,
        waiver1String,
        waiver3String,
        waiver5String
    };
    assertWaiversCsv(exportCsv, expectedResults);

    headers.componentHeader().click();
    table.firstWaiver().component().shouldHave(text("Group1 : Artifact1 : 1.2.3"));
    table.lastWaiver().component().shouldHave(text("All Components"));

    DashboardPage.exportResultsLink().shouldBe(visible).shouldHave(text("Export Waivers Data")).click();
    exportCsv = new String(responseCopyHandler.consumeResponse());
    expectedResults = new String[]{
        waiver1String,
        waiver2String,
        waiver3String,
        waiver4String,
        waiver5String,
        waiver6String
    };
    assertWaiversCsv(exportCsv, expectedResults);
  }

  private void assertWaiversCsv(String csv, String[] expectedSortedResults) {
    String[] lines = csv.split("\r\n");

    // assert CSV header
    assertThat(lines[0]).isEqualTo(CSV_HEADERS);

    // assert CSV results
    String[] results = Arrays.copyOfRange(lines, 1, lines.length);
    assertThat(results).isEqualTo(expectedSortedResults);
  }

  @Test
  public void testSortsOnBackendByThreat() {
    Instant now = Instant.now();
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy Threat" + i,
              i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by threat desc
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeDown();

    table.firstWaiver().threatNumber().shouldHave(text("10"));
    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiver(25).threatNumber().shouldHave(text("7"));
    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiver(78).threatNumber().shouldHave(text("3"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().threatNumber().shouldHave(text("1"));
    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    // sort by threat asc
    headers.threatHeader().click();
    headers.threatHeader().sortArrows().shouldBeUp();

    table.firstWaiver().threatNumber().shouldHave(text("1"));
    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiver(25).threatNumber().shouldHave(text("3"));
    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiver(78).threatNumber().shouldHave(text("7"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().threatNumber().shouldHave(text("10"));
    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll"));
  }

  @Test
  public void testSortsOnBackendByCreatedDate() {
    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Create " + i, i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(twoDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(threeDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by creation date asc
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeUp();

    table.firstWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysAgo))));
    table.waiver(52).createTime().shouldHave(text(dateFormat.format(Date.from(threeDaysAgo))));
    table.lastWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));

    // sort by creation date desc
    headers.dateHeader().click();
    headers.dateHeader().sortArrows().shouldBeDown();

    table.firstWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(twoDaysAgo))));
    table.waiver(50).createTime().shouldHave(text(dateFormat.format(Date.from(threeDaysAgo))));
    table.lastWaiver().createTime().shouldHave(text(dateFormat.format(Date.from(fiveDaysAgo))));
  }

  @Test
  public void testSortsOnBackendByExpirationDate() {
    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    Instant lastExpiryDate = null;
    Instant firstExpiryDate = null;

    // create 101 waivers
    for (int i = 0; i < 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Expiry " + i, i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      if (i == 0) {
        firstExpiryDate = expiration;
      }
      if (i == 24) {
        tempEntity.newWaiver("hash" + i + 1 + policy.getId(), policy.getId(), app.getId(),
                policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
                Date.from(fiveDaysAgo), Date.from(expiration));
        lastExpiryDate = expiration;
      }
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(twoDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(threeDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), null);
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by expiration date desc
    headers.expirationHeader().click();
    headers.expirationHeader().sortArrows().shouldBeDown();

    table.firstWaiver().expiryTime().shouldHave(text("Never"));
    table.waiver(26).expiryTime().shouldHave(text(dateFormat.format(Date.from(lastExpiryDate))));
    table.lastWaiver().expiryTime().shouldHave(text(dateFormat.format(Date.from(firstExpiryDate))));

    // sort by expiration date asc
    headers.expirationHeader().click();
    headers.expirationHeader().sortArrows().shouldBeUp();

    table.firstWaiver().expiryTime().shouldHave(text(dateFormat.format(Date.from(firstExpiryDate))));
    table.waiver(75).expiryTime().shouldHave(text(dateFormat.format(Date.from(lastExpiryDate))));
    table.lastWaiver().expiryTime().shouldHave(text("Never"));
  }

  @Test
  public void testSortsOnBackendByPolicy() {
    Instant now = Instant.now();
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy " + i,
              i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by policy asc
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeUp();
    eyesWatcher.eyesCheck();

    table.firstWaiver().policy().shouldHave(text("Dashboard Policy 0"));
    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiver(25).policy().shouldHave(text("Dashboard Policy 14"));
    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiver(78).policy().shouldHave(text("Dashboard Policy 3"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().policy().shouldHave(text("Dashboard Policy 8"));
    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    // sort by policy desc
    headers.policyHeader().click();
    headers.policyHeader().sortArrows().shouldBeDown();

    table.firstWaiver().policy().shouldHave(text("Dashboard Policy 9"));
    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll"));

    table.waiver(25).policy().shouldHave(text("Dashboard Policy 3"));
    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll B"));

    table.waiver(78).policy().shouldHave(text("Dashboard Policy 14"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().policy().shouldHave(text("Dashboard Policy 1"));
    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll"));
  }

  @Test
  public void testSortsOnBackendByScope() {
    Instant now = Instant.now();
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 2");
    Application app = tempEntity.newApplication("App Test Scroll A", "appTestScroll", organization.getId());
    Application app2 = tempEntity.newApplication("App Test Scroll B", "appTestScroll2", organization.getId());

    // create 100+ waivers
    for (int i = 0; i <= 25; i++) {
      Instant pastTime = now.minus(i, ChronoUnit.DAYS);
      Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Dashboard Policy Scope" + i, i % 10 + 1);

      PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(),
              StageTypes.RELEASE.getId(), "scan" + i, false, false, Date.from(pastTime));
      PolicyViolation policyViolation = tempEntity.newPolicyViolation(evaluation, policy);

      Instant expiration = now.plus(i, ChronoUnit.DAYS);
      tempEntity.newWaiver("hash" + i + app.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + app2.getId(), policy.getId(), app2.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + organization.getId(), policy.getId(), organization.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
      tempEntity.newWaiver("hash" + i + policy.getId(), policy.getId(), app.getId(),
              policyViolation.getConstraintFacts(), EXACT_COMPONENT, "comment",
              Date.from(fiveDaysAgo), Date.from(expiration));
    }

    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.RELEASE.getId(),
            "scan1", false, false, Date.from(now.minus(29, ChronoUnit.DAYS)));

    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.maxResultsMessage().shouldBe(visible);
    table.waivers().shouldHaveSize(100);

    // sort by scope asc
    headers.scopeHeader().click();
    headers.scopeHeader().sortArrows().shouldBeUp();

    table.firstWaiver().scope().shouldHave(text("Application - App Test Scroll A"));

    table.waiver(25).scope().shouldHave(text("Application - App Test Scroll A"));
    table.waiver(52).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiver(77).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiver(78).scope().shouldHave(text("Organization - Org 2"));

    table.lastWaiver().scope().shouldHave(text("Organization - Org 2"));

    // sort by scope desc
    headers.scopeHeader().click();
    headers.scopeHeader().sortArrows().shouldBeDown();

    table.firstWaiver().scope().shouldHave(text("Organization - Org 2"));

    table.waiver(25).scope().shouldHave(text("Organization - Org 2"));
    table.waiver(40).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiver(51).scope().shouldHave(text("Application - App Test Scroll B"));
    table.waiver(52).scope().shouldHave(text("Application - App Test Scroll A"));

    table.lastWaiver().scope().shouldHave(text("Application - App Test Scroll A"));
  }

  @Test
  public void testWaiversTableRowClick() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    table.noDataMessage().shouldBe(visible).shouldHave(text(NO_DATA_MSG));

    policyWaivers = createWaivers();
    refreshOrOpen(DashboardPage.urlToWaivers());
    showAllWaivers();
    table.waivers().shouldHaveSize(6);

    // get first waiver row in table
    table.firstWaiver().click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("organization",
        organization.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));

    refreshOrOpen(DashboardPage.urlToWaivers());

    // get second waiver row in table
    table.waiver(1).click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("application",
            application2.getId(), policyWaivers.get(1).getId(), "waiver", "filter"));

    refreshOrOpen(DashboardPage.urlToWaivers());

    // get third waiver row in table
    table.waiver(2).click();
    waitUntilUrl(WaiverDetailsPage.urlWithQueryParams("organization",
        organization.getId(), policyWaivers.get(3).getId(), "waiver", "filter"));

    refreshOrOpen(DashboardPage.urlToWaivers());
  }

  private ArrayList<PolicyWaiver> createWaivers() {
    organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    application2 = tempEntity.newApplication("App 2", "app2", organization.getId());

    ArrayList<Policy> securityPolicies = new ArrayList<Policy>() {{
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 9));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 3", 3));
        this.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 4", 4));
      }};

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
            StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application2.getId(),
            StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    tempEntity.newPolicyViolation(policyEvaluation1, securityPolicies.get(0), "Group1",
            "Artifact1", "Version1", "hash1", "sonatype-2017-0507");
    tempEntity.newPolicyViolation(policyEvaluation1, securityPolicies.get(1), "Group2",
            "Artifact2", "Version2", "hash2", "sonatype-2017-8912");
    tempEntity.newPolicyViolation(policyEvaluation2, securityPolicies.get(2), "Group3",
            "Artifact3", "Version3", "hash3", "sonatype-2017-7848");

    // Component identifier for waivers
    TreeMap<String, String> coordinates = new TreeMap<String, String>() {{
        this.put("artifactId", "Artifact1");
        this.put("groupId", "Group1");
        this.put("version", "1.2.3");
      }};

    ComponentIdentifier componentIdentifier = new ComponentIdentifier("maven", coordinates);
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    // Default sorting: closer to expire at the top
    return new ArrayList<PolicyWaiver>() {{
        this.add(tempEntity.newWaiver("hash1", securityPolicies.get(0).getId(), organization.getId(),
                null, purl, EXACT_COMPONENT, "comment",
                Date.from(twoDaysAgo), Date.from(threeDaysFromNow)));
        this.add(tempEntity.newWaiver("hash2", securityPolicies.get(2).getId(), application2.getId(),
                null, purl, EXACT_COMPONENT, "comment",
                Date.from(threeDaysAgo), Date.from(fiveDaysFromNow)));
        this.add(tempEntity.newWaiver("hash3", securityPolicies.get(2).getId(), application.getId(),
                null, purl, EXACT_COMPONENT, "comment",
                Date.from(fiveDaysAgo),  null));
        this.add(tempEntity.newWaiver("hash4", securityPolicies.get(1).getId(), organization.getId(),
                null, purl, ALL_COMPONENTS, "org all components",
                Date.from(sixDaysAgo), Date.from(sixDaysFromNow)));
        this.add(tempEntity.newWaiver("hash5", securityPolicies.get(1).getId(), application.getId(),
                null, purl, ALL_VERSIONS, "app all versions",
                Date.from(sevenDaysAgo), Date.from(sevenDaysFromNow)));
        this.add(tempEntity.newWaiver("hash6", securityPolicies.get(3).getId(), rootOrg.getId(),
                null, purl, EXACT_COMPONENT, "comment",
                Date.from(eightDaysAgo), Date.from(eightDaysFromNow)));
      }};
  }

  private void showAllWaivers() {
    DashboardPage.filterToggle().click();
    DashboardFilters.organizationFilter().twisty().click();
    DashboardFilters.organizationFilter().checkboxItem(2).click();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(1, 10);
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.apply();
    DashboardFilters.closeButton().click();
  }
}
