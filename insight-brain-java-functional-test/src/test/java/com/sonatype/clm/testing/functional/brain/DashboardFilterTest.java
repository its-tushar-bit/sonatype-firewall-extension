/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsResults;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentsResults;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.AgeFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.CategoryFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.DeleteFilterDialog;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.ManageFiltersDropdown;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.PolicyTypeFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.PolicyViolationStateFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.SaveFilterDialog;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.StageFilter;
import com.sonatype.clm.testing.functional.elements.DashboardViolations;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationTile;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationsResults;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxPolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.elements.NxTreeViewMultiSelect;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.elements.ChangePasswordModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.InternalRealm;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.ACTIVE;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.NO_CHANGES_MESSAGE;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.SELECTED_SAVED_FILTER_OPTION;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.applicationsTab;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.componentsTab;
import static com.sonatype.clm.testing.functional.pages.DashboardPage.violationsTab;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardFilterTest
    extends AbstractFunctionalTest
{
  private static final ComponentIdentifier DEFAULT_COMPONENT_IDENTIFIER = createMavenCoordinates("Group1", "Artifact1",
      "Version1");

  private static ApplicationDAO appDAO = new ApplicationDAO();

  private static Organization org;

  private static Application firstApp;

  private static Tag firstAppCategory1;

  private static Tag firstAppCategory2;

  private static Application secondApp;

  private static Policy policy;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setupData();
    refreshOrOpen(DashboardPage.urlToViolations());
    loginAsAdmin();
  }

  @Before
  public void before() {
    refreshOrOpen(DashboardPage.urlToViolations());
  }

  @After
  public void after() {
    testCLMServer.getCLMServer().getConfiguration().setNeedsAcknowledgementOfInitialDashboardFilter(false);
    clearFilters();
  }

  public void clearFilters() {
    new DashboardFilterDAO().deleteByUsernameAndRealmId(User.ADMIN_USERNAME, InternalRealm.ID);
  }

  private static void setupData() {
    org = staticTempEntity.newOrganization("DashboardTest");
    staticTempEntity.newOrganization("DashboardTestEmptyOrg");
    firstApp = staticTempEntity.newApplication("DashboardTestAppOne", "DashboardTestAppOne", org.getId());
    firstAppCategory1 = staticTempEntity.newTag(org.getId(), "DashboardSpecAppOneCategory1", Color.dark_blue);
    firstAppCategory2 = staticTempEntity.newTag(org.getId(), "DashboardSpecAppOneCategory2", Color.dark_red);
    staticTempEntity.newApplicationTag(firstApp.getId(), firstAppCategory1.getId());
    staticTempEntity.newApplicationTag(firstApp.getId(), firstAppCategory2.getId());

    secondApp = staticTempEntity.newApplication("DashboardTestAppTwo", "DashboardTestAppTwo", org.getId());

    policy = staticTempEntity.newPolicy(org.getId(), "DashboardTestPolicy");

    DateTime now = DateTime.now();

    //first evaluation dated a week ago
    PolicyEvaluation firstPolicyEvaluation = staticTempEntity
        .newPolicyEvaluation(firstApp.getId(), BuildStageType.ID, "DashboardTestFirstEvaluation",
            now.minusDays(7).toDate());
    PolicyViolation firstViolation = staticTempEntity
        .newPolicyViolation(firstPolicyEvaluation, policy, 5, PolicyThreatCategory.LICENSE, "Group1", "Artifact1",
            "Version1", "hash", FailActionType.ID);
    staticTempEntity
        .newApplicationComponent(firstPolicyEvaluation.getApplicationId(), firstPolicyEvaluation.getStageTypeId(),
            firstViolation.getHash(), DEFAULT_COMPONENT_IDENTIFIER);
    staticTempEntity
        .newApplicationComponent(firstPolicyEvaluation.getApplicationId(), firstPolicyEvaluation.getStageTypeId(),
            "987654321", MatchState.SIMILAR, false);
    staticTempEntity
        .newApplicationComponent(firstPolicyEvaluation.getApplicationId(), firstPolicyEvaluation.getStageTypeId(),
            "987654322", MatchState.UNKNOWN, false);

    //same policy as first evaluation, but a different stage and earlier
    PolicyEvaluation firstPolicyEvaluationSecondStage = staticTempEntity.newPolicyEvaluation(firstApp.getId(),
        StageReleaseStageType.ID, "DashboardTestFirstEvaluationSecondStage", now.minusDays(14).toDate());
    PolicyViolation firstViolationSecondStage = staticTempEntity.newPolicyViolation(firstPolicyEvaluationSecondStage,
        policy, 5, PolicyThreatCategory.LICENSE, "Group1", "Artifact1", "Version1", "hash", WarnActionType.ID);
    staticTempEntity.newApplicationComponent(firstPolicyEvaluationSecondStage.getApplicationId(),
        firstPolicyEvaluationSecondStage.getStageTypeId(), firstViolationSecondStage.getHash(),
        DEFAULT_COMPONENT_IDENTIFIER);

    // evaluation in yet another stage
    PolicyEvaluation thirdPolicyEvaluation = staticTempEntity.newPolicyEvaluation(firstApp.getId(), ReleaseStageType.ID,
        "DashboardTestThirdEvaluation", now.minusDays(8).toDate());
    staticTempEntity.newPolicyViolation(thirdPolicyEvaluation, policy, 2, PolicyThreatCategory.QUALITY, "Group1",
        "Artifact1", "Version1");

    // and one more stage to cover them all
    PolicyEvaluation forthPolicyEvaluation = staticTempEntity.newPolicyEvaluation(firstApp.getId(), OperateStageType.ID,
        "DashboardTestForthEvaluation", now.minusDays(9).toDate());
    staticTempEntity.newPolicyViolation(forthPolicyEvaluation, policy, 1, PolicyThreatCategory.OTHER, "Group1",
        "Artifact1", "Version1");

    //most recent evaluation
    PolicyEvaluation secondPolicyEvaluation = staticTempEntity
        .newPolicyEvaluation(secondApp.getId(), ReleaseStageType.ID,
            "DashboardTestSecondEvaluation", now.minusDays(1).toDate());
    staticTempEntity.newPolicyViolation(secondPolicyEvaluation, policy, 10, PolicyThreatCategory.QUALITY);

    PolicyWaiver policyWaiver = staticTempEntity.newWaiver("hash-waived", policy.getId(), secondApp.getId());
    staticTempEntity.newWaivedPolicyViolation(secondPolicyEvaluation, policy, 3, PolicyThreatCategory.QUALITY,
        ComponentIdentifier.createMavenCoordinates("Group2", "Artifact2", "Version2"), "hash-waived", policyWaiver);

    Policy grandfatherPolicy = staticTempEntity.newPolicy(org.getId(), "GrandfatherTestPolicy");
    staticTempEntity.newGrandfatheredPolicyViolation(secondPolicyEvaluation, grandfatherPolicy,
        ComponentIdentifier.createMavenCoordinates("Group3", "ArtifactGrandfather", "Version3"), "hash-grandfathered");
  }

  /**
   * Age only applies to violations tab and defaults to 'past 30 days'.
   */
  @Test
  public void testAgeFilter() {
    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.filterToggle().shouldBe(visible).click();
    AgeFilter ageFilter = DashboardFilters.ageFilter();
    ageFilter.shouldBe(hidden);

    refreshOrOpen(DashboardPage.urlToComponents());
    ageFilter.shouldBe(hidden);

    refreshOrOpen(DashboardPage.urlToViolations());
    ageFilter.shouldBe(visible).shouldHave(text("past 30 days"));
    ageFilter.twisty().click();
    ageFilter.singleSelectList().shouldHaveSize(6).shouldHave(
        texts("past 24 hours", "past 7 days", "past 30 days", "past 90 days", "past 12 months", "all time"));
    ageFilter.past30days().shouldBe(selected);
    ageFilter.past90days().shouldNotBe(selected).click();
    ageFilter.past90days().shouldBe(selected);

    // make sure the tabs are updated
    violationsTab().counter().shouldBe(visible).shouldHave(text("3"));
    componentsTab().counter().shouldBe(visible).shouldHave(text("1"));
    applicationsTab().counter().shouldBe(visible).shouldHave(text("2"));
    DashboardFilters.apply();
    ageFilter.singleSelectList().shouldHaveSize(6);

    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.filterToggle().shouldBe(visible).click();
    ageFilter.twisty().click();
    ageFilter.past30days().shouldNotBe(selected).click();
    // check that revert button restores previously applied value
    DashboardFilters.revertButton().shouldNotBe(DISABLED).click();
    ageFilter.past90days().shouldBe(selected);
    ageFilter.past30days().shouldNotBe(selected).click();
    DashboardFilters.apply();
    ageFilter.twisty().click();
    ageFilter.singleSelectList().forEach(selenideElement -> selenideElement.shouldBe(hidden));
  }

  @Test
  public void testFiltersWithNoPermissions() {
    createUser();
    logout();
    login();
    refreshOrOpen(DashboardPage.urlToViolations());

    DashboardPage.filterToggle().shouldBe(visible).click();
    assertFilterDisabled(DashboardFilters.applicationFilter(), "applications");
    assertStageFilterDefaultState();
    assertCategoryFilterDefaultState();
    assertPolicyTypeFilterDefaultState();
    assertThreatLevelFilterDefaultState();

    DashboardFilters.closeButton().shouldBe(visible).click();
    logout();
    loginAsAdmin();
  }

  @Test
  public void testFilters() throws Exception {
    DashboardPage.filterToggle().shouldBe(visible).click();
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    assertDefaultFilterState();
    assertAgeFilterDefaultState();

    // assert no filtering is done
    DashboardPage.violationsView().results().violations().shouldHaveSize(3);
    DashboardPage.violationsTab().counter().shouldHave(text("3"));
    DashboardPage.componentsTab().counter().shouldNot(exist);
    DashboardPage.applicationsTab().counter().shouldNot(exist);

    DashboardFilters.applyButton().shouldBe(DISABLED).hover();
    Tooltip.get().shouldHave(NO_CHANGES_MESSAGE);
    DashboardPage.violationsView().results().mask().shouldBe(hidden);

    // check that counters get updated
    setSomeFilterValues();

    DashboardFilters.applyButton().shouldNotBe(DISABLED).hover();
    Tooltip.get().shouldBe(hidden);

    // violations should be covered by the mask
    DashboardPage.violationsView().results().mask().shouldBe(visible);
    DashboardPage.violationsView().results().lastViolation().shouldBe(visible);

    // Ideally we would check here that the violation is not clickable.  There doesn't appear to be a way to do
    // that however, at least not one that works in PhantomJS

    DashboardPage.componentsTab().click();
    DashboardPage.componentsView().resultsMask().shouldBe(visible);

    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().resultsMask().shouldBe(visible);

    DashboardPage.violationsTab().click();

    assertNewCounterState();

    // check revert
    DashboardFilters.revertButton().click();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    assertDefaultFilterState();
    DashboardPage.violationsView().results().mask().shouldBe(hidden);

    // make sure changes persist after save + reload
    setSomeFilterValues();
    DashboardFilters.apply();
    DashboardPage.violationsView().results().mask().shouldBe(hidden);

    refresh();
    DashboardPage.filterToggle().shouldBe(visible).click();
    assertNewCounterState();
    DashboardPage.violationsView().results().mask().shouldBe(hidden);

    // assert stored filter
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filter = new DashboardFilterDAO()
        .getByUsernameAndRealmId("admin", InternalRealm.ID);
    assertThat(filter.get(0).getFilter().replace("\r\n", "\n")).isEqualTo("{\n" +
        "  \"minPolicyThreatLevel\" : 2,\n" +
        "  \"maxPolicyThreatLevel\" : 7,\n" +
        "  \"applicationFilters\" : [ \"" + firstApp.getId() + "\" ],\n" +
        "  \"organizationFilters\" : [ ],\n" +
        "  \"tagFilters\" : [ \"" + firstAppCategory1.getId() + "\" ],\n" +
        "  \"policyThreatCategoryFilters\" : [ \"QUALITY\" ],\n" +
        "  \"stageTypeFilters\" : [ \"release\" ],\n" +
        "  \"maxDaysOld\" : 30,\n" +
        "  \"policyViolationStates\" : [ \"OPEN\", \"WAIVED\", \"GRANDFATHERED\" ]\n" +
        "}");

    // assert applied filters
    DashboardPage.violationsView().results().violations().shouldHaveSize(1);
    DashboardPage.violationsTab().counter().shouldHave(text("1"));
    ViolationTile violation = DashboardPage.violationsView().results().firstViolation();
    violation.threatNumber().shouldHave(text("2"));
    violation.policy().shouldHave(text("DashboardTestPolicy"));
    violation.application().shouldHave(text("DashboardTestAppOne"));

    // enable app 2, but don't enable apps with no categories.  Results should not change
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.apply();

    DashboardPage.violationsView().results().violations().shouldHaveSize(1);
    DashboardPage.violationsTab().counter().shouldHave(text("1"));
    violation = DashboardPage.violationsView().results().firstViolation();
    violation.threatNumber().shouldHave(text("2"));
    violation.policy().shouldHave(text("DashboardTestPolicy"));
    violation.application().shouldHave(text("DashboardTestAppOne"));

    // enable "uncategorized applications" so that secondApp results show
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.applicationCategoryFilter().getFilterCheckboxAt(0).hover();
    Tooltip.get().shouldNotBe(visible);
    DashboardFilters.applicationCategoryFilter().noCategory().hover();
    Tooltip.get().shouldNotBe(visible);
    DashboardFilters.applicationCategoryFilter().getFilterCheckboxAt(2).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("in DashboardTest"));
    DashboardFilters.applicationCategoryFilter().getFilterCheckboxAt(3).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("in DashboardTest"));
    DashboardFilters.applicationCategoryFilter().noCategory().click();
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.apply();

    DashboardPage.violationsView().results().violations().shouldHaveSize(2);
    DashboardPage.violationsTab().counter().shouldHave(text("2"));
    ViolationTile firstViolation = DashboardPage.violationsView().results().firstViolation();
    firstViolation.threatNumber().shouldHave(text("3"));
    firstViolation.policy().shouldHave(text("DashboardTestPolicy"));
    firstViolation.application().shouldHave(text("DashboardTestAppTwo"));
    firstViolation.component().shouldHave(text("Artifact2"));

    ViolationTile secondViolation = DashboardPage.violationsView().results().lastViolation();
    secondViolation.threatNumber().shouldHave(text("2"));
    secondViolation.policy().shouldHave(text("DashboardTestPolicy"));
    secondViolation.application().shouldHave(text("DashboardTestAppOne"));
    secondViolation.component().shouldHave(text("Artifact1"));

    // check other tabs
    DashboardPage.componentsTab().click();
    DashboardPage.componentsView().results().components().shouldHaveSize(2);
    DashboardPage.violationsTab().counter().shouldHave(text("2"));
    DashboardPage.componentsTab().counter().shouldHave(text("2"));
    DashboardPage.applicationsTab().counter().shouldNot(exist);
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHaveSize(2);
    DashboardPage.violationsTab().counter().shouldHave(text("2"));
    DashboardPage.componentsTab().counter().shouldHave(text("2"));
    DashboardPage.applicationsTab().counter().shouldHave(text("2"));

    // unselect all policy violation statuses
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().allItems().shouldBe(selected).click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardPage.applicationsView().resultsMask().shouldBe(visible);

    DashboardFilters.apply();
    DashboardPage.applicationsView().resultsMask().shouldBe(hidden);

    // check all tabs - should have the same results
    DashboardPage.violationsTab().counter().shouldNot(exist);
    DashboardPage.componentsTab().counter().shouldNot(exist);
    DashboardPage.applicationsTab().counter().shouldHave(text("2"));
    DashboardPage.violationsTab().click();
    DashboardPage.violationsView().results().violations().shouldHaveSize(2);
    DashboardPage.violationsTab().counter().shouldHave(text("2"));
    DashboardPage.componentsTab().counter().shouldNot(exist);
    DashboardPage.applicationsTab().counter().shouldHave(text("2"));
    DashboardPage.violationsView().results().firstViolation().component().shouldHave(text("Artifact2"));
    DashboardPage.violationsView().results().lastViolation().component().shouldHave(text("Artifact1"));
    DashboardPage.componentsTab().click();
    DashboardPage.componentsView().results().components().shouldHaveSize(2);
    DashboardPage.violationsTab().counter().shouldHave(text("2"));
    DashboardPage.componentsTab().counter().shouldHave(text("2"));
    DashboardPage.applicationsTab().counter().shouldHave(text("2"));
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHaveSize(2);
    DashboardPage.violationsTab().counter().shouldHave(text("2"));
    DashboardPage.componentsTab().counter().shouldHave(text("2"));
    DashboardPage.applicationsTab().counter().shouldHave(text("2"));
    DashboardPage.componentsTab().click();

    // filter WAIVED only
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().waived().click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardPage.componentsView().resultsMask().shouldBe(visible);

    DashboardFilters.apply();
    DashboardPage.componentsView().resultsMask().shouldBe(hidden);

    // components tab should have only waived component
    DashboardPage.componentsView().results().components().shouldHaveSize(1);
    DashboardPage.componentsView().results().firstComponent().shouldHave(text("Artifact2"));

    // violations tab should have only waived violation
    DashboardPage.violationsTab().click();
    DashboardPage.violationsView().results().violations().shouldHaveSize(1);
    ViolationTile waivedViolation = DashboardPage.violationsView().results().firstViolation();
    waivedViolation.component().shouldHave(text("Artifact2"));

    // applications tab should have only waived violation app
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHaveSize(1);
    DashboardPage.applicationsView().results().firstApplication().shouldHave(text("DashboardTestAppTwo"));

    DashboardPage.filterToggle().shouldBe(visible).click();
    selectDefaultFilter();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    assertDefaultFilterState();

    // filter GRANDFATHERED only
    DashboardPage.componentsTab().click();
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().open().click();
    DashboardFilters.policyViolationStateFilter().grandfathered().click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardPage.componentsView().resultsMask().shouldBe(visible);

    DashboardFilters.apply();
    DashboardPage.componentsView().resultsMask().shouldBe(hidden);

    // components tab should have only grandfathered component
    DashboardPage.componentsView().results().components().shouldHaveSize(1);
    DashboardPage.componentsView().results().firstComponent().shouldHave(text("ArtifactGrandfather"));

    // violations tab should have only grandfathered violation
    DashboardPage.violationsTab().click();
    DashboardPage.violationsView().results().violations().shouldHaveSize(1);
    ViolationTile grandfatheredViolation = DashboardPage.violationsView().results().firstViolation();
    grandfatheredViolation.component().shouldHave(text("ArtifactGrandfather"));

    // applications tab should have only grandfathered violation app
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHaveSize(1);
    DashboardPage.applicationsView().results().firstApplication().shouldHave(text("DashboardTestAppTwo"));
  }

  @Test
  public void testFilterSingleAppWithMultipleCategories() {
    DashboardPage.filterToggle().shouldBe(visible).click();
    CategoryFilter categoryFilter = DashboardFilters.applicationCategoryFilter();
    categoryFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("3"));
    categoryFilter.twisty().shouldBe(visible).click();
    categoryFilter.multiSelectList().shouldHave(size(4));
    categoryFilter.checkboxItem(3).shouldNotBe(selected).click();
    categoryFilter.checkboxItem(4).shouldNotBe(selected).click();
    categoryFilter.counter().shouldBe(ACTIVE).shouldHave(text("2 of 3"));

    DashboardFilters.apply();
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHaveSize(1);
  }

  @Test
  public void testFilterOutAllResults() throws Exception {
    DashboardPage.filterToggle().shouldBe(visible).click();
    // filter only for security policy type
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.policyTypeFilter().security().click();
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.apply();
    refresh();
    DashboardPage.filterToggle().shouldBe(visible).click();
    // verify policy filter counter
    DashboardFilters.policyTypeFilter().counter().shouldBe(ACTIVE).shouldHave(text("1 of 4"));
    // verify no violations row shown
    DashboardPage.violationsView().results().violations().shouldHaveSize(0);
    DashboardPage.violationsTab().counter().shouldHave(text("0"));
    DashboardPage.componentsTab().counter().shouldNot(exist);
    DashboardPage.applicationsTab().counter().shouldNot(exist);
    // verify no data message
    DashboardPage.violationsView().results().noDataMessage().shouldBe(visible)
        .shouldHave(text("No data available in the last 30 days given the applied filters and permissions."));
  }

  @Test
  public void testApplyChangesToDefaultFilter() {
    DashboardPage.filterToggle().shouldBe(visible).click();
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);

    setSomeFilterValues();
    DashboardFilters.apply();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);

    // check that the 'dirty' asterisk remains after reload
    refresh();
    DashboardPage.filterToggle().shouldBe(visible).click();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);
  }

  @Test
  public void testSaveFilter() {
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(hidden);
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default")).click();
    // no saved filters
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    manage.dropdownMenu().options().shouldHaveSize(1);
    manage.dropdownMenu().defaultFilterOption().shouldHave(text("Default"));
    manage.dropdownMenu().emptyListMessage().shouldBe(visible).shouldHave(text("No saved filters"));

    manage.openMenuButton().click();

    manage.dropdownMenu().shouldNotBe(visible);

    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);

    // save initial filter
    saveFilter("Initial", null, false, true);

    manage.openMenuButton().click();
    manage.dropdownMenu().options().shouldHaveSize(2);
    manage.dropdownMenu().option(1).shouldHave(text("Initial"));

    eyesWatcher.eyesCheck("Initial filter saved");
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);

    // Overwrite the filter, verifies the confirmation path
    overwriteFilter("Initial");
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);

    // "save filter" should be disabled if filter changes are not applied
    setSomeFilterValues();
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    DashboardFilters.saveButton().shouldHave(DISABLED);
    DashboardFilters.saveButton().hover();
    DashboardFilters.saveButtonTooltip().shouldBe(visible).shouldHave(text("Please apply filter before saving"));
    DashboardFilters.saveButton().click();
    DashboardFilters.saveFilterDialog().shouldNotBe(visible);
    DashboardFilters.closeButton().shouldBe(visible, DISABLED).click();
    DashboardFilters.closeButton().shouldBe(visible); // click should have no effect
    DashboardFilters.closeButton().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Please apply or revert filter"));

    // apply new filter
    DashboardFilters.apply();
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);
    DashboardFilters.saveButton().shouldNotBe(DISABLED);

    DashboardFilters.closeButton().shouldNotBe(DISABLED).click();

    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Initial"));
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(visible);

    // check that the 'dirty' asterisk remains after reload
    refresh();
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(visible);
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Initial")).click();
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);

    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filters = new DashboardFilterDAO()
        .getByUsernameAndRealmId("admin", InternalRealm.ID);
    assertThat(filters).hasSize(2);
    assertThat(filters.get(0).getName()).isEqualTo("");
    assertThat(filters.get(0).getBasedOnFilterName()).isEqualTo("Initial");

    // save new filter
    saveFilter("New Filter", "Initial", false, false);
    manage.selectedFilterLabel().shouldHave(exactText("New Filter"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);

    manage.openMenuButton().click();
    manage.dropdownMenu().options().shouldHaveSize(3);
    manage.dropdownMenu().option(2).shouldHave(text("New Filter"));

    DashboardFilters.closeButton().click();
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("New Filter"));
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(hidden);

    // load other filter
    ViolationsResults table = DashboardPage.violationsView().results();
    table.violations().shouldHaveSize(1);
    DashboardPage.filterToggle().shouldBe(visible).click();
    manage.openMenuButton().click();
    manage.dropdownMenu().option(1).selectFilterButton().click();
    manage.dropdownMenu().shouldBe(hidden);

    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    assertDefaultFilterState();
    table.violations().shouldHaveSize(3);
    DashboardFilters.closeButton().click();
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Initial"));
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(hidden);

    // check that refreshing doesn't change anything
    refresh();
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(hidden);
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Initial")).click();
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    assertDefaultFilterState();
    table.violations().shouldHaveSize(3);

    // go back to New Filter
    manage.openMenuButton().click();
    manage.dropdownMenu().option(2).selectFilterButton().shouldHave(text("New Filter")).click();

    // save as existing filter name
    saveFilter("Initial", "New Filter", true, true);
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    table.violations().shouldHaveSize(1);

    // save as Default filter name should be disallowed
    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    SaveFilterDialog saveDialog = DashboardFilters.saveFilterDialog();
    saveDialog.shouldBe(visible).saveAsRadio().click();
    saveDialog.nameInput().val("Default");
    saveDialog.saveButton().shouldBe(disabled);
    saveDialog.cancelButton().click();
    saveDialog.shouldNotBe(visible);
  }

  @Test
  public void testDeleteSavedFilter() {
    DashboardPage.filterToggle().shouldBe(visible).click();
    // create filters
    saveFilter("Delete", null, false, false);
    saveFilter("Do not delete", "Delete", false, false);

    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    DeleteFilterDialog deleteFilterDialog = DashboardFilters.deleteFilterDialog();

    // document click should close the dropdown
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    DashboardPage.violationsTab().click();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // delete filter - cancel
    DashboardPage.filterToggle().shouldBe(visible).click();
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    manage.dropdownMenu().options().shouldHaveSize(3);
    manage.dropdownMenu().option(2).shouldHave(text("Do not delete")).shouldBe(SELECTED_SAVED_FILTER_OPTION);
    manage.dropdownMenu().option(1).shouldHave(text("Delete")).deleteFilterButton().click();
    // delete filter button shouldn't close the filters dropdown
    manage.dropdownMenu().shouldBe(visible);
    eyesWatcher.eyesCheck();
    deleteFilterDialog.shouldBe(visible).confirmation()
        .shouldHave(exactText("You are about to delete \"Delete\" filter. This action can not be undone."));
    // document click while delete dialog is open shouldn't close the filters dropdown
    deleteFilterDialog.confirmation().click();
    manage.dropdownMenu().shouldBe(visible);
    // cancel dialog button shouldn't close the filters dropdown
    deleteFilterDialog.cancelButton().click();
    deleteFilterDialog.shouldBe(hidden);
    manage.dropdownMenu().shouldBe(visible);

    // delete filter - continue
    manage.dropdownMenu().option(1).deleteFilterButton().click();
    deleteFilterDialog.shouldBe(visible);
    deleteFilterDialog.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteFilterDialog.shouldBe(hidden);
    manage.dropdownMenu().shouldBe(visible);

    // verify delete
    manage.dropdownMenu().options().shouldHaveSize(2);
    manage.dropdownMenu().option(1).shouldHave(text("Do not delete"));

    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filters =
        dashboardFilterDAO.getByUsernameAndRealmId("admin", InternalRealm.ID);
    assertThat(filters).hasSize(2);
    assertThat(filters.get(0).getName()).isEqualTo(""); // default filter
    assertThat(filters.get(1).getName()).isEqualTo("Do not delete");
  }

  @Test
  public void testDeleteSavedFilter_appliedFilter() {
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    DeleteFilterDialog deleteFilterDialog = DashboardFilters.deleteFilterDialog();
    String filter1 = "Applied Filter Is Based On Me";

    DashboardPage.filterToggle().shouldBe(visible).click();
    // save a filter
    saveFilter(filter1, null, false, false);
    // modify, apply, but don't save
    setSomeFilterValues();
    DashboardFilters.apply();
    manage.selectedFilterLabel().shouldHave(text("Applied Filter Is Based On Me"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);

    // delete selected filter
    manage.openMenuButton().click();
    manage.dropdownMenu().option(1).shouldBe(SELECTED_SAVED_FILTER_OPTION).deleteFilterButton().click();
    deleteFilterDialog.shouldBe(visible);
    deleteFilterDialog.continueButton().click();
    FormMask.seeAndWaitForDismissal();

    // check Default filter is now shown as selected with the asterisk
    manage.selectedFilterLabel().shouldHave(text("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);
    manage.dropdownMenu().defaultFilterOption().shouldBe(SELECTED_SAVED_FILTER_OPTION);

    // verify that applied filter is no longer based on the deleted one
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    DashboardFilter filter = dashboardFilterDAO.getByUsernameAndRealmIdAndName("admin", InternalRealm.ID, "");
    assertThat(filter.getBasedOnFilterName()).isNull();
  }

  @Test
  public void testNeedsAcknowledgement() {
    testCLMServer.getCLMServer().getConfiguration().setNeedsAcknowledgementOfInitialDashboardFilter(true);
    refreshOrOpen(DashboardPage.urlToViolations());

    DashboardFilters.filterContainer().shouldBe(visible);

    DashboardFilters.closeButton().shouldBe(visible, DISABLED).click();
    DashboardFilters.closeButton().shouldBe(visible); // click should have no effect
    DashboardFilters.closeButton().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Please apply a filter"));

    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    DashboardPage.needsAcknowledgementMessage().shouldBe(visible)
        .shouldHave(text(DashboardPage.NEEDS_ACKNOWLEDGEMENT_MESSAGE));
    DashboardPage.violationsView().results().violations().shouldHaveSize(0);

    DashboardPage.componentsTab().click();
    DashboardPage.needsAcknowledgementMessage().shouldBe(visible)
        .shouldHave(text(DashboardPage.NEEDS_ACKNOWLEDGEMENT_MESSAGE));
    DashboardPage.componentsView().results().components().shouldHaveSize(0);

    DashboardPage.applicationsTab().click();
    DashboardPage.needsAcknowledgementMessage().shouldBe(visible)
        .shouldHave(text(DashboardPage.NEEDS_ACKNOWLEDGEMENT_MESSAGE));
    DashboardPage.applicationsView().results().applications().shouldHaveSize(0);

    DashboardPage.violationsTab().click();

    // even though the apply button is enabled the revert button should be disabled since the form isn't dirty
    DashboardFilters.filterContainer().shouldBe(visible);
    DashboardFilters.revertButton().shouldBe(visible, DISABLED);

    // change filter, the Close button tooltip should no change
    setSomeFilterValues();
    DashboardFilters.closeButton().shouldBe(visible, DISABLED).click();
    DashboardFilters.closeButton().shouldBe(visible); // click should have no effect
    DashboardFilters.closeButton().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Please apply a filter"));

    DashboardFilters.revertButton().shouldNotBe(DISABLED).click();
    DashboardFilters.apply();

    assertNeedsAcknowledgementPostFilterState(null);
  }

  @Test
  public void testNeedsAcknowledgement_ExistingSavedFilter() {
    DashboardPage.filterToggle().shouldBe(visible).click();
    String filterName = "Saved Filter";
    saveFilter(filterName, null, false, false);
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filters =
        dashboardFilterDAO.getByUsernameAndRealmId("admin", InternalRealm.ID);

    assertThat(filters).hasSize(2); // one for the active and named
    assertThat(filters.get(0).isAcknowledged()).isFalse();
    assertThat(filters.get(1).isAcknowledged()).isFalse();

    testCLMServer.getCLMServer().getConfiguration().setNeedsAcknowledgementOfInitialDashboardFilter(true);
    refreshOrOpen(DashboardPage.urlToViolations());

    DashboardPage.filterToggle().shouldBe(visible).click();
    assertNeedsAcknowledgementPostFilterState(filterName);
  }

  @Test
  public void testOrgFilterIncludesNewApplications() {
    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.filterToggle().shouldBe(visible).click();

    // filter by Org
    DashboardFilters.organizationFilter().twisty().click();
    DashboardFilters.organizationFilter().checkboxItem(2).click();
    DashboardFilters.apply();

    ApplicationsResults results = DashboardPage.applicationsView().results();
    results.applications().shouldHaveSize(2);

    // add new App to same Org
    Application thirdApp = staticTempEntity.newApplication("DashboardTestAppThree", "DashboardTestAppThree",
        org.getId());
    PolicyEvaluation appThreePolicyEvaluation = staticTempEntity.newPolicyEvaluation(thirdApp.getId(),
        BuildStageType.ID, "DashboardTestEvaluationForAppThree", DateTime.now().minusDays(1).toDate());
    staticTempEntity.newPolicyViolation(appThreePolicyEvaluation, policy, 5, PolicyThreatCategory.LICENSE, "Group1",
        "Artifact1", "Version1", "hash", FailActionType.ID);

    // new App should be included in results
    refreshOrOpen(DashboardPage.urlToApplications());
    results.applications().shouldHaveSize(3);
    eyesWatcher.eyesCheck();

    appDAO.delete(thirdApp);
    refreshOrOpen(DashboardPage.urlToApplications());
    results.applications().shouldHaveSize(2);
  }

  @Test
  public void testNoResultsShownForEmptyOrg() {
    DashboardPage.filterToggle().shouldBe(visible).click();
    // filter by Empty Org
    DashboardFilters.organizationFilter().twisty().click();
    DashboardFilters.organizationFilter().checkboxItem(3).click();
    DashboardFilters.apply();

    ViolationsResults violationsResults = DashboardPage.violationsView().results();
    violationsResults.violations().shouldHaveSize(0);
    violationsResults.noDataMessage().shouldBe(visible);

    refreshOrOpen(DashboardPage.urlToApplications());
    ApplicationsResults applicationsResults = DashboardPage.applicationsView().results();
    applicationsResults.applications().shouldHaveSize(0);
    applicationsResults.noDataMessage().shouldBe(visible);

    refreshOrOpen(DashboardPage.urlToComponents());
    ComponentsResults componentsResults = DashboardPage.componentsView().results();
    componentsResults.components().shouldHaveSize(0);
    componentsResults.noDataMessage().shouldBe(visible);
  }

  @Test
  public void testEscEvents() {
    // filter container is open
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // filters are dirty - Esc should not close filterContainer
    DashboardPage.filterToggle().shouldBe(visible).click();
    setSomeFilterValues();
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldBe(visible);
    DashboardFilters.revertButton().click();
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // filter container and dropdown are open
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.manageFiltersDropdown().openMenuButton().click();
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldBe(visible);
    pressEscape();
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldNotBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // filter container, dropdown and delete modal are open
    DashboardPage.filterToggle().shouldBe(visible).click();
    saveFilter("test filter", null, false, false);
    DashboardFilters.manageFiltersDropdown().openMenuButton().click();
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldBe(visible).option(1).deleteFilterButton().click();
    DashboardFilters.deleteFilterDialog().shouldBe(visible);
    pressEscape();
    DashboardFilters.deleteFilterDialog().shouldNotBe(visible);
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldNotBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // after deleting a filter
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.manageFiltersDropdown().openMenuButton().click();
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldBe(visible).option(1).deleteFilterButton().click();
    DashboardFilters.deleteFilterDialog().shouldBe(visible).continueButton().click();
    DashboardFilters.deleteFilterDialog().shouldNotBe(visible);
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldNotBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // filter container and save modal are open
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    SaveFilterDialog saveDialog = DashboardFilters.saveFilterDialog();
    saveDialog.shouldBe(visible);
    pressEscape();
    saveDialog.shouldNotBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);
  }

  @Test
  public void testOffClickEvents() {
    // clicking within filter panel should not close it
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.applyButton().shouldBe(visible).click();
    DashboardFilters.filterContainer().shouldBe(visible);

    // clicking outside of filter panel should close it
    DashboardPage.violationsTab().click();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // clicking within filters dropdown should not close it
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    DashboardPage.filterToggle().shouldBe(visible).click();
    manage.openMenuButton().click();
    manage.dropdownMenu().emptyListMessage().shouldBe(visible).click();
    manage.dropdownMenu().shouldBe(visible);

    // clicking outside of filters dropdown should close it
    DashboardFilters.applyButton().click();
    manage.dropdownMenu().shouldNotBe(visible);

    // clicking outside of filter panel should close both filters dropdown and filter panel
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    DashboardPage.violationsTab().click();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // when filter changes are not applied, clicking outside of filter panel should close filters dropdown
    // but not filter panel
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.filterContainer().shouldBe(visible);
    setSomeFilterValues();
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    new DashboardViolations().results().mask().shouldBe(visible).click();
    manage.dropdownMenu().shouldNotBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);

    // clicking anywhere when Save Filter modal is open, should not close filter panel
    DashboardFilters.revertButton().shouldNotBe(disabled).click();
    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    SaveFilterDialog saveDialog = DashboardFilters.saveFilterDialog();
    saveDialog.shouldBe(visible);

    DashboardFilters.modalBackdrop().shouldBe(visible).click();
    DashboardFilters.filterContainer().shouldBe(visible);

    saveDialog.header().click();
    DashboardFilters.filterContainer().shouldBe(visible);

    saveDialog.cancelButton().click();
    DashboardFilters.filterContainer().shouldBe(visible);

    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    saveDialog.nameInput().val("New Filter");
    saveDialog.saveButton().shouldNotHave(disabled).click();
    DashboardFilters.filterContainer().shouldBe(visible);

    // clicking anywhere when Delete Filter modal is open, should not close filters dropdown and filter panel
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    manage.dropdownMenu().option(1).deleteFilterButton().shouldBe(visible).click();

    DashboardFilters.modalBackdrop().shouldBe(visible).click();
    manage.dropdownMenu().shouldBe(visible);

    DashboardFilters.deleteFilterDialog().shouldBe(visible).click();
    manage.dropdownMenu().shouldBe(visible);

    DashboardFilters.deleteFilterDialog().cancelButton().click();
    manage.dropdownMenu().shouldBe(visible);

    manage.dropdownMenu().option(1).deleteFilterButton().shouldBe(visible).click();
    DashboardFilters.deleteFilterDialog().continueButton().click();
    manage.dropdownMenu().shouldBe(visible);
  }

  @Test
  public void testDisplayModalOverFilter() {
    DashboardPage.filterToggle().shouldBe(visible).click();
    DashboardFilters.filterContainer().shouldBe(visible);
    setSomeFilterValues();

    MainHeader.userMenu().dropdownToggle().click();
    MainHeader.userMenu().changePassword().click();

    ChangePasswordModal modal = new ChangePasswordModal();
    modal.shouldBe(visible);

    eyesWatcher.eyesCheck("Modal Overlay is on top of Dashboard filter");
  }

  private void assertNeedsAcknowledgementPostFilterState(String filterName) {
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();

    if (filterName != null) {
      manage.selectedFilterLabel().shouldHave(exactText(filterName));
    }
    else {
      manage.selectedFilterLabel().shouldHave(exactText("Default"));
    }
    DashboardPage.needsAcknowledgementMessage().shouldBe(hidden);
    DashboardPage.violationsView().results().violations().shouldHaveSize(3);
    ViolationTile violation = DashboardPage.violationsView().results().firstViolation();
    violation.threatNumber().shouldHave(text("10"));
    violation.policy().shouldHave(text("DashboardTestPolicy"));
    violation.application().shouldHave(text("DashboardTestAppTwo"));

    DashboardPage.componentsTab().click();
    DashboardPage.needsAcknowledgementMessage().shouldBe(hidden);
    DashboardPage.componentsView().results().components().shouldHaveSize(1);
    DashboardPage.componentsView().results().firstComponent().shouldHave(text("Artifact1"));

    DashboardPage.applicationsTab().click();
    DashboardPage.needsAcknowledgementMessage().shouldBe(hidden);
    DashboardPage.applicationsView().results().applications().shouldHaveSize(2);
    DashboardPage.applicationsView().results().firstApplication().shouldHave(text("DashboardTestAppTwo"));

    // go back to violations so refreshOrOpen calls work properly
    DashboardPage.violationsTab().click();
  }

  /**
   * Save the filter under the given name. Executes and asserts the "Save As" workflow of the Save Filter modal.
   * Does not test the "Overwrite" workflow
   *
   * @param existingExpected Is there expected to be an existing filter with a matching name
   * @param useVisualTesting determines whether or not to visually validate when saving filters
   */
  private void saveFilter(String filterName,
                          String existingFilterName,
                          boolean existingExpected,
                          boolean useVisualTesting)
  {
    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    SaveFilterDialog saveDialog = DashboardFilters.saveFilterDialog();
    saveDialog.shouldBe(visible);
    // Avoid superfluous validations
    if (useVisualTesting) {
      eyesWatcher.eyesCheck("Save filter");
    }
    saveDialog.header().shouldHave(text("Save Filter"));

    if (existingFilterName != null) {
      saveDialog.saveButton().shouldNotBe(disabled).shouldHave(text("Save"));
      saveDialog.overwriteRadio().shouldBe(selected);
      saveDialog.saveAsRadio().shouldNotBe(selected);
      saveDialog.overwriteRadio().shouldHave(text("save (overwrite " + existingFilterName + ")"));
      saveDialog.nameInput().shouldBe(hidden);

      saveDialog.saveAsRadio().click();
    }
    else {
      saveDialog.overwriteRadio().shouldBe(disabled);
    }

    saveDialog.saveButton().shouldBe(disabled).shouldHave(text("Save"));
    saveDialog.overwriteRadio().shouldNotBe(selected);
    saveDialog.saveAsRadio().shouldBe(selected);
    saveDialog.nameInput().shouldBe(Condition.empty).shouldBe(visible);

    saveDialog.nameInput().val(filterName);
    saveDialog.saveButton().shouldNotHave(disabled).click();

    if (existingExpected) {
      saveDialog.header().shouldHave(text("Name In Use"));
      if (useVisualTesting) {
        eyesWatcher.eyesCheck("Save filter confirmation");
      }
      saveDialog.confirmation().shouldBe(visible).shouldHave(text("\"" + filterName + "\" is already in use."
          + " Continuing will permanently overwrite \"" + filterName + "\". This action cannot be undone."));

      // test cancel
      saveDialog.cancelButton().shouldHave(text("Cancel")).click();

      saveDialog.header().shouldHave(text("Save Filter"));
      saveDialog.saveAsRadio().shouldBe(selected);
      saveDialog.nameInput().shouldHave(value(filterName)).shouldBe(visible);
      saveDialog.saveButton().click();

      // back on the confirmation warning, continue this time
      saveDialog.header().shouldHave(text("Name In Use"));
      saveDialog.saveButton().shouldHave(text("Continue")).click();
    }

    FormMask.seeAndWaitForDismissal();
    saveDialog.shouldBe(hidden);
  }

  private void overwriteFilter(String currentFilterName) {
    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    SaveFilterDialog saveDialog = DashboardFilters.saveFilterDialog();
    saveDialog.shouldBe(visible);
    saveDialog.header().shouldHave(text("Save Filter"));

    saveDialog.overwriteRadio().shouldBe(selected);
    saveDialog.saveAsRadio().shouldNotBe(selected);
    saveDialog.nameInput().shouldBe(hidden);

    // test cancel
    saveDialog.cancelButton().click();
    saveDialog.shouldBe(hidden);
    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    saveDialog.overwriteRadio().shouldBe(selected);

    saveDialog.saveButton().shouldNotBe(DISABLED).click();

    saveDialog.header().shouldHave(text("Overwrite Filter"));
    eyesWatcher.eyesCheck("Overwrite filter confirmation");
    saveDialog.confirmation().shouldBe(visible).shouldHave(
        text("You are about to permanently overwrite " + currentFilterName + ". This action cannot be undone."));

    // test cancel
    saveDialog.cancelButton().shouldHave(text("Cancel")).click();

    saveDialog.header().shouldHave(text("Save Filter"));
    saveDialog.overwriteRadio().shouldBe(selected);
    saveDialog.saveButton().click();

    // back on the confirmation warning, continue this time
    saveDialog.header().shouldHave(text("Overwrite Filter"));
    saveDialog.saveButton().shouldHave(text("Continue")).click();

    FormMask.seeAndWaitForDismissal();
    saveDialog.shouldBe(hidden);
  }

  private void selectDefaultFilter() {
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.openMenuButton().click();
    manage.dropdownMenu().defaultFilterOption().selectFilterButton().click();
  }

  private void setSomeFilterValues() {
    DashboardFilters.stageFilter().twisty().click();
    DashboardFilters.stageFilter().release().click();
    DashboardFilters.stageFilter().twisty().click();
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.policyTypeFilter().quality().click();
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.applicationCategoryFilter().checkboxItem(3).click();
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().waived().click();
    DashboardFilters.policyViolationStateFilter().grandfathered().click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 7);
    DashboardFilters.policyThreatLevelFilter().twisty().click();
  }

  private void assertDefaultFilterState() {
    NxTreeViewMultiSelect appFilter = DashboardFilters.applicationFilter();

    appFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("2"));
    appFilter.multiSelectList().filter(visible).shouldBe(empty);
    appFilter.twisty().shouldBe(visible).click();
    appFilter.multiSelectList().filter(visible).shouldHave(size(3));
    appFilter.checkboxItem(1).shouldNotBe(selected).label().shouldHave(text("all/none"));
    appFilter.checkboxItem(2).shouldNotBe(selected).label().shouldHave(text(firstApp.getName()));
    appFilter.checkboxItem(3).shouldNotBe(selected).label().shouldHave(text(secondApp.getName()));
    appFilter.twisty().click();

    CategoryFilter categoryFilter = DashboardFilters.applicationCategoryFilter();
    categoryFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("3"));
    // children of a closed filter are still there but hidden from view
    categoryFilter.multiSelectList().forEach(selenideElement -> selenideElement.shouldBe(hidden));
    categoryFilter.twisty().shouldBe(visible).click();
    categoryFilter.multiSelectList().shouldHave(size(4));
    categoryFilter.checkboxItem(1).shouldNotBe(selected).label().shouldHave(text("all/none"));
    categoryFilter.noCategory().shouldNotBe(selected).label().shouldHave(text("uncategorized applications"));
    categoryFilter.checkboxItem(3).shouldNotBe(selected).label().shouldHave(text(firstAppCategory1.getName()));
    categoryFilter.checkboxItem(4).shouldNotBe(selected).label().shouldHave(text(firstAppCategory2.getName()));
    categoryFilter.twisty().click();

    assertStageFilterDefaultState();
    assertPolicyTypeFilterDefaultState();
    assertThreatLevelFilterDefaultState();
    assertPolicyViolationStateFilterDefaultState();
  }

  private void assertThreatLevelFilterDefaultState() {
    NxPolicyThreatLevelFilter threatLevelFilter = DashboardFilters.policyThreatLevelFilter();
    threatLevelFilter.counter().shouldBe(visible).shouldBe(ACTIVE).shouldHave(text("2 – 10"));
    threatLevelFilter.slider().shouldBe(hidden);
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().shouldBe(visible);
    threatLevelFilter.twisty().click();
  }

  private void assertAgeFilterDefaultState() {
    AgeFilter ageFilter = DashboardFilters.ageFilter();
    ageFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("past 30 days"));
    ageFilter.singleSelectList().forEach(selenideElement -> selenideElement.shouldBe(hidden));
    ageFilter.twisty().click();
    ageFilter.singleSelectList().shouldHaveSize(6);
    ageFilter.past30days().shouldBe(selected).label().shouldHave(text("past 30 days"));
    ageFilter.twisty().click();
  }

  private void assertPolicyTypeFilterDefaultState() {
    PolicyTypeFilter policyTypeFilter = DashboardFilters.policyTypeFilter();
    policyTypeFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("4"));
    policyTypeFilter.multiSelectList().forEach(selenideElement -> selenideElement.shouldBe(hidden));
    policyTypeFilter.twisty().shouldBe(visible).click();
    policyTypeFilter.multiSelectList().shouldHave(size(5));
    policyTypeFilter.allItems().shouldNotBe(selected).label().shouldHave(text("all/none"));
    policyTypeFilter.license().shouldNotBe(selected).label().shouldHave(text("License"));
    policyTypeFilter.other().shouldNotBe(selected).label().shouldHave(text("Other"));
    policyTypeFilter.quality().shouldNotBe(selected).label().shouldHave(text("Quality"));
    policyTypeFilter.security().shouldNotBe(selected).label().shouldHave(text("Security"));
    policyTypeFilter.twisty().click();
  }

  private void assertStageFilterDefaultState() {
    StageFilter stageFilter = DashboardFilters.stageFilter();
    stageFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("5"));
    stageFilter.multiSelectList().forEach(selenideElement -> selenideElement.shouldBe(hidden));
    stageFilter.twisty().shouldBe(visible).click();
    stageFilter.multiSelectList().shouldHave(size(6));
    stageFilter.allItems().shouldNotBe(selected).label().shouldHave(text("all/none"));
    stageFilter.source().shouldNotBe(selected).label().shouldHave(text("Source"));
    stageFilter.build().shouldNotBe(selected).label().shouldHave(text("Build"));
    stageFilter.stageRelase().shouldNotBe(selected).label().shouldHave(text("Stage Release"));
    stageFilter.release().shouldNotBe(selected).label().shouldHave(text("Release"));
    stageFilter.operate().shouldNotBe(selected).label().shouldHave(text("Operate"));
    stageFilter.twisty().click();
  }

  private void assertCategoryFilterDefaultState() {
    CategoryFilter categoryFilter = DashboardFilters.applicationCategoryFilter();
    categoryFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("1"));
    categoryFilter.multiSelectList().forEach(selenideElement -> selenideElement.shouldBe(hidden));
    categoryFilter.twisty().shouldBe(visible).click();
    categoryFilter.multiSelectList().shouldHave(size(2));
    categoryFilter.allItems().shouldNotBe(selected).label().shouldHave(text("all/none"));
    categoryFilter.noCategory().shouldNotBe(selected).label().shouldHave(text("uncategorized applications"));
    categoryFilter.twisty().click();
  }

  private void assertPolicyViolationStateFilterDefaultState() {
    PolicyViolationStateFilter policyViolationStateFilter = DashboardFilters.policyViolationStateFilter();
    policyViolationStateFilter.counter().shouldBe(visible).shouldHave(text("1 of 3"));
    policyViolationStateFilter.multiSelectList().forEach(selenideElement -> selenideElement.shouldBe(hidden));
    policyViolationStateFilter.twisty().shouldBe(visible).click();
    policyViolationStateFilter.multiSelectList().shouldHave(size(4));
    policyViolationStateFilter.allItems().shouldNotBe(selected).label().shouldHave(text("all/none"));
    policyViolationStateFilter.open().shouldBe(selected).label().shouldHave(text("Open"));
    policyViolationStateFilter.waived().shouldNotBe(selected).label().shouldHave(text("Waived"));
    policyViolationStateFilter.grandfathered().shouldNotBe(selected).label().shouldHave(text("Grandfathered"));
    policyViolationStateFilter.twisty().click();
  }

  private void assertNewCounterState() {
    DashboardFilters.applicationFilter().counter().shouldHave(cssClass("nx-counter--active"))
        .shouldHave(text("1 of 2"));
    DashboardFilters.applicationCategoryFilter().counter().shouldBe(ACTIVE).shouldHave(text("1 of 3"));
    DashboardFilters.stageFilter().counter().shouldBe(ACTIVE).shouldHave(text("1 of 5"));
    DashboardFilters.policyTypeFilter().counter().shouldBe(ACTIVE).shouldHave(text("1 of 4"));
    DashboardFilters.policyViolationStateFilter().counter().shouldBe(ACTIVE).shouldHave(text("3 of 3"));
    DashboardFilters.policyThreatLevelFilter().counter().shouldHave(text("2 – 7"));
  }

  private void assertFilterDisabled(NxTreeViewMultiSelect filter, String filterType) {
    filter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("0"));

    filter.shouldHave(cssClass("nx-tree-view--disabled"));
    filter.multiSelectList().filter(visible).shouldBe(empty);
    filter.twisty().shouldBe(visible).click();
    filter.multiSelectList().filter(visible).shouldBe(empty);

    Tooltip.get().shouldBe(visible).shouldHave(text("There are no " + filterType + " to filter"));
  }

  private void pressEscape() {
    Selenide.actions().sendKeys(Keys.ESCAPE).perform();
  }
}
