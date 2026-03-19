/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.AttributionSummaryTile;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.Obligations;
import com.sonatype.clm.testing.functional.pages.EditAllObligationsModal;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ObligationStatus;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exactText;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class AllObligationsTest
    extends AbstractFunctionalTest
{
  private final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");

  private Application app;

  private Organization rootOrganization;

  private OrganizationDAO organizationDAO;

  private ComponentObligationDAO componentObligationDAO;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    organizationDAO = lookup(OrganizationDAO.class);
    componentObligationDAO = lookup(ComponentObligationDAO.class);

    app = tempEntity.newApplicationWithParent(AllObligationsTest.class.getSimpleName(), "app", "org");
    rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);

    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID,
        "033e7a20b23ea284d474", componentId);
    tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "MIT");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalCommentHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");

    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetails.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils.toString(this.getClass().getResourceAsStream("/legal/componentDetailsList.json"),
            StandardCharsets.UTF_8))
        .atUri("rest/ci/componentDetails/list");

    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
  }

  @Test
  public void testFulfillAllObligations_noneExist() {
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));

    assertThat(componentObligationDAO.getByOwnerIdAndComponentIdentifier(app.getId(), componentId)).isEmpty();

    Obligations obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHave(size(10));
    for (int i = 0; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Unreviewed");
    }

    ComponentLegalOverviewPage.resolveAllObligationsButton().shouldBe(Condition.visible).click();
    EditAllObligationsModal modal = new EditAllObligationsModal();
    modal.should(Condition.appear);
    assertThat(modal.statusDropdown().getText()).isEqualTo("Fulfilled");
    assertThat(modal.commentTextInput().getText()).isEmpty();
    assertOption(modal.scopeDropdown().getSelectedOption(), rootOrganization);

    modal.commentTextInput().setValue("my comment");

    modal.save().click();
    modal.shouldNotBe(Condition.visible);

    obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHave(size(10));
    for (int i = 0; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Fulfilled");
    }
    List<ComponentObligation> componentObligations = componentObligationDAO.getAll();
    assertThat(componentObligations).hasSize(10);
    componentObligations.forEach(co -> {
      assertThat(co.getStatus()).isEqualTo(ObligationStatus.FULFILLED);
      assertThat(co.getComment()).isEqualTo("my comment");
    });
    eyesWatcher.eyesCheck("Obligations section none exist");
  }

  @Test
  public void testFulfillAllObligations_noObligations() {
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/license/metadata");
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));

    assertThat(componentObligationDAO.getByOwnerIdAndComponentIdentifier(app.getId(), componentId)).isEmpty();

    Obligations obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHave(size(0));
    ComponentLegalOverviewPage.resolveAllObligationsButton().shouldNotBe(Condition.visible);
  }

  @Test
  public void testAccordionsExpanded() {
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    AttributionSummaryTile attributionSummaryTile = new AttributionSummaryTile();
    ElementsCollection accordions = attributionSummaryTile.getAllAccordions();
    accordions.shouldHave(size(7));
    SelenideElement accordion;
    for (int i = 0; i < accordions.size(); i++) {
      accordion = attributionSummaryTile.getAccordionByIndex(i);
      assertThat(accordion.attr("aria-expanded")).isEqualTo("true");
      assertThat(accordion.attr("open")).isEqualTo("true");
    }
  }

  private void assertOption(SelenideElement option, Owner owner) {
    option.shouldHave(Condition.value(owner.getId()));
    option.shouldHave(exactText(getOptionText(owner)));
  }

  private String getOptionText(Owner owner) {
    return StringUtils.capitalize(owner.getType().toString()) + " - " + owner.getName();
  }
}
