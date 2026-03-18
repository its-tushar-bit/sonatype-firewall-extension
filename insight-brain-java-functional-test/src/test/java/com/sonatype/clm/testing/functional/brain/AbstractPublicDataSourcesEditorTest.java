/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.PublicDataSourcesEditorPage;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationRequest;
import com.sonatype.insight.brain.cpematching.CpeMatchingConfigurationService;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.partialText;

public abstract class AbstractPublicDataSourcesEditorTest
    extends AbstractFunctionalTest
{
  protected static final String RADIO_SELECTED_CSS_CLASS = "tm-checked";

  protected static final String RADIO_UNSELECTED_CSS_CLASS = "tm-unchecked";

  protected Owner currentOwner;

  protected Owner rootOrganization;

  protected CpeMatchingConfigurationService cpeMatchingConfigurationService;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected static final String INHERIT_CONFIG_RADIO_TEXT = "Inherit from parent";

  protected OwnerDAO ownerDao;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(PublicDataSourcesEditorPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    ownerDao = lookup(OwnerDAO.class);
    cpeMatchingConfigurationService = lookup(CpeMatchingConfigurationService.class);
    productLicenseManager.setFeatures(LicensedFeature.CPE_MATCHING);
    rootOrganization = ownerDao.getById(Organization.ROOT_ORGANIZATION_ID);
    refreshOrOpen(PublicDataSourcesEditorPage.urlToRootOrg());
  }

  protected SelenideElement findRadioInputByPartialText(String text) {
    return PublicDataSourcesEditorPage.radioInputs().findBy(partialText(text));
  }

  protected SelenideElement findRadioInputByText(String text) {
    return PublicDataSourcesEditorPage.radioInputs().findBy(exactText(text));
  }

  protected void initPublicDataConfiguration(Owner owner, boolean enabled, boolean allowOverride) {
    CpeMatchingConfigurationRequest cfgRequest = new CpeMatchingConfigurationRequest();
    cfgRequest.enabled = enabled;
    cfgRequest.allowOverride = allowOverride;
    cpeMatchingConfigurationService.updateCpeMatchingConfiguration(owner.getType(), owner.getId(),
        cfgRequest);
  }
}
