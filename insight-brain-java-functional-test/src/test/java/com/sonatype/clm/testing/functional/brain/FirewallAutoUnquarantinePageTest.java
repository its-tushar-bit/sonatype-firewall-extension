/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallConfigurationModal;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineMtd;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineStatus;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineYtd;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallPolicyConditionTypes;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.google.common.collect.ImmutableMap.of;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallAutoUnquarantinePageTest
    extends AbstractFunctionalTest
{
  private final FirewallAutoUnquarantinePage page = new FirewallAutoUnquarantinePage();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL, LicensedFeature.RELEASE_INTEGRITY);

    refreshOrOpen(FirewallAutoUnquarantinePage.url());
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
  public void testFirewallAutoUnquarantinePageAutoUnquarantineFeatureIsNotSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), false));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);
    page.firewallAutoUnquarantineStatus().shouldBe(hidden);
    page.firewallAutoReleaseQuarantineMtd().shouldBe(hidden);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(hidden);
    page.firewallUnquarantineTable().shouldBe(hidden);
    page.firewallConfigurationModal().shouldBe(hidden);
    page.firewallPolicyConditionTypes().shouldBe(hidden);
    page.backToFirewallButton().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoUnquarantineFeatureSet() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);
    page.firewallAutoUnquarantineStatus().shouldBe(visible);
    page.firewallAutoReleaseQuarantineMtd().shouldBe(visible);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(visible);
    page.firewallUnquarantineTable().shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
    page.firewallPolicyConditionTypes().shouldBe(visible);
    page.backToFirewallButton().shouldBe(visible);
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoReleaseQuarantineMtd_showsCount() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantineMtd firewallAutoUnquarantineMtd = page.firewallAutoReleaseQuarantineMtd();
    firewallAutoUnquarantineMtd.shouldBe(visible);
    firewallAutoUnquarantineMtd.shouldBe(visible);
    firewallAutoUnquarantineMtd.cardContent().shouldBe(Condition.text("0"));
  }

  @Test
  public void testFirewallAutoUnquarantinePageAutoReleaseQuarantineYtd_showsCount() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());
    page.shouldBe(visible);

    FirewallAutoUnquarantineYtd firewallAutoUnquarantineYtd = page.firewallAutoReleaseQuarantineYtd();
    firewallAutoUnquarantineYtd.shouldBe(visible);
    firewallAutoUnquarantineYtd.shouldBe(visible);
    firewallAutoUnquarantineYtd.cardContent().shouldBe(Condition.text("0"));
  }

  @Test
  public void testFirewallAutoUnquarantinePage_OpenCloseModal() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);

    page.firewallAutoUnquarantineStatus().configureLink().click();
    page.firewallConfigurationModal().shouldBe(visible);
    page.firewallConfigurationModal().loadError().shouldBe(hidden);
    page.firewallConfigurationModal().saveButton().shouldBe(visible);
    page.firewallConfigurationModal().cancelButton().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineToggle().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineCheckBox().shouldNotBe(checked);

    page.firewallConfigurationModal().cancelButton().click();
    page.firewallConfigurationModal().shouldBe(hidden);

    page.firewallPolicyConditionTypes().moreLink().click();
    page.firewallConfigurationModal().shouldBe(visible);
    page.firewallConfigurationModal().loadError().shouldBe(hidden);
    page.firewallConfigurationModal().saveButton().shouldBe(visible);
    page.firewallConfigurationModal().cancelButton().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineToggle().shouldBe(visible);
    page.firewallConfigurationModal().autoUnquarantineCheckBox().shouldNotBe(checked);
  }

  @Test
  public void testFirewallAutoUnquarantinePage_EnableAutoUnquarantineFromStatus() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus = page.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = page.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription().shouldBe(hidden);

    //open modal
    firewallAutoUnquarantineStatus.configureLink().click();

    //toggle
    page.firewallConfigurationModal().autoUnquarantineToggle().click();

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
    firewallAutoUnquarantineStatus.statusDescription().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantine_DisableAutoUnquarantineFromStatus() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId());

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus = page.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = page.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Active"));
    firewallAutoUnquarantineStatus.statusDescription().shouldBe(hidden);

    //open modal
    page.firewallAutoUnquarantineStatus().configureLink().click();

    //toggle
    page.firewallConfigurationModal().autoUnquarantineToggle().click();

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
    firewallAutoUnquarantineStatus.statusDescription().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantinePage_EnableAutoUnquarantineFromPolicyConditionTypes() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    final FirewallPolicyConditionTypes firewallPolicyConditionTypes = page.firewallPolicyConditionTypes();
    firewallPolicyConditionTypes.shouldBe(visible);

    final FirewallConfigurationModal firewallConfigurationModal = page.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //open modal
    firewallPolicyConditionTypes.moreLink().click();

    //toggle
    page.firewallConfigurationModal().autoUnquarantineToggle().click();

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
    page.firewallAutoUnquarantineStatus().statusIndicatorIcon().shouldBe(visible);
    page.firewallAutoUnquarantineStatus().statusIndicatorIconActive().shouldBe(visible);
    page.firewallAutoUnquarantineStatus().statusLabel().shouldHave(Condition.text("Active"));
    page.firewallAutoUnquarantineStatus().statusDescription().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantine_DisableAutoUnquarantineFromPolicyConditionTypes() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));
    tempEntity.newPolicyMonitoring(REPOSITORY_CONTAINER_ID, StageTypes.PROXY.getId());

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    final FirewallPolicyConditionTypes firewallPolicyConditionTypes = page.firewallPolicyConditionTypes();
    firewallPolicyConditionTypes.shouldBe(visible);

    final FirewallConfigurationModal firewallConfigurationModal = page.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //open modal
    page.firewallAutoUnquarantineStatus().configureLink().click();

    //toggle
    page.firewallConfigurationModal().autoUnquarantineToggle().click();

    //save
    firewallConfigurationModal.saveButton().click();

    //after save
    NxSubmitMask.seeAndWaitForDismissal();
    firewallConfigurationModal.shouldBe(hidden);

    assertThat(policyMonitoringDAO.getByOwnerId(REPOSITORY_CONTAINER_ID)).isNull();

    //verify auto unquarantine status after save
    page.firewallAutoUnquarantineStatus().statusIndicatorIcon().shouldBe(visible);
    page.firewallAutoUnquarantineStatus().statusIndicatorIconActive().shouldBe(hidden);
    page.firewallAutoUnquarantineStatus().statusLabel().shouldHave(Condition.text("Inactive"));
    page.firewallAutoUnquarantineStatus().statusDescription().shouldBe(hidden);
  }

  @Test
  public void testFirewallAutoUnquarantine_BackToFirewallButton() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    page.shouldBe(visible);

    // click button
    page.backToFirewallButton().click();

    // verify firewall page loads
    waitUntilUrl(FirewallPage.url());
  }

  @Test
  public void testFirewallAutoUnquarantinePage_LoadErrorTest() {
    testCLMServer.getCLMServer().getConfiguration()
        .setExperimentalFeatures(of(Feature.FIREWALL_AUTO_UNQUARANTINE.getFlag(), true));

    //induce error by removing feature
    testProductLicense.setMissingFeatures(LicensedFeature.FIREWALL);

    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    //verify initial status with error
    page.shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
    page.firewallAutoUnquarantineStatus().shouldBe(hidden);
    page.firewallAutoReleaseQuarantineMtd().shouldBe(hidden);
    page.firewallPolicyConditionTypes().shouldBe(hidden);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(hidden);
    page.loadError().shouldBe(visible);
    page.retryButton().shouldBe(visible);

    //resolve error
    testProductLicense.reset();

    //retry
    page.retryButton().click();

    page.shouldBe(visible);
    page.firewallConfigurationModal().shouldBe(hidden);
    page.firewallAutoUnquarantineStatus().shouldBe(visible);
    page.firewallAutoReleaseQuarantineMtd().shouldBe(visible);
    page.firewallPolicyConditionTypes().shouldBe(visible);
    page.firewallAutoReleaseQuarantineYtd().shouldBe(visible);
    page.loadError().shouldBe(hidden);
    page.retryButton().shouldBe(hidden);
  }
}
