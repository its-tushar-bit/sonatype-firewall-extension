/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallConfigurationModal;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantine;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineStatus;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallPageTest
    extends AbstractFunctionalTest
{
  private final FirewallPage page = new FirewallPage();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.RELEASE_INTEGRITY);

    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    //Clear the experimental feature flag after running the test
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);

    hardreset();
  }

  @Test
  public void testFirewallAutoUnquarantineFeatureIsNotSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(hidden);
    page.firewallQuarantineStatus().shouldBe(hidden);
    page.firewallAutoUnquarantineStatus().shouldBe(hidden);
    page.firewallQuarantine().shouldBe(hidden);
    page.firewallAutoReleaseQuarantine().shouldBe(hidden);
    page.firewallQuarantineTable().shouldBe(hidden);
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantineFeatureSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallStatus().shouldBe(visible);
    page.firewallQuarantineStatus().shouldBe(visible);
    page.firewallAutoUnquarantineStatus().shouldBe(visible);
    page.firewallQuarantine().shouldBe(visible);
    page.firewallAutoReleaseQuarantine().shouldBe(visible);
    page.firewallQuarantineTable().shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoReleaseQuarantine_showsCount() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantine firewallAutoUnquarantine = page.firewallAutoReleaseQuarantine();
    firewallAutoUnquarantine.shouldBe(visible);
    firewallAutoUnquarantine.shouldBe(visible);
    firewallAutoUnquarantine.cardContent().shouldBe(Condition.text("0"));
    firewallAutoUnquarantine.autoUnquarantineLink().shouldBe(visible);
  }

  @Test
  public void testFirewallAutoUnquarantine_OpenCloseModal() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);

    page.firewallAutoUnquarantineStatus().configureLink().click();
    page.firewallConfigurationModal().shouldBe(visible);
    page.firewallConfigurationModal().loadError().shouldBe(hidden);
    page.firewallConfigurationModal().saveButton().shouldBe(visible);
    page.firewallConfigurationModal().cancelButton().shouldBe(visible);
    page.firewallConfigurationModal().cancelButton().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineToggle().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineCheckBox().shouldNotBe(checked);

    page.firewallConfigurationModal().cancelButton().click();
    page.firewallConfigurationModal().shouldBe(hidden);
  }

  @Test
  @Ignore("Updating in separate PR")
  public void testFirewallAutoUnquarantine_EnableAutoUnquarantine() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus = page.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = page.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription().shouldHave(Condition.text("releasing 0 of 1 policy types"));

    //open modal
    firewallAutoUnquarantineStatus.configureLink().click();

    //verify initial configuration status
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().shouldNotBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible).shouldHave(Condition.cssClass("disabled")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(Condition.text("There are no changes to save."));

    //verify clicking disabled save button does nothing
    firewallConfigurationModal.saveButton().click();
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().shouldNotBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible).shouldHave(Condition.cssClass("disabled")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(Condition.text("There are no changes to save."));

    //toggle
    page.firewallConfigurationModal().autoUnquarantineToggle().click();

    //verify after toggle
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().input().shouldBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible, enabled);

    //save
    firewallConfigurationModal.saveButton().click();

    //after save
    NxSubmitMask.seeAndWaitForDismissal();
    firewallConfigurationModal.shouldBe(hidden);

    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring).isNotNull();
    assertThat(policyMonitoring.getOwnerId()).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());

    //verify auto unquarantine status after save
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Active"));
    firewallAutoUnquarantineStatus.statusDescription().shouldHave(Condition.text("releasing 1 of 1 policy types"));
  }

  @Test
  @Ignore("Updating in separate PR")
  public void testFirewallAutoUnquarantine_DisableAutoUnquarantine() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId());

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus = page.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = page.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Active"));
    firewallAutoUnquarantineStatus.statusDescription().shouldHave(Condition.text("releasing 1 of 1 policy types"));

    //open modal
    page.firewallAutoUnquarantineStatus().configureLink().click();

    //verify initial configuration status
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().input().shouldBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible).shouldHave(Condition.cssClass("disabled")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(Condition.text("There are no changes to save."));

    //verify clicking disabled save button does nothing
    firewallConfigurationModal.saveButton().click();
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().input().shouldBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible).shouldHave(Condition.cssClass("disabled")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(Condition.text("There are no changes to save."));

    //toggle
    page.firewallConfigurationModal().autoUnquarantineToggle().click();

    //verify after toggle
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().input().shouldNotBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible, enabled);

    //save
    firewallConfigurationModal.saveButton().click();

    //after save
    NxSubmitMask.seeAndWaitForDismissal();
    firewallConfigurationModal.shouldBe(hidden);

    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isNull();

    //verify auto unquarantine status after save
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription().shouldHave(Condition.text("releasing 0 of 1 policy types"));
  }

  @Test
  public void testFirewallAutoUnquarantine_LoadErrorTest() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = page.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //induce error by removing feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //open modal
    page.firewallAutoUnquarantineStatus().configureLink().click();

    //verify initial status with error
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.modalContent().shouldBe(hidden);
    firewallConfigurationModal.loadError().shouldBe(visible);
    firewallConfigurationModal.retryButton().shouldBe(visible);

    //resolve error
    testProductLicense.reset();

    //retry
    firewallConfigurationModal.retryButton().click();

    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().input().shouldNotBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible);
    firewallConfigurationModal.loadError().shouldBe(hidden);
    firewallConfigurationModal.retryButton().shouldBe(hidden);
  }

  @Test
  @Ignore("Updating in separate PR")
  public void testFirewallAutoUnquarantine_SaveErrorTest() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = page.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //open modal
    page.firewallAutoUnquarantineStatus().configureLink().click();

    //verify initial status
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().input().shouldNotBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible).shouldHave(Condition.cssClass("disabled"));

    //toggle
    page.firewallConfigurationModal().autoUnquarantineToggle().click();

    //verify after toggle
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().input().shouldBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible, enabled);

    //induce error by removing feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //try save
    firewallConfigurationModal.saveButton().click();

    //verify after error
    firewallConfigurationModal.loadError().shouldBe(visible);
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggle().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBox().input().shouldBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(hidden);
    firewallConfigurationModal.retryButton().shouldBe(visible);

    //resolve error
    testProductLicense.reset();

    //retry
    firewallConfigurationModal.retryButton().click();

    //after save
    NxSubmitMask.seeAndWaitForDismissal();
    firewallConfigurationModal.shouldBe(hidden);

    PolicyMonitoring policyMonitoring = policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring).isNotNull();
    assertThat(policyMonitoring.getOwnerId()).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(policyMonitoring.getStageTypeId()).isEqualTo(StageTypes.PROXY.getId());
  }

  @Test
  public void testFirewall_AutoUnquarantineLink() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallPage.url());

    page.shouldBe(visible);

    // click button
    page.firewallAutoReleaseQuarantine().autoUnquarantineLink().click();

    // verify firewall page loads
    waitUntilUrl(FirewallAutoUnquarantinePage.url());
  }
}
