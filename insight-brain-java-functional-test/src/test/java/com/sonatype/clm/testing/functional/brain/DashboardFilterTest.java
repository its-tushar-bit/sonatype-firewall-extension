/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.DashboardFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.DeleteDialog;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.DeleteFiltersDialog;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.ManageFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.PolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.PolicyTypeFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.PolicyViolationStateFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.SaveFilterDialog;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.StageFilter;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationTile;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationsResults;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
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
import com.sonatype.insight.brain.model.tag.Tag;

import com.codeborne.selenide.Condition;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.createMavenCoordinates;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.INACTIVE;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.NO_CHANGES_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class DashboardFilterTest
    extends AbstractFunctionalTest
{
  private static final ComponentIdentifier DEFAULT_COMPONENT_IDENTIFIER = createMavenCoordinates("Group1", "Artifact1",
      "Version1");

  private static Application firstApp;

  private static Tag firstAppCategory;

  private static Application secondApp;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setupData();
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);
  }

  @After
  public void clearFilters() {
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filters = dashboardFilterDAO.getByUsername("admin");
    for (com.sonatype.insight.brain.model.filter.DashboardFilter filter : filters) {
      dashboardFilterDAO.delete(filter);
    }
  }

  private static void setupData() {
    Organization org = staticTempEntity.newOrganization("DashboardTest");
    firstApp = staticTempEntity.newApplication("DashboardTestAppOne", "DashboardTestAppOne", org.getId());
    firstAppCategory = staticTempEntity.newTag(org.getId(), "DashboardSpecAppOneCategory", Color.dark_blue);
    staticTempEntity.newApplicationTag(firstApp.getId(), firstAppCategory.getId());

    secondApp = staticTempEntity.newApplication("DashboardTestAppTwo", "DashboardTestAppTwo", org.getId());

    Policy policy = staticTempEntity.newPolicy(org.getId(), "DashboardTestPolicy");

    DateTime now = DateTime.now();

    //first evaluation dated a week ago
    PolicyEvaluation firstPolicyEvaluation = staticTempEntity
        .newPolicyEvaluation(firstApp.getId(), BuildStageType.ID, "DashboardTestFirstEvaluation",
            now.minusDays(7).toDate());
    PolicyViolation firstViolation = staticTempEntity
        .newPolicyViolation(firstPolicyEvaluation, policy, 5, PolicyThreatCategory.LICENSE, "Group1", "Artifact1",
            "Version1", "hash", FailActionType.ID);
    staticTempEntity.newFirstOccurrencePolicyViolation(firstViolation.getId(), firstPolicyEvaluation.getApplicationId(),
        firstPolicyEvaluation.getStageTypeId());
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
    staticTempEntity.newFirstOccurrencePolicyViolation(firstViolationSecondStage.getId(),
        firstPolicyEvaluationSecondStage.getApplicationId(), firstPolicyEvaluationSecondStage.getStageTypeId());
    staticTempEntity.newApplicationComponent(firstPolicyEvaluationSecondStage.getApplicationId(),
        firstPolicyEvaluationSecondStage.getStageTypeId(), firstViolationSecondStage.getHash(),
        DEFAULT_COMPONENT_IDENTIFIER);

    // evaluation in yet another stage
    PolicyEvaluation thirdPolicyEvaluation = staticTempEntity.newPolicyEvaluation(firstApp.getId(), ReleaseStageType.ID,
        "DashboardTestThirdEvaluation", now.minusDays(8).toDate());
    PolicyViolation thirdViolation = staticTempEntity.
        newPolicyViolation(thirdPolicyEvaluation, policy, 2, PolicyThreatCategory.QUALITY,
            "Group1", "Artifact1", "Version1");
    staticTempEntity.newFirstOccurrencePolicyViolation(thirdViolation.getId(), thirdPolicyEvaluation.getApplicationId(),
        thirdPolicyEvaluation.getStageTypeId());

    // and one more stage to cover them all
    PolicyEvaluation forthPolicyEvaluation = staticTempEntity.newPolicyEvaluation(firstApp.getId(), OperateStageType.ID,
        "DashboardTestForthEvaluation", now.minusDays(9).toDate());
    PolicyViolation forthViolation = staticTempEntity.
        newPolicyViolation(forthPolicyEvaluation, policy, 1, PolicyThreatCategory.OTHER,
            "Group1", "Artifact1", "Version1");
    staticTempEntity.newFirstOccurrencePolicyViolation(forthViolation.getId(), forthPolicyEvaluation.getApplicationId(),
        forthPolicyEvaluation.getStageTypeId());

    //most recent evaluation
    PolicyEvaluation secondPolicyEvaluation = staticTempEntity
        .newPolicyEvaluation(secondApp.getId(), ReleaseStageType.ID,
            "DashboardTestSecondEvaluation", now.toDate());
    PolicyViolation secondViolation = staticTempEntity.newPolicyViolation(secondPolicyEvaluation, policy, 10,
        PolicyThreatCategory.QUALITY);
    staticTempEntity
        .newFirstOccurrencePolicyViolation(secondViolation.getId(), secondPolicyEvaluation.getApplicationId(),
            secondPolicyEvaluation.getStageTypeId());

    PolicyViolation waivedPolicyViolation = staticTempEntity.newPolicyViolation(secondPolicyEvaluation, policy, 3,
        PolicyThreatCategory.QUALITY, "Group2", "Artifact2", "Version2", "hash-waived");
    PolicyWaiver policyWaiver = staticTempEntity.newWaiver("hash-waived", policy.getId(), secondApp.getId());
    staticTempEntity.newWaivedPolicyViolation(waivedPolicyViolation, policyWaiver);
  }

  @Test
  public void testFiltersWithNoPermissions() {
    createUser();
    logout();
    login();
    refreshOrOpen(DashboardPage.VIOLATIONS_URL);

    assertFilterDisabled(DashboardFilters.applicationFilter(), "applications");
    assertFilterDisabled(DashboardFilters.applicationCategoryFilter(), "application categories");
    assertStageFilterInitialState();
    assertPolicyTypeFilterInitialState();
    assertThreatLevelFilterInitialState();

    logout();
    loginAsAdmin();
  }

  @Test
  public void testFilters() throws Exception {
    assertInitialFilterState();

    // assert no filtering is done
    DashboardPage.violationsView().results().violations().shouldHaveSize(3);

    DashboardFilters.applyButton().shouldBe(DISABLED).hover().tooltip().shouldHave(NO_CHANGES_MESSAGE);

    // check that counters get updated
    setSomeFilterValues();

    DashboardFilters.applyButton().shouldNotBe(DISABLED).hover().tooltip().shouldNotBe(visible);

    assertNewCounterState();

    // check revert
    DashboardFilters.revertButton().click();
    assertInitialFilterState();

    // make sure changes persist after save + reload
    setSomeFilterValues();
    DashboardFilters.applyButton().click();
    refresh();
    assertNewCounterState();

    // assert stored filter
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filter = new DashboardFilterDAO()
        .getByUsername("admin");
    assertThat(filter.get(0).getFilter().replace("\r\n", "\n"), is("{\n" +
        "  \"minPolicyThreatLevel\" : 2,\n" +
        "  \"maxPolicyThreatLevel\" : 7,\n" +
        "  \"applicationFilters\" : [ \"" + firstApp.getId() + "\" ],\n" +
        "  \"organizationFilters\" : [ ],\n" +
        "  \"tagFilters\" : [ \"" + firstAppCategory.getId() + "\" ],\n" +
        "  \"policyThreatCategoryFilters\" : [ \"QUALITY\" ],\n" +
        "  \"stageTypeFilters\" : [ \"release\" ],\n" +
        "  \"maxDaysOld\" : 30,\n" +
        "  \"policyViolationStates\" : [ \"OPEN\", \"WAIVED\" ]\n" +
        "}"));

    // assert applied filters
    DashboardPage.violationsView().results().violations().shouldHaveSize(1);
    ViolationTile violation = DashboardPage.violationsView().results().firstViolation();
    violation.threatNumber().shouldHave(text("2"));
    violation.policy().shouldHave(text("DashboardTestPolicy"));
    violation.application().shouldHave(text("DashboardTestAppOne"));

    // also show the waived violation from secondApp
    DashboardFilters.toggleTwisties();
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.applicationCategoryFilter().checkboxItem(2).click();
    DashboardFilters.toggleTwisties();
    DashboardFilters.applyButton().click();

    DashboardPage.violationsView().results().violations().shouldHaveSize(2);
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
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHaveSize(2);

    // unselect all policy violation statuses
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().allItems().shouldBe(selected).click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.applyButton().click();

    // check all tabs - should have the same results
    DashboardPage.violationsTab().click();
    DashboardPage.violationsView().results().violations().shouldHaveSize(2);
    DashboardPage.violationsView().results().firstViolation().component().shouldHave(text("Artifact2"));
    DashboardPage.violationsView().results().lastViolation().component().shouldHave(text("Artifact1"));
    DashboardPage.componentsTab().click();
    DashboardPage.componentsView().results().components().shouldHaveSize(2);
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHaveSize(2);

    // filter WAIVED only
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().waived().click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.applyButton().click();

    // violations tab should have only waived violation
    DashboardPage.violationsTab().click();
    DashboardPage.violationsView().results().violations().shouldHaveSize(1);
    ViolationTile waivedViolation = DashboardPage.violationsView().results().firstViolation();
    waivedViolation.component().shouldHave(text("Artifact2"));

    // components tab should have only waived component
    DashboardPage.componentsTab().click();
    DashboardPage.componentsView().results().components().shouldHaveSize(1);
    DashboardPage.componentsView().results().firstComponent().shouldHave(text("Artifact2"));

    // applications tab should have only waived violation app
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHaveSize(1);
    DashboardPage.applicationsView().results().firstApplication().shouldHave(text("DashboardTestAppTwo"));

    // check reset
    DashboardFilters.clearButton().click();
    assertInitialFilterState();
  }

  @Test
  public void testFilterOutAllResults() throws Exception {
    // filter only for security policy type
    DashboardFilters.toggleTwisties();
    DashboardFilters.policyTypeFilter().security().click();
    DashboardFilters.toggleTwisties();
    DashboardFilters.applyButton().click();
    refresh();
    // verify policy filter counter
    DashboardFilters.policyTypeFilter().counter().shouldNotBe(INACTIVE).shouldHave(text("1 of 4"));
    // verify no violations row shown
    DashboardPage.violationsView().results().violations().shouldHaveSize(0);
    // verify no data message
    DashboardPage.violationsView().results().noDataMessage().shouldBe(visible)
        .shouldHave(text("No data available in the last 30 days given the applied filters and available permissions."));
  }

  @Test
  public void testSaveLoadFilter() {
    ManageFilters manage = DashboardFilters.manage();

    // no saved filters
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    manage.emptyListMessage().shouldBe(visible).shouldHave(text("No saved filters."));
    manage.openMenuButton().click();
    DashboardFilters.saveFilterNameLabel().shouldNotBe(visible);
    DashboardFilters.saveFilterDirtyAsterisk().shouldNotBe(visible);

    // save initial filter
    saveFilter("Initial", null);
    manage.openMenuButton().click();
    manage.filters().shouldHaveSize(1);
    manage.filter(0).shouldHave(text("Initial"));
    manage.openMenuButton().click();
    DashboardFilters.saveFilterNameLabel().shouldHave(text("Initial"));
    DashboardFilters.saveFilterDirtyAsterisk().shouldNotBe(visible);

    // Overwrite the filter, verifies the confirmation path
    saveFilter("Initial", "Initial");
    DashboardFilters.saveFilterNameLabel().shouldHave(text("Initial"));
    DashboardFilters.saveFilterDirtyAsterisk().shouldNotBe(visible);

    // "save filter" should be disabled if filter changes are not applied
    setSomeFilterValues();
    DashboardFilters.saveFilterDirtyAsterisk().shouldNotBe(visible);
    manage.openMenuButton().click();
    manage.saveFilter().shouldHave(DISABLED).click();
    manage.saveFilterDialog().shouldNotBe(visible);
    manage.saveFilter().hover();
    manage.tooltip().shouldHave(text("Please apply filter before saving"));

    // apply new filter
    DashboardFilters.applyButton().click();
    DashboardFilters.saveFilterNameLabel().shouldHave(text("Initial"));
    DashboardFilters.saveFilterDirtyAsterisk().shouldBe(visible);

    // check that the 'dirty' asterisk remains after reload
    refresh();
    DashboardFilters.saveFilterDirtyAsterisk().shouldBe(visible);
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filters = new DashboardFilterDAO()
        .getByUsername("admin");
    assertThat(filters.size(), is(2));
    assertThat(filters.get(0).getName(), is(""));
    assertThat(filters.get(0).getBasedOnFilterName(), is("Initial"));

    // save new filter
    saveFilter("New Filter", "Initial");
    DashboardFilters.saveFilterDirtyAsterisk().shouldNotBe(visible);
    DashboardFilters.saveFilterNameLabel().shouldHave(text("New Filter"));
    manage.openMenuButton().click();
    manage.filters().shouldHaveSize(2);
    manage.filter(1).shouldHave(text("New Filter"));

    // load other filter
    ViolationsResults table = DashboardPage.violationsView().results();
    table.violations().shouldHaveSize(1);
    manage.filter(0).click();
    assertInitialFilterState("Initial");
    table.violations().shouldHaveSize(3);

    // check that refreshing doesn't change anything
    refresh();
    assertInitialFilterState("Initial");
    table.violations().shouldHaveSize(3);
  }

  @Test
  public void testDeleteFilter() {
    ManageFilters manage = DashboardFilters.manage();
    String filter1 = "Delete";
    String filter2 = "Do not delete";

    // no filters to delete
    manage.openMenuButton().click();
    manage.deleteFilters().shouldHave(DISABLED).click();
    manage.deleteFiltersDialog().shouldNotBe(visible);
    manage.openMenuButton().click();

    // create filters
    saveFilter(filter1, null);
    saveFilter(filter2, filter1);
    manage.openMenuButton().click();

    // delete filter button disabled if nothing selected
    manage.deleteFilters().shouldNotHave(DISABLED).click();
    DeleteFiltersDialog deleteFiltersDialog = manage.deleteFiltersDialog();
    deleteFiltersDialog.shouldBe(visible);
    deleteFiltersDialog.filters().shouldHaveSize(2);
    deleteFiltersDialog.checkboxItem(1).input().shouldNotBe(selected);
    deleteFiltersDialog.checkboxItem(2).input().shouldNotBe(selected);
    deleteFiltersDialog.checkboxItem(1).label().shouldHave(text(filter1));
    deleteFiltersDialog.checkboxItem(2).label().shouldHave(text(filter2));
    deleteFiltersDialog.deleteButton().shouldHave(DISABLED);
    
    // delete filter
    deleteFiltersDialog.checkboxItem(1).click();
    deleteFiltersDialog.deleteButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    // delete confirmation cancel
    deleteFiltersDialog.shouldNotBe(visible);
    DeleteDialog deleteDialog = manage.deleteDialog();
    deleteDialog.shouldBe(visible);
    deleteDialog.body().shouldHave(
        text("You are about to remove 1 filter(s). This action cannot be undone."));
    deleteDialog.cancelButton().click();

    //delete confirmation continue
    deleteFiltersDialog.deleteButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    deleteDialog.shouldBe(visible);
    deleteDialog.continueButton().click();
    FormMask.seeAndWaitForDismissal();

    deleteDialog.shouldNotBe(visible);
    deleteFiltersDialog.shouldNotBe(visible);

    // verify delete
    manage.openMenuButton().click();
    manage.deleteFilters().shouldNotHave(DISABLED).click();
    deleteFiltersDialog.filters().shouldHaveSize(1);
    deleteFiltersDialog.checkboxItem(1).input().shouldNotBe(selected);
    deleteFiltersDialog.checkboxItem(1).label().shouldHave(text(filter2));

    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filters = dashboardFilterDAO.getByUsername("admin");
    assertThat(filters.size(), is(2));
    assertThat(filters.get(0).getName(), is("")); // default filter
    assertThat(filters.get(1).getName(), is(filter2));
  }

  @Test
  public void testDeleteSavedFilter_appliedFilter() {
    ManageFilters manage = DashboardFilters.manage();
    String filter1 = "Applied Filter Is Based On Me";

    // save a filter
    saveFilter(filter1, null);
    // modify, apply, but don't save
    setSomeFilterValues();
    DashboardFilters.applyButton().shouldNotBe(DISABLED).click();
    DashboardFilters.saveFilterNameLabel().shouldHave(text("Applied Filter Is Based On Me"));
    DashboardFilters.saveFilterDirtyAsterisk().shouldBe(visible);

    // delete the saved filter
    manage.openMenuButton().click();
    manage.deleteFilters().shouldNotHave(DISABLED).click();
    DeleteFiltersDialog deleteFiltersDialog = manage.deleteFiltersDialog();
    deleteFiltersDialog.checkboxItem(1).click();
    deleteFiltersDialog.deleteButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    manage.deleteDialog().continueButton().click();
    FormMask.seeAndWaitForDismissal();

    // check the UI is in a clean state
    DashboardFilters.saveFilterNameLabel().shouldNotBe(visible);
    DashboardFilters.saveFilterDirtyAsterisk().shouldNotBe(visible);

    // verify that applied filter is no longer based on the deleted one
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO();
    com.sonatype.insight.brain.model.filter.DashboardFilter filter = dashboardFilterDAO
        .getByUsernameAndName("admin", "");
    assertThat(filter.getBasedOnFilterName(), is(nullValue()));
  }
  
  private void saveFilter(String filterName, String existingFilterName) {
    ManageFilters manage = DashboardFilters.manage();
    manage.openMenuButton().click();
    manage.saveFilter().shouldNotHave(DISABLED).click();
    SaveFilterDialog saveDialog = manage.saveFilterDialog();
    saveDialog.shouldBe(visible);
    if (existingFilterName != null) {
      saveDialog.saveButton().shouldNotHave(DISABLED);
      saveDialog.nameInput().shouldHave(value(existingFilterName));
    }
    else {
      saveDialog.saveButton().shouldHave(DISABLED);
      saveDialog.nameInput().shouldBe(Condition.empty);
    }
    saveDialog.nameInput().val(filterName);
    saveDialog.saveButton().shouldNotHave(DISABLED).click();

    if (filterName.equals(existingFilterName)) {
      saveDialog.confirmation().shouldBe(visible);
      saveDialog.confirmContinue().click();
    }
    FormMask.seeAndWaitForDismissal();
    saveDialog.shouldNotBe(visible);
  }

  private void setSomeFilterValues() {
    DashboardFilters.toggleTwisties();
    DashboardFilters.stageFilter().release().click();
    DashboardFilters.policyTypeFilter().quality().click();
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.applicationCategoryFilter().checkboxItem(2).click();
    DashboardFilters.policyViolationStateFilter().waived().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 7);
    DashboardFilters.toggleTwisties();
  }

  private void assertInitialFilterState() {
    assertInitialFilterState("");
  }

  private void assertInitialFilterState(final String savedFilterName) {
    DashboardFilter appFilter = DashboardFilters.applicationFilter();

    appFilter.counter().shouldBe(visible, INACTIVE).shouldHave(text("2"));
    appFilter.multiSelectList().shouldBe(empty);
    appFilter.twisty().shouldBe(visible).click();
    appFilter.multiSelectList().shouldHave(size(3));
    appFilter.checkboxItem(1).shouldNotBe(selected).label().shouldHave(text("all applications"));
    appFilter.checkboxItem(2).shouldNotBe(selected).label().shouldHave(text(firstApp.getName()));
    appFilter.checkboxItem(3).shouldNotBe(selected).label().shouldHave(text(secondApp.getName()));
    appFilter.twisty().click();

    DashboardFilter categoryFilter = DashboardFilters.applicationCategoryFilter();
    categoryFilter.counter().shouldBe(visible, INACTIVE).shouldHave(text("1"));
    categoryFilter.multiSelectList().shouldBe(empty);
    categoryFilter.twisty().shouldBe(visible).click();
    categoryFilter.multiSelectList().shouldHave(size(2));
    categoryFilter.checkboxItem(1).shouldNotBe(selected).label().shouldHave(text("all application categories"));
    categoryFilter.checkboxItem(2).shouldNotBe(selected).label().shouldHave(text(firstAppCategory.getName()));
    categoryFilter.twisty().click();

    if (savedFilterName.isEmpty()) {
      DashboardFilters.saveFilterNameLabel().shouldNotBe(visible);
      DashboardFilters.saveFilterDirtyAsterisk().shouldNotBe(visible);
    }
    else {
      DashboardFilters.saveFilterNameLabel().shouldBe(text(savedFilterName));
      DashboardFilters.saveFilterDirtyAsterisk().shouldNotBe(visible);
    }

    assertStageFilterInitialState();
    assertPolicyTypeFilterInitialState();
    assertThreatLevelFilterInitialState();
    assertPolicyViolationStateFilterInitialState();
  }

  private void assertThreatLevelFilterInitialState() {
    PolicyThreatLevelFilter threatLevelFilter = DashboardFilters.policyThreatLevelFilter();
    threatLevelFilter.counter().shouldBe(visible).shouldNotBe(INACTIVE).shouldHave(text("2 – 10"));
    threatLevelFilter.slider().shouldNotBe(visible);
    threatLevelFilter.twisty().click();
    threatLevelFilter.slider().shouldBe(visible);
    threatLevelFilter.twisty().click();
  }

  private void assertPolicyTypeFilterInitialState() {
    PolicyTypeFilter policyTypeFilter = DashboardFilters.policyTypeFilter();
    policyTypeFilter.counter().shouldBe(visible, INACTIVE).shouldHave(text("4"));
    policyTypeFilter.multiSelectList().shouldBe(empty);
    policyTypeFilter.twisty().shouldBe(visible).click();
    policyTypeFilter.multiSelectList().shouldHave(size(5));
    policyTypeFilter.allItems().shouldNotBe(selected).label().shouldHave(text("all policy types"));
    policyTypeFilter.license().shouldNotBe(selected).label().shouldHave(text("License"));
    policyTypeFilter.other().shouldNotBe(selected).label().shouldHave(text("Other"));
    policyTypeFilter.quality().shouldNotBe(selected).label().shouldHave(text("Quality"));
    policyTypeFilter.security().shouldNotBe(selected).label().shouldHave(text("Security"));
    policyTypeFilter.twisty().click();
  }

  private void assertStageFilterInitialState() {
    StageFilter stageFilter = DashboardFilters.stageFilter();
    stageFilter.counter().shouldBe(visible, INACTIVE).shouldHave(text("4"));
    stageFilter.multiSelectList().shouldBe(empty);
    stageFilter.twisty().shouldBe(visible).click();
    stageFilter.multiSelectList().shouldHave(size(5));
    stageFilter.allItems().shouldNotBe(selected).label().shouldHave(text("all stages"));
    stageFilter.build().shouldNotBe(selected).label().shouldHave(text("Build"));
    stageFilter.stageRelase().shouldNotBe(selected).label().shouldHave(text("Stage Release"));
    stageFilter.release().shouldNotBe(selected).label().shouldHave(text("Release"));
    stageFilter.operate().shouldNotBe(selected).label().shouldHave(text("Operate"));
    stageFilter.twisty().click();
  }

  private void assertPolicyViolationStateFilterInitialState() {
    PolicyViolationStateFilter policyViolationStateFilter = DashboardFilters.policyViolationStateFilter();
    policyViolationStateFilter.counter().shouldBe(visible).shouldHave(text("1 of 2"));
    policyViolationStateFilter.multiSelectList().shouldBe(empty);
    policyViolationStateFilter.twisty().shouldBe(visible).click();
    policyViolationStateFilter.multiSelectList().shouldHave(size(3));
    policyViolationStateFilter.allItems().shouldNotBe(selected).label().shouldHave(text("all violation states"));
    policyViolationStateFilter.open().shouldBe(selected).label().shouldHave(text("Open"));
    policyViolationStateFilter.waived().shouldNotBe(selected).label().shouldHave(text("Waived"));
    policyViolationStateFilter.twisty().click();
  }

  private void assertNewCounterState() {
    DashboardFilters.applicationFilter().counter().shouldNotBe(INACTIVE).shouldHave(text("1 of 2"));
    DashboardFilters.applicationCategoryFilter().counter().shouldNotBe(INACTIVE).shouldHave(text("1 of 1"));
    DashboardFilters.stageFilter().counter().shouldNotBe(INACTIVE).shouldHave(text("1 of 4"));
    DashboardFilters.policyTypeFilter().counter().shouldNotBe(INACTIVE).shouldHave(text("1 of 4"));
    DashboardFilters.policyViolationStateFilter().counter().shouldNotBe(INACTIVE).shouldHave(text("2 of 2"));
    DashboardFilters.policyThreatLevelFilter().counter().shouldHave(text("2 – 7"));
  }

  private void assertFilterDisabled(DashboardFilter filter, String filterType) {
    filter.counter().shouldBe(visible, INACTIVE).shouldHave(text("0"));

    filter.anchor().shouldBe(DISABLED);
    filter.multiSelectList().shouldBe(empty);
    filter.twisty().shouldBe(visible).shouldHave(cssClass("disabled"), cssClass("cannot-select")).click();
    filter.multiSelectList().shouldBe(empty);

    filter.hover().tooltip().shouldHave(text("There are no " + filterType + " to filter."));
  }

}
