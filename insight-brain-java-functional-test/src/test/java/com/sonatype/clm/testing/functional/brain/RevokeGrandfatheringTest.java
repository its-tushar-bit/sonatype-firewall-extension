/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.RevokeGrandfatheringModal;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
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

public class RevokeGrandfatheringTest
    extends AbstractFunctionalTest
{
  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));
  }

  @Test
  public void testRevokeGrandfathering_ModalInitialState() {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.revokeGrandfathered().shouldBe(visible).click();
    RevokeGrandfatheringModal modal = new RevokeGrandfatheringModal();
    modal.shouldBe(visible);
    modal.revokeButton().shouldBe(visible);
    modal.retryButton().shouldBe(hidden);
    modal.cancelButton().shouldBe(visible);
  }

  @Test
  public void testGrandfather_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.revokeGrandfathered().shouldBe(visible).shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible)
        .shouldHave(text("Policy Violation Grandfathering is not supported by your license"));
    ActionDropDown.grandfather().click();
    RevokeGrandfatheringModal modal = new RevokeGrandfatheringModal();
    modal.shouldNotBe(visible);
  }

  @Test
  public void testRevokeGrandfathering_Revoke() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation grandfatheredPolicyViolation = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered()).isTrue();

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.revokeGrandfathered().shouldBe(visible).click();
    RevokeGrandfatheringModal modal = new RevokeGrandfatheringModal();

    modal.revokeButton().click();
    FormMask.seeAndWaitForDismissal();

    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered()).isFalse();
    modal.shouldBe(hidden);
  }

  @Test
  public void testRevokeGrandfathering_Cancel() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation grandfatheredPolicyViolation = tempEntity.newGrandfatheredPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered()).isTrue();

    ActionDropDown.actionButton().click();
    ActionDropDown.revokeGrandfathered().click();
    RevokeGrandfatheringModal modal = new RevokeGrandfatheringModal();

    modal.cancelButton().click();

    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered()).isTrue();
    modal.shouldBe(hidden);
  }
}
