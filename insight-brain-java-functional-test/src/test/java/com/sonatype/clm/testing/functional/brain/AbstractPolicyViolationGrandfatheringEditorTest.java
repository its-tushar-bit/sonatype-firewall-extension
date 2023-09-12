/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyViolationGrandfatheringEditorPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.LegacyViolationService.LegacyViolationStatusDTO;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Condition;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.CLM.NX_RADIO_CHECKBOX_DISABLED;
import static com.sonatype.clm.testing.functional.elements.CLM.NX_RADIO_SELECTED;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractPolicyViolationGrandfatheringEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  private Organization parentOrg;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private LegacyViolationService legacyViolationService;

  private Boolean legacyViolationsEnabled;

  private boolean legacyViolationsOverrideEnabled;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    this.parentOrg = organizationDAO.getById(currentOwner.getParentOwnerId());

    // Save the root org legacy violations settings so we can restore them after the tests.
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    legacyViolationsEnabled = rootOrg.isPolicyViolationGrandfatheringEnabled();
    legacyViolationsOverrideEnabled = rootOrg.isAllowPolicyViolationGrandfatheringOverride();

    legacyViolationService = testCLMServer.getCLMServer().getInstance(LegacyViolationService.class);
  }

  @After
  public void restoreRootOrganizationPolicyViolationGrandfatheringSettings() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setPolicyViolationGrandfatheringEnabled(legacyViolationsEnabled);
    rootOrg.setAllowPolicyViolationGrandfatheringOverride(legacyViolationsOverrideEnabled);
    organizationDAO.update(rootOrg);
  }

  void testPolicyViolationGrandfatheringConfiguration_Editable() {
    configureOrganizationsAndApplications(true);

    LegacyViolationStatusDTO legacyViolationStatusDTO = legacyViolationService
        .getLegacyViolationsStatus(currentOwner.getType(), currentOwner.getPublicId());
    String summaryText = PolicyViolationGrandfatheringEditorPage.statusMessageText(
        legacyViolationStatusDTO.inheritedFromOrganizationName, legacyViolationStatusDTO.enabled);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.legacyViolations().shouldHave(text(summaryText)).click();
    PolicyViolationGrandfatheringEditorPage.disabledMessage().shouldNotBe(visible);

    if (Organization.ROOT_ORGANIZATION_ID.equals(currentOwner.getId())) {
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().shouldHaveSize(2);
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(0).shouldHave(text("Enabled"));
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(1).shouldHave(text("Disabled"));

      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldBe(NX_RADIO_SELECTED);
    }
    else {
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().shouldHaveSize(3);
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(0)
              .shouldHave(text("Inherit from parent (Disabled)"));
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(1).shouldHave(text("Enabled"));
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(2).shouldHave(text("Disabled"));

      PolicyViolationGrandfatheringEditorPage.grandfatheringInherited(legacyViolationStatusDTO.enabledInParent)
              .shouldBe(visible).shouldBe(NX_RADIO_SELECTED);
    }

    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      PolicyViolationGrandfatheringEditorPage.overridesCheckbox().shouldBe(visible).shouldBe(NX_RADIO_SELECTED);
    }
    else {
      PolicyViolationGrandfatheringEditorPage.overridesCheckbox().shouldNotBe(visible);
    }

    PolicyViolationGrandfatheringEditorPage.grandfatheringEnabled().click();
    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      PolicyViolationGrandfatheringEditorPage.overridesCheckbox().click();
    }

    PolicyViolationGrandfatheringEditorPage.updateButton().click();
    FormMask.seeAndWaitForDismissal();
    PolicyViolationGrandfatheringEditorPage.updateButton().shouldBe(visible);

    legacyViolationStatusDTO =
        legacyViolationService.getLegacyViolationsStatus(currentOwner.getType(), currentOwner.getPublicId());

    assertThat(legacyViolationStatusDTO.enabled).isTrue();
    assertThat(legacyViolationStatusDTO.allowOverride).isFalse();
  }

  @Test
  public void testPolicyViolationGrandfatheringConfiguration_NotEditable() {
    configureOrganizationsAndApplications(false);

    LegacyViolationStatusDTO legacyViolationStatusDTO = legacyViolationService
        .getLegacyViolationsStatus(currentOwner.getType(), currentOwner.getPublicId());
    String summaryText = PolicyViolationGrandfatheringEditorPage.statusMessageText(
        legacyViolationStatusDTO.inheritedFromOrganizationName, legacyViolationStatusDTO.enabled);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.legacyViolations().shouldHave(text(summaryText)).click();

    if (Organization.ROOT_ORGANIZATION_ID.equals(currentOwner.getId())) {
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().shouldHaveSize(2);
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(0).shouldHave(text("Enabled"));
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(1).shouldHave(text("Disabled"));

      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldBe(NX_RADIO_SELECTED);
      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldNotHave(NX_RADIO_CHECKBOX_DISABLED);
      PolicyViolationGrandfatheringEditorPage.grandfatheringEnabled().shouldNotHave(NX_RADIO_CHECKBOX_DISABLED);
      PolicyViolationGrandfatheringEditorPage.disabledMessage().shouldNotBe(visible);
    }
    else {
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().shouldHaveSize(3);
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons()
              .get(0).shouldHave(text("Inherit from parent (Disabled)"));
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(1).shouldHave(text("Enabled"));
      PolicyViolationGrandfatheringEditorPage.policyRadioButttons().get(2).shouldHave(text("Disabled"));

      PolicyViolationGrandfatheringEditorPage.grandfatheringInherited(legacyViolationStatusDTO.enabledInParent)
              .shouldHave(NX_RADIO_CHECKBOX_DISABLED);
      PolicyViolationGrandfatheringEditorPage.grandfatheringDisabled().shouldHave(NX_RADIO_CHECKBOX_DISABLED);
      PolicyViolationGrandfatheringEditorPage.grandfatheringEnabled().shouldHave(NX_RADIO_CHECKBOX_DISABLED);
      PolicyViolationGrandfatheringEditorPage.disabledMessage().shouldBe(visible);
      if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
        PolicyViolationGrandfatheringEditorPage.overridesCheckbox()
                .shouldHave(NX_RADIO_CHECKBOX_DISABLED).shouldBe(visible);
      }
    }

    PolicyViolationGrandfatheringEditorPage.updateButton().click();
    FormUtils.getAlertElement(PolicyViolationGrandfatheringEditorPage.form()).shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
  }

  @Test
  public void testPolicyViolationGrandfatheringConfiguration_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    Condition notLicensedText = PolicyViolationGrandfatheringEditorPage.unsupportedLicenseText();

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.legacyViolations().shouldHave(notLicensedText).click();

    // if the user gets there manually, show a warning
    refreshOrOpen(PolicyViolationGrandfatheringEditorPage.url(currentOwner));
    PolicyViolationGrandfatheringEditorPage.unsupportedLicenseWarning().shouldHave(notLicensedText);

    // make sure the owner detail sidebar item is disabled
    OwnerDetailSidebar.legacyViolations().shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldHave(notLicensedText);
  }

  private void configureOrganizationsAndApplications(boolean allowOverride) {
    LegacyViolationStatusDTO legacyViolationStatusDTO = new LegacyViolationStatusDTO();
    legacyViolationStatusDTO.enabled = null;
    legacyViolationStatusDTO.allowOverride = allowOverride;
    legacyViolationService.setLegacyViolationStatus(currentOwner.getType(), currentOwner.getPublicId(),
        legacyViolationStatusDTO);

    if (parentOrg != null) {
      legacyViolationStatusDTO.enabled = null;
      legacyViolationStatusDTO.allowOverride = allowOverride;
      legacyViolationService.setLegacyViolationStatus(OwnerType.ORGANIZATION, parentOrg.getId(),
          legacyViolationStatusDTO);
    }
  }
}
