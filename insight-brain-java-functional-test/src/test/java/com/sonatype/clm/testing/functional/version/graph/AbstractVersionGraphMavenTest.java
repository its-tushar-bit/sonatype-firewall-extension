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
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
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
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

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
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

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
      VersionsCIP.selectApplications().selectByIndex(1);
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
      VersionsCIP.applicationsElement().selectOption(1);
      VersionsCIP.applicationsElement().getSelectedOption().shouldHave(text(appFirst.getName()));
      VersionsCIP.applicationsElement().selectOption(2);
      VersionsCIP.applicationsElement().getSelectedOption().shouldHave(text(app.getName()));
      VersionsCIP.applicationsElement().selectOption(3);
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
      VersionsCIP.versionGraphLabels()
          .shouldHave(exactTexts(
              "Popularity",
              "Policy Threat",
              "Details",
              "Security",
              "License",
              "Quality",
              "Other"));
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
    VersionsCIP.highestPolicyThreat().shouldHave(text("10"));
    VersionsCIP.highestPolicyThreatIndicator().shouldHave(cssClass("nx-threat-indicator--critical"));
    VersionsCIP.policyCount().shouldHave(exactText("3"));
    VersionsCIP.highestSecurityThreat().shouldHave(text("9.1"));
    VersionsCIP.securityCount().shouldHave(exactText("3"));
    VersionsCIP.hygieneRating().shouldHave(text("Exemplar"));
    VersionsCIP.integrityRating().shouldHave(text("Normal"));
    VersionsCIP.integrityRating().shouldNotHave(cssClass("iq-version-graph-component-details__suspicious-integrity"));
    VersionsCIP.componentCategory().shouldHave(text("Programming Language Utilites"));
    VersionsCIP.viewDetailsButton().shouldBe(visible);

    VersionsCIP.showDetailsLink().shouldBe(visible).click();

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
    VersionsCIP.highestPolicyThreat().shouldHave(text("1"));
    VersionsCIP.highestPolicyThreatIndicator().shouldHave(cssClass("nx-threat-indicator--low"));
    VersionsCIP.policyCount().shouldNotBe(visible);
    VersionsCIP.highestSecurityThreat().shouldHave(text("N/A"));
    VersionsCIP.securityCount().shouldNotBe(visible);
    VersionsCIP.hygieneRating().shouldNotBe(visible);
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
    VersionsCIP.highestPolicyThreat().shouldHave(text("10"));
    VersionsCIP.highestPolicyThreatIndicator().shouldHave(cssClass("nx-threat-indicator--critical"));
    VersionsCIP.policyCount().shouldHave(exactText("3"));
    VersionsCIP.highestSecurityThreat().shouldHave(text("9.1"));
    VersionsCIP.securityCount().shouldHave(exactText("3"));
    VersionsCIP.hygieneRating().shouldHave(text("Exemplar"));
    VersionsCIP.componentCategory().shouldHave(text("Programming Language Utilites"));
    VersionsCIP.viewDetailsButton().shouldBe(visible);

    VersionsCIP.showDetailsLink().shouldBe(visible).click();
  }

  private void verifyMessageIsPresentWhenNoApplicationIsSelected() {
    if (VersionsCIP.selectApplications().getFirstSelectedOption() == null) {
      VersionsCIP.selectAnApplicationMessage().shouldBe(visible).shouldHave(text("Select an application."));
    }
  }

  protected Policy createPolicy(
      String ownerId,
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

  protected boolean isApplicationSelectionNeeded() {
    return false;
  }
}
