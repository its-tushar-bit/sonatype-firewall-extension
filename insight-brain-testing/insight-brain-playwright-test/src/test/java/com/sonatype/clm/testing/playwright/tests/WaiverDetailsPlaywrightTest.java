/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.WaiverDetailsPage;
import com.sonatype.clm.testing.playwright.pages.WaiverDetailsPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the Waiver Details page.
 */
public class WaiverDetailsPlaywrightTest
    extends AbstractIqUiTest
{

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

  private record PolicyData(String name, int threatLevel)
  {
  }

  private record ComponentData(
      String groupId,
      String artifactId,
      String version,
      String hash,
      String cveId,
      String purlVersion)
  {
    String coords() {
      return groupId + " : " + artifactId + " : " + purlVersion;
    }
  }

  private static final String ORG_NAME = "Org 1";

  private static final String APP_NAME = "App 1";

  private static final String APP_ID = "app1";

  private static final List<PolicyData> POLICIES = List.of(
      new PolicyData("Policy 1", 7),
      new PolicyData("Policy 2", 9),
      new PolicyData("Policy 3", 3),
      new PolicyData("Policy 4", 8));

  private static final List<ComponentData> COMPONENTS = List.of(
      new ComponentData("Group1", "Artifact1", "Version1", "hash1", "sonatype-2017-0507", "1.2.3"),
      new ComponentData("Group2", "Artifact2", "Version2", "hash2", "sonatype-2017-8912", null),
      new ComponentData("Group3", "Artifact3", "Version3", "hash3", "sonatype-2017-7848", null),
      new ComponentData("Group4", "Artifact4", "Version4", null, "sonatype-2017-7859", null));

  private static final String WAIVER_REASON_NAME = "Reason type";

  private static final String WAIVER_REASON_TEXT = "some reason text";

  private static final String COMMENT = "comment";

  private static final String CREATOR_NAME = "Test User";

  private static final String NO_REASON_TEXT = "--";

  private static final String VULNERABILITY_DETAILS_BUTTON_TEXT = "Vulnerability Details";

  private static final String DETAILS_TILE_HEADER = "Waiver Detail View";

  private static final String CONSTRAINT_TEXT = "Test Constraint is in violation for:";

  private static final String SIDEBAR_NAV_TITLE = "waivers";

  private static String sidebarNavPolicyLabel(PolicyData policy) {
    return policy.threatLevel() + " " + policy.name();
  }

  private Application application;

  private List<PolicyWaiver> policyWaivers;

  @Before
  public void seedWaiversAndLogin() {
    Instant now = Instant.now();

    PolicyWaiverReason waiverReason = tempEntity.newWaiverReason(WAIVER_REASON_NAME, WAIVER_REASON_TEXT);

    Organization organization = tempEntity.newOrganization(ORG_NAME);
    application = tempEntity.newApplication(APP_NAME, APP_ID, organization.getId());

    List<Policy> policies = POLICIES
        .stream()
        .map(p -> tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, p.name(), p.threatLevel()))
        .toList();

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "scan1", false, false,
        Date.from(now.minus(2, ChronoUnit.DAYS)));

    // Violation 3 (policy 4) re-uses component[0]'s hash, not component[3]'s — preserves the
    // pre-existing behaviour where component 4's row in the JSON had no hash field and the test
    // explicitly passed component1Hash. Other violations use their own component's hash.
    ComponentData c0 = COMPONENTS.get(0);
    ComponentData c1 = COMPONENTS.get(1);
    ComponentData c2 = COMPONENTS.get(2);
    ComponentData c3 = COMPONENTS.get(3);

    List<PolicyViolation> violations = List.of(
        tempEntity.newPolicyViolation(evaluation, policies.get(0),
            c0.groupId(), c0.artifactId(), c0.version(), c0.hash(), c0.cveId()),
        tempEntity.newPolicyViolation(evaluation, policies.get(1),
            c1.groupId(), c1.artifactId(), c1.version(), c1.hash(), c1.cveId()),
        tempEntity.newPolicyViolation(evaluation, policies.get(2),
            c2.groupId(), c2.artifactId(), c2.version(), c2.hash(), c2.cveId()),
        tempEntity.newPolicyViolation(evaluation, policies.get(3),
            c3.groupId(), c3.artifactId(), c3.version(), c0.hash(), c3.cveId()));

    TreeMap<String, String> coordinates = new TreeMap<>();
    coordinates.put("artifactId", c0.artifactId());
    coordinates.put("groupId", c0.groupId());
    coordinates.put("version", c0.purlVersion());
    String purl = PackageUrlIdentifier
        .fromComponentIdentifier(new ComponentIdentifier("maven", coordinates))
        .getPackageUrl();

    // Waiver 3 (policy 4) re-uses component[2]'s hash, not component[3]'s — preserves the
    // pre-existing test mapping (waiver 4 was created with component3Hash in the original code).
    policyWaivers = List.of(
        tempEntity.newWaiver(new PolicyWaiver()
            .setHash(c0.hash())
            .setPolicyId(policies.get(0).getId())
            .setOwnerId(application.getId())
            .setAssociatedPackageUrl(purl)
            .setConstraintFacts(violations.get(0).getConstraintFacts())
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment(COMMENT)
            .setCreateTime(Date.from(now.minus(2, ChronoUnit.DAYS)))
            .setExpiryTime(Date.from(now.plus(3, ChronoUnit.DAYS)))
            .setCreatorName(CREATOR_NAME)
            .setComponentUpgradeAvailable(true)),
        tempEntity.newWaiver(new PolicyWaiver()
            .setHash(c1.hash())
            .setPolicyId(policies.get(1).getId())
            .setOwnerId(application.getId())
            .setAssociatedPackageUrl(purl)
            .setConstraintFacts(violations.get(1).getConstraintFacts())
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment(COMMENT)
            .setCreateTime(Date.from(now.minus(3, ChronoUnit.DAYS)))
            .setExpiryTime(Date.from(now.plus(5, ChronoUnit.DAYS)))),
        tempEntity.newWaiver(new PolicyWaiver()
            .setHash(c2.hash())
            .setPolicyId(policies.get(2).getId())
            .setOwnerId(application.getId())
            .setAssociatedPackageUrl(purl)
            .setConstraintFacts(violations.get(2).getConstraintFacts())
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment(COMMENT)
            .setCreateTime(Date.from(now.minus(5, ChronoUnit.DAYS)))
            .setExpiryTime(Date.from(now.plus(7, ChronoUnit.DAYS)))),
        tempEntity.newWaiver(new PolicyWaiver()
            .setHash(c2.hash())
            .setPolicyId(policies.get(3).getId())
            .setOwnerId(application.getId())
            .setAssociatedPackageUrl(purl)
            .setConstraintFacts(violations.get(3).getConstraintFacts())
            .setComponentMatchStrategy(EXACT_COMPONENT)
            .setComment(COMMENT)
            .setCreateTime(Date.from(now.minus(7, ChronoUnit.DAYS)))
            .setExpiryTime(Date.from(now.plus(9, ChronoUnit.DAYS)))
            .setCreatorName(CREATOR_NAME)
            .setWaiverReasonId(waiverReason.getId())));

    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @Test
  @Category(SanityTest.class)
  public void testSidebarNav_DeepLink() {
    String ownerType = application.getType().toString().toLowerCase(Locale.ROOT);
    playwrightRefreshOrOpen(WaiverDetailsPage.url(ownerType, application.getId(), policyWaivers.get(0).getId()));

    WaiverDetailsPage waiverPage = new WaiverDetailsPage();
    WaiverDetailsPageAssertions waiverAssertions = new WaiverDetailsPageAssertions(waiverPage);
    waiverAssertions.shouldShowSidebarNav(SIDEBAR_NAV_TITLE, 1);
    waiverAssertions.shouldShowSidebarNavItem(0,
        sidebarNavPolicyLabel(POLICIES.get(0)),
        COMPONENTS.get(0).coords());
  }

  @Test
  @Category(SanityTest.class)
  public void testPageLayout() {
    Instant now = Instant.now();
    String createdDate = DATE_FMT.format(now.minus(2, ChronoUnit.DAYS));
    String expirationDate = DATE_FMT.format(now.plus(3, ChronoUnit.DAYS));

    String ownerType = application.getType().toString().toLowerCase(Locale.ROOT);
    playwrightRefreshOrOpen(WaiverDetailsPage.urlWithQueryParams(
        ownerType, application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));

    ComponentData c0 = COMPONENTS.get(0);
    WaiverDetailsPage waiverPage = new WaiverDetailsPage();
    WaiverDetailsPageAssertions waiverAssertions = new WaiverDetailsPageAssertions(waiverPage);
    waiverAssertions.shouldShowPageLayout(
        DETAILS_TILE_HEADER,
        POLICIES.get(0).name(),
        CONSTRAINT_TEXT,
        c0.cveId(),
        VULNERABILITY_DETAILS_BUTTON_TEXT,
        APP_NAME,
        NO_REASON_TEXT,
        c0.purlVersion(),
        c0.coords(),
        expirationDate,
        COMMENT,
        CREATOR_NAME,
        createdDate);
  }

  @Test
  @Category(SanityTest.class)
  public void testVulnerabilityDetailsModal() {
    String ownerType = application.getType().toString().toLowerCase(Locale.ROOT);
    playwrightRefreshOrOpen(WaiverDetailsPage.urlWithQueryParams(
        ownerType, application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));

    WaiverDetailsPage waiverPage = new WaiverDetailsPage();
    WaiverDetailsPageAssertions waiverAssertions = new WaiverDetailsPageAssertions(waiverPage);
    waiverPage.clickVulnerabilityDetailsButton();
    waiverAssertions.shouldShowVulnerabilityDetailsModal();
    waiverPage.clickVulnerabilityDetailsModalCloseButton();
    waiverAssertions.shouldHideVulnerabilityDetailsModal();
  }

  @Test
  @Category(SanityTest.class)
  public void testDeleteWaiversModal() {
    String ownerType = application.getType().toString().toLowerCase(Locale.ROOT);
    playwrightRefreshOrOpen(WaiverDetailsPage.urlWithQueryParams(
        ownerType, application.getId(), policyWaivers.get(0).getId(), "waiver", "filter"));

    WaiverDetailsPage waiverPage = new WaiverDetailsPage();
    WaiverDetailsPageAssertions waiverAssertions = new WaiverDetailsPageAssertions(waiverPage);
    waiverAssertions.shouldShowPolicy(POLICIES.get(0).name());
    waiverPage.clickDeleteWaiverButton();
    PlaywrightWaitUtils.clickAndWaitForHidden(
        waiverPage.deleteWaiverModalYesButton(), waiverPage.deleteWaiverModal(),
        PlaywrightTiming.ELEMENT_TIMEOUT_MS, PlaywrightTiming.POLL_INTERVAL_MS);

    playwrightRefreshOrOpen(WaiverDetailsPage.urlWithQueryParams(
        ownerType, application.getId(), policyWaivers.get(1).getId(), "waiver", "filter"));
    waiverAssertions.shouldShowPolicy(POLICIES.get(1).name());
  }
}
