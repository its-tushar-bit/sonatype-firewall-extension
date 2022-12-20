/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.version.graph;

import java.util.Collections;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.VersionsCIP;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;

public abstract class AbstractVersionGraphMavenTest
    extends AbstractFunctionalTest
{
  public static final String JAVA_SCRIPT_TO_EXECUTE =
      "Insight.setCoordinates('maven', {\"groupId\":\"javancss\", \"artifactId\":\"javancss\", \"version\":\"29.50\"," +
          " \"extension\":\"jar\", \"classifier\":\"\"},{\"matchState\":\"exact\",\"proprietary\":\"false\"," +
          "\"filename\":\"aaaa\", \"hash\":\"aaa\", \"appId\":\"ApplicationReportTest\"});";

  private Application appFirst;

  private Application app;

  private Application appLast;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() {
    appFirst = tempEntity.newApplicationWithParent("app-123", "app-123");
    appLast = tempEntity.newApplicationWithParent("SomeApp", "Some App");
    app = tempEntity.newApplicationWithParent("ApplicationReportTest", "ApplicationReportTest");
    // add Security policy
    createPolicy(app.getId(), 10, "SecurityPolicy", SecurityVulnerabilitySeverityConditionType.ID, "=", "9.1");
    // add License policy
    createPolicy(app.getId(), 5, "LicensePolicy", LicenseThreatGroupLevelConditionType.ID, ">=", "9");
    // add Quality policy
    createPolicy(app.getId(), 2, "QualityPolicy", RelativePopularityConditionType.ID, "<=", "1");
    // add Other policy
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "CoordinatesPolicy", CoordinatesConditionType.ID, "match",
        "maven:javancss*");

    refreshOrOpen(getStartPageUrl());
    if (isApplicationSelectionNeeded()) {
      VersionsCIP.selectApplications().selectByIndex(0);
    }
  }

  protected abstract String getStartPageUrl();

  @Test
  public void testApplicationsInAlphabeticalOrder() {
    if (isApplicationSelectionNeeded()) {
      refreshOrOpen(getStartPageUrl());
      verifyMessageIsPresentWhenNoApplicationIsSelected();
      VersionsCIP.applicationsElement().shouldBe(visible);
      VersionsCIP.applicationsElement().shouldNotBe(selected);
      VersionsCIP.applicationsElement().selectOption(0);
      VersionsCIP.applicationsElement().getSelectedOption().shouldHave(text(appFirst.getName()));
      VersionsCIP.applicationsElement().selectOption(1);
      VersionsCIP.applicationsElement().getSelectedOption().shouldHave(text(app.getName()));
      VersionsCIP.applicationsElement().selectOption(2);
      VersionsCIP.applicationsElement().getSelectedOption().shouldHave(text(appLast.getName()));
    }
  }

  @Test
  public void testVersionGraph() {
    if (isApplicationSelectionNeeded()) {
      setupHdsResponses();
      mockHdsResponseForRemediation();
      mockHdsResponseForFirstComponent();
      VersionsCIP.selectApplications().selectByVisibleText("ApplicationReportTest (ApplicationReportTest)");
      executeJavaScript(JAVA_SCRIPT_TO_EXECUTE);

      VersionsCIP.versionGraph().shouldHave(attribute("height", "153"));
      VersionsCIP.versionGraphLabels().shouldHave(exactTexts(
          "Popularity",
          "Policy Threat",
          "Details",
          "Security",
          "License",
          "Quality",
          "Other"
      ));
    }
  }
  
  @Test
  public void testCIPWithRemediation() {
    setupHdsResponses();
    mockHdsResponseForRemediation();
    mockHdsResponseForFirstComponent();

    if (isApplicationSelectionNeeded()) {
      verifyMessageIsPresentWhenNoApplicationIsSelected();
      VersionsCIP.selectApplications().selectByVisibleText("ApplicationReportTest (ApplicationReportTest)");
    }

    executeJavaScript(JAVA_SCRIPT_TO_EXECUTE);

    VersionsCIP.componentType().shouldHave(text("maven"));
    VersionsCIP.groupId().shouldHave(text("javancss"));
    VersionsCIP.artifactId().shouldHave(text("javancss"));
    VersionsCIP.version().shouldHave(text("29.50"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Apache-2.0"));
    VersionsCIP.observedLicenses().shouldHave(texts("GPL-2.0"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Apache-2.0", "GPL-2.0"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("10"), cssClass("critical"));
    VersionsCIP.policyCount().shouldHave(exactText("within 3 policies"));
    VersionsCIP.highestSecurityThreat().shouldHave(text("9.1"));
    VersionsCIP.securityCount().shouldHave(exactText("within 3 security issues"));
    VersionsCIP.hygieneRating().shouldHave(text("Exemplar"));
    VersionsCIP.integrityRating().shouldHave(text("Normal"));
    VersionsCIP.integrityRating().shouldNotHave(cssClass("cip-color-suspicious"));
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.componentCategory().shouldHave(text("Programming Language Utilites"));
    if (isVersionRecommendationSupported()) {
      VersionsCIP.recommendedVersionsHeader().shouldBe(visible);
      VersionsCIP.nextNoViolationVersionLink().shouldBe(visible).shouldHave(text("Select 31.52"));
      VersionsCIP.nextNoFailVersionLink().shouldBe(hidden);
    }
    else {
      VersionsCIP.recommendedVersionsHeader().shouldBe(hidden);
      VersionsCIP.nextNoViolationVersionLink().shouldBe(hidden);
      VersionsCIP.nextNoFailVersionLink().shouldBe(hidden);
    }
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    if (shouldShowMigrateButton()) {
      VersionsCIP.migrateButton().shouldBe(visible);
    }
    else {
      VersionsCIP.migrateButton().shouldNotBe(visible);
    }
    VersionsCIP.noVersionsAvailable().shouldNotBe(visible);

    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);

    // test hovering over version bar shows version number
    VersionsCIP.versionBar(1).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(1).shouldHave(text("21.41"));
    VersionsCIP.versionBar(2).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(2).shouldHave(text("25.45"));
    VersionsCIP.versionBar(3).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(3).shouldHave(text("26.46"));
    VersionsCIP.versionBar(4).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(4).shouldHave(text("28.49"));
    VersionsCIP.versionBar(5).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(5).shouldHave(text("29.50"));
    VersionsCIP.versionBar(6).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(6).shouldHave(text("30.51"));

    // mock request for version 21.41
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-21.41.json"))
        .atUri("rest/ide/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-21.41.json"))
        .atUri("rest/rm/componentDetails");

    VersionsCIP.versionBar(1).shouldBe(visible).click();
    VersionsCIP.version().shouldHave(text("21.41"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Not Declared"));
    VersionsCIP.observedLicenses().shouldHave(texts("No Sources"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Not Declared", "No Sources"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("1"), cssClass("none"));
    VersionsCIP.policyCount().shouldNotBe(visible);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.securityCount().shouldNotBe(visible);
    VersionsCIP.hygieneRating().shouldNotBe(visible);

    if (isVersionRecommendationSupported()) {
      // mock request for version 31.52
      testCLMServer.getHdsServer()
          .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-31.52.json"))
          .atUri("rest/ide/componentDetails");
      testCLMServer.getHdsServer()
          .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-31.52.json"))
          .atUri("rest/rm/componentDetails");

      VersionsCIP.selectNoViolation().shouldBe(visible).click();

      VersionsCIP.version().shouldHave(text("31.52"));
      VersionsCIP.declaredLicenses().shouldHave(texts("BSD-3-Clause"));
      VersionsCIP.observedLicenses().shouldHave(texts("BSD-3-Clause"));
      VersionsCIP.effectiveLicenses().shouldHave(texts("BSD-3-Clause"));
      VersionsCIP.highestPolicyThreat().shouldHave(text("NA"), cssClass("unspecified"));
      VersionsCIP.policyCount().shouldNotBe(visible);
      VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
      VersionsCIP.securityCount().shouldNotBe(visible);
      VersionsCIP.hygieneRating().shouldNotBe(visible);
    }
  }

  @Test
  public void testCIPWithoutRemediation() {
    setupHdsResponsesForNoRemediation();
    mockHdsResponseForFirstComponent();
    mockHdsResponseForRemediation();

    if (isApplicationSelectionNeeded()) {
      VersionsCIP.selectApplications().selectByVisibleText("ApplicationReportTest (ApplicationReportTest)");
    }

    executeJavaScript(JAVA_SCRIPT_TO_EXECUTE);

    VersionsCIP.componentType().shouldHave(text("maven"));
    VersionsCIP.groupId().shouldHave(text("javancss"));
    VersionsCIP.artifactId().shouldHave(text("javancss"));
    VersionsCIP.version().shouldHave(text("29.50"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Apache-2.0"));
    VersionsCIP.observedLicenses().shouldHave(texts("GPL-2.0"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Apache-2.0", "GPL-2.0"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("10"), cssClass("critical"));
    VersionsCIP.policyCount().shouldHave(exactText("within 3 policies"));
    VersionsCIP.highestSecurityThreat().shouldHave(text("9.1"));
    VersionsCIP.securityCount().shouldHave(exactText("within 3 security issues"));
    VersionsCIP.hygieneRating().shouldHave(text("Exemplar"));
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.componentCategory().shouldHave(text("Programming Language Utilites"));
    if (isVersionRecommendationSupported()) {
      VersionsCIP.recommendedVersionsHeader().shouldBe(visible);
      VersionsCIP.nextNoViolationVersionLink().shouldBe(hidden);
      VersionsCIP.nextNoFailVersionLink().shouldBe(hidden);
      VersionsCIP.noVersionsAvailable().shouldBe(visible);
    }
    else {
      VersionsCIP.recommendedVersionsHeader().shouldBe(hidden);
      VersionsCIP.nextNoViolationVersionLink().shouldBe(hidden);
      VersionsCIP.nextNoFailVersionLink().shouldBe(hidden);
      VersionsCIP.noVersionsAvailable().shouldBe(hidden);
    }
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    if (shouldShowMigrateButton()) {
      VersionsCIP.migrateButton().shouldBe(visible);
    }
    else {
      VersionsCIP.migrateButton().shouldNotBe(visible);
    }

    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);
  }

  @Test
  public void testCapabilitiesEnable() {
    setupHdsResponses();
    mockHdsResponseForRemediation();
    mockHdsResponseForFirstComponent();

    if (isApplicationSelectionNeeded()) {
      VersionsCIP.selectApplications().selectByVisibleText("ApplicationReportTest (ApplicationReportTest)");
    }

    executeJavaScript(JAVA_SCRIPT_TO_EXECUTE);

    VersionsCIP.groupId().shouldHave(text("javancss"));
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    if (shouldShowMigrateButton()) {
      VersionsCIP.migrateButton().shouldBe(visible);
    }
    else {
      VersionsCIP.migrateButton().shouldNotBe(visible);
    }

    executeJavaScript("Insight.setCapabilities({viewDetails: false, migrate: false})");
    VersionsCIP.viewDetailsButton().shouldNotBe(visible);
    VersionsCIP.migrateButton().shouldNotBe(visible);

    eyesWatcher.eyesCheck("Component Info Screen");

    executeJavaScript("Insight.setCapabilities({\"viewDetails\": true, \"migrate\": true})");
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    VersionsCIP.migrateButton().shouldBe(visible);

    executeJavaScript("Insight.setCapabilities({'viewDetails': true, 'migrate': false})");
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    VersionsCIP.migrateButton().shouldNotBe(visible);

    executeJavaScript("Insight.setCapabilities({viewDetails: false})");
    VersionsCIP.viewDetailsButton().shouldNotBe(visible);
    VersionsCIP.migrateButton().shouldNotBe(visible);

    executeJavaScript(
        String.format("Insight.setCapabilities({viewDetails: null, migrate: true})"));
    VersionsCIP.viewDetailsButton().shouldNotBe(visible);
    VersionsCIP.migrateButton().shouldBe(visible);

  }

  @Test
  public void testBreakingChangesHeatmap() {
    setupHdsResponsesForBreakingChanges();

    if (isApplicationSelectionNeeded()) {
      VersionsCIP.selectApplications().selectByVisibleText("ApplicationReportTest (ApplicationReportTest)");
    }

    executeJavaScript(JAVA_SCRIPT_TO_EXECUTE);

    VersionsCIP.versionGraphLoading().should(disappear);

    VersionsCIP.viewDetailsButton().shouldBe(visible);

    VersionsCIP.groupId().shouldHave(text("javancss"));
    VersionsCIP.viewDetailsButton().shouldBe(visible);
  }

  private void verifyMessageIsPresentWhenNoApplicationIsSelected() {
    if (VersionsCIP.selectApplications().getFirstSelectedOption() == null) {
      VersionsCIP.selectAnApplicationMessage().shouldBe(visible).shouldHave(text("Select an application."));
    }
  }

  protected Policy createPolicy(String ownerId,
                              int threatLevel,
                              String name,
                              String conditionType,
                              String operator,
                              String value)
  {
    Policy p = new Policy(null, name);
    p.setThreatLevel(threatLevel);
    p.setOwnerId(ownerId);
    Constraint constraint = new Constraint(null, name + " constraint", LogicalOperator.AND);
    com.sonatype.insight.brain.model.policy.Condition condition = new com.sonatype.insight.brain.model.policy.Condition(
        conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    p.setConstraints(Collections.singletonList(constraint));
    return tempEntity.newPolicy(p);
  }

  protected void mockHdsResponseForFirstComponent() {
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/ide/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetails-29.50.json"))
        .atUri("rest/rm/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri("rest/component/dependencies");
  }

  protected void setupHdsResponses() {
    mockHdsResponseForFirstComponent();
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/componentDetails/javancssComponentDetailsListWithNoViolationsVersion.json"))
        .atUri("rest/ide/componentDetails/list");
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/componentDetails/javancssComponentDetailsListWithNoViolationsVersion.json"))
        .atUri("rest/ci/componentDetails/list");
    testCLMServer.getHdsServer()
        .respondWith(
            getClass().getResource("/componentDetails/javancssComponentDetailsListWithNoViolationsVersion.json"))
        .atUri("rest/rm/componentDetails/list");
  }

  protected void setupHdsResponsesForNoRemediation() {
    mockHdsResponseForFirstComponent();
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetailsList.json"))
        .atUri("rest/ide/componentDetails/list");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetailsList.json"))
        .atUri("rest/ci/componentDetails/list");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/javancssComponentDetailsList.json"))
        .atUri("rest/rm/componentDetails/list");
  }

  protected void setupHdsResponsesForBreakingChanges() {
    mockHdsResponseForFirstComponent();
    testCLMServer.getHdsServer()
            .respondWith(getClass().getResource("/componentDetails/componentDetailsBreakingChangesList.json"))
            .atUri("rest/ide/componentDetails/list");
    testCLMServer.getHdsServer()
            .respondWith(getClass().getResource("/componentDetails/componentDetailsBreakingChangesList.json"))
            .atUri("rest/ci/componentDetails/list");
    testCLMServer.getHdsServer()
            .respondWith(getClass().getResource("/componentDetails/componentDetailsBreakingChangesList.json"))
            .atUri("rest/rm/componentDetails/list");
  }

  protected void mockHdsResponseForRemediation() {
    testCLMServer.getHdsServer().respondWith("{\"known\":true}").atUri("rest/component/summary");
  }

  protected abstract boolean shouldShowMigrateButton();

  protected boolean isVersionRecommendationSupported() {
    return true;
  }

  protected boolean isApplicationSelectionNeeded() {
    return false;
  }
}
