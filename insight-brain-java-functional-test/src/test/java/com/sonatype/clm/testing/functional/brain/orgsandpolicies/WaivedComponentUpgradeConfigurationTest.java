/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.orgsandpolicies;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxRadio;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.WaivedComponentUpgradeConfigurationPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.selected;
import static org.assertj.core.api.Assertions.assertThat;

public class WaivedComponentUpgradeConfigurationTest
    extends AbstractFunctionalTest
{
  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private String waivedComponentUpgradeStageTypeId;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void before() {
    // Capture the original root org waived component upgrade stage id, so we can restore it after the tests.
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    waivedComponentUpgradeStageTypeId = rootOrg.getWaivedComponentUpgradeStageTypeId();
  }

  @After
  public void restoreRootOrganizationState() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setWaivedComponentUpgradeStageTypeId(waivedComponentUpgradeStageTypeId);
    organizationDAO.update(rootOrg);
  }

  /**
   * This is the only test that should have a visual check
   */
  @Test
  public void testLayoutAndText() {
    openConfigurationPage();
    List<NxRadio> stages = new WaivedComponentUpgradeConfigurationPage().stages();
    assertThat(stages.stream().map(nxRadio -> nxRadio.label().text())).hasSize(7)
        .containsExactly("None", "Develop", "Source", "Build", "Stage Release", "Release", "Operate");

    // Perform eyes check to verify the helper text and layout of the page
    eyesWatcher.eyesCheck("Waived Component Upgrade Configuration layout and text");
  }

  @Test
  public void testUpdateConfiguration_noneToValidStage() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setWaivedComponentUpgradeStageTypeId(null);
    organizationDAO.update(rootOrg);

    openConfigurationPage();
    WaivedComponentUpgradeConfigurationPage configurationPage = new WaivedComponentUpgradeConfigurationPage();
    NxRadio stageBuildRadio = configurationPage.stagesByLabel().get("Build");
    stageBuildRadio.click();
    configurationPage.updateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    refresh();
    configurationPage.stagesByLabel().get("None").shouldNotBe(selected);
    configurationPage.stagesByLabel().get("Build").shouldBe(selected);
  }

  @Test
  public void testUpdateConfiguration_validStageToNone() {
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    rootOrg.setWaivedComponentUpgradeStageTypeId(OperateStageType.ID);
    organizationDAO.update(rootOrg);

    openConfigurationPage();
    WaivedComponentUpgradeConfigurationPage configurationPage = new WaivedComponentUpgradeConfigurationPage();
    NxRadio stageNoneRadio = configurationPage.stagesByLabel().get("None");
    stageNoneRadio.click();
    configurationPage.updateButton().click();
    NxSubmitMask.seeAndWaitForDismissal();

    refresh();
    configurationPage.stagesByLabel().get("None").shouldBe(selected);
  }

  private void openConfigurationPage() {
    refreshOrOpen(WaivedComponentUpgradeConfigurationPage.url());
  }
}
