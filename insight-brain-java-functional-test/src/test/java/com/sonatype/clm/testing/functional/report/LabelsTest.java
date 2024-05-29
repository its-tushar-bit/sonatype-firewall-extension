/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.report;

import java.io.IOException;
import java.net.URL;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.LabelsCIP;
import com.sonatype.clm.testing.functional.elements.LabelsCIP.AddLabelModal;
import com.sonatype.clm.testing.functional.elements.LabelsCIP.RemoveLabelModal;
import com.sonatype.clm.testing.functional.elements.ReportCip;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ReportPage;
import com.sonatype.clm.testing.functional.pages.ReportPolicyPage;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.security.MemberType.USER;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;

public class LabelsTest
    extends AbstractFunctionalTest
{
  public static final String BUILD_SCAN_ID = "BUILD_SCAN_ID";

  private Application app;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() throws IOException {
    URL referencePolicyUrl = getClass().getResource("/reference-policies-v3.json");
    PolicyExportResult referencePolicies = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    PolicyImportExport policyImportExport = lookup(PolicyImportExport.class);

    Organization org = tempEntity.newOrganization("Org");
    policyImportExport.importOrganization(org, referencePolicies);
    tempEntity.newUser("user1", "first", "last", "a@a.com");
    app = tempEntity.newApplication("app", "app", org.getId(), "user1");
    URL zippedSmallReport = ReportHelper.zipReport("/canned-reports/small-report", tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

    TestReportEvaluator evaluatorBuild = new TestReportEvaluator(app, BUILD_SCAN_ID, zippedSmallReport,
        Configuration.baseUrl, work);
    evaluatorBuild.evaluatePolicy();
  }

  private void browseToCipLabelsTab() {
    refreshOrOpen(ReportPage.url(app, BUILD_SCAN_ID));
    ReportPage.policyTabButton().click();
    ReportPolicyPage.row(1).openCip();
    ReportCip.labelsTab().should(appear).click();
  }

  @Test
  public void testCipLabels() {
    browseToCipLabelsTab();

    LabelsCIP.appliedLabels().shouldHave(size(0));
    LabelsCIP.availableLabels().shouldHave(size(3));
    LabelsCIP.availableLabel(1).shouldHave(text("Architecture-Blacklisted"));
    LabelsCIP.availableLabel(2).shouldHave(text("Architecture-Cleanup"));
    LabelsCIP.availableLabel(3).shouldHave(text("Architecture-Deprecated")).action().click();

    AddLabelModal.saveButton().shouldBe(visible).click();
    AddLabelModal.root().shouldBe(hidden);

    LabelsCIP.appliedLabels().shouldHave(size(1));
    LabelsCIP.availableLabels().shouldHave(size(2));

    // login with lesser privileges
    refreshOrOpen(ReportListPage.url());
    logout();
    User user = tempEntity.newUser("username", "john", "doe", "john@doe");
    tempEntity.newMembershipMapping(app.getId(), DEVELOPER_ROLE_ID, user.getUsername(), USER);
    login(user.getUsername(), user.getPassword());
    browseToCipLabelsTab();

    LabelsCIP.appliedLabels().shouldHave(size(1));
    LabelsCIP.availableLabels().shouldHave(size(2));

    LabelsCIP.availableLabel(1).shouldHave(text("Architecture-Blacklisted")).action().click();
    AddLabelModal.root().should(appear);
    AddLabelModal.error().shouldBe(visible).shouldHave(text("Insufficient Permissions"));
    AddLabelModal.closeButton().shouldBe(visible).click();
    AddLabelModal.root().should(disappear);

    LabelsCIP.appliedLabel(1).shouldHave(text("Architecture-Deprecated")).action().click();
    RemoveLabelModal.root().should(appear);
    RemoveLabelModal.error().shouldBe(visible).shouldHave(text("Insufficient Permissions"));
    RemoveLabelModal.closeButton().shouldBe(visible).click();
    RemoveLabelModal.root().should(disappear);

    LabelsCIP.appliedLabels().shouldHave(size(1));
    LabelsCIP.availableLabels().shouldHave(size(2));

    // re-login as admin
    refreshOrOpen(ReportListPage.url());
    logout();
    loginAsAdmin();
    browseToCipLabelsTab();

    LabelsCIP.appliedLabel(1).shouldHave(text("Architecture-Deprecated")).action().click();
    RemoveLabelModal.confirmButton().shouldBe(visible).click();
    RemoveLabelModal.root().shouldBe(hidden);
    LabelsCIP.availableLabels().shouldHave(size(3));
  }
}
