/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.componentdetails.InnerSourceProducerAlert;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.InnerSourceProducerPermissionsModal;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.InnerSourceProducerReportModal;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.ResultRow;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.security.MemberType.USER;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;

public class ComponentDetailsInnerSourceTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization("Test Organization");
    policyImportExport.importOrganization(org, referencePolicies);
    app = tempEntity.newApplicationWithSpecificId("8bbaa746602142d9adf2de00a9ca4d4a", "ApplicationReportTest",
        "ApplicationReportTest", org.getId());
    URL zippedReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work);
    evaluator.evaluatePolicy();
    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
  }

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testOverviewTab_innerSourceProducerAlertNewVersionModal() {
    String packageUrl = InnerSourceUtils
        .getVersionlessPackageUrl(ComponentIdentifier.createMavenCoordinates("java2html", "j2h", "1.3.1", "", "jar"))
        .getPackageUrl();
    tempEntity.newInnerSourceComponent(packageUrl, app, "0.0.0");

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage =
        openComponentDetailsPageForViolationAtRow(5, "18d393ad345b03b49c62");
    componentDetailsPage.overviewTab().shouldBe(visible);

    InnerSourceProducerAlert innerSourceProducerAlert = new InnerSourceProducerAlert();
    innerSourceProducerAlert.content().shouldBe(visible);
    innerSourceProducerAlert.latestReportLink().shouldBe(visible);
    innerSourceProducerAlert.shouldBe(visible);
    innerSourceProducerAlert.latestReportLink().click();

    InnerSourceProducerReportModal innerSourceProducerReportModal = new InnerSourceProducerReportModal();
    innerSourceProducerReportModal.shouldBe(visible);
    innerSourceProducerReportModal.header().shouldHave(exactText("Newer Component Version Found in Report"));
    innerSourceProducerReportModal.content()
        .shouldHave(exactText("A newer version of the InnerSource component is being used in the latest report."));
    innerSourceProducerReportModal.continueToReportButton().shouldBe(visible);
    innerSourceProducerReportModal.cancelButton().click();
    innerSourceProducerReportModal.shouldNotBe(visible);
  }

  @Test
  public void testOverviewTab_innerSourceProducerAlertInsufficientPermissionModal() {
    logout();
    User user = tempEntity.newUser("user-no-permission", "user", "no-permission", "user@no-permission");
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, user.getUsername(), USER);
    tempEntity.newApplicationWithSpecificId("09895fab56384b61adb3161fa1ec58cd", "ApplicationReportTest2",
        "ApplicationReportTest2", app.getOrganizationId());
    login(user.getUsername(), user.getPassword());

    refreshOrOpen(ApplicationReportPage.url(app, SCAN_ID));
    ComponentDetailsPage componentDetailsPage =
        openComponentDetailsPageForViolationAtRow(11, "cefa389a797ca9d030ef");
    componentDetailsPage.overviewTab().shouldBe(visible);

    InnerSourceProducerAlert innerSourceProducerAlert = new InnerSourceProducerAlert();
    innerSourceProducerAlert.content().shouldBe(visible);
    innerSourceProducerAlert.latestReportLink().shouldBe(visible);
    innerSourceProducerAlert.shouldBe(visible);
    innerSourceProducerAlert.latestReportLink().click();

    InnerSourceProducerPermissionsModal innerSourceProducerPermissionsModal =
        new InnerSourceProducerPermissionsModal();
    innerSourceProducerPermissionsModal.shouldBe(visible);
    innerSourceProducerPermissionsModal.header().shouldHave(exactText("Insufficient Permissions"));
    innerSourceProducerPermissionsModal.content()
        .shouldHave(exactText("Insufficient permissions to view the report for ApplicationReportTest2. Please contact"
            + " your Policy Administrator or an Owner to request access."));
    innerSourceProducerPermissionsModal.closeButton().shouldBe(visible).click();
    innerSourceProducerPermissionsModal.shouldNotBe(visible);

    logout();
    loginAsAdmin();
  }

  private ComponentDetailsPage openComponentDetailsPageForViolationAtRow(
      int rowIndex,
      String hash)
  {
    ResultRow resultRow = reportPage.resultRow(rowIndex);
    resultRow.click();
    waitUntilUrl(ComponentDetailsPage.urlToOverview(app, SCAN_ID,
        hash));
    return new ComponentDetailsPage();
  }
}
