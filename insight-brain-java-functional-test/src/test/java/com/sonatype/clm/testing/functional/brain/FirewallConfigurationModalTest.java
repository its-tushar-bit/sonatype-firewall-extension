/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;
import java.util.stream.IntStream;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.FirewallConfigurationModal;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineStatus;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyMonitoringDAO;
import com.sonatype.insight.brain.model.policy.AutoUnquarantinePolicyConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.IntegrityRatingConditionType;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.checked;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallConfigurationModalTest
    extends AbstractFunctionalTest
{
  private final FirewallPage firewallPage = new FirewallPage();

  private final AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO =
      new AutoUnquarantinePolicyConditionTypeDAO();

  private final int supportedConditionTypesExcludingIntegrityRatingCount =
      (int) ConditionTypes.getAllWithAutoUnquarantineSupported()
          .stream()
          .filter(conditionType -> !conditionType.getId().equals(IntegrityRatingConditionType.ID))
          .count();

  private final PolicyMonitoringDAO policyMonitoringDAO = new PolicyMonitoringDAO();

  @Before
  public void before() {
    setFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY);

    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    policyMonitoringDAO.getAll().forEach(policyMonitoringDAO::delete);
    autoUnquarantinePolicyConditionTypeDAO.getAll().forEach(autoUnquarantinePolicyConditionTypeDAO::delete);

    hardreset();
  }

  @Test
  public void testFirewallConfigurationModal_DefaultValues() {
    refreshOrOpen(FirewallPage.url());

    firewallPage.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus = firewallPage.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallPage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription()
        .shouldHave(Condition.text("releasing 0 of 6 policy condition types"));

    //open modal
    firewallAutoUnquarantineStatus.configureLink().click();

    //verify initial configuration status
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().shouldNotBe(checked);
    IntStream.range(1, supportedConditionTypesExcludingIntegrityRatingCount + 1).forEach(index -> {
      firewallConfigurationModal.autoUnquarantineToggleWithIndex(index).shouldBe(visible);
      firewallConfigurationModal.autoUnquarantineCheckBoxWithIndex(index).input().shouldNotBe(checked);
    });
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible).shouldHave(Condition.cssClass("disabled")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(Condition.text("There are no changes to save."));

    //verify clicking disabled save button does nothing
    firewallConfigurationModal.saveButton().click();
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().shouldNotBe(checked);
    IntStream.range(1, supportedConditionTypesExcludingIntegrityRatingCount + 1).forEach(index -> {
      firewallConfigurationModal.autoUnquarantineToggleWithIndex(index).shouldBe(visible);
      firewallConfigurationModal.autoUnquarantineCheckBoxWithIndex(index).input().shouldNotBe(checked);
    });
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible).shouldHave(Condition.cssClass("disabled")).hover();
    Tooltip.get().shouldBe(visible).shouldHave(Condition.text("There are no changes to save."));
  }

  @Test
  public void testFirewallConfigurationModal_ToggleIntegrityRating() {
    refreshOrOpen(FirewallPage.url());

    firewallPage.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus = firewallPage.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallPage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription()
        .shouldHave(Condition.text("releasing 0 of 6 policy condition types"));

    //open modal
    firewallAutoUnquarantineStatus.configureLink().click();

    //toggle
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().click();

    //verify after toggle
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().input().shouldBe(checked);
    IntStream.range(1, supportedConditionTypesExcludingIntegrityRatingCount + 1).forEach(index -> {
      firewallConfigurationModal.autoUnquarantineToggleWithIndex(index).shouldBe(visible);
      firewallConfigurationModal.autoUnquarantineCheckBoxWithIndex(index).input().shouldNotBe(checked);
    });
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible, enabled);

    //save
    firewallConfigurationModal.saveButton().click();

    //after save
    NxSubmitMask.seeAndWaitForDismissal();
    firewallConfigurationModal.shouldBe(hidden);

    AutoUnquarantinePolicyConditionType autoUnquarantinePolicyConditionType =
        autoUnquarantinePolicyConditionTypeDAO.getById(IntegrityRatingConditionType.ID);
    assertThat(autoUnquarantinePolicyConditionType).isNotNull();

    //verify auto unquarantine status after save
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Active"));
    firewallAutoUnquarantineStatus.statusDescription()
        .shouldHave(Condition.text("releasing 1 of 6 policy condition types"));
  }

  @Test
  public void testFirewallConfigurationModal_ToggleSingle() {
    refreshOrOpen(FirewallPage.url());

    firewallPage.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus = firewallPage.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallPage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription()
        .shouldHave(Condition.text("releasing 0 of 6 policy condition types"));

    //open modal
    firewallAutoUnquarantineStatus.configureLink().click();

    //toggle
    firewallConfigurationModal.autoUnquarantineToggleWithIndex(1).click();

    //verify after toggle
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().input().shouldNotBe(checked);
    IntStream.range(2, supportedConditionTypesExcludingIntegrityRatingCount + 1).forEach(index -> {
      firewallConfigurationModal.autoUnquarantineToggleWithIndex(index).shouldBe(visible);
      firewallConfigurationModal.autoUnquarantineCheckBoxWithIndex(index).input().shouldNotBe(checked);
    });
    firewallConfigurationModal.autoUnquarantineCheckBoxWithIndex(1).shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxWithIndex(1).shouldBe(visible);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible, enabled);

    //save
    firewallConfigurationModal.saveButton().click();

    //after save
    NxSubmitMask.seeAndWaitForDismissal();
    firewallConfigurationModal.shouldBe(hidden);

    List<AutoUnquarantinePolicyConditionType> autoUnquarantinePolicyConditionTypes =
        autoUnquarantinePolicyConditionTypeDAO.getAll();
    assertThat(autoUnquarantinePolicyConditionTypes).isNotNull().isNotEmpty().hasSize(1);

    //verify auto unquarantine status after save
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Active"));
    firewallAutoUnquarantineStatus.statusDescription()
        .shouldHave(Condition.text("releasing 1 of 6 policy condition types"));
  }

  @Test
  public void testFirewallConfigurationModal_LoadErrorTest() {
    refreshOrOpen(FirewallPage.url());

    firewallPage.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallPage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //induce error by removing feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //open modal
    firewallPage.firewallAutoUnquarantineStatus().configureLink().click();

    //verify initial status with error
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.modalContent().shouldBe(hidden);
    firewallConfigurationModal.loadError().shouldBe(visible);
    firewallConfigurationModal.retryButton().shouldBe(visible);

    //resolve error
    testProductLicense.reset();

    //retry
    firewallConfigurationModal.retryButton().click();

    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().input().shouldNotBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible);
    firewallConfigurationModal.loadError().shouldBe(hidden);
    firewallConfigurationModal.retryButton().shouldBe(hidden);
  }

  @Test
  public void testFirewallConfigurationModal_SaveErrorTest() {
    refreshOrOpen(FirewallPage.url());

    firewallPage.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallPage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //open modal
    firewallPage.firewallAutoUnquarantineStatus().configureLink().click();

    //verify initial status
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().input().shouldNotBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible).shouldHave(Condition.cssClass("disabled"));

    //toggle
    firewallPage.firewallConfigurationModal().autoUnquarantineToggleIntegrityRating().click();

    //verify after toggle
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().input().shouldBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);
    firewallConfigurationModal.saveButton().shouldBe(visible, enabled);

    //induce error by removing feature
    testProductLicense.setMissingFeatures(LicensedFeature.RELEASE_INTEGRITY);

    //try save
    firewallConfigurationModal.saveButton().click();

    //verify after error
    firewallConfigurationModal.loadError().shouldBe(visible);
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().input().shouldBe(checked);
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

    List<AutoUnquarantinePolicyConditionType> autoUnquarantinePolicyConditionTypes =
        autoUnquarantinePolicyConditionTypeDAO.getAll();
    assertThat(autoUnquarantinePolicyConditionTypes).isNotNull().isNotEmpty().hasSize(1);
  }
}
