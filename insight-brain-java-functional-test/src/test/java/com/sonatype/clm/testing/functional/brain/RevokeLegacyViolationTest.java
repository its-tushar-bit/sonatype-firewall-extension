/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.RevokeLegacyViolationModal;
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

public class RevokeLegacyViolationTest
    extends AbstractFunctionalTest
{
  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    policyViolationDAO = lookup(PolicyViolationDAO.class);

    application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));
  }

  @Test
  public void testRevokeLegacyViolation_ModalInitialState() {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.revokeLegacyViolation().shouldBe(visible).click();
    RevokeLegacyViolationModal modal = new RevokeLegacyViolationModal();
    modal.shouldBe(visible);
    modal.header().shouldHave(text("Revoke Legacy Violation Status"));
    modal.body()
        .shouldHave(text(
            "Subsequent scans and re-evaluations will treat applicable policy violations "
                + "as active and trigger configured actions."));
    modal.revokeButton().shouldBe(visible);
    modal.retryButton().shouldBe(hidden);
    modal.cancelButton().shouldBe(visible);
  }

  @Test
  public void testRevokeLegacyViolation_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);

    refreshOrOpen(OwnerSummaryPage.url(application));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(application.getName()));

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.revokeLegacyViolation().shouldBe(visible).shouldBe(DISABLED).hover();
    Tooltip.get()
        .shouldBe(visible)
        .shouldHave(text("Legacy Violations are not supported by your license"));
    ActionDropDown.legacyViolation().click();
    RevokeLegacyViolationModal modal = new RevokeLegacyViolationModal();
    modal.shouldNotBe(visible);
  }

  @Test
  public void testRevokeLegacyViolation_Revoke() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation legacyPolicyViolation = tempEntity.newLegacyPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(legacyPolicyViolation.getId()).isLegacyViolation()).isTrue();

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.revokeLegacyViolation().shouldBe(visible).click();
    RevokeLegacyViolationModal modal = new RevokeLegacyViolationModal();

    modal.revokeButton().click();
    FormMask.seeAndWaitForDismissal();

    assertThat(policyViolationDAO.getById(legacyPolicyViolation.getId()).isLegacyViolation()).isFalse();
    modal.shouldBe(hidden);
  }

  @Test
  public void testRevokeLegacyViolation_Cancel() {
    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation legacyPolicyViolation = tempEntity.newLegacyPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(legacyPolicyViolation.getId()).isLegacyViolation()).isTrue();
    ActionDropDown.actionButton().click();
    ActionDropDown.revokeLegacyViolation().click();
    RevokeLegacyViolationModal modal = new RevokeLegacyViolationModal();
    modal.cancelButton().click();

    assertThat(policyViolationDAO.getById(legacyPolicyViolation.getId()).isLegacyViolation()).isTrue();
    modal.shouldBe(hidden);
  }
}
