/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyViolationGrandfatheringEditorPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService.PolicyViolationGrandfatheringDTO;
import com.sonatype.insight.brain.policy.PolicyViolationPersistenceLocks;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public abstract class AbstractPolicyViolationGrandfatheringEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  private Organization parentOrg;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  private PolicyViolationPersistenceLocks policyViolationPersistenceLocks = new PolicyViolationPersistenceLocks();

  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService = new PolicyViolationGrandfatheringService(
      applicationDAO, organizationDAO, policyViolationDAO, policyViolationPersistenceLocks);

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OrganizationManagementPage.ROOT_ORG_URL);
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    this.parentOrg = organizationDAO.getById(currentOwner.getParentOwnerId());
  }

  @Test
  public void testPolicyViolationGrandfatheringConfiguration_Editable() {
    configureOrganizationsAndApplications(true);

    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = policyViolationGrandfatheringService
        .getGrandfathering(currentOwner.getType(), currentOwner.getPublicId());
    String summaryText = PolicyViolationGrandfatheringEditorPage.statusMessageText(
        policyViolationGrandfatheringDTO.inheritedFromOrganizationName, policyViolationGrandfatheringDTO.enabled);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.policyTile().violationGrandfathering().shouldHave(text(summaryText)).click();

    PolicyViolationGrandfatheringEditorPage.statusMessage().shouldHave(text(summaryText));

    if (Organization.ROOT_ORGANIZATION_ID.equals(currentOwner.getId())) {
      PolicyViolationGrandfatheringEditorPage.grandfatheringInherited().shouldNotBe(visible);
      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldBe(selected);
    }
    else {
      PolicyViolationGrandfatheringEditorPage.grandfatheringInherited().shouldBe(visible).shouldBe(selected);
    }

    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      PolicyViolationGrandfatheringEditorPage.overridesCheckbox().shouldBe(visible).shouldBe(selected);
    }
    else {
      PolicyViolationGrandfatheringEditorPage.overridesCheckbox().shouldNotBe(visible);
    }

    PolicyViolationGrandfatheringEditorPage.grandfatheringEnabled().click();
    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      PolicyViolationGrandfatheringEditorPage.overridesCheckbox().click();
    }

    PolicyViolationGrandfatheringEditorPage.updateButton().shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyViolationGrandfatheringEditorPage.updateButton().shouldBe(CLM.DISABLED);

    policyViolationGrandfatheringDTO = policyViolationGrandfatheringService.getGrandfathering(currentOwner.getType(),
        currentOwner.getPublicId());

    assertThat(policyViolationGrandfatheringDTO.enabled, is(true));
    assertThat(policyViolationGrandfatheringDTO.allowOverride, is(false));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testPolicyViolationGrandfatheringConfiguration_NotEditable() {
    configureOrganizationsAndApplications(false);

    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = policyViolationGrandfatheringService
        .getGrandfathering(currentOwner.getType(), currentOwner.getPublicId());
    String summaryText = PolicyViolationGrandfatheringEditorPage.statusMessageText(
        policyViolationGrandfatheringDTO.inheritedFromOrganizationName, policyViolationGrandfatheringDTO.enabled);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.policyTile().violationGrandfathering().shouldHave(text(summaryText)).click();

    PolicyViolationGrandfatheringEditorPage.statusMessage().shouldHave(text(summaryText));

    if (Organization.ROOT_ORGANIZATION_ID.equals(currentOwner.getId())) {
      PolicyViolationGrandfatheringEditorPage.grandfatheringInherited().shouldNotBe(visible);
      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldBe(selected);
      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldNotBe(disabled);
      PolicyViolationGrandfatheringEditorPage.grandfatheringEnabled().shouldNotBe(disabled);
    }
    else {
      PolicyViolationGrandfatheringEditorPage.grandfatheringInherited().shouldBe(disabled);
      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldBe(disabled);
      PolicyViolationGrandfatheringEditorPage.grandfatheringEnabled().shouldBe(disabled);
      if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
        PolicyViolationGrandfatheringEditorPage.overridesCheckbox().shouldBe(disabled).shouldBe(visible);
      }
    }

    PolicyViolationGrandfatheringEditorPage.updateButton().shouldBe(CLM.DISABLED);

    eyesWatcher.eyesCheck();
  }

  private void configureOrganizationsAndApplications(boolean allowOverride) {
    PolicyViolationGrandfatheringDTO policyViolationGrandfatheringDTO = new PolicyViolationGrandfatheringDTO();
    policyViolationGrandfatheringDTO.enabled = null;
    policyViolationGrandfatheringDTO.allowOverride = allowOverride;
    policyViolationGrandfatheringService.setGrandfathering(currentOwner.getType(), currentOwner.getPublicId(),
        policyViolationGrandfatheringDTO);

    if (parentOrg != null) {
      policyViolationGrandfatheringDTO.enabled = null;
      policyViolationGrandfatheringDTO.allowOverride = allowOverride;
      policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, parentOrg.getId(),
          policyViolationGrandfatheringDTO);
    }

    if (OwnerType.APPLICATION.equals(allowOverride)) {
      policyViolationGrandfatheringDTO.enabled = null;
      policyViolationGrandfatheringDTO.allowOverride = allowOverride;
      policyViolationGrandfatheringService.setGrandfathering(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID,
          policyViolationGrandfatheringDTO);
    }
  }
}
