/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.net.URL;
import java.util.Date;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.*;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockServer;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

import static com.codeborne.selenide.Condition.*;

public class FirewallContainerReportPageTest
    extends AbstractFunctionalTest
{
  public static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  private PolicyEvaluationDAO policyEvaluationDAO;

  @Test
  public void testContent() throws Exception {
    setFeatures(
        LicensedFeature.CONTAINER_IMAGES_EVALUATION,
        LicensedFeature.FIREWALL_AUTO_UNQUARANTINE,
        LicensedFeature.APPLICATION_EVALUATION,
        LicensedFeature.APPLICATION_REPORTS);
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);

    hardreset();
    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();

    policyEvaluationDAO = lookup(PolicyEvaluationDAO.class);

    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization();
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplication("ContainerReportTest", "ContainerReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/large-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work, Stage.ID_PROXY);
    evaluator.evaluatePolicyForScanIdWithScanTriggerType(ScanTriggerType.CLI);

    mockHdsResponseForDownloadingReport(HdsMockServer.RestServlet.SCAN_ID);
    PolicyEvaluation policyEvaluation = policyEvaluationDAO.getLastByApplicationIdAndScanId(app.getId(), SCAN_ID);
    Date policyEvaluationTime = policyEvaluation.getTime();

    String policyEvaluationTimeStr = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss 'UTC'Z")
        .print(policyEvaluationTime.getTime());

    refreshOrOpen(ApplicationReportPage.firewallContainerReportUrl(app.getPublicId(), SCAN_ID));

    reportPage.shouldBe(visible);
    reportPage.reportTitle().shouldHave(text(app.getName() + " Proxy Report"));
    reportPage.reportDescription()
        .shouldHave(text("Triggered by " + policyEvaluation.getScanTriggerType().getDisplayName()));
    reportPage.reportDescription().shouldNotHave(text("(Continuous Monitoring)"));
    reportPage.reportDescription().shouldNotHave(text("(Re-evaluation)"));
    reportPage.reportDescription().shouldHave(text("on " + policyEvaluationTimeStr));
    reportPage.reportDescription().shouldHave(text(policyEvaluation.getCommitHash()));

    reportPage.reportApplicationRiskScoreDescription().shouldHave(text("CONTAINER RISK SCORE"));

    reportPage.reevaluateButton().shouldHave(text("Re-Evaluate Container"));
    reportPage.goToDependencyTreeButton().shouldNotBe(visible);
    reportPage.filterToggle().shouldNotBe(visible);
  }
}
