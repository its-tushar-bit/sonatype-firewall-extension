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
import com.sonatype.clm.testing.functional.pages.ComponentLegalOverviewPage.Notices;
import com.sonatype.clm.testing.functional.pages.ComponentNoticeDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.EditNoticesModal;
import com.sonatype.clm.testing.functional.pages.EditNoticesModal.Notice;
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
import com.sonatype.insight.brain.model.repository.Repository;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static org.assertj.core.api.Assertions.assertThat;

public class EditNoticesTest
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
    componentLegalFileDAO = lookup(ComponentLegalFileDAO.class);
    legalFileOverrideDAO = lookup(LegalFileOverrideDAO.class);
  }

  private void loadByHash() {
    refreshOrOpen(ComponentLegalOverviewPage.url(app, "033e7a20b23ea284d474"));
  }

  private void loadByHash(Owner scope) {
    refreshOrOpen(ComponentLegalOverviewPage.url(scope, "033e7a20b23ea284d474"));
  }

  private void loadByComponentIdentifier() throws UnsupportedEncodingException {
    Repository repository = tempEntity.newRepository();
    refreshOrOpen(ComponentLegalOverviewPage.urlByComponentIdentifier(componentIdentifier, repository.getId()));
  }

  private void doTestNoticesTile_InitialState() {
    Notices notices = ComponentLegalOverviewPage.notices();
    notices.all().shouldHave(size(2));
    assertNotice(notices.at(0), "META-INF/NOTICE", "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n");
    assertNotice(notices.at(1), "notice", "content");
  }

  @Test
  public void testNoticesTile_InitialStateByHash() {
    loadByHash();
    doTestNoticesTile_InitialState();
  }

  @Test
  public void testNoticesTile_InitialStateByComponentIdentifier() throws UnsupportedEncodingException {
    loadByComponentIdentifier();
    doTestNoticesTile_InitialState();
  }

  private void doTestNoticesModal_InitialState(int expectedScopeCount) {
    ComponentLegalOverviewPage.editNoticesButton().click();
    EditNoticesModal editNoticesModal = new EditNoticesModal();
    editNoticesModal.shouldBe(Condition.visible);
    editNoticesModal.allNotices().shouldHave(size(2));
    assertNotice(editNoticesModal.noticeAt(0), "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n", true);
    assertNotice(editNoticesModal.noticeAt(1), "content", true);
    assertOption(editNoticesModal.scopeDropdown().getSelectedOption(), rootOrg);
    ElementsCollection options = editNoticesModal.scopeDropdown().$$("option");
    options.shouldHave(size(expectedScopeCount));
    if (expectedScopeCount == 1) {
      assertOption(options.get(0), rootOrg);
    }
    else {
      assertOption(options.get(0), app);
      assertOption(options.get(1), org);
      assertOption(options.get(2), rootOrg);
    }
    assertButton(editNoticesModal.cancel(), true, null);
  }

  @Test
  public void testNoticesModal_InitialStateByHash() {
    loadByHash();
    doTestNoticesModal_InitialState(3);
    eyesWatcher.eyesCheck("Component legal edit notice files modal");
  }

  @Test
  public void testNoticesModal_InitialStateByComponentIdentifier() throws UnsupportedEncodingException {
    loadByComponentIdentifier();
    doTestNoticesModal_InitialState(1);
  }

  private void doTestAddNoticeByHash() {
    ComponentLegalOverviewPage.editNoticesButton().click();
    EditNoticesModal editNoticesModal = new EditNoticesModal();
    editNoticesModal.addNoticeButton().click();
    editNoticesModal.allNotices().shouldHave(size(3));
    assertNotice(editNoticesModal.noticeAt(2), "", true);
    assertButton(editNoticesModal.cancel(), true, null);
    editNoticesModal.noticeAt(2).textInput().setValue("custom");
    assertButton(editNoticesModal.save(), true, null);
    editNoticesModal.save().click();
    editNoticesModal.shouldNotBe(Condition.visible);
    assertNotice(ComponentLegalOverviewPage.notices().at(2), null, "custom");
  }

  @Test
  public void testAddNoticeByHash() {
    loadByHash();
    doTestAddNoticeByHash();
  }

  @Test
  public void testAddNoticeByComponentIdentifier() throws UnsupportedEncodingException {
    loadByComponentIdentifier();
    doTestAddNoticeByHash();
  }

  private void doTestChangeNoticeTextByHash() {
    ComponentLegalOverviewPage.editNoticesButton().click();
    EditNoticesModal editNoticesModal = new EditNoticesModal();
    Notice notice = editNoticesModal.noticeAt(0);
    String originalValue = notice.textInput().getValue();
    notice.textInput().setValue("changed");
    assertNotice(ComponentLegalOverviewPage.notices().at(0), "META-INF/NOTICE", originalValue);
    assertButton(editNoticesModal.save(), true, null);
    notice.textInput().setValue(originalValue);
    notice.textInput().setValue("changed");
    assertButton(editNoticesModal.save(), true, null);
    editNoticesModal.save().click();
    editNoticesModal.shouldNotBe(Condition.visible);
    assertNotice(ComponentLegalOverviewPage.notices().at(0), "META-INF/NOTICE", "changed");
  }

  @Test
  public void testChangeNoticeTextByHash() {
    loadByHash();
    doTestChangeNoticeTextByHash();
  }

  @Test
  public void testChangeNoticeTextByComponentIdentifier() throws UnsupportedEncodingException {
    loadByComponentIdentifier();
    doTestChangeNoticeTextByHash();
  }

  private void doTestChangeNoticeStatus() {
    ComponentLegalOverviewPage.editNoticesButton().click();
    EditNoticesModal editNoticesModal = new EditNoticesModal();
    Notice notice = editNoticesModal.noticeAt(0);
    notice.statusCheckbox().shouldBe(selected);
    notice.textInput().shouldBe(Condition.enabled);
    notice.statusToggle().click();
    notice.statusCheckbox().shouldNotBe(selected);
    notice.textInput().shouldBe(Condition.disabled);
    assertButton(editNoticesModal.save(), true, null);
    notice.statusToggle().click();
    notice.statusCheckbox().shouldBe(selected);
    notice.textInput().shouldBe(Condition.enabled);
    notice.statusToggle().click();
    notice.statusCheckbox().shouldNotBe(selected);
    notice.textInput().shouldBe(Condition.disabled);
    assertButton(editNoticesModal.save(), true, null);
    editNoticesModal.save().click();
    editNoticesModal.shouldNotBe(Condition.visible);
    ComponentLegalOverviewPage.notices().all().shouldHave(size(1));
    ComponentLegalOverviewPage.editNoticesButton().click();
    notice.statusCheckbox().shouldNotBe(selected);
  }

  @Test
  public void testChangeNoticeStatusByHash() {
    loadByHash();
    doTestChangeNoticeStatus();
  }

  @Test
  public void testChangeNoticeStatusByComponentIdentifier() throws UnsupportedEncodingException {
    loadByComponentIdentifier();
    doTestChangeNoticeStatus();
  }

  private void doTestCancel() {
    ComponentLegalOverviewPage.editNoticesButton().click();
    EditNoticesModal editNoticesModal = new EditNoticesModal();
    editNoticesModal.allNotices().shouldHave(size(2));
    Notice notice = editNoticesModal.noticeAt(0);
    assertNotice(notice, "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n", true);
    notice.textInput().setValue("changed");
    notice.statusToggle().click();
    assertNotice(notice, "changed", false);
    editNoticesModal.addNoticeButton().click();
    editNoticesModal.allNotices().shouldHave(size(3));
    editNoticesModal.cancel().click();
    editNoticesModal.shouldNotBe(Condition.visible);
    ComponentLegalOverviewPage.editNoticesButton().click();
    editNoticesModal.allNotices().shouldHave(size(2));
    assertNotice(notice, "\nApache ServiceComb" +
        "\nCopyright 2017-2021 The Apache Software Foundation" +
        "\n\nThis product includes software developed at" +
        "\nThe Apache Software Foundation (http://www.apache.org/).\n\n\n", true);
  }

  @Test
  public void testCancelByHash() {
    loadByHash();
    doTestCancel();
  }

  @Test
  public void testCancelByComponentIdentifier() throws UnsupportedEncodingException {
    loadByComponentIdentifier();
    doTestCancel();
  }

  @Test
  public void testLoadExisting_RootOrgLegalFile_AppScope() {
    testLoadExistingByHash(rootOrg, app);
  }

  @Test
  public void testLoadExisting_OrgLegalFile_AppScope() {
    testLoadExistingByHash(org, app);
  }

  @Test
  public void testLoadExisting_AppLegalFile_AppScope() {
    testLoadExistingByHash(app, app);
  }

  @Test
  public void testLoadExisting_RootOrgLegalFile_OrgScope() {
    testLoadExistingByHash(rootOrg, org);
  }

  @Test
  public void testLoadExisting_OrgLegalFile_OrgScope() {
    testLoadExistingByHash(org, org);
  }

  @Test
  public void testLoadExisting_RootOrgLegalFile_RootOrgScope() {
    testLoadExistingByHash(rootOrg, rootOrg);
  }

  private void testLoadExistingByHash(Owner owner, Owner scope) {
    ComponentLegalFile componentLegalFile =
        tempEntity.newComponentLegalFile(componentIdentifier, owner.getId(), LegalFileType.NOTICE, "legalContentHash");
    LegalFileOverride legalFileOverride = tempEntity.newLegalFileOverride(
        "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
        ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    loadByHash(scope);
    Notices notices = ComponentLegalOverviewPage.notices();
    assertNotice(notices.at(0), "META-INF/NOTICE", legalFileOverride.getContent());
    ComponentLegalOverviewPage.editNoticesButton().click();
    EditNoticesModal editNoticesModal = new EditNoticesModal();
    assertNotice(editNoticesModal.noticeAt(0), legalFileOverride);
    assertOption(editNoticesModal.scopeDropdown().getSelectedOption(), owner);
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
    loadByHash(scope);
    ComponentLegalOverviewPage.editNoticesButton().click();
    EditNoticesModal editNoticesModal = new EditNoticesModal();
    editNoticesModal.noticeAt(0).textInput().setValue("changed");
    editNoticesModal.scopeDropdown().selectOption(getOptionText(owner));
    List<Tuple> noticeContentsAndStatuses = getNoticeContentsAndStatuses(editNoticesModal);
    editNoticesModal.save().click();
    editNoticesModal.shouldNotBe(Condition.visible);
    assertThat(componentLegalFileDAO.getAll()).hasSize(1);
    assertSaved(noticeContentsAndStatuses, owner, componentIdentifier);
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
            .newComponentLegalFile(componentIdentifier, owners.get(index).getId(), LegalFileType.NOTICE,
                "legalContentHash" + index);
        tempEntity.newLegalFileOverride(
            "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
            ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
      }
    }
    else {
      ComponentLegalFile componentLegalFile =
          tempEntity
              .newComponentLegalFile(componentIdentifier, owner.getId(), LegalFileType.NOTICE, "legalContentHash");
      tempEntity.newLegalFileOverride(
          "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "content",
          ComponentLegalPartStatus.ENABLED, componentLegalFile.getId());
    }
    int size = componentLegalFileDAO.getAll().size();
    refreshOrOpen(ComponentLegalOverviewPage.url(scope, "033e7a20b23ea284d474"));
    ComponentLegalOverviewPage.editNoticesButton().click();
    EditNoticesModal editNoticesModal = new EditNoticesModal();
    editNoticesModal.scopeDropdown().selectOption(getOptionText(newOwner));
    List<Tuple> noticeContentsAndStatuses = getNoticeContentsAndStatuses(editNoticesModal);
    editNoticesModal.save().click();
    editNoticesModal.shouldNotBe(Condition.visible);
    if (owners.indexOf(newOwner) > owners.indexOf(owner)) {
      assertThat(componentLegalFileDAO.getAll()).hasSize(size - 1);
      assertThat(componentLegalFileDAO
          .getByOwnerIdAndComponentIdentifierAndType(owner.getId(), componentIdentifier, LegalFileType.NOTICE))
              .isNull();
    }
    else {
      assertThat(componentLegalFileDAO.getAll()).hasSize(size + 1);
    }
    assertSaved(noticeContentsAndStatuses, newOwner, componentIdentifier);
  }

  @Test
  public void testViewMoreDetailsNavigation_ByHash() {
    loadByHash();
    Notices notices = ComponentLegalOverviewPage.notices();
    notices.all().shouldHave(size(2));
    notices.at(0).viewMoreDetailsLink().click();
    ComponentNoticeDetailsPage.noticeOverview().shouldBe(visible);
    ComponentNoticeDetailsPage.noticeOverview()
        .getNoticeText()
        .shouldHave(text("Apache ServiceComb"));
  }

  @Test
  public void testViewMoreDetailsNavigation_ByComponentIdentifier() throws UnsupportedEncodingException {
    loadByComponentIdentifier();
    Notices notices = ComponentLegalOverviewPage.notices();
    notices.all().shouldHave(size(2));
    notices.at(0).viewMoreDetailsLink().click();
    ComponentNoticeDetailsPage.noticeOverview().shouldBe(visible);
    ComponentNoticeDetailsPage.noticeOverview()
        .getNoticeText()
        .shouldHave(text("Apache ServiceComb"));
  }

  private void assertNotice(ComponentLegalOverviewPage.Notice notice, String relPath, String text) {
    if (relPath == null) {
      notice.relPath().shouldBe(empty);
    }
    else {
      notice.relPath().shouldHave(exactText(relPath));
    }
    notice.text().shouldHave(exactText(text));
  }

  private void assertNotice(Notice notice, LegalFileOverride legalFileOverride) {
    assertNotice(notice, legalFileOverride.getContent(),
        legalFileOverride.getStatus() == ComponentLegalPartStatus.ENABLED);
  }

  private void assertNotice(Notice notice, String text, boolean enabled) {
    notice.textInput().shouldHave(exactText(text));
    if (enabled) {
      notice.statusToggle().shouldHave(exactText("Included"));
      notice.statusCheckbox().shouldBe(selected);
    }
    else {
      notice.statusToggle().shouldHave(exactText("Excluded"));
      notice.statusCheckbox().shouldNotBe(selected);
    }
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

  private List<Tuple> getNoticeContentsAndStatuses(EditNoticesModal editNoticesModal) {
    List<Tuple> result = new ArrayList<>();
    for (int index = 0; index < editNoticesModal.allNotices().size(); index++) {
      Notice notice = editNoticesModal.noticeAt(index);
      result.add(new Tuple(notice.textInput().getValue(), notice.statusCheckbox().isSelected()));
    }
    return result;
  }

  private List<Tuple> getNoticeContentsAndStatuses(List<LegalFileOverride> legalFileOverrides) {
    return legalFileOverrides.stream()
        .map(legalFileOverride -> new Tuple(legalFileOverride.getContent(),
            legalFileOverride.getStatus() == ComponentLegalPartStatus.ENABLED))
        .collect(Collectors.toList());
  }

  private void assertSaved(
      List<Tuple> expectedNoticeContentsAndStatuses,
      Owner owner,
      ComponentIdentifier componentIdentifier)
  {
    ComponentLegalFile componentLegalFile =
        componentLegalFileDAO
            .getByOwnerIdAndComponentIdentifierAndType(owner.getId(), componentIdentifier, LegalFileType.NOTICE);
    assertThat(componentLegalFile).isNotNull();
    List<LegalFileOverride> legalFileOverrides =
        legalFileOverrideDAO.getByComponentLegalFileId(componentLegalFile.getId());
    assertThat(expectedNoticeContentsAndStatuses)
        .containsExactlyInAnyOrder(getNoticeContentsAndStatuses(legalFileOverrides).toArray(new Tuple[0]));
  }
}
