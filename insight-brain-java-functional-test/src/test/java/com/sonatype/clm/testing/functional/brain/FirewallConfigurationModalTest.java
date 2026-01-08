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
import com.sonatype.clm.testing.functional.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.functional.pages.FirewallConfigurationModal;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.FirewallPageComponents.FirewallAutoUnquarantineStatus;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.policy.AutoUnquarantinePolicyConditionTypeDAO;
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
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class FirewallConfigurationModalTest
    extends AbstractFunctionalTest
{
  private final FirewallAutoUnquarantinePage firewallAutoUnquarantinePage = new FirewallAutoUnquarantinePage();

  private final int supportedConditionTypesExcludingIntegrityRatingCount =
      (int) ConditionTypes.getAllWithAutoUnquarantineSupported()
          .stream()
          .filter(conditionType -> !conditionType.getId().equals(IntegrityRatingConditionType.ID))
          .count();

  private AutoUnquarantinePolicyConditionTypeDAO autoUnquarantinePolicyConditionTypeDAO;

  @Before
  public void before() {
    autoUnquarantinePolicyConditionTypeDAO = lookup(AutoUnquarantinePolicyConditionTypeDAO.class);

    setFeatures(LicensedFeature.FIREWALL_AUTO_UNQUARANTINE, LicensedFeature.RELEASE_INTEGRITY);

    refreshOrOpen(FirewallPage.url());
    loginAsAdmin();
  }

  @After
  public void after() {
    hardreset();
  }

  @Test
  public void testFirewallConfigurationModal_InfoAlertAndReadMoreLink() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    firewallAutoUnquarantinePage.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus
        = firewallAutoUnquarantinePage.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallAutoUnquarantinePage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //open modal
    firewallAutoUnquarantineStatus.configureLink().click();

    //verify modal is visible
    firewallConfigurationModal.shouldBe(visible);

    //verify info alert is displayed
    firewallConfigurationModal.infoAlert().shouldBe(visible);
    firewallConfigurationModal.infoAlert()
        .shouldHave(Condition.text(
            "Components will only auto-release from quarantine if its status changes within the 14 day window."
            )
        );

    //verify "Read More" link is displayed
    firewallConfigurationModal.readMoreLink().shouldBe(visible);
    firewallConfigurationModal.readMoreLink().shouldHave(Condition.text("Read More"));

    //verify link has correct href
    firewallConfigurationModal.readMoreLink()
        .shouldHave(Condition.attribute("href",
            "https://links.sonatype.com/products/firewall/doc/automatic-quarantine-release"));

    //verify link opens in new tab
    firewallConfigurationModal.readMoreLink()
        .shouldHave(Condition.attribute("target", "_blank"));

    //verify link has security attributes
    firewallConfigurationModal.readMoreLink()
        .shouldHave(Condition.attribute("rel", "noreferrer"));

    //close modal
    firewallConfigurationModal.cancelButton().click();
    firewallConfigurationModal.shouldBe(hidden);
  }

  @Test
  public void testFirewallConfigurationModal_DefaultValues() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    firewallAutoUnquarantinePage.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus
        = firewallAutoUnquarantinePage.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallAutoUnquarantinePage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription()
        .shouldHave(Condition.text("releasing 0 of 11 policy condition types"));

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
    firewallConfigurationModal.saveButton().shouldBe(visible).click();
    FormUtils.getAlertElement(firewallConfigurationModal)
        .shouldHave(Condition.text(DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));

    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().shouldNotBe(checked);
    IntStream.range(1, supportedConditionTypesExcludingIntegrityRatingCount + 1).forEach(index -> {
      firewallConfigurationModal.autoUnquarantineToggleWithIndex(index).shouldBe(visible);
      firewallConfigurationModal.autoUnquarantineCheckBoxWithIndex(index).input().shouldNotBe(checked);
    });
    firewallConfigurationModal.cancelButton().shouldBe(visible);
  }

  @Test
  public void testFirewallConfigurationModal_ToggleIntegrityRating() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    firewallAutoUnquarantinePage.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus
        = firewallAutoUnquarantinePage.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallAutoUnquarantinePage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription()
        .shouldHave(Condition.text("releasing 0 of 11 policy condition types"));

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
        .shouldHave(Condition.text("releasing 1 of 11 policy condition types"));
  }

  @Test
  public void testFirewallConfigurationModal_ToggleSingle() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    firewallAutoUnquarantinePage.shouldBe(visible);

    final FirewallAutoUnquarantineStatus firewallAutoUnquarantineStatus
        = firewallAutoUnquarantinePage.firewallAutoUnquarantineStatus();
    firewallAutoUnquarantineStatus.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallAutoUnquarantinePage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //verify initial auto unquarantine status
    firewallAutoUnquarantineStatus.statusIndicatorIcon().shouldBe(visible);
    firewallAutoUnquarantineStatus.statusIndicatorIconActive().shouldBe(hidden);
    firewallAutoUnquarantineStatus.statusLabel().shouldHave(Condition.text("Inactive"));
    firewallAutoUnquarantineStatus.statusDescription()
        .shouldHave(Condition.text("releasing 0 of 11 policy condition types"));

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
        .shouldHave(Condition.text("releasing 1 of 11 policy condition types"));
  }

  @Test
  public void testFirewallConfigurationModal_SaveErrorTest() {
    refreshOrOpen(FirewallAutoUnquarantinePage.url());

    firewallAutoUnquarantinePage.shouldBe(visible);

    FirewallConfigurationModal firewallConfigurationModal = firewallAutoUnquarantinePage.firewallConfigurationModal();
    firewallConfigurationModal.shouldBe(hidden);

    //open modal
    firewallAutoUnquarantinePage.firewallAutoUnquarantineStatus().configureLink().click();

    //verify initial status
    firewallConfigurationModal.shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineToggleIntegrityRating().shouldBe(visible);
    firewallConfigurationModal.autoUnquarantineCheckBoxIntegrityRating().input().shouldNotBe(checked);
    firewallConfigurationModal.cancelButton().shouldBe(visible);

    //toggle
    firewallAutoUnquarantinePage.firewallConfigurationModal().autoUnquarantineToggleIntegrityRating().click();

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
