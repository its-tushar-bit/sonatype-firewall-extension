/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.FirewallCDPPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.facts.MatchFact;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.policy.evaluator.ComponentPolicyEvaluator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.ElementsCollection;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallCDPTest
    extends AbstractFunctionalTest
{
  private static final ComponentIdentifier CRITICAL_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("critical",
      "threat", "1.0");

  private static final String MATCH_STATE_POLICY_NAME = "Match State Policy";

  private static final String COORDINATES_POLICY_NAME = "Coordinates Policy";

  FirewallCDPPage firewallCDPPage = new FirewallCDPPage();

  private Repository repo;

  private Policy matchStatePolicy;

  private Policy coordinatesPolicy;

  private String criticalComponentHash;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY);
    repo = tempEntity.newRepository("testRepo");

    matchStatePolicy = createPolicy(10, MATCH_STATE_POLICY_NAME, MatchStateConditionType.ID, "is",
        MatchState.EXACT.toString());
    coordinatesPolicy =
        createPolicy(9, COORDINATES_POLICY_NAME, CoordinatesConditionType.ID, "match", "maven:critical:*");

    new InsightWork(testCLMServer.getCLMServer().getConfiguration());
  }

  private Policy createPolicy(
      final int threatLevel,
      final String name,
      final String conditionType,
      final String op,
      final String value)
  {
    final Policy p = new Policy(null, name);
    p.setThreatLevel(threatLevel);
    p.setOwnerId(Organization.ROOT_ORGANIZATION_ID);
    final Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    final com.sonatype.insight.brain.model.policy.Condition condition =
        new com.sonatype.insight.brain.model.policy.Condition(
            conditionType, op, value);
    constraint.setConditions(Collections.singletonList(condition));
    p.setConstraints(Collections.singletonList(constraint));
    return tempEntity.newPolicy(p);
  }

  private void createPolicyViolation(final RepositoryComponent component, final Policy policy) {
    final Constraint constraint = policy.getConstraints().get(0);
    final ConstraintFact constraintFact = new ConstraintFact(constraint.getId(), constraint.getName(),
        constraint.getOperator().name());

    final Component c = new Component(component.getComponentIdentifier());
    c.setMatchState(MatchState.EXACT);
    final int conditionIndex = 0;
    constraintFact.addConditionFact(ComponentPolicyEvaluator
        .createConditionFact(policy.getConstraints().get(0).getConditions().get(conditionIndex), new MatchFact(c,
            policy.getId(), policy.getConstraints().get(0).getId(), Collections.emptyList())));

    final RepositoryPolicyViolation violation = tempEntity.newRepositoryPolicyViolation(component.getRepositoryId(),
        policy.getThreatLevel(), component.getPathname(), false, Action.ID_FAIL, policy.getId(), policy.getName(),
        component.getComponentIdentifier());

    violation.setConstraintFacts(Collections.singletonList(constraintFact));
    new RepositoryPolicyViolationDAO().update(violation);
  }

  private void setupHdsResponse() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("componentDetails/componentDetails.json"))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("componentDetails/componentDetailsList.json"))
        .atUri("rest/ci/componentDetails/list");
  }

  private void setupHdsFirewallResponse() {
    final ComponentEvaluationData component = new ComponentEvaluationData();
    component.componentIdentifier = CRITICAL_IDENTIFIER;
    component.declaredLicenses = Collections.emptySet();
    component.observedLicenses = Collections.emptySet();
    component.hash = criticalComponentHash;
    component.matchState = MatchState.EXACT.toString();
    final ComponentEvaluationDataList response = new ComponentEvaluationDataList();
    response.components.add(component);
    testCLMServer.getHdsServer().respondWith(response).atUri("rest/component/details/firewall");
  }

  private RepositoryComponent cdpSetup() {
    final RepositoryComponent component = tempEntity.newRepositoryComponent(repo.getId(), MatchState.EXACT,
        CRITICAL_IDENTIFIER, true);
    criticalComponentHash = component.getHash();

    createPolicyViolation(component, matchStatePolicy);
    createPolicyViolation(component, coordinatesPolicy);

    setupHdsFirewallResponse();
    setupHdsResponse();
    return component;
  }

  private Wait<WebDriver> getWebDriverAwait() {
    return new FluentWait<>(getWebDriver())
        .withTimeout(Duration.ofSeconds(240))
        .pollingEvery(Duration.ofSeconds(2))
        .ignoring(NoSuchElementException.class);
  }

  private void waitUntilSpinnersGone() {
    Wait<WebDriver> wait = getWebDriverAwait();
    wait.until(ExpectedConditions.invisibilityOf(firewallCDPPage.getAllLoadingSpinners().get(0)));
    firewallCDPPage.getAllLoadingSpinners().shouldHave(size(0));
  }

  @Test
  public void testTitle() throws UnsupportedEncodingException {
    RepositoryComponent component = cdpSetup();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();
    firewallCDPPage.title().should(exist).shouldHave(text("critical : threat : 1.0"));
    eyesWatcher.eyesCheck("Firewall component details page.");
  }

  @Test
  public void testFormatTag() throws UnsupportedEncodingException {
    RepositoryComponent component = cdpSetup();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();
    firewallCDPPage.formatTag().should(exist).shouldHave(text("Maven"));
  }

  @Test
  public void testTabs() throws UnsupportedEncodingException {
    RepositoryComponent component = cdpSetup();
    refreshOrOpen(FirewallCDPPage.url(component));
    waitUntilSpinnersGone();
    ElementsCollection tabs = firewallCDPPage.tabs();
    tabs.shouldHaveSize(5);
    tabs.first().shouldHave(cssClass("active"));

    assertThat(getWebDriver().getCurrentUrl()).contains("/" + component.getMatchStateId() + "?");

    tabs.get(1).click();
    tabs.get(1).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/violations?");

    tabs.get(2).click();
    tabs.get(2).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/security?");

    tabs.get(3).click();
    tabs.get(3).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/legal?");

    tabs.get(4).click();
    tabs.get(4).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/labels?");

    tabs.get(0).click();
    tabs.get(0).shouldHave(cssClass("active"));
    assertThat(getWebDriver().getCurrentUrl()).contains("/overview?");
  }
}
