/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyViolationGrandfatheringEditorPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService;
import com.sonatype.insight.brain.policy.PolicyViolationGrandfatheringService.PolicyViolationGrandfatheringDTO;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Condition;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractPolicyViolationGrandfatheringEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  private Organization parentOrg;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private OrganizationDAO organizationDAO = new OrganizationDAO();

  private PolicyViolationGrandfatheringService policyViolationGrandfatheringService;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    this.parentOrg = organizationDAO.getById(currentOwner.getParentOwnerId());
    policyViolationGrandfatheringService =
        testCLMServer.getCLMServer().getInstance(PolicyViolationGrandfatheringService.class);
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
    PolicyViolationGrandfatheringEditorPage.disabledMessage().shouldNotBe(visible);

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

    PolicyViolationGrandfatheringEditorPage.updateButton().shouldNotBe(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    PolicyViolationGrandfatheringEditorPage.updateButton().shouldBe(DISABLED);

    policyViolationGrandfatheringDTO = policyViolationGrandfatheringService.getGrandfathering(currentOwner.getType(),
        currentOwner.getPublicId());

    assertThat(policyViolationGrandfatheringDTO.enabled).isTrue();
    assertThat(policyViolationGrandfatheringDTO.allowOverride).isFalse();

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
      PolicyViolationGrandfatheringEditorPage.disabledMessage().shouldNotBe(visible);
    }
    else {
      PolicyViolationGrandfatheringEditorPage.grandfatheringInherited().shouldBe(disabled);
      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldBe(disabled);
      PolicyViolationGrandfatheringEditorPage.grandfatheringEnabled().shouldBe(disabled);
      PolicyViolationGrandfatheringEditorPage.disabledMessage().shouldBe(visible);
      if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
        PolicyViolationGrandfatheringEditorPage.overridesCheckbox().shouldBe(disabled).shouldBe(visible);
      }
    }

    PolicyViolationGrandfatheringEditorPage.updateButton().shouldBe(DISABLED);

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testPolicyViolationGrandfatheringConfiguration_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    Condition notLicensedText = PolicyViolationGrandfatheringEditorPage.unsupportedLicenseText();

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.policyTile().violationGrandfathering().shouldHave(notLicensedText).click();

    PolicyViolationGrandfatheringEditorPage.title().shouldNotBe(visible);

    // if the user gets there manually, show a warning
    refreshOrOpen(PolicyViolationGrandfatheringEditorPage.url(currentOwner));
    PolicyViolationGrandfatheringEditorPage.unsupportedLicenseWarning().shouldHave(notLicensedText);

    // make sure the owner detail tree view item is disabled
    OwnerDetailTreeView.policyGroup().item(1).shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldHave(notLicensedText);
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
