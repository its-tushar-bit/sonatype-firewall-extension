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
import com.sonatype.clm.testing.functional.elements.AccessTile.InheritedAccess;
import com.sonatype.clm.testing.functional.elements.AccessTile.InheritedAccessList;
import com.sonatype.clm.testing.functional.elements.AccessTileList;
import com.sonatype.clm.testing.functional.elements.AccessTileList.AccessTileListElement;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.ArtifactoryRepositoryTile;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.GreedyTable.HeaderColumn;
import com.sonatype.clm.testing.functional.elements.InnerSourceRepositoryTile;
import com.sonatype.clm.testing.functional.elements.LabelTile;
import com.sonatype.clm.testing.functional.elements.LabelTile.InheritedLabel;
import com.sonatype.clm.testing.functional.elements.LabelTile.InheritedLabelsList;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile.ApplicableLicenseThreatGroupSection;
import com.sonatype.clm.testing.functional.elements.LicenseThreatGroupSummaryTile.LicenseThreatGroupElement;
import com.sonatype.clm.testing.functional.elements.NavPills;
import com.sonatype.clm.testing.functional.elements.NxAlert;
import com.sonatype.clm.testing.functional.elements.NxBreadcrumb;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.NxList;
import com.sonatype.clm.testing.functional.elements.OwnerEditorDialog;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.PolicyTileList;
import com.sonatype.clm.testing.functional.elements.PolicyTileList.PolicyTileListElement;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PublicDataSourcesEditorPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.NxColor;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
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
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
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
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSummaryViewTest
    extends AbstractFunctionalTest
{
  private OwnerDAO ownerDAO;

  private ApplicationDAO applicationDAO;

  private OrganizationDAO organizationDAO;

  private RoleDAO roleDAO;

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

  @Before
  public void before() {
    ownerDAO = lookup(OwnerDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    roleDAO = lookup(RoleDAO.class);
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
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

    NxAlert error = OwnerSummaryPage.loadErrorMessage();
    error.shouldBe(visible);
    if (currentOwner.getType().equals(OwnerType.APPLICATION)) {
      error.shouldHave(text("An error occurred loading data. Could not find an "
          + currentOwner.getType().toString() + " with public ID fakeid."));
    }
    else {
      error.shouldHave(text("An error occurred loading data. Organization with ID fakeid does not exist.\n" +
          "Retry"));
    }
    error.button().shouldBe(visible, enabled);
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
  public void testNavigationPills() {
    NavPills navPills = OwnerSummaryPage.navigationPills();

    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      navPills.pills().shouldHave(size(11));
    }
    else if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      navPills.pills().shouldHave(size(12));
    }

    navPills.appCategory().click();
    OwnerSummaryPage.categoryTile().shouldBe(visible);

    navPills.policy().click();
    OwnerSummaryPage.policyTile().shouldBe(visible);

    navPills.legacyViolations().click();
    OwnerSummaryPage.legacyViolations().shouldBe(visible);

    navPills.continuousMonitoring().click();
    OwnerSummaryPage.monitoredStage().shouldBe(visible);

    navPills.proprietaryComponents().click();
    OwnerSummaryPage.proprietaryComponentMatchers().shouldBe(visible);

    navPills.labels().click();
    OwnerSummaryPage.labelTile().shouldBe(visible);

    navPills.ltg().click();
    OwnerSummaryPage.licenseThreatGroupSummaryTile().shouldBe(visible);

    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      navPills.retention().shouldNot(exist);
    }
    else if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      navPills.retention().click();
      OwnerSummaryPage.dataRetentionTile().shouldBe(visible);
    }

    navPills.sourceControl().click();
    OwnerSummaryPage.sourceControlTile().shouldBe(visible);

    navPills.innerSource().click();
    OwnerSummaryPage.innerSourceRepositoryTile().shouldBe(visible);

    navPills.autoWaivers().click();
    OwnerSummaryPage.autoWaiversTile().shouldBe(visible);

    navPills.publicDataSources().shouldNotBe(visible);
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
    testAccessTile_no_local_access();
    testPolicyTile_no_policies();
  }

  @Test
  public void testInnerSourceRepositoryTile_NotConfigured() {
    setCurrentOwnerRepositoryConnectionStatus(currentOwner, true);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible);
    InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
    innerSourceRepositoryTile.should(exist);
    ScrollUtil.scrollIntoViewInstantly(innerSourceRepositoryTile.getElement());
    innerSourceRepositoryTile.listTitle().should(exist);
    ElementsCollection rows = innerSourceRepositoryTile.rows();
    rows.shouldHave(size(1));
    rows.get(0).shouldBe(text("No InnerSource repository connections are configured"));
    innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));
  }

  @Test
  public void testInnerSourceRepositoryTile_Disabled() {
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible);
    InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
    innerSourceRepositoryTile.should(exist);
    ScrollUtil.scrollIntoViewInstantly(innerSourceRepositoryTile.getElement());

    innerSourceRepositoryTile.listTitle().should(exist);
    ElementsCollection rows = innerSourceRepositoryTile.rows();
    rows.shouldHave(size(1));
    rows.get(0).shouldBe(text("InnerSource repository connections are disabled"));
    innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));
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
      OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible);
      InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
      innerSourceRepositoryTile.should(exist);
      ScrollUtil.scrollIntoViewInstantly(innerSourceRepositoryTile.getElement());

      innerSourceRepositoryTile.listTitle().shouldHave(text("Local"));
      ElementsCollection rows = innerSourceRepositoryTile.rows();
      rows.shouldHave(size(2));
      rows.get(0).shouldBe(text(repositoryConnection1.getBaseUrl() + "\n" + repositoryConnection1.getFormat()));
      rows.get(1).shouldBe(text(repositoryConnection2.getBaseUrl() + "\n" + repositoryConnection2.getFormat()));
      innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));
    }
    finally {
      setCurrentOwnerRepositoryConnectionStatus(currentOwner, null);
    }
  }

  private void setCurrentOwnerRepositoryConnectionStatus(Owner currentOwner, Boolean repoConnectionsEnabled) {
    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      Application app = (Application) currentOwner;
      app.setRepositoryConnectionEnabled(repoConnectionsEnabled);
      applicationDAO.update(app);
    }
    else if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      Organization org = (Organization) currentOwner;
      org.setRepositoryConnectionEnabled(repoConnectionsEnabled);
      organizationDAO.update(org);
    }
  }

  @Test
  public void testInnerSourceRepositoryTile_Configured_Inherited() {
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
      OwnerSummaryPage.summaryTile().innerSourceRepositoryButton().shouldBe(visible);
      InnerSourceRepositoryTile innerSourceRepositoryTile = OwnerSummaryPage.innerSourceRepositoryTile();
      innerSourceRepositoryTile.should(exist);
      ScrollUtil.scrollIntoViewInstantly(innerSourceRepositoryTile.getElement());

      innerSourceRepositoryTile.listTitle().shouldHave(text("Inherited from " + parentOwner.getName()));
      ElementsCollection rows = innerSourceRepositoryTile.rows();
      rows.shouldHave(size(2));
      rows.get(0).shouldBe(text(repositoryConnection1.getBaseUrl() + "\n" + repositoryConnection1.getFormat()));
      rows.get(1).shouldBe(text(repositoryConnection2.getBaseUrl() + "\n" + repositoryConnection2.getFormat()));
      innerSourceRepositoryTile.editButton().shouldHave(text("Edit"));
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
  }

  @Test
  public void testArtifactoryRepositoryTile_NotConfigured() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    setCurrentOwnerArtifactoryConnectionStatus(currentOwner, true);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible);
    ArtifactoryRepositoryTile artifactoryRepositoryTile = OwnerSummaryPage.artifactoryRepositoryTile();
    artifactoryRepositoryTile.should(exist);
    ScrollUtil.scrollIntoViewInstantly(artifactoryRepositoryTile.getElement());
    artifactoryRepositoryTile.listTitle().should(exist);
    ElementsCollection rows = artifactoryRepositoryTile.rows();
    rows.shouldHave(size(1));
    rows.get(0).shouldBe(text("No Artifactory repository connection is configured"));
    artifactoryRepositoryTile.editButton().shouldHave(text("Edit"));
  }

  @Test
  public void testArtifactoryRepositoryTile_Disabled() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible);
    ArtifactoryRepositoryTile artifactoryRepositoryTile = OwnerSummaryPage.artifactoryRepositoryTile();
    artifactoryRepositoryTile.should(exist);
    ScrollUtil.scrollIntoViewInstantly(artifactoryRepositoryTile.getElement());
    artifactoryRepositoryTile.listTitle().should(exist);
    ElementsCollection rows = artifactoryRepositoryTile.rows();
    rows.shouldHave(size(1));
    rows.get(0).shouldBe(text("Artifactory repository connection is disabled"));
    artifactoryRepositoryTile.editButton().shouldHave(text("Edit"));
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
      OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible);
      ArtifactoryRepositoryTile artifactoryRepositoryTile = OwnerSummaryPage.artifactoryRepositoryTile();
      artifactoryRepositoryTile.should(exist);
      ScrollUtil.scrollIntoViewInstantly(artifactoryRepositoryTile.getElement());

      artifactoryRepositoryTile.listTitle().shouldHave(text("Local"));
      ElementsCollection rows = artifactoryRepositoryTile.rows();
      rows.shouldHave(size(1));
      rows.get(0).shouldBe(text(artifactoryConnection.getBaseUrl()));
      artifactoryRepositoryTile.editButton().shouldHave(text("Edit"));
    }
    finally {
      setCurrentOwnerArtifactoryConnectionStatus(currentOwner, null);
    }
  }

  private void setCurrentOwnerArtifactoryConnectionStatus(Owner currentOwner, Boolean artifactoryConnectionEnabled) {
    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      Application app = (Application) currentOwner;
      app.setArtifactoryConnectionEnabled(artifactoryConnectionEnabled);
      applicationDAO.update(app);
    }
    else if (OwnerType.ORGANIZATION.equals(currentOwner.getType())) {
      Organization org = (Organization) currentOwner;
      org.setArtifactoryConnectionEnabled(artifactoryConnectionEnabled);
      organizationDAO.update(org);
    }
  }

  @Test
  public void testArtifactoryRepositoryTile_Configured_Inherited() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    Organization parentOwner = organizationDAO.getById(currentOwner.getParentOwnerId());
    try {
      ArtifactoryConnection artifactoryConnection = tempEntity.newArtifactoryConnection(currentOwner.getParentOwnerId(),
          "http://some.base.url", null, null);
      parentOwner.setAllowArtifactoryConnectionOverride(false);
      parentOwner.setArtifactoryConnectionEnabled(true);
      organizationDAO.update(parentOwner);

      refresh();
      SidebarNavigation.closeNavigationSidebar();
      OwnerSummaryPage.summaryTile().artifactoryRepositoryButton().shouldBe(visible);
      ArtifactoryRepositoryTile artifactoryRepositoryTile = OwnerSummaryPage.artifactoryRepositoryTile();
      artifactoryRepositoryTile.should(exist);
      ScrollUtil.scrollIntoViewInstantly(artifactoryRepositoryTile.getElement());

      artifactoryRepositoryTile.listTitle().shouldHave(text("Inherited from " + parentOwner.getName()));
      ElementsCollection rows = artifactoryRepositoryTile.rows();
      rows.shouldHave(size(1));
      rows.get(0).shouldBe(text(artifactoryConnection.getBaseUrl()));
      artifactoryRepositoryTile.editButton().shouldHave(text("Edit"));
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
  }

  public void testLabelTile_no_labels() {
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    labelTile.subHeader().shouldBe(visible).shouldHave(LabelTile.subHeaderText(currentOwner.getName()));
    labelTile.newButton().shouldBe(visible, enabled);

    labelTile.labelLists().shouldHave(size(1));

    // scroll to the labels tile
    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(labelTile.getElement());
    NxList list = labelTile.labelList(0);
    labelTile.labelListSubheader(0).shouldBe(visible).shouldHave(text("Local"));
    list.emptyDescriptor().shouldBe(visible);
  }

  public void testAccessTile_no_local_access() {
    int hierarchySize = getHierarchySize(currentOwner);
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.subHeader().shouldBe(visible).shouldHave(AccessTile.subHeaderText(currentOwner.getName()));
    accessTile.addRoleButton().shouldBe(visible, enabled);
    accessTile.accessLists().shouldHave(size(1));

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(accessTile.getElement());

    for (int i = 0; i < hierarchySize; i++) {
      AccessTileList list = accessTile.accessList(i);

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local to " + currentOwner.getName()));
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
    policyTile.newButton().shouldBe(visible, enabled);
    policyTile.localEmptyDescriptor().shouldBe(visible);
    policyTile.localEmptyDescriptor().shouldHave(text("No local policies defined"));

    policyTile.policyLists().shouldHave(size(0));
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
    testLTGTile_Local(locaLTGs);
    testAccessTile_Local(testUser);
    testPolicyTile_Local(localPolicies);
  }

  private void testLabelTile_Local(List<Label> localLabels) {
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    labelTile.labelLists().shouldHave(size(1));

    // scroll to the labels tile
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(labelTile.getElement());

    NxList list = labelTile.labelList(0);
    labelTile.labelListSubheader(0).shouldBe(visible).shouldHave(text("Local"));
    list.emptyDescriptor().shouldNot(exist);
    list.elements().shouldHave(size(localLabels.size()));

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

  private void testLTGTile_Local(List<LicenseThreatGroup> localLTGs) {
    LicenseThreatGroupSummaryTile ltgTile = OwnerSummaryPage.licenseThreatGroupSummaryTile();
    ltgTile.getAllApplicableLicenseThreatGroupSection().shouldHave(size(2));

    ScrollUtil.scrollIntoViewInstantly(ltgTile.licenseThreatGroupsTable());

    if (OwnerType.APPLICATION.equals(currentOwner.getType())) {
      ltgTile.addLTGButton().shouldNot(exist);
    }
    else {
      ltgTile.addLTGButton().shouldBe(visible);
    }

    // scroll to the ltgs
    OwnerSummaryPage.summaryTile().ltgsButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(ltgTile.licenseThreatGroupsTable());

    ApplicableLicenseThreatGroupSection section = ltgTile.getApplicableLicenseThreatGroupSection(0);

    ScrollUtil.scrollIntoViewInstantly(section.getTitle());

    section.getTitle().shouldBe(visible).shouldHave(text("Local to " + currentOwner.getName()));
    section.getCollapsibleIcon().shouldNot(exist);
    section.getEmptyDescriptor().shouldNot(exist);
    section.getSectionContentRows().shouldHave(size(localLTGs.size()));

    for (int j = 0; j < localLTGs.size(); j++) {
      LicenseThreatGroupElement actualLTG = section.getLicenseThreatGroupElement(section.getLTG(j));
      LicenseThreatGroup expectedLTG = localLTGs.get(j);

      actualLTG.getName().shouldBe(visible).shouldHave(text(expectedLTG.getName()));

      String threatLevel = String.valueOf(expectedLTG.getThreatLevel());
      actualLTG.getThreatLevelValue().shouldBe(visible).shouldHave(text(threatLevel));
      actualLTG.getThreatLevelIndicator()
          .shouldBe(visible)
          .shouldHave(LicenseThreatGroupElement.threatLevel(expectedLTG.getThreatLevel()));
      actualLTG.getChevron().shouldBe(visible);
    }

    section = ltgTile.getApplicableLicenseThreatGroupSection(1);
    ScrollUtil.scrollIntoViewInstantly(section.getTitle());

    section.getTitle().shouldBe(visible).shouldHave(text("Inherited from Root Organization"));
    section.getEmptyDescriptor().shouldBe(hidden);
    section.getSectionContentRows()
        .shouldHave(size(LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT));
  }

  private void testAccessTile_Local(User testUser) {

    int hierarchySize = getHierarchySize(currentOwner);
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    accessTile.accessLists().shouldHave(size(1));

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(accessTile.getElement());

    for (int i = 0; i < hierarchySize; i++) {
      AccessTileList list = accessTile.accessList(i);
      list.emptyDescriptor().shouldBe(hidden);

      if (i == 0) {
        list.elements().shouldHave(size(2));
        list.ownerName().shouldBe(visible).shouldHave(text("Local to " + currentOwner.getName()));

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
        list.elements().shouldHave(size(0));
      }
    }
  }

  private void testPolicyTile_Local(List<Policy> localPolicies) {
    PolicyTile policyTile = OwnerSummaryPage.policyTile();

    // scroll to the policy tile
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(policyTile.getElement());

    if (policyTile.policyLists().size() == 3) { // for application test
      PolicyTileList inheritedPolicieslist = policyTile.policyList(1);
      inheritedPolicieslist.rows().shouldHave(size(2)); // +1 for header
      inheritedPolicieslist.ownerName().shouldHave(text("Inherited from"));
      inheritedPolicieslist.emptyDescriptor().shouldBe(visible).shouldHave(text("No Ye Ole "));

      PolicyTileList inheritedPolicieslist2 = policyTile.policyList(2);
      inheritedPolicieslist2.rows().shouldHave(size(2)); // +1 for header
      inheritedPolicieslist2.ownerName().shouldHave(text("Inherited from"));
      inheritedPolicieslist2.emptyDescriptor().shouldBe(visible).shouldHave(text("No Root"));
    }
    else { // for org test
      PolicyTileList inheritedPolicieslist = policyTile.policyList(1);
      inheritedPolicieslist.rows().shouldHave(size(2)); // +1 for header
      inheritedPolicieslist.ownerName().shouldHave(text("Inherited from"));
      inheritedPolicieslist.emptyDescriptor().shouldBe(visible).shouldHave(text("No Root"));
    }

    PolicyTileList policyTable = policyTile.policyTileTable();
    PolicyTileList localPolicieslist = policyTile.localPolicyList();
    localPolicieslist.emptyDescriptor().shouldBe(hidden);

    localPolicieslist.rows().shouldHave(size(4)); // 3 rows plus header
    localPolicieslist.ownerName().shouldBe(visible).shouldHave(text("Local"));

    PolicyTileListElement policyElement1 = localPolicieslist.row(1);
    Policy actualPolicy1 = localPolicies.get(0);
    assertPolicy(policyElement1, actualPolicy1);
    PolicyTileListElement policyElement2 = localPolicieslist.row(2);
    Policy actualPolicy2 = localPolicies.get(1);
    assertPolicy(policyElement2, actualPolicy2);
    PolicyTileListElement policyElement3 = localPolicieslist.row(3);
    Policy actualPolicy3 = localPolicies.get(2);
    assertPolicy(policyElement3, actualPolicy3);

    policyTable.buildHeaderColumn().nxAnchor().click();
    policyTable.buildHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldBe(visible);
    policyTable.buildHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldBe(visible);
    localPolicieslist.threatLegendHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldNotBe(visible);
    localPolicieslist.threatLegendHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldNotBe(visible);

    policyElement1 = localPolicieslist.row(1);
    policyElement2 = localPolicieslist.row(2);
    policyElement3 = localPolicieslist.row(3);
    assertPolicy(policyElement1, actualPolicy1);
    assertPolicy(policyElement2, actualPolicy2);
    assertPolicy(policyElement3, actualPolicy3);

    policyTable.buildHeaderColumn().nxAnchor().click();
    policyTable.buildHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldBe(visible);
    policyTable.buildHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldBe(visible);

    policyElement1 = localPolicieslist.row(1);
    policyElement2 = localPolicieslist.row(2);
    policyElement3 = localPolicieslist.row(3);
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

    for (Owner owner : ownerDAO.walkHierarchy(currentOwner.getParentOwnerId())) {
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
    testLTGTile_Inherited(inheritedLTGs, parentOwners);
    testAccessTile_Inherited(testUser, parentOwners);
    testPolicyTile_Inherited(inheritedPolicies, parentOwners);
  }

  private void testLabelTile_Inherited(List<List<Label>> inheritedLabels, List<Owner> parentOwners) {
    LabelTile labelTile = OwnerSummaryPage.labelTile();
    assertThat(inheritedLabels).hasSameSizeAs(parentOwners);
    labelTile.labelLists().shouldHave(size(1));
    labelTile.inheritedLabelsLists().shouldHave(size(parentOwners.size()));

    // scroll to the labels tile
    OwnerSummaryPage.summaryTile().labelsButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(labelTile.getElement());

    NxList list = labelTile.labelList(0);
    labelTile.labelListSubheader(0).shouldBe(visible).shouldHave(text("Local to " + currentOwner.getName()));
    list.emptyDescriptor().shouldBe(visible);
    list.elements().shouldBe(CollectionCondition.empty);

    for (int i = 0; i < parentOwners.size(); i++) {
      String ownerId = parentOwners.get(i).getId();
      labelTile.labelListSubheader(i + 1)
          .shouldBe(visible)
          .shouldHave(LabelTile.inheritedText(parentOwners.get(i).getName()));
      InheritedLabelsList inheritedLabelList = labelTile.inheritedLabelsList(ownerId);
      inheritedLabelList.should(exist).shouldBe(visible);
      labelTile.labelListSubheader(i + 1).shouldBe(visible).click();
      inheritedLabelList.shouldNotBe(visible);
      labelTile.labelListSubheader(i + 1).shouldBe(visible).click();
      inheritedLabelList.should(exist).shouldBe(visible);
      int expectedLabelCount = inheritedLabels.get(i).size();
      inheritedLabelList.elements().shouldHave(size(expectedLabelCount));

      for (int j = 0; j < expectedLabelCount; j++) {
        InheritedLabel actualLabel = inheritedLabelList.element(j);
        Label expectedLabel = inheritedLabels.get(i).get(j);

        if (expectedLabel.getDescription() == null) {
          actualLabel.description().shouldBe(empty).shouldBe(visible);
        }
        else {
          actualLabel.description().shouldBe(visible).shouldHave(text(expectedLabel.getDescription()));
        }

        String nxColorClass = NxColor.getNxColorFromColor(expectedLabel.getColor()).toNxClass();

        actualLabel.icon().shouldBe(visible).shouldHave(cssClass(nxColorClass));
        actualLabel.label().shouldBe(visible).shouldHave(text(expectedLabel.getLabel()));
        actualLabel.getElement().$(".nx-chevron.nx-icon").shouldNot(exist);
      }
    }
  }

  @Test
  public void testDeleteOwner() {
    List<Owner> parentOwners = new ArrayList<>();

    for (Owner owner : ownerDAO.walkHierarchy(currentOwner.getParentOwnerId())) {
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

    currentOwner = ownerDAO.getById(currentOwner.getId());

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

    currentOwner = ownerDAO.getById(currentOwner.getId());

    assertThat(currentOwner).isNull();

    if (Organization.ROOT_ORGANIZATION_ID.equals(parentOwners.get(parentOwners.size() - 1).getId())) {
      OwnerSummaryPage.summaryTile().name().shouldBe(visible).shouldNotHave(text(ownerName));
    }
    else {
      OwnerSummaryPage.summaryTile()
          .name()
          .shouldBe(visible)
          .shouldNotHave(text(parentOwners.get(parentOwners.size() - 1).getName()));
    }
  }

  @Test
  public void testBreadcrumb() {
    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    breadcrumb.current().text().equals(currentOwner.getName());

    // test navigation
    breadcrumb.links().first().click();
    waitUntilUrl(OwnerSummaryPage.urlToRootOrg());

    OwnerSummaryPage.summaryTile().name().shouldHave(text("Root Organization"));
  }

  private void testLTGTile_Inherited(List<List<LicenseThreatGroup>> inheritedLTGs, List<Owner> parentOwners) {
    LicenseThreatGroupSummaryTile ltgTile = OwnerSummaryPage.licenseThreatGroupSummaryTile();

    // scroll to the ltgs
    OwnerSummaryPage.summaryTile().ltgsButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(ltgTile.licenseThreatGroupsTable());

    final int hierarchyCount = ltgTile.getAllApplicableLicenseThreatGroupSection().size();
    for (int i = 0; i < hierarchyCount; i++) {
      ApplicableLicenseThreatGroupSection section = ltgTile.getApplicableLicenseThreatGroupSection(i);

      if (i == 0) {
        // If local, should show local title and should not show expand icon
        if (!OwnerType.APPLICATION.equals(currentOwner.getType())) {
          section.getTitle().shouldBe(visible).shouldHave(text("Local to " + currentOwner.getName()));
          section.getCollapsibleIcon().shouldNot(exist);
          SelenideElement emptyDescriptor = section.getEmptyDescriptor();
          if (section.getEmptyDescriptor() != null) {
            emptyDescriptor.should(exist).shouldBe(visible);
            section.getEmptyRows().shouldHave(size(1));
          }
        }
      }
      else {
        // If inherited, should show inherited text and expand/collapse icon
        int element = i - 1;
        int expectedTestLTGSize = Organization.ROOT_ORGANIZATION_ID.equals(parentOwners.get(element).getId())
            ? LicenseThreatGroupDataHelper.TEST_LICENSE_THREAT_GROUP_COUNT
            : 0;
        int expectedLTGCount = inheritedLTGs.get(element).size() + expectedTestLTGSize;
        section.getSectionContentRows().shouldHave(size(expectedLTGCount));
        section.getTitle()
            .shouldBe(visible)
            .shouldHave(LicenseThreatGroupSummaryTile.inheritedText(parentOwners.get(element).getName()));
        section.getCollapsibleIcon().shouldBe(visible);

        // Test expand/collapse
        SelenideElement firstInheritedLTG = section.getLTG(0);
        firstInheritedLTG.shouldBe(visible);
        section.getCollapsibleIcon().click();
        firstInheritedLTG.shouldNotBe(visible);
        section.getCollapsibleIcon().click();

        for (int j = 0; j < expectedLTGCount; j++) {
          LicenseThreatGroupElement actualLTG = section.getLicenseThreatGroupElement(section.getLTG(j));

          if (inheritedLTGs.size() < i) {
            LicenseThreatGroup expectedLTG = inheritedLTGs.get(i - 1).get(j);
            actualLTG.getName().shouldBe(visible).shouldHave(text(expectedLTG.getName()));
            actualLTG.getThreatLevelValue()
                .shouldBe(visible)
                .shouldHave(LicenseThreatGroupElement.threatLevel(expectedLTG.getThreatLevel()));
          }

          actualLTG.getChevron().shouldNot(exist);
        }
      }
    }
  }

  private void testAccessTile_Inherited(User testUser, List<Owner> parentOwners) {
    AccessTile accessTile = OwnerSummaryPage.accessTile();
    int hierarchySize = getHierarchySize(currentOwner);
    accessTile.accessLists().shouldHave(size(hierarchySize));
    accessTile.inheritedAccessLists().shouldHave(size(parentOwners.size()));

    // scroll to the access tile
    OwnerSummaryPage.summaryTile().accessButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(accessTile.getElement());

    AccessTileList list = accessTile.accessList(0);
    list.ownerName().shouldBe(visible).shouldHave(text("Local to " + currentOwner.getName()));
    list.emptyDescriptor().shouldBe(exist);

    for (int i = 0; i < parentOwners.size(); i++) {
      Owner parent = parentOwners.get(i);
      InheritedAccessList inheritedAccessList = accessTile.inheritedAccessList(parent.getId());

      // Expanded by default
      accessTile.accessListSubheader(i)
          .shouldBe(visible)
          .shouldHave(AccessTile.inheritedText(parentOwners.get(i).getName()));
      inheritedAccessList.should(exist).shouldBe(visible);
      // Collapse
      accessTile.accessListSubheader(i).shouldBe(visible).click();
      inheritedAccessList.shouldNotBe(visible);
      // Expanded again
      accessTile.accessListSubheader(i).shouldBe(visible).click();

      inheritedAccessList.elements().shouldHave(size(2));

      InheritedAccess readOnly = inheritedAccessList.element(0);
      readOnly.label().shouldBe(visible).shouldHave(text("Read Only"));

      AccessTileListElement descriptionRead = readOnly.description();
      descriptionRead.chevron().shouldBe(hidden);
      descriptionRead.groupIcon().shouldBe(visible);
      descriptionRead.members().shouldBe(visible).shouldHave(text("Group"));

      InheritedAccess writeOnly = inheritedAccessList.element(1);
      writeOnly.label().shouldBe(visible).shouldHave(text("Write Only"));

      AccessTileListElement descriptionWrite = writeOnly.description();
      descriptionWrite.chevron().shouldBe(hidden);
      descriptionWrite.userIcon().shouldBe(visible);
      descriptionWrite.members().shouldBe(visible).shouldHave(text(testUser.calculateDisplayName()));
    }
  }

  private void testPolicyTile_Inherited(List<List<Policy>> inheritedPolicies, List<Owner> parentOwners) {
    int hierarchySize = parentOwners.size() + 1;
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    PolicyTileList policyTable = policyTile.policyTileTable();
    assertThat(policyTile.inheritedPolicyLists()).hasSameSizeAs(inheritedPolicies);
    policyTile.policyLists().shouldHave(size(hierarchySize));

    // scroll to the policy tile
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(policyTile.getElement());

    for (int i = 0; i < hierarchySize; i++) {
      PolicyTileList list = policyTile.policyList(i); // starts from 1

      if (i == 0) {
        list.ownerName().shouldBe(visible).shouldHave(text("Local"));
      }
      else {
        list.emptyDescriptor().shouldBe(hidden);
        list.ownerName().shouldBe(visible).shouldHave(PolicyTile.inheritedText(parentOwners.get(i - 1).getName()));
        list.rows().shouldHave(size(3)); // 2 rows plus header

        PolicyTileListElement policyElement1 = list.row(1);
        Policy actualPolicy1 = inheritedPolicies.get(i - 1).get(0);
        assertPolicy(policyElement1, actualPolicy1);
        PolicyTileListElement policyElement2 = list.row(2);
        Policy actualPolicy2 = inheritedPolicies.get(i - 1).get(1);
        assertPolicy(policyElement2, actualPolicy2);

        policyTable.threatLegendHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldBe(visible);
        policyTable.threatLegendHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldBe(visible);

        policyTable.nameHeaderColumn().nxAnchor().click();

        policyTable.nameHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldBe(visible);
        policyTable.nameHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldBe(visible);
        policyTable.threatLegendHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldNot(exist);
        policyTable.threatLegendHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldNot(exist);

        assertPolicy(policyElement1, actualPolicy1);
        assertPolicy(policyElement2, actualPolicy2);

        policyTable.threatLegendHeaderColumn().nxAnchor().click();

        policyTable.threatLegendHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldBe(visible);
        policyTable.threatLegendHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldBe(visible);
        policyTable.nameHeaderColumn().sort(HeaderColumn.NX_UP_SELECTED).shouldNot(exist);
        policyTable.nameHeaderColumn().sort(HeaderColumn.NX_DOWN_SELECTED).shouldNot(exist);

        assertPolicy(policyElement1, actualPolicy2);
        assertPolicy(policyElement2, actualPolicy1);

        // reset sorting to original state
        policyTable.threatLegendHeaderColumn().nxAnchor().click();
      }
    }
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
      policy.build()
          .find("span")
          .shouldHave(PolicyTileListElement.WARN)
          .shouldHave(text("warn"))
          .shouldNotHave(PolicyTileListElement.FAIL);
    }
    else if (actionTypeId.equals(Action.ID_FAIL)) {
      policy.build()
          .find("span")
          .shouldHave(PolicyTileListElement.FAIL)
          .shouldHave(text("fail"))
          .shouldNotHave(PolicyTileListElement.WARN);
    }
  }

  @Test
  public void testPolicyTile_Foundation_Firewall() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    Policy policy = tempEntity.newPolicy(currentOwner.getId(), "Policy 1", 10, null, null, null);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible);

    assertPolicyTile_Foundation(policy);
  }

  @Test
  public void testPolicyTile_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    Policy policy = tempEntity.newPolicy(currentOwner.getId(), "Policy 1", 10, null, null, null);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible);

    assertPolicyTile_Foundation(policy, true);
  }

  private void assertPolicyTile_Foundation(Policy policy, boolean... proxyActionReadOnly) {
    boolean isProxyActionReadOnly = proxyActionReadOnly.length >= 1 && proxyActionReadOnly[0];
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    ScrollUtil.scrollIntoViewInstantly(policyTile.getElement());

    PolicyTileList policyTable = policyTile.policyTileTable();
    PolicyTileList list = policyTile.policyList(0);

    PolicyTileListElement policyElement = list.row(1);
    policyElement.threatLegend().shouldBe(visible).shouldHave(text("" + policy.getThreatLevel()));
    policyElement.name().shouldBe(visible).shouldHave(text(policy.getName()));

    policyTable.proxyHeaderColumn().nxAnchorHeader().shouldHave(text("PROXY"));
    policyTable.developHeaderColumn().nxAnchorHeader().shouldHave(text("DEVELOP"));
    policyTable.sourceHeaderColumn().nxAnchorHeader().shouldHave(text("SOURCE"));
    policyTable.buildHeaderColumn().nxAnchorHeader().shouldHave(text("BUILD"));
    policyTable.stageHeaderColumn().nxAnchorHeader().shouldHave(text("STAGE"));
    policyTable.releaseHeaderColumn().nxAnchorHeader().shouldHave(text("RELEASE"));
    policyTable.operateHeaderColumn().nxAnchorHeader().shouldHave(text("OPERATE"));
    policyTable.operateHeaderColumn().nxAnchorHeader().shouldNotHave(text("COMPLIANCE"));

    // Start on proxy column and don't include last column (select row)
    ElementsCollection headerColumns = policyTile.headerColumns();
    for (int i = 2; i < headerColumns.size() - 1; i++) {
      HeaderColumn header = policyTable.header(i);
      // If not read only, only proxy should not be disabled
      if (i == 2 && !isProxyActionReadOnly) {
        header.root.shouldNotHave(PolicyTileList.CELL_DISABLED);
        policyElement.column(i + 1)
            .shouldBe(visible)
            .shouldHave(PolicyTile.noActionText())
            .shouldNotHave(PolicyTileList.CELL_DISABLED);
      }
      else {
        header.root.shouldHave(PolicyTileList.CELL_DISABLED);
        policyElement.column(i + 1)
            .shouldBe(visible)
            .shouldHave(PolicyTile.noActionText())
            .shouldHave(PolicyTileList.CELL_DISABLED);
      }
    }
  }

  @Test
  public void testPolicyTile_LimitedStageLicensing() {
    List<Policy> localPolicies = new ArrayList<>();
    localPolicies
        .add(tempEntity.newPolicy(currentOwner.getId(), "Release", 10, Action.ID_FAIL, Stage.ID_RELEASE, null));

    setLicensedProducts(ProductLicenseDetails.PRODUCT_RISK);

    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    PolicyTileList policyTable = policyTile.policyTileTable();

    OwnerSummaryPage.summaryTile().policyButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(policyTile.getElement());

    PolicyTileList list = policyTile.policyList(0);

    PolicyTileListElement policyElement = list.row(1);
    Policy actualPolicy = localPolicies.get(0);
    // Validate that headers only have proxy and release columns
    policyTable.proxyHeaderColumn().nxAnchorHeader().shouldHave(text("PROXY"));
    // Release column and select row are next after proxy and there are no additional columns
    policyTable.header(3).nxAnchorHeader().shouldHave(text("RELEASE"));
    policyTable.header(4).nxAnchorHeader().shouldHave(text("SELECT ROW"));
    policyTable.header(5).nxAnchorHeader().shouldNot(exist);

    policyElement.chevron().shouldBe(visible);
    policyElement.threatLegend().shouldBe(visible).shouldHave(text("" + actualPolicy.getThreatLevel()));
    policyElement.name().shouldBe(visible).shouldHave(text(actualPolicy.getName()));
    policyElement.column(3).shouldBe(visible).shouldHave(PolicyTile.noActionText());
    policyElement.column(4).shouldBe(visible).shouldHave(text(actualPolicy.getActions().get(Stage.ID_RELEASE)));
    policyElement.chevronColumn(5).shouldHave(PolicyTileListElement.CHEVRON);
  }

  @Test
  public void testPublicDataSources_isVisible() {
    productLicenseManager.setFeatures(LicensedFeature.CPE_MATCHING);
    refresh();
    // Pill is visible
    OwnerSummaryPage.navigationPills().publicDataSources().shouldBe(visible);
    OwnerSummaryPage.navigationPills().publicDataSources().click();

    OwnerSummaryPage.publicDataSourcesTile().shouldBe(visible);
    OwnerSummaryPage.publicDataSourcesTile().title().shouldHave(text("Public Data Sources"));
    OwnerSummaryPage.publicDataSourcesTile().content().shouldHave(text("Public Data Sources are disabled"));
  }

  @Test
  public void testPublicDataSources_isNotVisible() {
    refresh();
    // Pill is not visible due lack of CPE_MATCHING feature
    OwnerSummaryPage.navigationPills().publicDataSources().shouldNotBe(visible);
    OwnerSummaryPage.publicDataSourcesTile().shouldNotBe(visible);
  }

  @Test
  public void testPublicDataSources_navigateToEditForm() {
    productLicenseManager.setFeatures(LicensedFeature.CPE_MATCHING);
    refresh();

    OwnerSummaryPage.navigationPills().publicDataSources().shouldBe(visible);
    OwnerSummaryPage.navigationPills().publicDataSources().click();

    OwnerSummaryPage.publicDataSourcesTile().shouldBe(visible);
    OwnerSummaryPage.publicDataSourcesTile().content().click();

    PublicDataSourcesEditorPage.title().shouldBe(visible);
    PublicDataSourcesEditorPage.radioInputs().forEach(radio -> {
      radio.shouldBe(visible);
      radio.shouldHave(cssClass("nx-radio"));
    });
  }

  protected int getHierarchySize(Owner owner) {
    int hierarchySize = 0;
    Iterator<Owner> iterator = ownerDAO.walkHierarchy(owner).iterator();

    for (; iterator.hasNext(); ++hierarchySize) {
      iterator.next();
    }
    return hierarchySize;
  }

  protected abstract void testReportLinks();

  protected abstract void testApplicationCategoryTile();
}
