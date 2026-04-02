/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.sbom;

import com.codeborne.selenide.Selenide;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.SbomManagerContinuousMonitoringPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SbomManagerContinuousMonitoringPageTest
    extends AbstractFunctionalTest
{
  private static SbomManagerContinuousMonitoringPage continuousMonitoringPage;

  private static Organization organization;

  private static Application application;

  @BeforeClass
  public static void beforeClass() {
    continuousMonitoringPage = new SbomManagerContinuousMonitoringPage();
    Selenide.open("/#");
    loginAsAdmin();
  }

  @Before
  public void beforeEachMethod() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.POLICY_MONITORING);
    organization = tempEntity.newOrganization("test-organization");
    application = tempEntity.newApplication("Test Application", "test-application", organization.getId());
    refreshOrOpen("about");
  }

  @Test
  @Ignore
  public void testSbomManagerContinuousMonitoring_EditorPage() {
    // Root Organization level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url("ROOT_ORGANIZATION_ID", true));
    continuousMonitoringPage.container().shouldBe(visible);
    continuousMonitoringPage.title().shouldHave(text("Continuous Monitoring"));
    continuousMonitoringPage.errorAlert().shouldNotBe(visible);
    continuousMonitoringPage.submitButton().shouldHave(text("Update"));
    continuousMonitoringPage.stageStatusLabel()
        .shouldHave(text(
            "Enable continuous monitoring for SBOM Manager"));
    continuousMonitoringPage.toggleInput().shouldNotBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(enabled);
    continuousMonitoringPage.toggleButton().shouldHave(text("Disabled"));

    // Sub Organization level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url(organization.getId(), true));
    continuousMonitoringPage.container().shouldBe(visible);
    continuousMonitoringPage.title().shouldHave(text("Continuous Monitoring"));
    continuousMonitoringPage.errorAlert().shouldNotBe(visible);
    continuousMonitoringPage.submitButton().shouldHave(text("Update"));
    continuousMonitoringPage.stageStatusLabel()
        .shouldHave(text(
            "Continuous Monitoring is currently disabled at the root organization. " +
                "Would you like to enable it for this organization and all its dependents?"));
    continuousMonitoringPage.toggleInput().shouldNotBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(enabled);
    continuousMonitoringPage.toggleButton().shouldHave(text("Disabled"));

    // Application level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url(application.getPublicId(), false));
    continuousMonitoringPage.container().shouldBe(visible);
    continuousMonitoringPage.title().shouldHave(text("Continuous Monitoring"));
    continuousMonitoringPage.errorAlert().shouldNotBe(visible);
    continuousMonitoringPage.submitButton().shouldHave(text("Update"));
    continuousMonitoringPage.stageStatusLabel()
        .shouldHave(text(
            "Continuous Monitoring is currently disabled at the root organization. " +
                "Would you like to enable it for this application?"));
    continuousMonitoringPage.toggleInput().shouldNotBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(enabled);
    continuousMonitoringPage.toggleButton().shouldHave(text("Disabled"));
  }

  @Test
  public void testSbomManagerContinuousMonitoring_EditorPage_Sub_Organization_Enabled() {
    // Sub Organization level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url(organization.getId(), true));
    continuousMonitoringPage.toggleButton().click();
    continuousMonitoringPage.toggleInput().shouldBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(enabled);
    continuousMonitoringPage.toggleButton().shouldHave(text("Enabled"));
    continuousMonitoringPage.submitButton().click();
    continuousMonitoringPage.errorAlert().shouldNotBe(visible);
    continuousMonitoringPage.stageStatusLabel().shouldHave(text("Disable continuous monitoring for SBOM Manager"));

    // Application level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url(application.getPublicId(), false));
    continuousMonitoringPage.stageStatusLabel()
        .shouldHave(text(
            "Continuous Monitoring is up and running at " + organization.getName() +
                ", so this means it's active for this application."));
    continuousMonitoringPage.toggleInput().shouldBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(disabled);
    continuousMonitoringPage.toggleButton().shouldBe(text("Enabled"));

    // Root Organization level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url("ROOT_ORGANIZATION_ID", true));
    continuousMonitoringPage.toggleInput().shouldNotBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(enabled);
    continuousMonitoringPage.stageStatusLabel()
        .shouldHave(text(
            "Enable continuous monitoring for SBOM Manager"));
  }

  @Test
  public void testSbomManagerContinuousMonitoring_EditorPage_Root_Organization_Enabled() {
    // Root Organization level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url("ROOT_ORGANIZATION_ID", true));
    continuousMonitoringPage.toggleInput().shouldNotBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(enabled);
    continuousMonitoringPage.toggleButton().shouldBe(text("Disabled"));
    continuousMonitoringPage.stageStatusLabel().shouldHave(text("Enable continuous monitoring for SBOM Manager"));
    continuousMonitoringPage.toggleButton().click();
    continuousMonitoringPage.toggleInput().shouldBe(checked);
    continuousMonitoringPage.toggleButton().shouldBe(text("Enabled"));
    continuousMonitoringPage.submitButton().click();
    continuousMonitoringPage.errorAlert().shouldNotBe(visible);
    continuousMonitoringPage.stageStatusLabel().shouldHave(text("Disable continuous monitoring for SBOM Manager"));

    // Sub Organization level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url(organization.getId(), true));
    continuousMonitoringPage.toggleInput().shouldBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(disabled);
    continuousMonitoringPage.toggleButton().shouldBe(text("Enabled"));
    continuousMonitoringPage.stageStatusLabel()
        .shouldHave(text(
            "Continuous Monitoring is up and running at Root Organization, " +
                "so this means it's active for this organization and all its dependents."));

    // Application level
    refreshOrOpen(SbomManagerContinuousMonitoringPage.url(application.getPublicId(), false));
    continuousMonitoringPage.stageStatusLabel()
        .shouldHave(text(
            "Continuous Monitoring is up and running at Root Organization, " +
                "so this means it's active for this application."));
    continuousMonitoringPage.toggleInput().shouldBe(checked);
    continuousMonitoringPage.toggleInput().shouldBe(disabled);
    continuousMonitoringPage.toggleButton().shouldBe(text("Enabled"));
    continuousMonitoringPage.submitButton().click();
    continuousMonitoringPage.errorAlert()
        .shouldHave(text("There were validation errors. There are no changes to save."));
  }
}
