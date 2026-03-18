/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.pages.LegacyViolationsEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.policy.LegacyViolationService;
import com.sonatype.insight.brain.policy.LegacyViolationService.LegacyViolationStatusDTO;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.WebElementCondition;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.NX_RADIO_CHECKBOX_DISABLED;
import static com.sonatype.clm.testing.functional.elements.CLM.NX_RADIO_SELECTED;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractLegacyViolationsEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  private Organization parentOrg;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private OrganizationDAO organizationDAO;

  private LegacyViolationService legacyViolationService;

  private Boolean legacyViolationsEnabled;

  private boolean legacyViolationsOverrideEnabled;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    organizationDAO = lookup(OrganizationDAO.class);
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    this.parentOrg = organizationDAO.getById(currentOwner.getParentOwnerId());

    // Save the root org legacy violations settings so we can restore them after the tests.
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    legacyViolationsEnabled = rootOrg.isLegacyViolationEnabled();
    legacyViolationsOverrideEnabled = rootOrg.isAllowLegacyViolationOverride();

    legacyViolationService = testCLMServer.getCLMServer().getInstance(LegacyViolationService.class);
  }

  @After
  public void restoreRootOrganizationLegacyViolationSettings() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setLegacyViolationEnabled(legacyViolationsEnabled);
    rootOrg.setAllowLegacyViolationOverride(legacyViolationsOverrideEnabled);
    organizationDAO.update(rootOrg);
  }

  void testLegacyViolationConfiguration_Editable() {
    configureOrganizationsAndApplications(true);

    LegacyViolationStatusDTO legacyViolationStatusDTO = legacyViolationService
        .getLegacyViolationsStatus(currentOwner.getType(), currentOwner.getPublicId());
    String summaryText = LegacyViolationsEditorPage.statusMessageText(
        legacyViolationStatusDTO.inheritedFromOrganizationName, legacyViolationStatusDTO.enabled);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.legacyViolations().shouldHave(text(summaryText)).click();
    LegacyViolationsEditorPage.disabledMessage().shouldNotBe(visible);

    if (Organization.ROOT_ORGANIZATION_ID.equals(currentOwner.getId())) {
      LegacyViolationsEditorPage.policyRadioButttons().shouldHave(size(2));
      LegacyViolationsEditorPage.policyRadioButttons().get(0).shouldHave(text("Enabled"));
      LegacyViolationsEditorPage.policyRadioButttons().get(1).shouldHave(text("Disabled"));
      LegacyViolationsEditorPage.legacyViolationDisabled().shouldBe(NX_RADIO_SELECTED);
    }
    else {
      LegacyViolationsEditorPage.policyRadioButttons().shouldHave(size(3));
      LegacyViolationsEditorPage.policyRadioButttons()
          .get(0)
          .shouldHave(text("Inherit from parent (Disabled)"));
      LegacyViolationsEditorPage.policyRadioButttons().get(1).shouldHave(text("Enabled"));
      LegacyViolationsEditorPage.policyRadioButttons().get(2).shouldHave(text("Disabled"));
      LegacyViolationsEditorPage.legacyViolationInherited(legacyViolationStatusDTO.enabledInParent)
          .shouldBe(visible)
          .shouldBe(NX_RADIO_SELECTED);
    }

    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      LegacyViolationsEditorPage.overridesCheckbox().shouldBe(visible).shouldBe(NX_RADIO_SELECTED);
    }
    else {
      LegacyViolationsEditorPage.overridesCheckbox().shouldNotBe(visible);
    }

    LegacyViolationsEditorPage.legacyViolationEnabled().click();
    if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      LegacyViolationsEditorPage.overridesCheckbox().click();
    }

    LegacyViolationsEditorPage.updateButton().click();
    FormMask.seeAndWaitForDismissal();
    LegacyViolationsEditorPage.updateButton().shouldBe(visible);

    legacyViolationStatusDTO =
        legacyViolationService.getLegacyViolationsStatus(currentOwner.getType(), currentOwner.getPublicId());

    assertThat(legacyViolationStatusDTO.enabled).isTrue();
    assertThat(legacyViolationStatusDTO.allowOverride).isFalse();
  }

  @Test
  public void testLegacyViolationConfiguration_NotEditable() {
    configureOrganizationsAndApplications(false);

    LegacyViolationStatusDTO legacyViolationStatusDTO = legacyViolationService
        .getLegacyViolationsStatus(currentOwner.getType(), currentOwner.getPublicId());
    String summaryText = LegacyViolationsEditorPage.statusMessageText(
        legacyViolationStatusDTO.inheritedFromOrganizationName, legacyViolationStatusDTO.enabled);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.legacyViolations().shouldHave(text(summaryText)).click();

    if (Organization.ROOT_ORGANIZATION_ID.equals(currentOwner.getId())) {
      LegacyViolationsEditorPage.policyRadioButttons().shouldHave(size(2));
      LegacyViolationsEditorPage.policyRadioButttons().get(0).shouldHave(text("Enabled"));
      LegacyViolationsEditorPage.policyRadioButttons().get(1).shouldHave(text("Disabled"));
      LegacyViolationsEditorPage.legacyViolationDisabled().shouldBe(NX_RADIO_SELECTED);
      LegacyViolationsEditorPage.legacyViolationDisabled().shouldNotHave(NX_RADIO_CHECKBOX_DISABLED);
      LegacyViolationsEditorPage.legacyViolationEnabled().shouldNotHave(NX_RADIO_CHECKBOX_DISABLED);
      LegacyViolationsEditorPage.disabledMessage().shouldNotBe(visible);
    }
    else {
      LegacyViolationsEditorPage.policyRadioButttons().shouldHave(size(3));
      LegacyViolationsEditorPage.policyRadioButttons()
          .get(0)
          .shouldHave(text("Inherit from parent (Disabled)"));
      LegacyViolationsEditorPage.policyRadioButttons().get(1).shouldHave(text("Enabled"));
      LegacyViolationsEditorPage.policyRadioButttons().get(2).shouldHave(text("Disabled"));

      LegacyViolationsEditorPage.legacyViolationInherited(legacyViolationStatusDTO.enabledInParent)
          .shouldHave(NX_RADIO_CHECKBOX_DISABLED);
      LegacyViolationsEditorPage.legacyViolationDisabled().shouldHave(NX_RADIO_CHECKBOX_DISABLED);
      LegacyViolationsEditorPage.legacyViolationEnabled().shouldHave(NX_RADIO_CHECKBOX_DISABLED);
      LegacyViolationsEditorPage.disabledMessage().shouldBe(visible);
      if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
        LegacyViolationsEditorPage.overridesCheckbox()
            .shouldHave(NX_RADIO_CHECKBOX_DISABLED)
            .shouldBe(visible);
      }
    }

    LegacyViolationsEditorPage.updateButton().click();
    FormUtils.getAlertElement(LegacyViolationsEditorPage.form())
        .shouldBe(visible)
        .shouldHave(text(FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX + " There are no changes to save."));
  }

  @Test
  public void testLegacyViolationConfiguration_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();

    WebElementCondition notLicensedText = LegacyViolationsEditorPage.unsupportedLicenseText();

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));

    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.legacyViolations().shouldBe(hidden);

    // if the user gets there manually, show a warning
    refreshOrOpen(LegacyViolationsEditorPage.url(currentOwner));
    LegacyViolationsEditorPage.unsupportedLicenseWarning().shouldHave(notLicensedText);

    // make sure the owner detail sidebar item is disabled
    OwnerDetailSidebar.legacyViolations().shouldBe(hidden);
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
