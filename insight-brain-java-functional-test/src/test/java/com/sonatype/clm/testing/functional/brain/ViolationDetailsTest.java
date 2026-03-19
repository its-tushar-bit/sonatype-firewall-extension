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

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationApplicableWaiversTab;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationConstraintInfo;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSecurityDetailsInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationApplicableWaiversInfoTile;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage.PolicyViolationSimilarWaiversInfoTile;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.textCaseSensitive;
import static com.codeborne.selenide.Condition.visible;

public class ViolationDetailsTest
    extends AbstractFunctionalTest
{
  private Application application;

  private PolicyViolation securityPolicyViolation;

  private PolicyViolation otherPolicyViolation;

  private PolicyViolation deletedPolicyViolation;

  private PolicyViolation nonSecurityPolicyViolation;

  private PolicyDAO policyDAO;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    policyDAO = lookup(PolicyDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);

    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);

    Organization organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    PolicyEvaluation policyEvaluation2 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.RELEASE.getId(), "scan2", false, false, Date.from(oneDayAgo));

    PolicyEvaluation policyEvaluation3 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.OPERATE.getId(), "scan3", false, false, Date.from(oneDayAgo));

    securityPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation1, securityPolicy, "Group1",
        "Artifact1", "Version1", "hash", "sonatype-2017-0507");
    securityPolicyViolation.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(securityPolicyViolation);

    Policy otherPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 2", 3);
    otherPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation1, otherPolicy);

    // The same as securityPolicyViolation, but for a different stage
    tempEntity.newPolicyViolation(policyEvaluation2, securityPolicy, "Group1", "Artifact1", "Version1", "hash",
        "sonatype-2017-0507");

    // The same as securityPolicyViolation, but for a different stage
    PolicyViolation policyViolation3 = tempEntity.newPolicyViolation(policyEvaluation3, securityPolicy, "Group1",
        "Artifact1", "Version1", "hash", "sonatype-2017-0507");
    policyViolation3.setActionTypeId(Action.ID_WARN);
    policyViolationDAO.update(policyViolation3);

    Policy deletedPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Deleted Policy", 2);
    deletedPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation1, deletedPolicy);
    policyDAO.delete(deletedPolicy);

    Policy nonSecurityPolicy = createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "Policy 4",
        LicenseThreatGroupLevelConditionType.ID, "<=", "1");
    nonSecurityPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation2, nonSecurityPolicy);

    mockHdsResponseForVulnerabilityDetails();
    // This ensures that the redux state has the updated information at the start of the tests
    refresh();
  }

  private Policy createPolicy(
      String ownerId,
      int threatLevel,
      String name,
      String conditionType,
      String operator,
      String value)
  {
    Policy policy = new Policy(null, name);
    policy.setThreatLevel(threatLevel);
    policy.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition = new com.sonatype.insight.brain.model.policy.Condition(
        conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    policy.setConstraints(Collections.singletonList(constraint));
    policy.setAction(ProxyStageType.ID, FailActionType.ID);
    return tempEntity.newPolicy(policy);
  }

  @Test
  public void testDetails() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    eyesWatcher.eyesCheck();

    tile.headerTitle().shouldHave(text("Violation of Policy 1"));
    ElementsCollection elements = tile.headerSubtitle().findAll(".iq-violation-details__subtitle-part");
    elements.shouldHave(size(3));
    elements.get(0).shouldHave(text("Org 1"));
    elements.get(1).shouldHave(text("App 1"));
    elements.get(2).shouldHave(text("Group1 : Artifact1 : Version1"));

    tile.firstReported().shouldHave(text("2 days ago"));
    tile.lastReported().shouldHave(text("1 day ago"));
    tile.policyType().shouldHave(text("Security"));
    tile.threatLevel().shouldHave(text("7"));
    tile.policyOwnerLink().shouldHave(text("Root Organization"));

    tile.stages().shouldHave(size(5));

    tile.stage(0).shouldHave(text("Source"));
    tile.stage(0).icon().should(exist);
    tile.stage(0).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(1).shouldHave(text("Build 2d"));
    tile.stage(1).icon().should(exist);
    tile.stage(1).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(2).shouldHave(text("Stage"));
    tile.stage(2).icon().should(exist);
    tile.stage(2).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(3).shouldHave(text("Release 1d"));
    tile.stage(3).icon().shouldNot(exist);
    tile.stage(3).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    // Uncomment after fixing CLM-18676
    // tile.stage(4).shouldHave(text("Operate 1d"));
    // tile.stage(4).icon().should(exist);
    // tile.stage(4).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());
  }

  @Test
  public void testPolicyViolationInfo() {
    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    PolicyViolationConstraintInfo constraintInfo = violationDetailsPage.policyViolationConstraintInfo();
    PolicyViolationSecurityDetailsInfoTile securityDetailsInfoTile =
        violationDetailsPage.securityVulnerabilityDetailsTile();

    constraintInfo.headerTitle().shouldBe(visible).shouldHave(exactText("Policy Constraint"));
    constraintInfo.subheaderTitle()
        .shouldBe(visible)
        .shouldHave(exactText("Test Constraint is in violation for the following reason(s):"));
    constraintInfo.reasons().shouldHave(size(1));
    constraintInfo.reason(0).shouldHave(exactText("sonatype-2017-0507"));

    securityDetailsInfoTile.vulnerabilityDetailsHeader()
        .shouldBe(visible)
        .shouldHave(exactText("sonatype-2017-0507"));
  }

  @Test
  public void testSecurityPolicyViolationTabTiles() {
    // Set up a waiver for security violation
    List<ConstraintFact> constraintFacts = securityPolicyViolation.getConstraintFacts();
    String policyId = securityPolicyViolation.getPolicyId();
    String policyName = securityPolicyViolation.getPolicyName();
    String orgId = application.getParentOwnerId();

    tempEntity.newWaiver(
        securityPolicyViolation.getHash(), policyId, orgId, constraintFacts, "A waiver comment");

    refreshOrOpen(ViolationDetailsPage.url(securityPolicyViolation.getId()));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();

    SelenideElement vulnerabilityTab = violationDetailsPage.securityVulnerabilityDetailsTab();
    PolicyViolationApplicableWaiversTab waiversTab = violationDetailsPage.applicableWaiversTab();
    SelenideElement similarWaiversTab = violationDetailsPage.similarWaiversTab();

    PolicyViolationSecurityDetailsInfoTile securityDetailsInfoTile =
        violationDetailsPage.securityVulnerabilityDetailsTile();
    PolicyViolationApplicableWaiversInfoTile applicableWaiversInfoTile =
        violationDetailsPage.applicableWaiversInfoTile();
    PolicyViolationSimilarWaiversInfoTile similarWaiversInfoTile = violationDetailsPage.similarWaiversInfoTile();

    // Check tabs presence
    vulnerabilityTab.shouldBe(visible).shouldHave(exactText("Vulnerability Details"));
    waiversTab.shouldBe(visible).shouldHave(textCaseSensitive("1 Applicable Waivers"));
    similarWaiversTab.shouldBe(visible).shouldHave(textCaseSensitive("Similar Waivers"));

    // Check that default tab (security vulnerability) is displayed and that info is correct.
    securityDetailsInfoTile.vulnerabilityDetailsHeader()
        .shouldBe(visible)
        .shouldHave(exactText("sonatype-2017-0507"));
    applicableWaiversInfoTile.shouldNotBe(visible);
    similarWaiversInfoTile.shouldNotBe(visible);

    // Switch tabs, check visibility
    waiversTab.click();
    securityDetailsInfoTile.shouldNotBe(visible);
    applicableWaiversInfoTile.shouldBe(visible);
    similarWaiversInfoTile.shouldNotBe(visible);
    applicableWaiversInfoTile.waiverListHeader()
        .shouldBe(visible)
        .shouldHave(exactText("Waivers applicable to this violation of " + policyName));
    applicableWaiversInfoTile.getApplicableWaiversTable().shouldBe(visible);

    // Switch tabs, check visibility
    similarWaiversTab.click();
    similarWaiversInfoTile.shouldBe(visible);
    securityDetailsInfoTile.shouldNotBe(visible);
    applicableWaiversInfoTile.shouldNotBe(visible);
    similarWaiversInfoTile.waiverListHeader()
        .shouldBe(visible)
        .shouldHave(exactText("Waivers for similar violations of " + policyName));
    similarWaiversInfoTile.waiverListSubtitle()
        .shouldBe(visible)
        .shouldHave(exactText("Across all component versions implicated by sonatype-2017-0507"));

    // Switch tabs again
    vulnerabilityTab.click();
    securityDetailsInfoTile.shouldBe(visible);
    applicableWaiversInfoTile.shouldNotBe(visible);
  }

  @Test
  public void testGoDirectlyToAddWaiver() {
    refreshOrOpen(ViolationDetailsPage.urlWithQueryParams(securityPolicyViolation.getId(), "violation", "filter"));
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();
    ViolationDetailsPage.ViolationDetailsTile detailsTile = violationDetailsPage.detailsTile();

    detailsTile.addWaiverButton().shouldBe(visible);
    detailsTile.addWaiverButton().click();

    waitUntilUrl(AddWaiverPage.url(securityPolicyViolation.getId()));
    AddWaiverPage addWaiverPage = new AddWaiverPage();

    addWaiverPage.artifactName().shouldHave(text("Artifact1"));
    addWaiverPage.policyName().shouldHave(text("Policy 1"));
    addWaiverPage.constraintName().shouldHave(text("Test Constraint"));
  }

  private void mockHdsResponseForVulnerabilityDetails() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri("rest/vulnerability/details/json/sonatype-2017-0507");
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri("rest/vulnerability/details/json");
  }
}
