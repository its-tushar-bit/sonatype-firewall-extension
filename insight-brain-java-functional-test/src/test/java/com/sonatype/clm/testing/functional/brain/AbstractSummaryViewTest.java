/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.AccessTile;
import com.sonatype.clm.testing.functional.elements.AccessTileList;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.ArtifactoryRepositoryTile;
import com.sonatype.clm.testing.functional.elements.ErrorBox;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.GreedyTable.HeaderColumn;
import com.sonatype.clm.testing.functional.elements.InnerSourceRepositoryTile;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile.ApplicableLicenseThreatGroupSection;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile.LicenseThreatGroupElement;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxList;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.NxColor;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.repository.RepositoryConnection;
import com.sonatype.insight.brain.model.repository.RepositoryFormat;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.GreedyTable.HeaderColumn.NX_DOWN_SELECTED;
import static com.sonatype.clm.testing.functional.elements.GreedyTable.HeaderColumn.NX_UP_SELECTED;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSummaryViewTest
    extends AbstractFunctionalTest
{
  protected Owner currentOwner;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  @After
  public void afterEach() {
    // This ensures no open modals that might interfere with further logic are open after each test
    refresh();
  }

  @Test
  public void testSummaryTile() {
    OwnerSummaryPage.summaryTile().name().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    OwnerSummaryPage.summaryTile().headerIcon().shouldBe(visible);

    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      OwnerSummaryPage.summaryTile().publicId().shouldBe(visible).shouldHave(text(currentOwner.getPublicId()));
    }
    else {
      OwnerSummaryPage.summaryTile().publicId().shouldBe(hidden);
    }
  }

  @Test
  public void testSummaryTile_missing() {
    refreshOrOpen(OwnerSummaryPage.url(currentOwner.getType(), "fakeid"));

    ErrorBox error = OwnerSummaryPage.summaryTile().error();
    error.shouldBe(visible);
    error.shouldHave(text("Could not find an " + currentOwner.getType().toString()));
    error.retryButton().shouldBe(visible, enabled);
  }

  @Test
  public void testActionDropDown() {
    ActionDropDown.menu().shouldBe(hidden);
    ActionDropDown.actionButton().shouldBe(visible).click();
    ActionDropDown.menu().shouldBe(visible);
    ActionDropDown.actionButton().click();
    ActionDropDown.menu().shouldBe(hidden);
  }

  @Test
  public void testEditAppOrgNameLink() {
    String shortTypeName = currentOwner.getType().toString().equalsIgnoreCase("application") ? "App" : "Org";
    ActionDropDown.actionButton().click();
    OwnerEditorDialog.root().shouldBe(hidden);
    ActionDropDown.editOwner().shouldHave(text(shortTypeName)).click();
    OwnerEditorDialog.root().shouldBe(visible);
    OwnerEditorDialog.title().shouldHave(text(currentOwner.getType().toString()));
  }

  @Test
  public void testTile_default() {
    testLabelTile_no_labels();
    // eyesWatcher.eyesCheck(String.format("empty label tile for %s %s",
    //  currentOwner.getType(), currentOwner.getName()));
    testAccessTile_no_local_access();
    eyesWatcher.eyesCheck(String.format("empty access tile with inherited data for %s %s", currentOwner.getType(),
        currentOwner.getName()));
    testPolicyTile_no_policies();
    eyesWatcher.eyesCheck(String.format("empty policy tile with inherited data for %s %s", currentOwner.getType(),
        currentOwner.getName()));
  }

  @Test
  public void testInnerSourceRepositoryTile_NotConfigured() {
    setCurrentOwnerRepositoryConnectionStatus(currentOwner, true);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
    InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
    innerSourceRepositoryTile.should(exist);
    innerSourceRepositoryTile.listTitle().should(exist);
    ElementsCollection rows = innerSourceRepositoryTile.rows();
    rows.shouldHaveSize(1);
    rows.get(0).shouldBe(text("No InnerSource repository connections are configured"));
    innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));

    eyesWatcher.eyesCheck(String.format("InnerSource repository tile not configured for %s %s", currentOwner.getType(),
        currentOwner.getName()));
  }

  @Test
  public void testInnerSourceRepositoryTile_Disabled() {
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
    InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
    innerSourceRepositoryTile.should(exist);
    innerSourceRepositoryTile.listTitle().should(exist);
    ElementsCollection rows = innerSourceRepositoryTile.rows();
    rows.shouldHaveSize(1);
    rows.get(0).shouldBe(text("InnerSource repository connections are disabled"));
    innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));

    eyesWatcher.eyesCheck(String.format("InnerSource repository tile disabled for %s %s", currentOwner.getType(),
        currentOwner.getName()));
  }

  @Test
  public void testInnerSourceRepositoryTile_Configured() {
    try {
      setCurrentOwnerRepositoryConnectionStatus(currentOwner, true);
      RepositoryConnection repositoryConnection1 = tempEntity.newRepositoryConnection(currentOwner.getId(),
          "http://some.base.url.1", RepositoryFormat.MAVEN, null, null);
      RepositoryConnection repositoryConnection2 = tempEntity.newRepositoryConnection(currentOwner.getId(),
          "http://some.base.url.2", RepositoryFormat.NPM, null, null);

      refresh();
      SidebarNavigation.closeNavigationSidebar();
      OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
      InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
      innerSourceRepositoryTile.should(exist);
      innerSourceRepositoryTile.listTitle().shouldHave(text("Local"));
      ElementsCollection rows = innerSourceRepositoryTile.rows();
      rows.shouldHaveSize(2);
      rows.get(0).shouldBe(text(repositoryConnection1.getBaseUrl() + "\n" + repositoryConnection1.getFormat()));
      rows.get(1).shouldBe(text(repositoryConnection2.getBaseUrl() + "\n" + repositoryConnection2.getFormat()));
      innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));

      eyesWatcher.eyesCheck(
          String.format("InnerSource repository tile local configured for %s %s", currentOwner.getType(),
              currentOwner.getName()));
    }
    finally {
      setCurrentOwnerRepositoryConnectionStatus(currentOwner, null);
    }
  }

  private void setCurrentOwnerRepositoryConnectionStatus(Owner currentOwner, Boolean repoConnectionsEnabled) {
    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      Application app = (Application) currentOwner;
      app.setRepositoryConnectionEnabled(repoConnectionsEnabled);
      new ApplicationDAO().update(app);
    }
    else if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      Organization org = (Organization) currentOwner;
      org.setRepositoryConnectionEnabled(repoConnectionsEnabled);
      new OrganizationDAO().update(org);
    }
  }

  @Test
  public void testInnerSourceRepositoryTile_Configured_Inherited() {
    OrganizationDAO organizationDAO = new OrganizationDAO();
    Organization parentOwner = organizationDAO.getById(currentOwner.getParentOwnerId());
    try {
      RepositoryConnection repositoryConnection1 = tempEntity.newRepositoryConnection(currentOwner.getParentOwnerId(),
          "http://some.base.url.1", RepositoryFormat.MAVEN, null, null);
      RepositoryConnection repositoryConnection2 = tempEntity.newRepositoryConnection(currentOwner.getParentOwnerId(),
          "http://some.base.url.2", RepositoryFormat.NPM, null, null);
      parentOwner.setAllowRepositoryConnectionOverride(false);
      parentOwner.setRepositoryConnectionEnabled(true);
      organizationDAO.update(parentOwner);

      refresh();
      SidebarNavigation.closeNavigationSidebar();
      OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible).click();
      InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
      innerSourceRepositoryTile.should(exist);
      innerSourceRepositoryTile.listTitle().shouldHave(text("Inherited from " + parentOwner.getName()));
      ElementsCollection rows = innerSourceRepositoryTile.rows();
      rows.shouldHaveSize(2);
      rows.get(0).shouldBe(text(repositoryConnection1.getBaseUrl() + "\n" + repositoryConnection1.getFormat()));
      rows.get(1).shouldBe(text(repositoryConnection2.getBaseUrl() + "\n" + repositoryConnection2.getFormat()));
      innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));

      eyesWatcher.eyesCheck(
          String.format("InnerSource repository tile configured inherit for %s %s", currentOwner.getType(),
              currentOwner.getName()));
    }
    finally {
      parentOwner.setAllowRepositoryConnectionOverride(true);
      parentOwner.setRepositoryConnectionEnabled(null);
      organizationDAO.update(parentOwner);
    }
  }

  @Test
  public void testInnerSourceRepositoryTile_FeatureDisabled() {
    SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION.setEnabled(false);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldNot(exist);
    OwnerSummaryPage.innerSourceRepositoryTile().shouldNot(exist);

    eyesWatcher.eyesCheck(String.format("InnerSource repository tile feature disabled %s %s", currentOwner.getType(),
        currentOwner.getName()));
  }

  @Test
  public void testArtifactoryRepositoryTile_NotConfigured() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    setCurrentOwnerArtifactoryConnectionStatus(currentOwner, true);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible).click();
    ArtifactoryRepositoryTile artifactoryRepositoryTile = OwnerSummaryPage.artifactoryRepositoryTile();
    artifactoryRepositoryTile.should(exist);
    artifactoryRepositoryTile.listTitle().should(exist);
    ElementsCollection rows = artifactoryRepositoryTile.rows();
    rows.shouldHaveSize(1);
    rows.get(0).shouldBe(text("No Artifactory repository connection is configured"));
    artifactoryRepositoryTile.editButton().shouldHave(text("Edit"));

    eyesWatcher.eyesCheck(String.format("Artifactory repository tile not configured for %s %s", currentOwner.getType(),
        currentOwner.getName()));
  }

  @Test
  public void testArtifactoryRepositoryTile_Disabled() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible).click();
    ArtifactoryRepositoryTile artifactoryRepositoryTile = OwnerSummaryPage.artifactoryRepositoryTile();
    artifactoryRepositoryTile.should(exist);
    artifactoryRepositoryTile.listTitle().should(exist);
    ElementsCollection rows = artifactoryRepositoryTile.rows();
    rows.shouldHaveSize(1);
    rows.get(0).shouldBe(text("Artifactory repository connection is disabled"));
    artifactoryRepositoryTile.editButton().shouldHave(text("Edit"));

    eyesWatcher.eyesCheck(String.format("Artifactory repository tile disabled for %s %s", currentOwner.getType(),
        currentOwner.getName()));
  }

  @Test
  public void testArtifactoryRepositoryTile_Configured() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    try {
      setCurrentOwnerArtifactoryConnectionStatus(currentOwner, true);
      ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(currentOwner.getId(),
          "http://some.base.url", null, null);

      refresh();
      SidebarNavigation.closeNavigationSidebar();
      OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible).click();
      ArtifactoryRepositoryTile artifactoryRepositoryTile = OwnerSummaryPage.artifactoryRepositoryTile();
      artifactoryRepositoryTile.should(exist);
      artifactoryRepositoryTile.listTitle().shouldHave(text("Local"));
      ElementsCollection rows = artifactoryRepositoryTile.rows();
      rows.shouldHaveSize(1);
      rows.get(0).shouldBe(text(artifactoryConnection.getBaseUrl()));
      artifactoryRepositoryTile.editButton().shouldHave(text("Edit"));

      eyesWatcher.eyesCheck(
          String.format("Artifactory repository tile configured local for %s %s", currentOwner.getType(),
              currentOwner.getName()));
    }
    finally {
      setCurrentOwnerArtifactoryConnectionStatus(currentOwner, null);
    }
  }

  private void setCurrentOwnerArtifactoryConnectionStatus(Owner currentOwner, Boolean artifactoryConnectionEnabled) {
    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      Application app = (Application) currentOwner;
      app.setArtifactoryConnectionEnabled(artifactoryConnectionEnabled);
      new ApplicationDAO().update(app);
    }
    else if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      Organization org = (Organization) currentOwner;
      org.setArtifactoryConnectionEnabled(artifactoryConnectionEnabled);
      new OrganizationDAO().update(org);
    }
  }

  @Test
  public void testArtifactoryRepositoryTile_Configured_Inherited() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    OrganizationDAO organizationDAO = new OrganizationDAO();
    Organization parentOwner = organizationDAO.getById(currentOwner.getParentOwnerId());
    try {
      ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(currentOwner.getParentOwnerId(),
          "http://some.base.url", null, null);
      parentOwner.setAllowArtifactoryConnectionOverride(false);
      parentOwner.setArtifactoryConnectionEnabled(true);
      organizationDAO.update(parentOwner);

      refresh();
      SidebarNavigation.closeNavigationSidebar();
      OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible).click();
      ArtifactoryRepositoryTile artifactoryRepositoryTile = OwnerSummaryPage.artifactoryRepositoryTile();
      artifactoryRepositoryTile.should(exist);
      artifactoryRepositoryTile.listTitle().shouldHave(text("Inherited from " + parentOwner.getName()));
      ElementsCollection rows = artifactoryRepositoryTile.rows();
      rows.shouldHaveSize(1);
      rows.get(0).shouldBe(text(artifactoryConnection.getBaseUrl()));
      artifactoryRepositoryTile.editButton().shouldHave(text("Edit"));

      eyesWatcher.eyesCheck(
          String.format("Artifactory repository tile configured inherit for %s %s", currentOwner.getType(),
              currentOwner.getName()));
    }
    finally {
      parentOwner.setAllowRepositoryConnectionOverride(true);
      parentOwner.setRepositoryConnectionEnabled(null);
      organizationDAO.update(parentOwner);
    }
  }

  @Test
  public void testArtifactoryRepositoryTile_FeatureDisabled() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(false);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldNot(exist);
    OwnerSummaryPage.artifactoryRepositoryTile().shouldNot(exist);

    eyesWatcher.eyesCheck(
        String.format("Artifactory repository tile feature disabled for %s %s", currentOwner.getType(),
            currentOwner.getName()));
  }

  public void testLabelTile_no_labels() {
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    labelTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(currentOwner.getName()));
    labelTile.newButton().shouldBe(visible, enabled);

    labelTile.labelLists().shouldHaveSize(1);

    // scroll to the labels tile
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible).click();

    NxList list = labelTile.labelList(0);
    labelTile.labelListSubheader(0).shouldBe(visible).shouldHave(text("Local"));
    list.emptyDescriptor().shouldBe(visible);
  }

  public void testAccessTile_no_local_access() {
    int hierarchySize = getHierarchySize(currentOwner);
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.subHeader().shouldBe(visible).shouldHave(AccessTile.subHeaderText(currentOwner.getName()));
    accessTile.newButton().shouldBe(visible, enabled);
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      AccessTileList list = accessTile.accessList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().should(exist);

      }
      else {
        list.ownerName().shouldBe(hidden);
        list.emptyDescriptor().shouldBe(hidden);
      }
    }
  }

  private void testPolicyTile_no_policies() {
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    policyTile.subHeader().shouldBe(visible).shouldHave(PolicyTile.subHeaderText(currentOwner.getName()));
    policyTile.newButton().shouldBe(visible, enabled);

    policyTile.policyLists().shouldHaveSize(1);

    PolicyTileList list = policyTile.policyList(0);
    list.ownerName().shouldBe(visible).shouldHave(text("Local"));
    list.localEmptyDescriptor().shouldBe(visible);
  }

  @Test
  public void testTiles_Local() {
    List<Label> localLabels = new ArrayList<>();
    localLabels.add(tempEntity.newLabel(currentOwner.getId(), "Temp Local Label 1", Color.dark_purple));
    localLabels.add(tempEntity.newLabel(currentOwner.getId(), "Temp Local Label 2", "With Subtitle", Color.dark_blue));

    List<LicenseThreatGroup> locaLTGs = new ArrayList<>();
    locaLTGs.add(tempEntity.newLicenseThreatGroup(currentOwner.getId(), "Temp Local License 1", 9));
    locaLTGs.add(tempEntity.newLicenseThreatGroup(currentOwner.getId(), "Temp Local License 2", 1));

    User testUser = tempEntity.newUser("testUser", "Test", "User", "testuser@sonatype.com");
    RoleDAO roleDAO = new RoleDAO();
    List<Role> roleList = new ArrayList<>(roleDAO.getApplicationRoles());
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);
    tempEntity.newMembershipMapping(currentOwner.getId(), writeRole.getId(), testUser.getUsername());
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    tempEntity.newMembershipMapping(currentOwner.getId(), readRole.getId(), "Group", MemberType.GROUP);
    roleList.add(readRole);
    roleList.add(writeRole);

    List<Policy> localPolicies = new ArrayList<>();
    localPolicies.add(tempEntity.newPolicy(currentOwner.getId(), "Policy 1", 10, Action.ID_FAIL, Stage.ID_BUILD, null));
    localPolicies.add(tempEntity.newPolicy(currentOwner.getId(), "Policy 2", 5, Action.ID_WARN, Stage.ID_BUILD, null));
    localPolicies.add(tempEntity.newPolicy(currentOwner.getId(), "Policy 3", 4, null, null, new Notifications(
        new UserNotification("test@test.com", Stage.ID_BUILD))));

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    SidebarNavigation.closeNavigationSidebar();
    testLabelTile_Local(localLabels);
    eyesWatcher.eyesCheck(
        String.format("label tile with local data for %s %s", currentOwner.getType(), currentOwner.getName()));
    testLTGTile_Local(locaLTGs);
    eyesWatcher.eyesCheck(
        String.format("license threat group tile with local data for %s %s", currentOwner.getType(),
            currentOwner.getName()));
    testAccessTile_Local(testUser);
    eyesWatcher.eyesCheck(
        String.format("access tile with local data for %s %s", currentOwner.getType(), currentOwner.getName()));
    testPolicyTile_Local(localPolicies);
    eyesWatcher.eyesCheck(
        String.format("policy tile with local data for %s %s", currentOwner.getType(), currentOwner.getName()));
  }

  private void testLabelTile_Local(List<Label> localLabels) {
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    labelTile.labelLists().shouldHaveSize(1);

    // scroll to the labels tile
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible).click();

    NxList list = labelTile.labelList(0);
    labelTile.labelListSubheader(0).shouldBe(visible).shouldHave(text("Local"));
    list.emptyDescriptor().shouldNot(exist);
    list.elements().shouldHaveSize(localLabels.size());

    for (int i = 0; i < localLabels.size(); i++) {
      NxList.NxListItem actualLabel = list.element(i);
      Label expectedLabel = localLabels.get(i);

      if (expectedLabel.getDescription() == null) {
        actualLabel.description().shouldNot(exist);
      }
      else {
        actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
      }

      String nxColorClass = NxColor.getNxColorFromColor(expectedLabel.getColor()).toNxClass();

      actualLabel.icon().shouldBe(visible).shouldHave(cssClass(nxColorClass));

      actualLabel.name().shouldBe(visible).shouldHave(text(expectedLabel.getLabel()));
      actualLabel.chevron().shouldBe(visible);
    }
  }

  private void testLTGTile_Local(List<LicenseThreatGroup> locaLTGs) {
    LicenseThreatGroupSummaryTile ltgTile = OwnerSummaryPage.licenseThreatGroupSummaryTile();
    ltgTile.getAllApplicableLicenseThreatGroupSection().shouldHaveSize(2);

    ScrollUtil.scrollIntoView(ltgTile.nxHeader());
    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      ltgTile.addLTGButton().shouldNot(exist);
    }
    else {
      ltgTile.addLTGButton().shouldBe(visible);
    }

    // scroll to the ltgs
    OwnerSummaryPage.summaryTile().ltgsButton().shouldBe(visible).click();

    ApplicableLicenseThreatGroupSection section = ltgTile.getApplicableLicenseThreatGroupSection(0);

    ScrollUtil.scrollIntoView(section.getTitle());
    section.getTitle().shouldBe(visible).shouldHave(text("Local"));
    section.getEmptyDescriptor().shouldNot(exist);
    section.getTableContent().shouldHaveSize(locaLTGs.size());

    for (int j = 0; j < locaLTGs.size(); j++) {
      LicenseThreatGroupElement actualLTG = section.getLicenseThreatGroupElement(section.getLTG(j));
      //ThreatGroupTileSimpleListElement actualLTG = list.element(j);
      LicenseThreatGroup expectedLTG = locaLTGs.get(j);

      actualLTG.getName().shouldBe(visible).shouldHave(text(expectedLTG.getName()));

      String threatLevel = String.valueOf(expectedLTG.getThreatLevel());
      actualLTG.getThreatLevelValue().shouldBe(visible).shouldHave(text(threatLevel));
      actualLTG.getThreatLevelIndicator().shouldBe(visible)
        .shouldHave(LicenseThreatGroupElement.threatLevel(expectedLTG.getThreatLevel()));
      actualLTG.getChevron().shouldBe(visible);
    }

    section = ltgTile.getApplicableLicenseThreatGroupSection(1);
    ScrollUtil.scrollIntoView(section.getTitle());
    section.getTitle().shouldBe(visible).shouldHave(text("INHERITED FROM ROOT ORGANIZATION"));
    section.getEmptyDescriptor().shouldBe(hidden);
    section.getTableContent().shouldHaveSize(LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT);
  }

  private void testAccessTile_Local(User testUser) {

    int hierarchySize = getHierarchySize(currentOwner);
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      AccessTileList list = accessTile.accessList(i);
      list.emptyDescriptor().shouldBe(hidden);

      if (i == 0) {
        list.elements().shouldHaveSize(2);
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));

        AccessTileListElement readOnly = list.element(0);
        readOnly.chevron().shouldBe(visible);
        readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
        readOnly.groupIcon().shouldBe(visible);
        readOnly.members().shouldBe(visible).shouldHave(text("Group"));

        AccessTileListElement writeOnly = list.element(1);
        writeOnly.chevron().shouldBe(visible);
        writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
        writeOnly.userIcon().shouldBe(visible);
        writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));
      }
      else {
        list.ownerName().shouldBe(hidden);
        list.elements().shouldHaveSize(0);
      }
    }
  }

  private void testPolicyTile_Local(List<Policy> localPolicies) {
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    policyTile.policyLists().shouldHaveSize(1);

    // scroll to the policy tile
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    PolicyTileList list = policyTile.policyList(0);
    list.emptyDescriptor().shouldBe(hidden);

    list.rows().shouldHaveSize(4); // 3 rows plus header
    list.ownerName().shouldBe(visible).shouldHave(text("Local"));

    assertPolicyHeader(list);

    PolicyTileListElement policyElement1 = list.row(1);
    Policy actualPolicy1 = localPolicies.get(0);
    assertPolicy(policyElement1, actualPolicy1);
    PolicyTileListElement policyElement2 = list.row(2);
    Policy actualPolicy2 = localPolicies.get(1);
    assertPolicy(policyElement2, actualPolicy2);
    PolicyTileListElement policyElement3 = list.row(3);
    Policy actualPolicy3 = localPolicies.get(2);
    assertPolicy(policyElement3, actualPolicy3);

    list.buildHeaderColumn().nxAnchor().click();
    list.buildHeaderColumn().sort(NX_UP_SELECTED).shouldBe(visible);
    list.buildHeaderColumn().sort(NX_DOWN_SELECTED).shouldBe(visible);
    list.threatLegendHeaderColumn().sort(NX_DOWN_SELECTED).shouldNotBe(visible);
    list.threatLegendHeaderColumn().sort(NX_UP_SELECTED).shouldNotBe(visible);

    policyElement1 = list.row(1);
    policyElement2 = list.row(2);
    policyElement3 = list.row(3);
    assertPolicy(policyElement1, actualPolicy1);
    assertPolicy(policyElement2, actualPolicy2);
    assertPolicy(policyElement3, actualPolicy3);

    list.buildHeaderColumn().nxAnchor().click();
    list.buildHeaderColumn().sort(NX_UP_SELECTED).shouldBe(visible);
    list.buildHeaderColumn().sort(NX_DOWN_SELECTED).shouldBe(visible);

    policyElement1 = list.row(1);
    policyElement2 = list.row(2);
    policyElement3 = list.row(3);
    assertPolicy(policyElement1, actualPolicy3);
    assertPolicy(policyElement2, actualPolicy2);
    assertPolicy(policyElement3, actualPolicy1);
  }

  @Test
  public void testTiles_Inherited() {
    List<List<Label>> inheritedLabels = new ArrayList<>();
    List<List<LicenseThreatGroup>> inheritedLTGs = new ArrayList<>();
    List<List<Policy>> inheritedPolicies = new ArrayList<>();

    User testUser = tempEntity.newUser("testUser", "Inherited Test", "User", "testuser@sonatype.com");
    Role readRole = tempEntity.newRole("Read Only", false, Permission.READ);
    Role writeRole = tempEntity.newRole("Write Only", false, Permission.WRITE);

    List<Owner> parentOwners = new ArrayList<>();

    for (Owner owner : new OwnerDAO().walkHierarchy(currentOwner.getParentOwnerId())) {
      List<LicenseThreatGroup> ltgs = new ArrayList<>();
      List<Label> labels = new ArrayList<>();
      List<Policy> policies = new ArrayList<>();
      parentOwners.add(owner);

      if (owner.getId() != null) {
        labels.add(tempEntity.newLabel(owner.getId(), "Inherited " + owner.getName() + " Label 1", Color.dark_purple));
        labels.add(tempEntity.newLabel(owner.getId(), "Inherited " + owner.getName() + " Label 2", "With Subtitle",
            Color.dark_blue));

        inheritedLabels.add(labels);

        ltgs.add(tempEntity.newLicenseThreatGroup(owner.getId(), "Temp License 1 - " + owner.getName(), 9));
        ltgs.add(tempEntity.newLicenseThreatGroup(owner.getId(), "Temp License 2 - " + owner.getName(), 1));

        inheritedLTGs.add(ltgs);

        tempEntity.newMembershipMapping(owner.getId(), writeRole.getId(), testUser.getUsername());
        tempEntity.newMembershipMapping(owner.getId(), readRole.getId(), "Group", MemberType.GROUP);

        policies.add(tempEntity.newPolicy(owner.getId(), "Policy 1 " + owner.getName(), 10, Action.ID_FAIL,
            Stage.ID_BUILD, null));
        policies.add(tempEntity.newPolicy(owner.getId(), "Policy 2 " + owner.getName(), 5, Action.ID_WARN,
            Stage.ID_BUILD, null));

        inheritedPolicies.add(policies);
      }
    }

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    SidebarNavigation.closeNavigationSidebar();
    testLabelTile_Inherited(inheritedLabels, parentOwners);
    // eyesWatcher.eyesCheck(
    //  String.format("label tile with inherited data for %s %s", currentOwner.getType(),
    //  currentOwner.getName()));
    testLTGTile_Inherited(inheritedLTGs, parentOwners);
    eyesWatcher.eyesCheck(
        String.format("license threat group tile with inherited data for %s %s", currentOwner.getType(),
            currentOwner.getName()));
    testAccessTile_Inherited(testUser, parentOwners);
    eyesWatcher.eyesCheck(
        String.format("access tile with inherited data for %s %s", currentOwner.getType(), currentOwner.getName()));
    testPolicyTile_Inherited(inheritedPolicies, parentOwners);
    eyesWatcher.eyesCheck(
        String.format("policy tile with inherited data for %s %s", currentOwner.getType(), currentOwner.getName()));
  }

  private void testLabelTile_Inherited(List<List<Label>> inheritedLabels, List<Owner> parentOwners) {
    final int hierarchySize = parentOwners.size() + 1;
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    assertThat(inheritedLabels).hasSameSizeAs(parentOwners);
    labelTile.labelLists().shouldHaveSize(hierarchySize);

    // scroll to the labels tile
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      NxList list = labelTile.labelList(i);

      if (i == 0) {
        labelTile.labelListSubheader(0).shouldBe(visible).shouldHave(text("Local"));

        list.emptyDescriptor().shouldBe(visible);
        list.elements().shouldBe(empty);
      }
      else {
        final int expectedLabelCount = inheritedLabels.get(i - 1).size();
        list.elements().shouldHaveSize(expectedLabelCount);
        labelTile.labelListSubheader(i).shouldBe(visible)
            .shouldHave(LabelTile.inheritedText(parentOwners.get(i - 1).getName()));

        for (int j = 0; j < expectedLabelCount; j++) {
          NxList.NxListItem actualLabel = list.element(j);
          Label expectedLabel = inheritedLabels.get(i - 1).get(j);

          if (expectedLabel.getDescription() == null) {
            actualLabel.description().shouldNot(exist);
          }
          else {
            actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
          }

          String nxColorClass = NxColor.getNxColorFromColor(expectedLabel.getColor()).toNxClass();

          actualLabel.icon().shouldBe(visible).shouldHave(cssClass(nxColorClass));
          actualLabel.name().shouldBe(visible).shouldHave(text(expectedLabel.getLabel()));
          actualLabel.chevron().shouldNot(exist);
        }
      }
    }
  }

  @Test
  public void testDeleteOwner() {
    List<Owner> parentOwners = new ArrayList<>();

    for (Owner owner : new OwnerDAO().walkHierarchy(currentOwner.getParentOwnerId())) {
      parentOwners.add(owner);
    }

    String ownerName = currentOwner.getName();

    // Test cancel button
    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().shouldBe(visible).click();

    NxDeleteModal deleteModal = new NxDeleteModal("#owner-delete-modal");
    deleteModal.shouldBe(visible);
    deleteModal.closeButton().click();

    deleteModal.shouldBe(hidden);

    currentOwner = new OwnerDAO().getById(currentOwner.getId());

    OwnerSummaryPage.summaryTile().name().shouldBe(visible).shouldHave(text(currentOwner.getName()));
    assertThat(currentOwner).isNotNull();

    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().shouldBe(visible).shouldHave(text(ownerName)).click();

    deleteModal.shouldBe(visible);
    deleteModal.header().shouldHave(text(currentOwner.getType().toString()));
    deleteModal.alertContent().shouldHave(text(ownerName));

    deleteModal.shouldBe(visible);
    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.shouldBe(hidden);

    currentOwner = new OwnerDAO().getById(currentOwner.getId());

    assertThat(currentOwner).isNull();

    if (Organization.ROOT_ORGANIZATION_ID.equals(parentOwners.get(parentOwners.size() - 1).getId())) {
      OwnerSummaryPage.summaryTile().name().shouldBe(visible).shouldNotHave(text(ownerName));
    }
    else {
      OwnerSummaryPage.summaryTile().name().shouldBe(visible)
          .shouldNotHave(text(parentOwners.get(parentOwners.size() - 1).getName()));
    }
  }

  private void testLTGTile_Inherited(List<List<LicenseThreatGroup>> inheritedLTGs, List<Owner> parentOwners) {
    LicenseThreatGroupSummaryTile ltgTile = OwnerSummaryPage.licenseThreatGroupSummaryTile();

    // scroll to the ltgs
    OwnerSummaryPage.summaryTile().ltgsButton().shouldBe(visible).click();

    final int hierarchyCount = ltgTile.getAllApplicableLicenseThreatGroupSection().size();
    for (int i = 0; i < hierarchyCount; i++) {
      ApplicableLicenseThreatGroupSection section = ltgTile.getApplicableLicenseThreatGroupSection(i);
      ScrollUtil.scrollIntoView(section.getTitle());

      boolean notAnApp = !OwnerType.APPLICATION.equals(currentOwner.getType());

      if (i == 0) {
        if (notAnApp) {
          section.getTitle().shouldBe(visible).shouldHave(text("Local"));
          SelenideElement emptyDescriptor = section.getEmptyDescriptor();
          if (section.getEmptyDescriptor() != null ) {
            emptyDescriptor.should(exist).shouldBe(visible);
            section.getTableContent().shouldHaveSize(1);
          }
        }
      }
      else {
        int element = notAnApp ? i - 1 : i;
        int expectedTestLTGSize = Organization.ROOT_ORGANIZATION_ID.equals(parentOwners.get(element).getId())
            ? LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT : 0;
        int expectedLTGCount = inheritedLTGs.get(element).size() + expectedTestLTGSize;
        section.getTableContent().shouldHaveSize(expectedLTGCount);
        section.getTitle().shouldBe(visible)
            .shouldHave(LicenseThreatGroupSummaryTile.inheritedText(parentOwners.get(element).getName()));

        for (int j = 0; j < expectedLTGCount; j++) {
          LicenseThreatGroupElement actualLTG = section.getLicenseThreatGroupElement(section.getLTG(j));

          if (inheritedLTGs.size() < i) {
            LicenseThreatGroup expectedLTG = inheritedLTGs.get(element).get(j);
            actualLTG.getName().shouldBe(visible).shouldHave(text(expectedLTG.getName()));
            actualLTG.getThreatLevelValue().shouldBe(visible)
                .shouldHave(LicenseThreatGroupElement.threatLevel(expectedLTG.getThreatLevel()));
          }

          actualLTG.getChevron().shouldNot(exist);
        }
      }
    }
  }

  private void testAccessTile_Inherited(User testUser, List<Owner> parentOwners) {

    int hierarchySize = getHierarchySize(currentOwner);
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.accessLists().shouldHaveSize(hierarchySize);

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      AccessTileList list = accessTile.accessList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.emptyDescriptor().should(exist);
      }
      else {
        list.emptyDescriptor().shouldBe(hidden);
        list.ownerName().shouldBe(visible).shouldHave(AccessTile.inheritedText(parentOwners.get(i - 1).getName()));
        list.elements().shouldHaveSize(2);

        AccessTileListElement readOnly = list.element(0);
        readOnly.chevron().shouldBe(hidden);
        readOnly.role().shouldBe(visible).shouldHave(text("Read Only"));
        readOnly.groupIcon().shouldBe(visible);
        readOnly.members().shouldBe(visible).shouldHave(text("Group"));

        AccessTileListElement writeOnly = list.element(1);
        writeOnly.chevron().shouldBe(hidden);
        writeOnly.role().shouldBe(visible).shouldHave(text("Write Only"));
        writeOnly.userIcon().shouldBe(visible);
        writeOnly.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));
      }
    }
  }

  private void testPolicyTile_Inherited(List<List<Policy>> inheritedPolicies, List<Owner> parentOwners) {
    int hierarchySize = parentOwners.size() + 1;
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    assertThat(inheritedPolicies).hasSameSizeAs(parentOwners);
    policyTile.policyLists().shouldHaveSize(hierarchySize);

    // scroll to the policy tile
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    for (int i = 0; i < hierarchySize; i++) {
      PolicyTileList list = policyTile.policyList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
        list.localEmptyDescriptor().should(exist);
      }
      else {
        list.emptyDescriptor().shouldBe(hidden);
        list.ownerName().shouldBe(visible).shouldHave(PolicyTile.inheritedText(parentOwners.get(i - 1).getName()));
        list.rows().shouldHaveSize(3); // 2 rows plus header

        assertPolicyHeader(list);

        PolicyTileListElement policyElement1 = list.row(1);
        Policy actualPolicy1 = inheritedPolicies.get(i - 1).get(0);
        assertPolicy(policyElement1, actualPolicy1);
        PolicyTileListElement policyElement2 = list.row(2);
        Policy actualPolicy2 = inheritedPolicies.get(i - 1).get(1);
        assertPolicy(policyElement2, actualPolicy2);

        list.nameHeaderColumn().nxAnchor().click();
        list.nameHeaderColumn().sort(NX_UP_SELECTED).shouldBe(visible);
        list.nameHeaderColumn().sort(NX_DOWN_SELECTED).shouldBe(visible);
        list.threatLegendHeaderColumn().sort(NX_UP_SELECTED).shouldNot(exist);
        list.threatLegendHeaderColumn().sort(NX_DOWN_SELECTED).shouldNot(exist);

        assertPolicy(policyElement1, actualPolicy1);
        assertPolicy(policyElement2, actualPolicy2);

        list.nameHeaderColumn().nxAnchor().click();
        list.nameHeaderColumn().sort(NX_UP_SELECTED).shouldBe(visible);
        list.nameHeaderColumn().sort(NX_DOWN_SELECTED).shouldBe(visible);

        assertPolicy(policyElement1, actualPolicy2);
        assertPolicy(policyElement2, actualPolicy1);
      }
    }
  }

  private void assertPolicyHeader(PolicyTileList list) {
    HeaderColumn column = list.nxSelectedHeaderColumn();
    column.sort(NX_DOWN_SELECTED).shouldBe(visible);
    column.sort(NX_UP_SELECTED).shouldBe(visible);
  }

  private void assertPolicy(PolicyTileListElement policy, Policy actualPolicy) {
    String actionTypeId = actualPolicy.getActions().get(Stage.ID_BUILD);
    if (actionTypeId == null) {
      actionTypeId = "—";
    }
    policy.chevron().shouldBe(visible);
    policy.threatLegend().shouldBe(visible).shouldHave(text("" + actualPolicy.getThreatLevel()));
    policy.name().shouldBe(visible).shouldHave(text(actualPolicy.getName()));
    policy.proxy().shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policy.develop().shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policy.build().shouldBe(visible).shouldHave(text(actionTypeId));
    policy.stageRelease().shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policy.release().shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policy.operate().shouldBe(visible).shouldHave(PolicyTile.noActionText());

    if (actionTypeId.equals(Action.ID_WARN)) {
      policy.build().find("span").shouldHave(PolicyTileListElement.WARN).shouldHave(text("warn"))
          .shouldNotHave(PolicyTileListElement.FAIL);
    }
    else if (actionTypeId.equals(Action.ID_FAIL)) {
      policy.build().find("span").shouldHave(PolicyTileListElement.FAIL).shouldHave(text("fail"))
          .shouldNotHave(PolicyTileListElement.WARN);
    }
  }

  @Test
  public void testPolicyTile_Foundation_Firewall() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    Policy policy = tempEntity.newPolicy(currentOwner.getId(), "Policy 1", 10, null, null, null);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    assertPolicyTile_Foundation(policy, false);

    eyesWatcher.eyesCheck(
        String.format("Policy tile foundation firewall for %s %s", currentOwner.getType(), currentOwner.getName()));
  }

  @Test
  public void testPolicyTile_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    Policy policy = tempEntity.newPolicy(currentOwner.getId(), "Policy 1", 10, null, null, null);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    assertPolicyTile_Foundation(policy, true);
    eyesWatcher.eyesCheck();
  }

  private void assertPolicyTile_Foundation(Policy policy, boolean proxyActionReadOnly) {
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    PolicyTileList list = policyTile.policyList(0);

    PolicyTileListElement policyElement = list.row(1);
    policyElement.threatLegend().shouldBe(visible).shouldHave(text("" + policy.getThreatLevel()));
    policyElement.name().shouldBe(visible).shouldHave(text(policy.getName()));

    HeaderColumn proxy = list.header(2);
    proxy.nxAnchorHeader().shouldHave(text("PROXY"));

    if (proxyActionReadOnly) {
      proxy.root.shouldHave(PolicyTileList.CELL_DISABLED);
      policyElement.column(3).shouldBe(visible).shouldHave(PolicyTile.noActionText())
          .shouldHave(PolicyTileList.CELL_DISABLED);
    }
    else {
      proxy.root.shouldNotHave(PolicyTileList.CELL_DISABLED);
      policyElement.column(3).shouldBe(visible).shouldHave(PolicyTile.noActionText())
          .shouldNotHave(PolicyTileList.CELL_DISABLED);
    }

    // check a few of the other stages (develop, and operate) and make sure they're
    // disabled.
    HeaderColumn develop = list.header(3);
    develop.nxAnchorHeader().shouldHave(text("DEVELOP"));
    develop.root.shouldHave(PolicyTileList.CELL_DISABLED);
    policyElement.column(4).shouldBe(visible).shouldHave(PolicyTile.noActionText())
        .shouldHave(PolicyTileList.CELL_DISABLED);
    HeaderColumn operate = list.header(8);
    // Uncomment when fixing CLM-18691
    // operate.anchor().shouldHave(text("OPERATE"));
    operate.root.shouldHave(PolicyTileList.CELL_DISABLED);
    policyElement.column(9).shouldBe(visible).shouldHave(PolicyTile.noActionText())
        .shouldHave(PolicyTileList.CELL_DISABLED);

    policyElement.chevron().shouldBe(visible);
  }

  @Test
  public void testPolicyTile_LimitedStageLicensing() {
    List<Policy> localPolicies = new ArrayList<>();
    localPolicies
        .add(tempEntity.newPolicy(currentOwner.getId(), "Release", 10, Action.ID_FAIL, Stage.ID_RELEASE, null));

    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    PolicyTile policyTile = OwnerSummaryPage.policyTile();

    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible).click();

    PolicyTileList list = policyTile.policyList(0);

    PolicyTileListElement policyElement = list.row(1);
    Policy actualPolicy = localPolicies.get(0);
    HeaderColumn proxy = list.header(2);

    // should only have proxy and release
    proxy.nxAnchorHeader().shouldHave(text("PROXY"));

    HeaderColumn release = list.header(3);
    release.nxAnchorHeader().shouldHave(text("RELEASE"));
    list.header(4).name().shouldNot(exist);

    policyElement.chevron().shouldBe(visible);
    policyElement.threatLegend().shouldBe(visible).shouldHave(text("" + actualPolicy.getThreatLevel()));
    policyElement.name().shouldBe(visible).shouldHave(text(actualPolicy.getName()));
    policyElement.column(3).shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policyElement.column(4).shouldBe(visible).shouldHave(text(actualPolicy.getActions().get(Stage.ID_RELEASE)));
    policyElement.chevronColumn(5).shouldHave(PolicyTileListElement.CHEVRON);
  }

  protected int getHierarchySize(Owner owner) {
    int hierarchySize = 0;
    Iterator<Owner> iterator = new OwnerDAO().walkHierarchy(owner).iterator();

    for (; iterator.hasNext(); ++hierarchySize) {
      iterator.next();
    }
    return hierarchySize;
  }

  protected abstract void testReportLinks();

  protected abstract void testApplicationCategoryTile();
}
