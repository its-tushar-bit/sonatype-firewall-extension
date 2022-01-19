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
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class AllObligationsTest
    extends AbstractFunctionalTest
{
  private Application app;

  private Organization rootOrganization;

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final ComponentObligationDAO componentObligationDAO = new ComponentObligationDAO();

  private final ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
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
    obligations.all().shouldHaveSize(9);
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
    obligations.all().shouldHaveSize(9);
    for (int i = 0; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Fulfilled");
    }
    List<ComponentObligation> componentObligations = componentObligationDAO.getAll();
    assertThat(componentObligations).hasSize(9);
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
    obligations.all().shouldHaveSize(0);
    ComponentLegalOverviewPage.resolveAllObligationsButton().shouldNotBe(Condition.visible);
  }

  @Test
  public void testFulfillAllObligations_overwriteExisting() {
    tempEntity.newComponentObligation(componentId, rootOrganization.getId(), "Inclusion of Copyright", "some comment",
        ObligationStatus.FLAGGED, "N/A");

    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));

    Obligations obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHaveSize(9);
    assertThat(obligations.at(0).getObligationStatus()).isEqualTo("Flagged");
    for (int i = 1; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Unreviewed");
    }

    ComponentLegalOverviewPage.resolveAllObligationsButton().shouldBe(Condition.visible).click();
    EditAllObligationsModal modal = new EditAllObligationsModal();
    modal.should(Condition.appear);
    assertThat(modal.statusDropdown().getText()).isEqualTo("Fulfilled");
    assertThat(modal.commentTextInput().getText()).isEmpty();
    assertOption(modal.scopeDropdown().getSelectedOption(), rootOrganization);

    modal.save().click();
    modal.shouldNotBe(Condition.visible);

    obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHaveSize(9);
    for (int i = 0; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Fulfilled");
    }
    List<ComponentObligation> componentObligations = componentObligationDAO.getAll();
    assertThat(componentObligations).hasSize(9);
    componentObligations.forEach(co -> {
      assertThat(co.getStatus()).isEqualTo(ObligationStatus.FULFILLED);
      assertThat(co.getComment()).isEmpty();
    });
  }

  @Test
  public void testFulfillAllObligations_deleteExisting() {
    tempEntity.newComponentObligation(componentId, rootOrganization.getId(), "Inclusion of Copyright", "some comment",
        ObligationStatus.FLAGGED, "N/A");
    tempEntity.newComponentObligation(componentId, rootOrganization.getId(), "Inclusion of Notice", "some comment",
        ObligationStatus.FULFILLED, "N/A");

    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));

    Obligations obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHaveSize(9);
    assertThat(obligations.at(0).getObligationStatus()).isEqualTo("Flagged");
    assertThat(obligations.at(1).getObligationStatus()).isEqualTo("Fulfilled");
    for (int i = 2; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Unreviewed");
    }

    ComponentLegalOverviewPage.resolveAllObligationsButton().shouldBe(Condition.visible).click();
    EditAllObligationsModal modal = new EditAllObligationsModal();
    modal.should(Condition.appear);
    assertThat(modal.statusDropdown().getText()).isEqualTo("Fulfilled");
    assertThat(modal.commentTextInput().getText()).isEmpty();
    assertOption(modal.scopeDropdown().getSelectedOption(), rootOrganization);

    modal.statusDropdown().click();

    modal.ignoredDropdownOption().shouldBe(Condition.visible);
    modal.flaggedDropdownOption().shouldBe(Condition.visible);
    modal.openDropdownOption().shouldBe(Condition.visible).click();

    modal.save().click();
    modal.shouldNotBe(Condition.visible);

    obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHaveSize(9);
    for (int i = 0; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Unreviewed");
    }
    List<ComponentObligation> componentObligations = componentObligationDAO.getAll();
    assertThat(componentObligations).isEmpty();
  }

  @Test
  public void testCancel() {
    Obligations obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHaveSize(9);
    for (int i = 0; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Unreviewed");
    }

    ComponentLegalOverviewPage.resolveAllObligationsButton().shouldBe(Condition.visible).click();
    EditAllObligationsModal modal = new EditAllObligationsModal();

    modal.should(Condition.appear);

    assertThat(modal.statusDropdown().getText()).isEqualTo("Fulfilled");
    assertThat(modal.commentTextInput().getText()).isEmpty();
    assertOption(modal.scopeDropdown().getSelectedOption(), rootOrganization);

    modal.statusDropdown().click();

    modal.ignoredDropdownOption().shouldBe(Condition.visible);
    modal.openDropdownOption().shouldBe(Condition.visible);
    modal.flaggedDropdownOption().shouldBe(Condition.visible).click();

    assertThat(modal.statusDropdown().getText()).isEqualTo("Flagged");

    modal.cancel().click();
    modal.shouldNotBe(Condition.visible);

    obligations = ComponentLegalOverviewPage.obligations();
    obligations.all().shouldHaveSize(9);
    for (int i = 0; i < obligations.all().size(); i++) {
      assertThat(obligations.at(i).getObligationStatus()).isEqualTo("Unreviewed");
    }
    List<ComponentObligation> componentObligations = componentObligationDAO.getAll();
    assertThat(componentObligations).isEmpty();
  }

  @Test
  public void testAccordionsExpanded() {
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    AttributionSummaryTile attributionSummaryTile = new AttributionSummaryTile();
    ElementsCollection accordions = attributionSummaryTile.getAllAccordions();
    accordions.shouldHaveSize(6);
    SelenideElement accordion;
    for (int i = 0; i < accordions.size(); i++) {
      accordion = attributionSummaryTile.getAccordionByIndex(i);
      assertThat(accordion.attr("aria-expanded")).isEqualTo("true");
      assertThat(accordion.attr("open")).isEqualTo("true");
    }
  }

  @Test
  public void testAccordionCollapsed() {
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    AttributionSummaryTile attributionSummaryTile = new AttributionSummaryTile();
    ElementsCollection accordions = attributionSummaryTile.getAllAccordions();
    accordions.shouldHaveSize(6);
    SelenideElement accordion;
    for (int i = 0; i < accordions.size(); i++) {
      accordion = attributionSummaryTile.getAccordionByIndex(i);
      accordion.$(".nx-accordion__header").click();
      assertThat(accordion.attr("aria-expanded")).isEqualTo("false");
      assertThat(accordion.attr("open")).isNull();
    }
  }

  @Test
  public void testModalOpenWhenAccordionExpanded() {
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    AttributionSummaryTile attributionSummaryTile = new AttributionSummaryTile();
    ElementsCollection accordions = attributionSummaryTile.getAllAccordions();
    accordions.shouldHaveSize(6);
    SelenideElement accordion;
    SelenideElement openModal;
    for (int i = 1; i < accordions.size(); i++) {
      accordion = attributionSummaryTile.getAccordionByIndex(i);
      assertThat(accordion.attr("aria-expanded")).isEqualTo("true");
      assertThat(accordion.attr("open")).isEqualTo("true");
      accordion.$(".nx-accordion__header .nx-btn").click();
      openModal = attributionSummaryTile.openModal();
      assertThat(openModal.isDisplayed()).isTrue();
      openModal.$(".nx-form__cancel-btn").click();
    }
  }

  @Test
  public void testModalOpenWhenAccordionCollapsed() {
    refreshOrOpen(ComponentLegalOverviewPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474"));
    AttributionSummaryTile attributionSummaryTile = new AttributionSummaryTile();
    ElementsCollection accordions = attributionSummaryTile.getAllAccordions();
    accordions.shouldHaveSize(6);
    SelenideElement accordion;
    SelenideElement openModal;
    for (int i = 1; i < accordions.size(); i++) {
      accordion = attributionSummaryTile.getAccordionByIndex(i);
      accordion.$(".nx-accordion__header").click();
      assertThat(accordion.attr("aria-expanded")).isEqualTo("false");
      assertThat(accordion.attr("open")).isNull();
      accordion.$(".nx-accordion__header .nx-btn").click();
      openModal = attributionSummaryTile.openModal();
      assertThat(openModal.isDisplayed()).isTrue();
      openModal.$(".nx-form__cancel-btn").click();
    }
  }

  private void assertOption(SelenideElement option, Owner owner) {
    option.shouldHave(Condition.value(owner.getId()));
    option.shouldHave(Condition.exactText(getOptionText(owner)));
  }

  private String getOptionText(Owner owner) {
    return StringUtils.capitalise(owner.getType().toString()) + " - " + owner.getName();
  }
}
