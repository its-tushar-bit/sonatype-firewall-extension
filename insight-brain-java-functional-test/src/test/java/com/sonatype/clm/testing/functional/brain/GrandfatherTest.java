/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.GrandfatherModal;
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

public class GrandfatherTest
    extends AbstractFunctionalTest
{
  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);
    application.setPolicyViolationGrandfatheringEnabled(true);
    applicationDAO.update(application);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));
  }

  @Test
  public void testGrandfather_ModalInitialState_GrandfatheringEnabled() {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.grandfather().shouldBe(visible).shouldNotBe(DISABLED).hover();
    if (Tooltip.get().getElement().isDisplayed()) {
      Tooltip.get().shouldNotHave(text("Grandfathering is not enabled for this application."));
      Tooltip.get().shouldHave(text("Grandfather " + YE_OLE_APPLICATION));
    }
    ActionDropDown.grandfather().click();
    GrandfatherModal modal = new GrandfatherModal();
    modal.shouldBe(visible);
    modal.grandfatherButton().shouldBe(visible);
    modal.retryButton().shouldBe(hidden);
    modal.cancelButton().shouldBe(visible);
  }

  @Test
  public void testGrandfather_ModalInitialState_GrandfatheringDisabled() {
    application.setPolicyViolationGrandfatheringEnabled(false);
    applicationDAO.update(application);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.grandfather().shouldBe(visible).shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Legacy violations are not enabled for this application."));
    ActionDropDown.grandfather().click();
    GrandfatherModal modal = new GrandfatherModal();
    modal.shouldNotBe(visible);
  }

  @Test
  public void testGrandfather_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.grandfather().shouldBe(visible).shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible)
        .shouldHave(text("Legacy Violations are not supported by your license"));
    ActionDropDown.grandfather().click();
    GrandfatherModal modal = new GrandfatherModal();
    modal.shouldNotBe(visible);
  }

  @Test
  public void testGrandfather_Grandfather() {
    Policy policy = tempEntity.newPolicy(application);
    policy.setPolicyViolationGrandfatheringAllowed(true);
    policyDAO.update(policy);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation grandfatheredPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered()).isFalse();

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.grandfather().shouldBe(visible).click();
    GrandfatherModal modal = new GrandfatherModal();

    modal.grandfatherButton().click();
    FormMask.seeAndWaitForDismissal();

    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered()).isTrue();
    modal.shouldBe(hidden);
  }

  @Test
  public void testGrandfather_Cancel() {
    Policy policy = tempEntity.newPolicy(application);
    policy.setPolicyViolationGrandfatheringAllowed(true);
    policyDAO.update(policy);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation grandfatheredPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered()).isFalse();

    ActionDropDown.actionButton().click();
    ActionDropDown.grandfather().click();
    GrandfatherModal modal = new GrandfatherModal();

    modal.cancelButton().click();

    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered()).isFalse();
    modal.shouldBe(hidden);
  }
}
