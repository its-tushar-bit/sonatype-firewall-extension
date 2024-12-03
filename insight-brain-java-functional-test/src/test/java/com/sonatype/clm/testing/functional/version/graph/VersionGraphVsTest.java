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
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class VersionGraphVsTest
    extends AbstractFunctionalTest
{
  public static final String JAVA_SCRIPT_TO_EXECUTE =
      "Insight.setCoordinates(\"nuget\", {\"packageId\":\"EntityFramework\",\"version\":\"4.3.0\"}, " +
          "{\"appId\":\"ApplicationReportTest\",\"hash\":\"hashValue\",\"matchState\":\"exact\"," +
          "\"proprietary\":\"false\",\"fileName\":\"fullInstallPath.nupkg\"});";

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

    refreshOrOpen("assets/version-graph/ide/visual-studio/index.html");
  }

  @Test
  public void testCIPWithRemediation() {
    setupHdsResponses();
    mockHdsResponseForRemediation();
    mockHdsResponseForFirstComponent();

    executeJavaScript(JAVA_SCRIPT_TO_EXECUTE);

    VersionsCIP.version().shouldHave(text("4.3.0"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Non-Standard"));
    VersionsCIP.observedLicenses().shouldHave(texts("Not Provided"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Non-Standard"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("2"), cssClass("moderate"));
    VersionsCIP.policyCount().shouldNotBe(visible);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.securityCount().shouldNotBe(visible);
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.recommendedVersionsHeader().shouldBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldHave(text("Select 5.0.0"));
    VersionsCIP.nextNoFailVersionLink().shouldNotBe(visible);
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    VersionsCIP.migrateButton().shouldNotBe(visible);
    VersionsCIP.noVersionsAvailable().shouldNotBe(visible);
    VersionsCIP.componentCategory().shouldHave(text("Other"));

    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);

    // test hovering over version bar shows version number
    VersionsCIP.versionBar(1).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(1).shouldHave(text("4.1.10311.0"));
    VersionsCIP.versionBar(2).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(2).shouldHave(text("4.1.10715.0"));
    VersionsCIP.versionBar(3).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(3).shouldHave(text("4.2.0.0"));
    VersionsCIP.versionBar(4).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(4).shouldHave(text("4.3.0-beta1"));
    VersionsCIP.versionBar(5).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(5).shouldHave(text("4.3.0"));
    VersionsCIP.versionBar(6).shouldBe(visible).hover();
    VersionsCIP.versionBarHoverText(6).shouldHave(text("4.3.1"));

    // mock request for version 21.41
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/componentDetailsEntityFramework-4-1-10311.json"))
        .atUri("rest/ide/componentDetails");

    VersionsCIP.versionBar(1).shouldBe(visible).click();
    VersionsCIP.version().shouldHave(text("4.1.10311.0"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Non-Standard"));
    VersionsCIP.observedLicenses().shouldHave(texts("Not Provided"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Non-Standard"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("2"), cssClass("moderate"));
    VersionsCIP.policyCount().shouldNotBe(visible);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.securityCount().shouldNotBe(visible);

    // mock request for version 31.52
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/componentDetailsEntityFramework-5-beta.json"))
        .atUri("rest/ide/componentDetails");

    VersionsCIP.selectNoViolation().shouldBe(visible).click();

    VersionsCIP.version().shouldHave(text("5.0.0"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Non-Standard"));
    VersionsCIP.observedLicenses().shouldHave(texts("Not Provided"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Non-Standard"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("NA"), cssClass("unspecified"));
    VersionsCIP.policyCount().shouldNotBe(visible);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.securityCount().shouldNotBe(visible);
  }

  @Test
  public void testCIPWithoutRemediation() {
    setupHdsResponsesForNoRemediation();
    mockHdsResponseForFirstComponent();
    mockHdsResponseForRemediation();

    executeJavaScript(JAVA_SCRIPT_TO_EXECUTE);

    VersionsCIP.version().shouldHave(text("4.3.0"));
    VersionsCIP.declaredLicenses().shouldHave(texts("Non-Standard"));
    VersionsCIP.observedLicenses().shouldHave(texts("Not Provided"));
    VersionsCIP.effectiveLicenses().shouldHave(texts("Non-Standard"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("2"), cssClass("moderate"));
    VersionsCIP.policyCount().shouldNotBe(visible);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.securityCount().shouldNotBe(visible);
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.recommendedVersionsHeader().shouldBe(visible);
    VersionsCIP.nextNoViolationVersionLink().shouldNotBe(visible);
    VersionsCIP.nextNoFailVersionLink().shouldNotBe(visible);
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    VersionsCIP.migrateButton().shouldNotBe(visible);
    VersionsCIP.noVersionsAvailable().shouldBe(visible);
    VersionsCIP.componentCategory().shouldHave(text("Other"));

    VersionsCIP.showDetailsLink().shouldBe(visible).click();
    VersionsCIP.hideDetailsLink().shouldBe(visible);
  }

  @Test
  public void testCapabilitiesEnable() {
    setupHdsResponses();
    mockHdsResponseForRemediation();
    mockHdsResponseForFirstComponent();

    executeJavaScript(JAVA_SCRIPT_TO_EXECUTE);
    VersionsCIP.version().shouldHave(text("4.3.0"));
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    VersionsCIP.migrateButton().shouldNotBe(visible);
    VersionsCIP.componentCategory().shouldHave(text("Other"));

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

    executeJavaScript("Insight.setCapabilities({viewDetails: null, migrate: true})");
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
        .respondWith(getClass().getResource("/componentDetails/componentDetailsEntityFramework-4-3-0.json"))
        .atUri("rest/ide/componentDetails");
  }

  protected void setupHdsResponses() {
    mockHdsResponseForFirstComponent();
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/componentDetailsListEntityFramework.json"))
        .atUri("rest/ide/componentDetails/list");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/componentDetailsListEntityFramework.json"))
        .atUri("rest/ci/componentDetails/list");
  }

  protected void setupHdsResponsesForNoRemediation() {
    mockHdsResponseForFirstComponent();
    testCLMServer.getHdsServer().respondWith(
        getClass().getResource("/componentDetails/componentDetailsListEntityFrameworkNoGoodVersion.json"))
        .atUri("rest/ide/componentDetails/list");
    testCLMServer.getHdsServer()
        .respondWith(getClass().getResource("/componentDetails/componentDetailsListEntityFrameworkNoGoodVersion.json"))
        .atUri("rest/ci/componentDetails/list");
  }

  protected void mockHdsResponseForRemediation() {
    testCLMServer.getHdsServer().respondWith("{\"known\":true}").atUri("rest/component/summary");
  }
}
