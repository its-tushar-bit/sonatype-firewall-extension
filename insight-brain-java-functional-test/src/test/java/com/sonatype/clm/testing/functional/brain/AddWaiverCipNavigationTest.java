/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.WaiverCip;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupLevelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.RelativePopularityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import com.codeborne.selenide.Configuration;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class AddWaiverCipNavigationTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "306e0a923df34c64b836358182b1b902";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    app = tempEntity.newApplicationWithParent("ApplicationReportTest", "ApplicationReportTest");
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, Configuration.baseUrl, work,
        StageTypes.BUILD.getId());

    // add Security policy
    createPolicy(app.getId(), 10, "SecurityPolicy", SecurityVulnerabilitySeverityConditionType.ID, "=", "9.1");
    // add License policy
    createPolicy(app.getId(), 5, "LicensePolicy", LicenseThreatGroupLevelConditionType.ID, ">=", "9");
    // add Quality policy
    createPolicy(app.getId(), 2, "QualityPolicy", RelativePopularityConditionType.ID, "<=", "1");
    // add Other policy
    createPolicy(Organization.ROOT_ORGANIZATION_ID, 1, "CoordinatesPolicy",
        CoordinatesConditionType.ID, "match", "maven:javancss*");
    evaluator.evaluatePolicy();

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @Test
  public void testNavigationToAddWaiverAndBackToCIP() {
    CipModal cipModal = reportPage.cipModal();

    reportPage.resultRow(1).click();
    cipModal.header().shouldHave(text("javancss : javancss : 29.50"));
    // open policy tab and verify routing to new add waiver page
    cipModal.tabLink(2).click();
    WaiverCip.rows().shouldHaveSize(2);

    // get policy violation id
    ComponentIdentifier componentIdentifierJavanCss = ComponentIdentifier.createMavenCoordinates(
        "javancss", "javancss", "29.50", "", "jar");
    String policyViolationId = getViolationForPolicyComponent("CoordinatesPolicy", componentIdentifierJavanCss);

    // click add waiver
    WaiverCip.row(1).waiveButton().shouldBe(visible, enabled).click();
    cipModal.shouldNotBe(visible);
    waitUntilUrl(AddWaiverPage.url(policyViolationId));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.saveButton().click();
    NxSubmitMask.seeAndWaitForDismissal();
    reportPage.shouldBe(visible);
    cipModal.shouldBe(visible);
    cipModal.header().shouldHave(text("javancss : javancss : 29.50"));
  }

  private String getViolationForPolicyComponent(String policyName, ComponentIdentifier componentIdentifier) {
    List<PolicyViolation> policyViolations = policyViolationDAO.getByApplicationId(app.getId());

    if (CollectionUtils.isNotEmpty(policyViolations)) {
      return policyViolations.stream()
          .filter(pv -> pv.getPolicyName().equals(policyName)
              && pv.getComponentIdentifier().equals(componentIdentifier))
          .findFirst()
          .map(PolicyViolation::getId)
          .orElse(null);
    }

    return null;
  }

  private Policy createPolicy(
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
    Condition condition = new Condition(conditionType, operator, value);
    constraint.setConditions(Collections.singletonList(condition));
    p.setConstraints(Collections.singletonList(constraint));
    return tempEntity.newPolicy(p);
  }
}
