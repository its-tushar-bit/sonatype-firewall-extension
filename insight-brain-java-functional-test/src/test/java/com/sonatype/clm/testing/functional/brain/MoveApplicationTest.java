/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.Dropdown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MoveApplicationDialog;
import com.sonatype.clm.testing.functional.elements.MoveApplicationErrorModal;
import com.sonatype.clm.testing.functional.elements.MoveApplicationSuccessModal;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class MoveApplicationTest
    extends AbstractFunctionalTest
{
  private static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private static final String YE_OLE_APPLICATION = "Ye Ole Application";

  private static final String SOME_OTHER_ORGANIZATION = "Some Other Organization";

  private static final String POLICY_MONITORING_MISSING_MSG = "The new parent organization does not use continuous"
      + " policy monitoring.";

  public static final Condition ERROR = cssClass("error");

  private Application application;

  private Organization otherOrg;

  private ApplicationDAO appDAO = new ApplicationDAO();

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
  public void testErrorLoadingDestinations() {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.moveApplication().shouldBe(visible).click();
    MoveApplicationDialog modal = new MoveApplicationDialog();
    modal.shouldBe(visible);
    modal.moveButton().shouldBe(hidden);
    modal.body().shouldBe(hidden);
    modal.footer().shouldHave(ERROR);
    modal.footer().shouldHave(text("No available destination organizations."));
    modal.dismissButton().shouldHave(text("OK")).shouldBe(visible).click();
    modal.shouldBe(hidden);
  }

  @Test
  public void testSuccessfullyMovedApplication() {
    otherOrg = tempEntity.newOrganization(SOME_OTHER_ORGANIZATION);

    MoveApplicationDialog modal = new MoveApplicationDialog();
    selectFirstOptionAndSubmit(modal);
    modal.shouldBe(hidden);

    // success modal should have only info messages
    MoveApplicationSuccessModal successDialog = new MoveApplicationSuccessModal();
    successDialog.shouldBe(visible);
    successDialog.infoSection().shouldBe(visible);
    successDialog.warningSection().shouldBe(hidden);
    eyesWatcher.eyesCheck();
    successDialog.okButton().click();
    successDialog.shouldBe(hidden);
    modal.shouldBe(hidden);

    // test new parent
    Application updatedApp = appDAO.getById(application.getId());
    assertThat(updatedApp.getParentOwnerId()).isEqualTo(otherOrg.getId());
  }

  @Test
  public void shouldSuccessfullyMoveApplicationWithWarningsIfPolicyMonitoringMissingInNewParent() {
    otherOrg = tempEntity.newOrganization(SOME_OTHER_ORGANIZATION);

    // set up current parent to have continuous policy monitoring
    tempEntity.newPolicyMonitoring(application.getParentOwnerId(), Stage.ID_RELEASE);

    MoveApplicationDialog modal = new MoveApplicationDialog();
    selectFirstOptionAndSubmit(modal);
    modal.shouldBe(hidden);

    // success modal should have warning messages
    MoveApplicationSuccessModal successModal = new MoveApplicationSuccessModal();
    successModal.shouldBe(visible);
    successModal.infoSection().shouldBe(visible);
    successModal.warningSection().shouldBe(visible).shouldHave(text(POLICY_MONITORING_MISSING_MSG));
    successModal.okButton().click();
    successModal.shouldBe(hidden);
    modal.shouldBe(hidden);

    // check continuous policy monitoring text is updated
    OwnerSummaryPage.monitoredStage()
        .shouldHave(text("Inherit from Some Other Organization (Do not monitor)"));

    // test new parent
    Application updatedApp = appDAO.getById(application.getId());
    assertThat(updatedApp.getParentOwnerId()).isEqualTo(otherOrg.getId());
  }

  @Test
  public void testErrorIncompatibleDestinationAndRetry() {
    otherOrg = tempEntity.newOrganization(SOME_OTHER_ORGANIZATION);

    // set up current parent to have a tag
    Tag tag = tempEntity.newTag(application.getParentOwnerId(), "MoveApplicationTest category");
    tempEntity.newApplicationTag(application.getId(), tag.getId());

    // move
    MoveApplicationDialog modal = new MoveApplicationDialog();
    selectFirstOptionAndSubmit(modal);

    // error state
    modal.shouldBe(visible);
    modal.footer().shouldBe(visible).shouldHave(ERROR);
    modal.retryButton().shouldBe(visible);
    modal.detailsButton().shouldBe(visible).click();
    modal.shouldBe(hidden);

    // error details modal
    MoveApplicationErrorModal errorModal = new MoveApplicationErrorModal();
    errorModal.shouldBe(visible);
    errorModal.body().shouldHave(text("Incompatible Destination"));
    eyesWatcher.eyesCheck();
    errorModal.okButton().click();
    errorModal.shouldBe(hidden);

    // retry and cancel
    modal.shouldBe(visible);
    modal.retryButton().shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    modal.dismissButton().click();
    modal.shouldBe(hidden);
  }

  private void selectFirstOptionAndSubmit(MoveApplicationDialog modal) {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.moveApplication().shouldBe(visible).click();
    modal.shouldBe(visible);
    modal.moveButton().shouldBe(DISABLED);
    modal.body().shouldBe(visible);

    Dropdown destinationDropdown = modal.destinationDropdown();
    destinationDropdown.shouldBe(visible).selectedItem().click();
    destinationDropdown.listItem(0).shouldHave(text(SOME_OTHER_ORGANIZATION)).click();

    modal.footer().shouldNotHave(ERROR);
    modal.dismissButton().shouldHave(text("Cancel"));
    modal.moveButton().shouldNotBe(DISABLED).click();
  }
}
