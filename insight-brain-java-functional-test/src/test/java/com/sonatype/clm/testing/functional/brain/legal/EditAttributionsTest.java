/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.Attribution;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.EditAttributionModal;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentObligationAttributionDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
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
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class EditAttributionsTest
    extends AbstractFunctionalTest
{
  private Organization rootOrg;

  private Organization org;

  private Application app;

  private List<Owner> owners;

  private ComponentIdentifier componentIdentifier;

  private ComponentObligationAttributionDAO componentObligationAttributionDAO;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    rootOrg = lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    owners = Arrays.asList(app, org, rootOrg);
    componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "", "jar");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "033e7a20b23ea284d474", componentIdentifier);
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
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalFileHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/source-link");
    componentObligationAttributionDAO = lookup(ComponentObligationAttributionDAO.class);
  }

  @Test
  public void testAdditionalAttributionTile_InitialState() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    Attribution attribution = ComponentLegalOverviewPage.attribution(null);
    attribution.content().shouldHave(Condition.text("None added"));
    attribution.button().shouldHave(Condition.text("Add"));
  }

  @Test
  public void testAdditionalAttributionModal_InitialState() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    Attribution attribution = ComponentLegalOverviewPage.attribution(null);
    attribution.button().click();
    EditAttributionModal editAttributionModal = new EditAttributionModal();
    editAttributionModal.shouldBe(Condition.visible);
    editAttributionModal.header().shouldHave(Condition.text("Add Additional Attribution"));
    editAttributionModal.attributionText().shouldBe(empty);
    assertOption(editAttributionModal.scopeDropdown().getSelectedOption(), rootOrg);
    ElementsCollection options = editAttributionModal.scopeDropdown().$$("option");
    options.shouldHave(size(3));
    assertOption(options.get(0), app);
    assertOption(options.get(1), org);
    assertOption(options.get(2), rootOrg);
    assertButton(editAttributionModal.cancel(), true, null);

    eyesWatcher.eyesCheck("Component legal edit additional attribution modal");
  }

  @Test
  public void testAddAdditionalAttribution() {
    assertThat(componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNames(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(null))).isEmpty();
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    Attribution attribution = ComponentLegalOverviewPage.attribution(null);
    attribution.button().shouldHave(Condition.text("Add")).click();
    EditAttributionModal editAttributionModal = new EditAttributionModal();
    editAttributionModal.shouldBe(Condition.visible);
    assertButton(editAttributionModal.cancel(), true, null);
    editAttributionModal.attributionText().setValue("test");
    assertButton(editAttributionModal.cancel(), true, null);
    assertButton(editAttributionModal.save(), true, null);
    editAttributionModal.save().click();
    editAttributionModal.shouldNotBe(Condition.visible);
    assertThat(componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNames(
        Organization.ROOT_ORGANIZATION_ID, componentIdentifier, Collections.singleton(null))).hasSize(1);
    attribution.content().shouldHave(Condition.text("test"));
    attribution.button().shouldHave(Condition.text("Edit"));
  }

  @Test
  public void testEditAdditionalAttributionText() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, app.getId(), null, "content", "legalContentHash");
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    Attribution attribution = ComponentLegalOverviewPage.attribution(null);
    attribution.content().shouldHave(Condition.text(componentObligationAttribution.getContent()));
    attribution.button().shouldHave(Condition.text("Edit"));
    attribution.button().click();
    EditAttributionModal editAttributionModal = new EditAttributionModal();
    editAttributionModal.header().shouldHave(Condition.text("Edit Additional Attribution"));
    editAttributionModal.attributionText().shouldHave(Condition.text(componentObligationAttribution.getContent()));
    assertOption(editAttributionModal.scopeDropdown().getSelectedOption(), app);
    ElementsCollection options = editAttributionModal.scopeDropdown().$$("option");
    options.shouldHave(size(3));
    assertOption(options.get(0), app);
    assertOption(options.get(1), org);
    assertOption(options.get(2), rootOrg);
    assertButton(editAttributionModal.cancel(), true, null);
    editAttributionModal.attributionText().setValue("changed");
    assertButton(editAttributionModal.cancel(), true, null);
    assertButton(editAttributionModal.save(), true, null);
    editAttributionModal.save().click();
    editAttributionModal.shouldNotBe(Condition.visible);
    assertThat(componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNames(app.getId(),
        componentIdentifier, Collections.singleton(null))).extracting(ComponentObligationAttribution::getContent)
            .containsExactly("changed");
    attribution.content().shouldHave(Condition.text("changed"));
    attribution.button().shouldHave(Condition.text("Edit"));
  }

  @Test
  public void testEditAdditionalAttributionScope() {
    ComponentObligationAttribution componentObligationAttribution = tempEntity.newComponentObligationAttribution(
        componentIdentifier, app.getId(), null, "content", "legalContentHash");
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    Attribution attribution = ComponentLegalOverviewPage.attribution(null);
    attribution.content().shouldHave(Condition.text(componentObligationAttribution.getContent()));
    attribution.button().shouldHave(Condition.text("Edit"));
    attribution.button().click();
    EditAttributionModal editAttributionModal = new EditAttributionModal();
    editAttributionModal.header().shouldHave(Condition.text("Edit Additional Attribution"));
    editAttributionModal.attributionText().shouldHave(Condition.text(componentObligationAttribution.getContent()));
    assertOption(editAttributionModal.scopeDropdown().getSelectedOption(), app);
    ElementsCollection options = editAttributionModal.scopeDropdown().$$("option");
    options.shouldHave(size(3));
    assertOption(options.get(0), app);
    assertOption(options.get(1), org);
    assertOption(options.get(2), rootOrg);
    assertButton(editAttributionModal.cancel(), true, null);
    editAttributionModal.scopeDropdown().selectOption(getOptionText(org));
    assertButton(editAttributionModal.cancel(), true, null);
    assertButton(editAttributionModal.save(), true, null);
    editAttributionModal.save().click();
    editAttributionModal.shouldNotBe(Condition.visible);
    assertThat(componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNames(org.getId(),
        componentIdentifier, Collections.singleton(null))).hasSize(1);
    attribution.content().shouldHave(Condition.text(componentObligationAttribution.getContent()));
    attribution.button().shouldHave(Condition.text("Edit"));
  }

  @Test
  public void testSave_ChangeScope_AppToOrg_AppScope() {
    testSave_ChangeOwner(app, org, app);
  }

  @Test
  public void testSave_ChangeScope_AppToRootOrg_AppScope() {
    testSave_ChangeOwner(app, rootOrg, app);
  }

  @Test
  public void testSave_ChangeScope_OrgToRootOrg_AppScope() {
    testSave_ChangeOwner(org, rootOrg, app);
  }

  @Test
  public void testSave_ChangeScope_OrgToRootOrg_OrgScope() {
    testSave_ChangeOwner(org, rootOrg, org);
  }

  @Test
  public void testSave_ChangeScope_OrgToApp_AppScope() {
    testSave_ChangeOwner(org, app, app);
  }

  @Test
  public void testSave_ChangeScope_RootOrgToApp_AppScope() {
    testSave_ChangeOwner(rootOrg, app, app);
  }

  @Test
  public void testSave_ChangeScope_RootOrgToOrg_AppScope() {
    testSave_ChangeOwner(rootOrg, org, app);
  }

  @Test
  public void testSave_ChangeScope_RootOrgToOrg_OrgScope() {
    testSave_ChangeOwner(rootOrg, org, org);
  }

  private void testSave_ChangeOwner(Owner owner, Owner newOwner, Owner scope) {
    if (owners.indexOf(newOwner) > owners.indexOf(owner)) {
      for (int index = owners.indexOf(owner); index < owners.size(); index++) {
        tempEntity.newComponentObligationAttribution(componentIdentifier, owners.get(index).getId(), null, "content",
            "legalContentHash" + index);
      }
    }
    else {
      tempEntity.newComponentObligationAttribution(componentIdentifier, owner.getId(), null, "content",
          "legalContentHash");
    }
    int size = componentObligationAttributionDAO.getAll().size();
    refreshOrOpen(ComponentLegalOverviewPage.url(scope, "033e7a20b23ea284d474"));
    Attribution attribution = ComponentLegalOverviewPage.attribution(null);
    attribution.button().click();
    EditAttributionModal editAttributionModal = new EditAttributionModal();
    editAttributionModal.scopeDropdown().selectOption(getOptionText(newOwner));
    String attributionText = editAttributionModal.attributionText().getText();
    editAttributionModal.save().click();
    editAttributionModal.shouldNotBe(Condition.visible);
    if (owners.indexOf(newOwner) > owners.indexOf(owner)) {
      assertThat(componentObligationAttributionDAO.getAll()).hasSize(size - 1);
      assertThat(componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNames(owner.getId(),
          componentIdentifier, Collections.singleton(null))).isEmpty();
    }
    else {
      assertThat(componentObligationAttributionDAO.getAll()).hasSize(size + 1);
    }
    assertThat(componentObligationAttributionDAO.getByOwnerIdAndComponentIdentifierAndObligationNames(newOwner.getId(),
        componentIdentifier, Collections.singleton(null))).extracting(ComponentObligationAttribution::getContent)
            .containsExactly(attributionText);
  }

  private void assertOption(SelenideElement option, Owner owner) {
    option.shouldHave(Condition.value(owner.getId()));
    option.shouldHave(exactText(getOptionText(owner)));
  }

  private String getOptionText(Owner owner) {
    return StringUtils.capitalize(owner.getType().toString()) + " - " + owner.getName();
  }

  private void assertButton(Button button, boolean enabled, String tooltip) {
    if (enabled) {
      button.shouldNotBe(DISABLED);
    }
    else {
      button.shouldBe(DISABLED);
    }
    button.hover();
    if (tooltip != null) {
      Tooltip.get().shouldBe(Condition.visible).shouldBe(exactText(tooltip));
    }
    else {
      Tooltip.get().shouldNotBe(Condition.visible);
    }
  }
}
