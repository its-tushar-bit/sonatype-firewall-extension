/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.LegacyViolationModal;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class LegacyViolationsTest
    extends AbstractFunctionalTest
{
  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  private PolicyDAO policyDAO;

  private PolicyViolationDAO policyViolationDAO;

  private ApplicationDAO applicationDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    policyDAO = lookup(PolicyDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);

    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);
    application.setLegacyViolationEnabled(true);
    applicationDAO.update(application);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));
  }

  @Test
  public void testLegacyViolations_ModalInitialState_LegacyViolationEnabled() {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.legacyViolation().shouldBe(visible).shouldNotBe(DISABLED).hover();
    if (Tooltip.get().getElement().isDisplayed()) {
      Tooltip.get().shouldNotHave(text("Legacy Violations are not enabled for this application."));
      Tooltip.get().shouldHave(text("Legacy existing violations "));
    }
    ActionDropDown.legacyViolation().click();
    LegacyViolationModal modal = new LegacyViolationModal();
    modal.shouldBe(visible);
    modal.updateButton().shouldBe(visible);
    modal.retryButton().shouldBe(hidden);
    modal.cancelButton().shouldBe(visible);
  }

  @Test
  public void testLegacyViolations_ModalInitialState_LegacyViolationDisabled() {
    application.setLegacyViolationEnabled(false);
    applicationDAO.update(application);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.legacyViolation().shouldBe(visible).shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Legacy Violations are not enabled for this application."));
    ActionDropDown.legacyViolation().click();
    LegacyViolationModal modal = new LegacyViolationModal();
    modal.shouldNotBe(visible);
  }

  @Test
  public void testLegacyViolations_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.legacyViolation().shouldBe(visible).shouldBe(DISABLED).hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Legacy Violations are not supported by your license"));
    ActionDropDown.legacyViolation().click();
    LegacyViolationModal modal = new LegacyViolationModal();
    modal.shouldNotBe(visible);
  }

  @Test
  public void testLegacyViolations_SetLegacyViolationStatus() {
    Policy policy = tempEntity.newPolicy(application);
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(legacyViolation.getId()).isLegacyViolation()).isFalse();

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.legacyViolation().shouldBe(visible).click();
    LegacyViolationModal modal = new LegacyViolationModal();

    modal.updateButton().click();
    FormMask.seeAndWaitForDismissal();

    assertThat(policyViolationDAO.getById(legacyViolation.getId()).isLegacyViolation()).isTrue();
    modal.shouldBe(hidden);
  }

  @Test
  public void testLegacyViolations_Cancel() {
    Policy policy = tempEntity.newPolicy(application);
    policy.setLegacyViolationAllowed(true);
    policyDAO.update(policy);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation legacyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(legacyViolation.getId()).isLegacyViolation()).isFalse();

    ActionDropDown.actionButton().click();
    ActionDropDown.legacyViolation().click();
    LegacyViolationModal modal = new LegacyViolationModal();

    modal.cancelButton().click();

    assertThat(policyViolationDAO.getById(legacyViolation.getId()).isLegacyViolation()).isFalse();
    modal.shouldBe(hidden);
  }
}
