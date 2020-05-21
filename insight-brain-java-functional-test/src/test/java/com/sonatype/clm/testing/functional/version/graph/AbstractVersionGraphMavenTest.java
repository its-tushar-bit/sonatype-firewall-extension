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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public abstract class AbstractVersionGraphMavenTest
    extends AbstractFunctionalTest
{
  public static final String JAVA_SCRIPT_TO_EXECUTE =
      "Insight.setCoordinates('maven', {\"groupId\":\"javancss\", \"artifactId\":\"javancss\", \"version\":\"29.50\"," +
          " \"extension\":\"jar\", \"classifier\":\"\"},{\"matchState\":\"exact\",\"proprietary\":\"false\"," +
          "\"filename\":\"aaaa\", \"hash\":\"aaa\", \"appId\":\"ApplicationReportTest\"});";

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() {
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

    refreshOrOpen("assets/version-graph/ide/eclipse/index.html");
  }

  @Test
  public void testCIPWithRemediation() {
    setupHdsResponses();
    mockHdsResponseForRemediation();
    mockHdsResponseForFirstComponent();

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
    VersionsCIP.recommendedVersionsHeader().shouldBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldHave(text("Select 31.52"));
    VersionsCIP.nextNoFailVersionLink().shouldNotBe(visible);
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
    eyesWatcher.eyesCheck("Component Info Screen");

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

  @Test
  public void testCIPWithoutRemediation() {
    setupHdsResponsesForNoRemediation();
    mockHdsResponseForFirstComponent();
    mockHdsResponseForRemediation();

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
    VersionsCIP.recommendedVersionsHeader().shouldBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldNotBe(visible);
    VersionsCIP.nextNoFailVersionLink().shouldNotBe(visible);
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    if (shouldShowMigrateButton()) {
      VersionsCIP.migrateButton().shouldBe(visible);
    }
    else {
      VersionsCIP.migrateButton().shouldNotBe(visible);
    }
    VersionsCIP.noVersionsAvailable().shouldBe(visible);

    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);
  }

  @Test
  public void testCapabilitiesEnable() {
    setupHdsResponses();
    mockHdsResponseForRemediation();
    mockHdsResponseForFirstComponent();

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

  protected void mockHdsResponseForRemediation() {
    testCLMServer.getHdsServer().respondWith("{\"known\":true}").atUri("rest/component/summary");
  }

  protected abstract boolean shouldShowMigrateButton();
}
