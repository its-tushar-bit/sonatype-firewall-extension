/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MoveOrganizationSuccessModal;
import com.sonatype.clm.testing.functional.elements.MoveOwnerDialog;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class MoveOrganizationTest
    extends AbstractFunctionalTest
{
  private static final String YE_OLE_PARENT_ORGANIZATION1 = "Ye Ole Parent Organization 1";

  private static final String YE_OLE_PARENT_ORGANIZATION2 = "Ye Ole Parent Organization 2";

  private static final String YE_OLE_CHILD_ORGANIZATION2 = "Ye Ole Child Organization";

  private static final String POLICY_MONITORING_MISSING_MSG = "The new parent organization does not use continuous"
      + " policy monitoring.";

  private OrganizationDAO orgDAO;

  private Organization parentOrg1;

  private Organization parentOrg2;

  private Organization childOrg;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    orgDAO = lookup(OrganizationDAO.class);

    parentOrg1 = tempEntity.newOrganization(YE_OLE_PARENT_ORGANIZATION1);
    parentOrg2 = tempEntity.newOrganization(YE_OLE_PARENT_ORGANIZATION2);
    childOrg = tempEntity.newOrganization(YE_OLE_CHILD_ORGANIZATION2, parentOrg1);
    tempEntity.newApplicationWithParent(childOrg);
    tempEntity.newApplicationWithParent(childOrg);

    refreshOrOpen(OwnerSummaryPage.url(childOrg));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(childOrg.getName()));
  }

  @Test
  public void testSuccessfullyMovedOrganization() {
    MoveOwnerDialog modal = new MoveOwnerDialog();
    selectOptionAndSubmit(modal, 2, YE_OLE_PARENT_ORGANIZATION2, childOrg.getName(), 2);
    modal.shouldBe(hidden);

    // success modal should have only info messages
    MoveOrganizationSuccessModal successDialog = new MoveOrganizationSuccessModal();
    successDialog.shouldBe(visible);
    successDialog.infoSection().shouldBe(visible);
    successDialog.warningSection().shouldBe(hidden);
    eyesWatcher.eyesCheck();
    successDialog.closeButton().click();
    successDialog.shouldBe(hidden);
    modal.shouldBe(hidden);

    // test new parent
    Organization updatedOrg = orgDAO.getById(childOrg.getId());
    assertThat(updatedOrg.getParentOwnerId()).isEqualTo(parentOrg2.getId());
  }

  @Test
  public void shouldSuccessfullyMoveOrganizationWithWarningsIfPolicyMonitoringMissingInNewParent() {

    // set up current parent to have continuous policy monitoring
    tempEntity.newPolicyMonitoring(childOrg.getParentOwnerId(), Stage.ID_RELEASE);
    MoveOwnerDialog modal = new MoveOwnerDialog();
    selectOptionAndSubmit(modal, 2, YE_OLE_PARENT_ORGANIZATION2, childOrg.getName(), 2);
    modal.shouldBe(hidden);

    // success modal should have warning messages
    MoveOrganizationSuccessModal successModal = new MoveOrganizationSuccessModal();
    successModal.shouldBe(visible);
    successModal.infoSection().shouldBe(visible);
    successModal.warningSection().shouldBe(visible).shouldHave(text(POLICY_MONITORING_MISSING_MSG));
    successModal.closeButton().click();
    successModal.shouldBe(hidden);
    modal.shouldBe(hidden);

    // check continuous policy monitoring text is updated
    OwnerSummaryPage.monitoredStage()
        .shouldHave(text("Inherit from Ye Ole Parent Organization 2 (Do not monitor)"));

    // test new parent
    Organization updatedOrg = orgDAO.getById(childOrg.getId());
    assertThat(updatedOrg.getParentOwnerId()).isEqualTo(parentOrg2.getId());
  }

  @Test
  public void testErrorIncompatibleDestinationAndRetry() {
    // set up a new policy to make targets incompatible
    tempEntity.newPolicy(childOrg.getParentOwnerId(), "policyName", 5, Action.ID_FAIL,
        StageTypes.BUILD.getId(), null);
    // move
    MoveOwnerDialog modal = new MoveOwnerDialog();
    selectOptionAndSubmit(modal, 2, YE_OLE_PARENT_ORGANIZATION2, childOrg.getName(), 2);
    // error state
    modal.shouldBe(visible);
    modal.retryButton().shouldBe(visible);
    modal.errorMessage()
        .shouldBe(visible)
        .shouldHave(text(
            "Incompatible Destination: There are configuration conflicts preventing the move operation."
                + " Errors details can be accessed by fetching a CSV file for download. Retry"));
    eyesWatcher.eyesCheck();
    modal.retryButton().shouldBe(visible).click();
    FormMask.seeAndWaitForDismissal();
    modal.dismissButton().click();
    modal.shouldBe(hidden);
  }

  @Test
  public void testErrorIncompatibleDestinationAndFetchCSVButton() {
    // set up a new policy to make targets incompatible
    tempEntity.newPolicy(childOrg.getParentOwnerId(), "policyName", 5, Action.ID_FAIL,
        StageTypes.BUILD.getId(), null);
    // move
    MoveOwnerDialog modal = new MoveOwnerDialog();
    selectOptionAndSubmit(modal, 2, YE_OLE_PARENT_ORGANIZATION2, childOrg.getName(), 2);
    // error state
    modal.shouldBe(visible);
    modal.fetchCSVButton().should(visible).shouldHave(text("Fetch CSV")).click();
  }

  private void selectOptionAndSubmit(
      MoveOwnerDialog modal,
      int option,
      String optionName,
      String orgName,
      int descendants)
  {
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.moveOwner().shouldBe(visible).click();
    eyesWatcher.eyesCheck("Move Owner Modal");
    modal.shouldBe(visible);
    modal.moveButton().shouldBe(visible);
    modal.body().shouldBe(visible);
    modal.header().shouldBe(visible).shouldHave(text("Move " + orgName));
    modal.message()
        .shouldBe(visible)
        .shouldHave(text("Moving " + orgName + " will move " + descendants + " descendants. "
            + "Confirm inheritance details after the move is complete."));

    NxFormSelect destinationDropdown = modal.destinationDropdown();
    destinationDropdown.shouldBe(visible).click();
    destinationDropdown.listItem(option).shouldHave(text(optionName)).click();

    modal.errorMessage().shouldBe(hidden);
    modal.dismissButton().shouldHave(text("Cancel"));
    modal.moveButton().shouldNotBe(DISABLED).click();
  }
}
