/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage;
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.Licenses;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.EditLicensesModal;
import com.sonatype.clm.testing.functional.pages.EditLicensesModal.License;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.legal.ComponentLegalFileDAO;
import com.sonatype.insight.brain.dataaccess.legal.LegalFileOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.assertj.core.groups.Tuple;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class EditLicensesTest
    extends AbstractFunctionalTest
{
  private Organization rootOrg;

  private Organization org;

  private Application app;

  private List<Owner> owners;

  private ComponentIdentifier componentIdentifier;

  private ComponentLegalFileDAO componentLegalFileDAO;

  private LegalFileOverrideDAO legalFileOverrideDAO;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    rootOrg = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
    org = tempEntity.newOrganization();
    app = tempEntity.newApplication(org.getId());
    owners = Arrays.asList(app, org, rootOrg);
    componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
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
    componentLegalFileDAO = new ComponentLegalFileDAO();
    legalFileOverrideDAO = new LegalFileOverrideDAO();
  }

  @Test
  public void testLicensesTile_InitialState() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    Licenses licenses = ComponentLegalOverviewPage.licenses();
    licenses.all().shouldHaveSize(2);
    assertLicense(licenses.at(0), "META-INF/LICENSE", "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n");
    assertLicense(licenses.at(1), "license", "content");
  }

  @Test
  public void testLicensesModal_InitialState() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal editLicensesModal = new EditLicensesModal();
    editLicensesModal.shouldBe(Condition.visible);
    editLicensesModal.allLicenses().shouldHaveSize(2);
    assertLicense(editLicensesModal.licenseAt(0), "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n", true);
    assertLicense(editLicensesModal.licenseAt(1), "content", true);
    assertOption(editLicensesModal.scopeDropdown().getSelectedOption(), rootOrg);
    ElementsCollection options = editLicensesModal.scopeDropdown().$$("option");
    options.shouldHaveSize(3);
    assertOption(options.get(0), app);
    assertOption(options.get(1), org);
    assertOption(options.get(2), rootOrg);
    assertButton(editLicensesModal.save(), false,
        "Must add a new license or change the content or status of a license.");
    assertButton(editLicensesModal.cancel(), true, null);

    eyesWatcher.eyesCheck("Component legal edit license files modal");
  }

  @Test
  public void testAddLicense() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal editLicensesModal = new EditLicensesModal();
    editLicensesModal.addLicenseButton().click();
    editLicensesModal.allLicenses().shouldHaveSize(3);
    assertLicense(editLicensesModal.licenseAt(2), "", true);
    assertButton(editLicensesModal.save(), false, "A custom license must have text.");
    assertButton(editLicensesModal.cancel(), true, null);
    editLicensesModal.licenseAt(2).textInput().setValue("custom");
    assertButton(editLicensesModal.save(), true, null);
    editLicensesModal.save().click();
    editLicensesModal.shouldNotBe(Condition.visible);
    assertLicense(ComponentLegalOverviewPage.licenses().at(2), null, "custom");
  }

  @Test
  public void testChangeLicenseText() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal editLicensesModal = new EditLicensesModal();
    License license = editLicensesModal.licenseAt(0);
    String originalValue = license.textInput().getValue();
    license.textInput().setValue("changed");
    assertLicense(ComponentLegalOverviewPage.licenses().at(0), "META-INF/LICENSE", originalValue);
    assertButton(editLicensesModal.save(), true, null);
    license.textInput().setValue(originalValue);
    assertButton(editLicensesModal.save(), false,
        "Must add a new license or change the content or status of a license.");
    license.textInput().setValue("changed");
    assertButton(editLicensesModal.save(), true, null);
    editLicensesModal.save().click();
    editLicensesModal.shouldNotBe(Condition.visible);
    assertLicense(ComponentLegalOverviewPage.licenses().at(0), "META-INF/LICENSE", "changed");
  }

  @Test
  public void testChangeLicenseStatus() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal editLicensesModal = new EditLicensesModal();
    License license = editLicensesModal.licenseAt(0);
    license.statusCheckbox().shouldBe(Condition.selected);
    license.textInput().shouldBe(Condition.enabled);
    license.statusToggle().click();
    license.statusCheckbox().shouldNotBe(Condition.selected);
    license.textInput().shouldBe(Condition.disabled);
    assertButton(editLicensesModal.save(), true, null);
    license.statusToggle().click();
    license.statusCheckbox().shouldBe(Condition.selected);
    license.textInput().shouldBe(Condition.enabled);
    assertButton(editLicensesModal.save(), false,
        "Must add a new license or change the content or status of a license.");
    license.statusToggle().click();
    license.statusCheckbox().shouldNotBe(Condition.selected);
    license.textInput().shouldBe(Condition.disabled);
    assertButton(editLicensesModal.save(), true, null);
    editLicensesModal.save().click();
    editLicensesModal.shouldNotBe(Condition.visible);
    ComponentLegalOverviewPage.editLicensesButton().click();
    license.statusCheckbox().shouldNotBe(Condition.selected);
  }

  @Test
  public void testCancel() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal editLicensesModal = new EditLicensesModal();
    editLicensesModal.allLicenses().shouldHaveSize(2);
    License license = editLicensesModal.licenseAt(0);
    assertLicense(license, "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n", true);
    license.textInput().setValue("changed");
    license.statusToggle().click();
    assertLicense(license, "changed", false);
    editLicensesModal.addLicenseButton().click();
    editLicensesModal.allLicenses().shouldHaveSize(3);
    editLicensesModal.cancel().click();
    editLicensesModal.shouldNotBe(Condition.visible);
    ComponentLegalOverviewPage.editLicensesButton().click();
    editLicensesModal.allLicenses().shouldHaveSize(2);
    assertLicense(license, "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n", true);
  }

  @Test
  public void testLoadExisting_RootOrgLegalFile_AppScope() {
    testLoadExisting(rootOrg, app);
  }

  @Test
  public void testLoadExisting_OrgLegalFile_AppScope() {
    testLoadExisting(org, app);
  }

  @Test
  public void testLoadExisting_AppLegalFile_AppScope() {
    testLoadExisting(app, app);
  }

  @Test
  public void testLoadExisting_RootOrgLegalFile_OrgScope() {
    testLoadExisting(rootOrg, org);
  }

  @Test
  public void testLoadExisting_OrgLegalFile_OrgScope() {
    testLoadExisting(org, org);
  }

  @Test
  public void testLoadExisting_RootOrgLegalFile_RootOrgScope() {
    testLoadExisting(rootOrg, rootOrg);
  }

  private void testLoadExisting(Owner owner, Owner scope) {
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, owner.getId(), "legalContentHash");
    LegalFileOverride legalFileOverride = tempEntity.newLegalFileOverride(LegalFileType.LICENSE,
        "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
        ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    refreshOrOpen(ComponentLegalOverviewPage.url(scope, "033e7a20b23ea284d474"));
    Licenses licenses = ComponentLegalOverviewPage.licenses();
    assertLicense(licenses.at(0), "META-INF/LICENSE", legalFileOverride.getContent());
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal editLicensesModal = new EditLicensesModal();
    assertLicense(editLicensesModal.licenseAt(0), legalFileOverride);
    assertOption(editLicensesModal.scopeDropdown().getSelectedOption(), owner);
  }

  @Test
  public void testSave_RootOrgLegalFile_AppScope() {
    testSave(rootOrg, app);
  }

  @Test
  public void testSave_OrgLegalFile_AppScope() {
    testSave(org, app);
  }

  @Test
  public void testSave_AppLegalFile_AppScope() {
    testSave(app, app);
  }

  @Test
  public void testSave_RootOrgLegalFile_OrgScope() {
    testSave(rootOrg, org);
  }

  @Test
  public void testSave_OrgLegalFile_OrgScope() {
    testSave(org, org);
  }

  @Test
  public void testSave_RootOrgLegalFile_RootOrgScope() {
    testSave(rootOrg, rootOrg);
  }

  private void testSave(Owner owner, Owner scope) {
    assertThat(componentLegalFileDAO.getAll()).isEmpty();
    refreshOrOpen(ComponentLegalOverviewPage.url(scope, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal editLicensesModal = new EditLicensesModal();
    editLicensesModal.licenseAt(0).textInput().setValue("changed");
    editLicensesModal.scopeDropdown().selectOption(getOptionText(owner));
    List<Tuple> licenseContentsAndStatuses = getLicenseContentsAndStatuses(editLicensesModal);
    editLicensesModal.save().click();
    editLicensesModal.shouldNotBe(Condition.visible);
    assertThat(componentLegalFileDAO.getAll()).hasSize(1);
    assertSaved(licenseContentsAndStatuses, owner, componentIdentifier);
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
        ComponentLegalFile componentLegalFile = tempEntity
            .newComponentLegalFile(componentIdentifier, owners.get(index).getId(), "legalContentHash" + index);
        tempEntity.newLegalFileOverride(LegalFileType.LICENSE,
            "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
            ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
      }
    }
    else {
      ComponentLegalFile componentLegalFile =
          tempEntity.newComponentLegalFile(componentIdentifier, owner.getId(), "legalContentHash");
      tempEntity.newLegalFileOverride(LegalFileType.LICENSE,
          "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
          ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    }
    int size = componentLegalFileDAO.getAll().size();
    refreshOrOpen(ComponentLegalOverviewPage.url(scope, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editLicensesButton().click();
    EditLicensesModal editLicensesModal = new EditLicensesModal();
    editLicensesModal.scopeDropdown().selectOption(getOptionText(newOwner));
    List<Tuple> licenseContentsAndStatuses = getLicenseContentsAndStatuses(editLicensesModal);
    editLicensesModal.save().click();
    editLicensesModal.shouldNotBe(Condition.visible);
    if (owners.indexOf(newOwner) > owners.indexOf(owner)) {
      assertThat(componentLegalFileDAO.getAll()).hasSize(size - 1);
      assertThat(componentLegalFileDAO.getByOwnerIdAndComponentIdentifier(owner.getId(), componentIdentifier)).isNull();
    }
    else {
      assertThat(componentLegalFileDAO.getAll()).hasSize(size + 1);
    }
    assertSaved(licenseContentsAndStatuses, newOwner, componentIdentifier);
  }

  private void assertLicense(ComponentLegalOverviewPage.License license, String relPath, String text) {
    if (relPath == null) {
      license.relPath().shouldHave(Condition.exactText(""));
    }
    else {
      license.relPath().shouldHave(Condition.exactText(relPath));
    }
    license.text().shouldHave(Condition.exactText(text));
  }

  private void assertLicense(License license, LegalFileOverride legalFileOverride) {
    assertLicense(license, legalFileOverride.getContent(),
        legalFileOverride.getStatus() == ComponentLegalPartStatus.ENABLED);
  }

  private void assertLicense(License license, String text, boolean enabled) {
    license.textInput().shouldHave(Condition.exactText(text));
    if (enabled) {
      license.statusToggle().shouldHave(Condition.exactText("Included"));
      license.statusCheckbox().shouldBe(Condition.selected);
    }
    else {
      license.statusToggle().shouldHave(Condition.exactText("Excluded"));
      license.statusCheckbox().shouldNotBe(Condition.selected);
    }
  }

  private void assertOption(SelenideElement option, Owner owner) {
    option.shouldHave(Condition.value(owner.getId()));
    option.shouldHave(Condition.exactText(getOptionText(owner)));
  }

  private String getOptionText(Owner owner) {
    return StringUtils.capitalise(owner.getType().toString()) + " - " + owner.getName();
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
      Tooltip.get().shouldBe(Condition.visible).shouldBe(Condition.exactText(tooltip));
    }
    else {
      Tooltip.get().shouldNotBe(Condition.visible);
    }
  }

  private List<Tuple> getLicenseContentsAndStatuses(EditLicensesModal editLicensesModal) {
    List<Tuple> result = new ArrayList<>();
    for (int index = 0; index < editLicensesModal.allLicenses().size(); index++) {
      License license = editLicensesModal.licenseAt(index);
      result.add(new Tuple(license.textInput().getValue(), license.statusCheckbox().isSelected()));
    }
    return result;
  }

  private List<Tuple> getLicenseContentsAndStatuses(List<LegalFileOverride> legalFileOverrides) {
    return legalFileOverrides.stream()
        .filter(legalFileOverride -> legalFileOverride.getType() == LegalFileType.LICENSE)
        .map(legalFileOverride -> new Tuple(legalFileOverride.getContent(),
            legalFileOverride.getStatus() == ComponentLegalPartStatus.ENABLED)).collect(Collectors.toList());
  }

  private void assertSaved(
      List<Tuple> expectedLicenseContentsAndStatuses,
      Owner owner,
      ComponentIdentifier componentIdentifier)
  {
    ComponentLegalFile componentLegalFile =
        componentLegalFileDAO.getByOwnerIdAndComponentIdentifier(owner.getId(), componentIdentifier);
    assertThat(componentLegalFile).isNotNull();
    List<LegalFileOverride> legalFileOverrides =
        legalFileOverrideDAO.getByComponentLegalFileId(componentLegalFile.getId());
    assertThat(expectedLicenseContentsAndStatuses)
        .containsExactlyInAnyOrder(getLicenseContentsAndStatuses(legalFileOverrides).toArray(new Tuple[0]));
  }
}
