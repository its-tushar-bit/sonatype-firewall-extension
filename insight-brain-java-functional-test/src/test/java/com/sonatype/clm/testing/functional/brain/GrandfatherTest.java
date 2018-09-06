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
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GrandfatherTest
    extends AbstractFunctionalTest
{
  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private Application application;

  private PolicyDAO policyDAO = new PolicyDAO();

  private PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.URL);
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
  public void testGrandfather_ModalInitialState() {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.grandfather().shouldBe(visible).click();
    GrandfatherModal modal = new GrandfatherModal();
    modal.shouldBe(visible);
    modal.grandfatherButton().shouldBe(visible);
    modal.retryButton().shouldBe(hidden);
    modal.cancelButton().shouldBe(visible);
  }

  @Test
  public void testGrandfather_Grandfather() {
    Policy policy = tempEntity.newPolicy(application.getId(), "policy");
    policy.setPolicyViolationGrandfatheringAllowed(true);
    policyDAO.update(policy);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation grandfatheredPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered(), is(false));

    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.grandfather().shouldBe(visible).click();
    GrandfatherModal modal = new GrandfatherModal();

    modal.grandfatherButton().click();
    FormMask.seeAndWaitForDismissal();

    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered(), is(true));
    modal.shouldBe(hidden);
  }

  @Test
  public void testGrandfather_Cancel() {
    Policy policy = tempEntity.newPolicy(application.getId(), "policy");
    policy.setPolicyViolationGrandfatheringAllowed(true);
    policyDAO.update(policy);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan");
    PolicyViolation grandfatheredPolicyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered(), is(false));

    ActionDropDown.actionButton().click();
    ActionDropDown.grandfather().click();
    GrandfatherModal modal = new GrandfatherModal();

    modal.cancelButton().click();

    assertThat(policyViolationDAO.getById(grandfatheredPolicyViolation.getId()).isGrandfathered(), is(false));
    modal.shouldBe(hidden);
  }
}
