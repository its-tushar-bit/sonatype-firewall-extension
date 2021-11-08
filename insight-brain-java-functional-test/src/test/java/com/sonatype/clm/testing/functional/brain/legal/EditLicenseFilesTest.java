/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.LicenseFiles;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.EditLicenseFilesModal;
import com.sonatype.clm.testing.functional.pages.EditLicenseFilesModal.LicenseFile;
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

public class EditLicenseFilesTest
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
    componentLegalFileDAO = new ComponentLegalFileDAO();
    legalFileOverrideDAO = new LegalFileOverrideDAO();
  }

  @Test
  public void testLicensesTile_InitialStateByHash() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    doTestLicensesTile_InitialState();
  }

  @Test
  public void testLicensesTile_InitialStateByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentIdentifier));
    doTestLicensesTile_InitialState();
  }

  private void doTestLicensesTile_InitialState() {
    LicenseFiles licenseFiles = ComponentLegalOverviewPage.licenseFiles();
    licenseFiles.all().shouldHaveSize(2);
    assertLicense(licenseFiles.at(0), "META-INF/LICENSE", "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n");
    assertLicense(licenseFiles.at(1), "license", "content");
  }

  @Test
  public void testLicensesModal_InitialStateByHash() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    doTestLicensesModal_InitialState(3);
    eyesWatcher.eyesCheck("Component legal edit license files modal");
  }

  @Test
  public void doTestLicensesModal_InitialStateByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentIdentifier));
    doTestLicensesModal_InitialState(1);
  }

  private void doTestLicensesModal_InitialState(int expectedScopeCount) {
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    EditLicenseFilesModal editLicenseFilesModal = new EditLicenseFilesModal();
    editLicenseFilesModal.shouldBe(Condition.visible);
    editLicenseFilesModal.allLicenses().shouldHaveSize(2);
    assertLicense(editLicenseFilesModal.licenseAt(0), "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n", true);
    assertLicense(editLicenseFilesModal.licenseAt(1), "content", true);
    assertOption(editLicenseFilesModal.scopeDropdown().getSelectedOption(), rootOrg);
    ElementsCollection options = editLicenseFilesModal.scopeDropdown().$$("option");
    options.shouldHaveSize(expectedScopeCount);
    if (expectedScopeCount == 1) {
      assertOption(options.get(0), rootOrg);
    }
    else {
      assertOption(options.get(0), app);
      assertOption(options.get(1), org);
      assertOption(options.get(2), rootOrg);
    }
    assertButton(editLicenseFilesModal.save(), false,
        "Must add a new license or change the content or status of a license.");
    assertButton(editLicenseFilesModal.cancel(), true, null);
  }

  @Test
  public void testAddLicenseByHash() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    doTestAddLicense("custom license by hash");
  }

  @Test
  public void testAddLicenseByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentIdentifier));
    doTestAddLicense("custom license by component identifier");
  }

  public void doTestAddLicense(String content) {
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    EditLicenseFilesModal editLicenseFilesModal = new EditLicenseFilesModal();
    editLicenseFilesModal.addLicenseButton().click();
    editLicenseFilesModal.allLicenses().shouldHaveSize(3);
    assertLicense(editLicenseFilesModal.licenseAt(2), "", true);
    assertButton(editLicenseFilesModal.save(), false, "A custom license must have text.");
    assertButton(editLicenseFilesModal.cancel(), true, null);
    editLicenseFilesModal.licenseAt(2).textInput().setValue(content);
    assertButton(editLicenseFilesModal.save(), true, null);
    editLicenseFilesModal.save().click();
    editLicenseFilesModal.shouldNotBe(Condition.visible);
    assertLicense(ComponentLegalOverviewPage.licenseFiles().at(2), null, content);
  }

  @Test
  public void testChangeLicenseTextByHash() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    doTestChangeLicenseText("value by hash");
  }

  @Test
  public void testChangeLicenseTextByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentIdentifier));
    doTestChangeLicenseText("value by component identifier");
  }

  private void doTestChangeLicenseText(String content) {
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    EditLicenseFilesModal editLicenseFilesModal = new EditLicenseFilesModal();
    LicenseFile license = editLicenseFilesModal.licenseAt(0);
    String originalValue = license.textInput().getValue();
    license.textInput().setValue(content);
    int index =
        ComponentLegalOverviewPage.licenseFiles().at(0).relPath().getText().contains("META-INF/LICENSE") ? 0 : 1;
    assertLicense(ComponentLegalOverviewPage.licenseFiles().at(index), "META-INF/LICENSE", originalValue);
    assertButton(editLicenseFilesModal.save(), true, null);
    license.textInput().setValue(originalValue);
    assertButton(editLicenseFilesModal.save(), false,
        "Must add a new license or change the content or status of a license.");
    license.textInput().setValue(content);
    assertButton(editLicenseFilesModal.save(), true, null);
    editLicenseFilesModal.save().click();
    editLicenseFilesModal.shouldNotBe(Condition.visible);
    index = ComponentLegalOverviewPage.licenseFiles().at(0).relPath().getText().contains("META-INF/LICENSE") ? 0 : 1;
    assertLicense(ComponentLegalOverviewPage.licenseFiles().at(index), "META-INF/LICENSE", content);
  }

  @Test
  public void testChangeLicenseStatusByHash() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    doTestChangeLicenseStatus();
  }

  @Test
  public void testChangeLicenseStatusByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentIdentifier));
    doTestChangeLicenseStatus();
  }

  private void doTestChangeLicenseStatus() {
    int initialLicenseFiles = ComponentLegalOverviewPage.licenseFiles().all().size();
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    EditLicenseFilesModal editLicenseFilesModal = new EditLicenseFilesModal();
    LicenseFile license = editLicenseFilesModal.licenseAt(0);
    license.statusCheckbox().shouldBe(Condition.selected);
    license.textInput().shouldBe(Condition.enabled);
    license.statusToggle().click();
    license.statusCheckbox().shouldNotBe(Condition.selected);
    license.textInput().shouldBe(Condition.disabled);
    assertButton(editLicenseFilesModal.save(), true, null);
    license.statusToggle().click();
    license.statusCheckbox().shouldBe(Condition.selected);
    license.textInput().shouldBe(Condition.enabled);
    assertButton(editLicenseFilesModal.save(), false,
        "Must add a new license or change the content or status of a license.");
    license.statusToggle().click();
    license.statusCheckbox().shouldNotBe(Condition.selected);
    license.textInput().shouldBe(Condition.disabled);
    assertButton(editLicenseFilesModal.save(), true, null);
    editLicenseFilesModal.save().click();
    editLicenseFilesModal.shouldNotBe(Condition.visible);
    ComponentLegalOverviewPage.licenseFiles().all().shouldHaveSize(initialLicenseFiles - 1);
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    license.statusCheckbox().shouldNotBe(Condition.selected);
  }

  @Test
  public void testCancelByHash() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
    doTestCancel();
  }

  @Test
  public void testCancelByComponentIdentifier() throws UnsupportedEncodingException {
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentIdentifier));
    doTestCancel();
  }

  private void doTestCancel() {
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    EditLicenseFilesModal editLicenseFilesModal = new EditLicenseFilesModal();
    editLicenseFilesModal.allLicenses().shouldHaveSize(2);
    LicenseFile license = editLicenseFilesModal.licenseAt(0);
    assertLicense(license, "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n", true);
    license.textInput().setValue("changed");
    license.statusToggle().click();
    assertLicense(license, "changed", false);
    editLicenseFilesModal.addLicenseButton().click();
    editLicenseFilesModal.allLicenses().shouldHaveSize(3);
    editLicenseFilesModal.cancel().click();
    editLicenseFilesModal.shouldNotBe(Condition.visible);
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    editLicenseFilesModal.allLicenses().shouldHaveSize(2);
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
        tempEntity.newComponentLegalFile(componentIdentifier, owner.getId(), LegalFileType.LICENSE, "legalContentHash");
    LegalFileOverride legalFileOverride = tempEntity.newLegalFileOverride(
        "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
        ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    refreshOrOpen(ComponentLegalOverviewPage.url(scope, "033e7a20b23ea284d474"));
    LicenseFiles licenseFiles = ComponentLegalOverviewPage.licenseFiles();
    assertLicense(licenseFiles.at(0), "META-INF/LICENSE", legalFileOverride.getContent());
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    EditLicenseFilesModal editLicenseFilesModal = new EditLicenseFilesModal();
    assertLicense(editLicenseFilesModal.licenseAt(0), legalFileOverride);
    assertOption(editLicenseFilesModal.scopeDropdown().getSelectedOption(), owner);
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
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    EditLicenseFilesModal editLicenseFilesModal = new EditLicenseFilesModal();
    editLicenseFilesModal.licenseAt(0).textInput().setValue("changed");
    editLicenseFilesModal.scopeDropdown().selectOption(getOptionText(owner));
    List<Tuple> licenseContentsAndStatuses = getLicenseContentsAndStatuses(editLicenseFilesModal);
    editLicenseFilesModal.save().click();
    editLicenseFilesModal.shouldNotBe(Condition.visible);
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
            .newComponentLegalFile(componentIdentifier, owners.get(index).getId(), LegalFileType.LICENSE,
                "legalContentHash" + index);
        tempEntity.newLegalFileOverride(
            "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
            ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
      }
    }
    else {
      ComponentLegalFile componentLegalFile =
          tempEntity
              .newComponentLegalFile(componentIdentifier, owner.getId(), LegalFileType.LICENSE, "legalContentHash");
      tempEntity.newLegalFileOverride(
          "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
          ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    }
    int size = componentLegalFileDAO.getAll().size();
    refreshOrOpen(ComponentLegalOverviewPage.url(scope, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editLicenseFilesButton().click();
    EditLicenseFilesModal editLicenseFilesModal = new EditLicenseFilesModal();
    editLicenseFilesModal.scopeDropdown().selectOption(getOptionText(newOwner));
    List<Tuple> licenseContentsAndStatuses = getLicenseContentsAndStatuses(editLicenseFilesModal);
    editLicenseFilesModal.save().click();
    editLicenseFilesModal.shouldNotBe(Condition.visible);
    if (owners.indexOf(newOwner) > owners.indexOf(owner)) {
      assertThat(componentLegalFileDAO.getAll()).hasSize(size - 1);
      assertThat(componentLegalFileDAO
          .getByOwnerIdAndComponentIdentifierAndType(owner.getId(), componentIdentifier, LegalFileType.LICENSE))
          .isNull();
    }
    else {
      assertThat(componentLegalFileDAO.getAll()).hasSize(size + 1);
    }
    assertSaved(licenseContentsAndStatuses, newOwner, componentIdentifier);
  }

  private void assertLicense(ComponentLegalOverviewPage.LicenseFile license, String relPath, String text) {
    if (relPath == null) {
      license.relPath().shouldHave(Condition.exactText(""));
    }
    else {
      license.relPath().shouldHave(Condition.exactText(relPath));
    }
    license.text().shouldHave(Condition.exactText(text));
  }

  private void assertLicense(LicenseFile license, LegalFileOverride legalFileOverride) {
    assertLicense(license, legalFileOverride.getContent(),
        legalFileOverride.getStatus() == ComponentLegalPartStatus.ENABLED);
  }

  private void assertLicense(LicenseFile license, String text, boolean enabled) {
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

  private List<Tuple> getLicenseContentsAndStatuses(EditLicenseFilesModal editLicenseFilesModal) {
    List<Tuple> result = new ArrayList<>();
    for (int index = 0; index < editLicenseFilesModal.allLicenses().size(); index++) {
      LicenseFile license = editLicenseFilesModal.licenseAt(index);
      result.add(new Tuple(license.textInput().getValue(), license.statusCheckbox().isSelected()));
    }
    return result;
  }

  private List<Tuple> getLicenseContentsAndStatuses(List<LegalFileOverride> legalFileOverrides) {
    return legalFileOverrides.stream()
        .map(legalFileOverride -> new Tuple(legalFileOverride.getContent(),
            legalFileOverride.getStatus() == ComponentLegalPartStatus.ENABLED)).collect(Collectors.toList());
  }

  private void assertSaved(
      List<Tuple> expectedLicenseContentsAndStatuses,
      Owner owner,
      ComponentIdentifier componentIdentifier)
  {
    ComponentLegalFile componentLegalFile =
        componentLegalFileDAO
            .getByOwnerIdAndComponentIdentifierAndType(owner.getId(), componentIdentifier, LegalFileType.LICENSE);
    assertThat(componentLegalFile).isNotNull();
    List<LegalFileOverride> legalFileOverrides =
        legalFileOverrideDAO.getByComponentLegalFileId(componentLegalFile.getId());
    assertThat(expectedLicenseContentsAndStatuses)
        .containsExactlyInAnyOrder(getLicenseContentsAndStatuses(legalFileOverrides).toArray(new Tuple[0]));
  }
}
