/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ChangePasswordModal;
import com.sonatype.clm.testing.functional.elements.DashboardApplications.ApplicationsResults;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentsResults;
import com.sonatype.clm.testing.functional.elements.DashboardFilters;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.AgeFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.CategoryFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.DeleteFilterDialog;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.ExpirationDateFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.ManageFiltersDropdown;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.PolicyTypeFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.PolicyViolationStateFilter;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.SaveFilterDialog;
import com.sonatype.clm.testing.functional.elements.DashboardFilters.StageFilter;
import com.sonatype.clm.testing.functional.elements.DashboardViolations;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationTile;
import com.sonatype.clm.testing.functional.elements.DashboardViolations.ViolationsResults;
import com.sonatype.clm.testing.functional.elements.DashboardWaivers.WaiverTile;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.NxPolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.elements.NxTreeViewMultiSelect;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.filter.DashboardFilter;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.OperateStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.security.InternalRealm;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Selenide;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.Keys;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.*;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.ACTIVE;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.NO_CHANGES_MESSAGE;
import static com.sonatype.clm.testing.functional.elements.DashboardFilters.SELECTED_SAVED_FILTER_OPTION;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class DashboardFilterTest
    extends AbstractFunctionalTest
{
  private static final String POLICY_WAIVER_REASON_ACKNOWLEDGED_VIOLATION_ID = "9b704ef5bc064fc29d7fe08a251ee9a6";

  private ApplicationDAO applicationDAO;

  private OrganizationDAO orgDAO;

  private Organization rootOrg;

  private Organization parentOrg;

  private Organization org;

  private Application firstApp;

  private Repository repository1;

  private Repository repository2;

  private Tag firstAppCategory1;

  private Tag firstAppCategory2;

  private Application secondApp;

  private Policy policy;

  private DashboardFilterDAO dashboardFilterDAO;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.waitUntilSpinnersGone();
    loginAsAdmin();
    DashboardPage.waitUntilSpinnersGone();
  }

  private void setupData() {
    orgDAO = lookup(OrganizationDAO.class);
    applicationDAO = lookup(ApplicationDAO.class);
    dashboardFilterDAO = lookup(DashboardFilterDAO.class);

    rootOrg = orgDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    repository1 = tempEntity.newRepository("Repository 1");
    repository2 = tempEntity.newRepository("Repository 2");
    parentOrg = tempEntity.newOrganization("ParentOrgTest");
    org = tempEntity.newOrganization("DashboardTest", parentOrg);
    tempEntity.newOrganization("DashboardTestEmptyOrg");
    firstApp = tempEntity.newApplication("DashboardTestAppOne", "DashboardTestAppOne", org.getId());
    firstAppCategory1 = tempEntity.newTag(org.getId(), "DashboardSpecAppOneCategory1", Color.dark_blue);
    firstAppCategory2 = tempEntity.newTag(org.getId(), "DashboardSpecAppOneCategory2", Color.dark_red);
    tempEntity.newApplicationTag(firstApp.getId(), firstAppCategory1.getId());
    tempEntity.newApplicationTag(firstApp.getId(), firstAppCategory2.getId());

    secondApp = tempEntity.newApplication("DashboardTestAppTwo", "DashboardTestAppTwo", org.getId());

    policy = tempEntity.newPolicy(org.getId(), "DashboardTestPolicy");
    Organization levelOneOrg = tempEntity.newOrganization("Level 1 Org");
    Organization levelTwoOrg = tempEntity.newOrganization("Level 2 Org", levelOneOrg);
    Organization levelThreeOrg = tempEntity.newOrganization("Level 3 Org", levelTwoOrg);
    Organization levelFourOrg = tempEntity.newOrganization("Level 4 Org", levelThreeOrg);
    Organization levelFiveOrg = tempEntity.newOrganization("Level 5 Org", levelFourOrg);

    tempEntity.newApplication("Level 1 App", "Level1App", levelOneOrg.getId());
    tempEntity.newApplication("Level 2 App", "Level2App", levelTwoOrg.getId());
    tempEntity.newApplication("Level 3 App", "Level3App", levelThreeOrg.getId());
    tempEntity.newApplication("Level 4 App", "Level4App", levelFourOrg.getId());
    tempEntity.newApplication("Level 5 App", "Level5App", levelFiveOrg.getId());

    DateTime now = DateTime.now();
    Instant seeDate = Instant.now();

    //first evaluation dated a week ago
    PolicyEvaluation firstPolicyEvaluation = tempEntity
        .newPolicyEvaluation(firstApp.getId(), BuildStageType.ID, "DashboardTestFirstEvaluation",
            now.minusDays(7).toDate());
    tempEntity.newPolicyViolation(firstPolicyEvaluation, policy, 5, PolicyThreatCategory.LICENSE, "Group1", "Artifact1",
        "Version1", "hash", FailActionType.ID);

    //same policy as first evaluation, but a different stage and earlier
    PolicyEvaluation firstPolicyEvaluationSecondStage = tempEntity.newPolicyEvaluation(firstApp.getId(),
        StageReleaseStageType.ID, "DashboardTestFirstEvaluationSecondStage", now.minusDays(14).toDate());
    tempEntity.newPolicyViolation(firstPolicyEvaluationSecondStage, policy, 5, PolicyThreatCategory.LICENSE, "Group1",
        "Artifact1", "Version1", "hash", WarnActionType.ID);

    // evaluation in yet another stage
    PolicyEvaluation thirdPolicyEvaluation = tempEntity.newPolicyEvaluation(firstApp.getId(), ReleaseStageType.ID,
        "DashboardTestThirdEvaluation", now.minusDays(8).toDate());
    tempEntity.newPolicyViolation(thirdPolicyEvaluation, policy, 2, PolicyThreatCategory.QUALITY, "Group1",
        "Artifact1", "Version1");

    // and one more stage to cover them all
    PolicyEvaluation forthPolicyEvaluation = tempEntity.newPolicyEvaluation(firstApp.getId(), OperateStageType.ID,
        "DashboardTestForthEvaluation", now.minusDays(9).toDate());
    tempEntity.newPolicyViolation(forthPolicyEvaluation, policy, 1, PolicyThreatCategory.OTHER, "Group1",
        "Artifact1", "Version1");

    //most recent evaluation
    PolicyEvaluation secondPolicyEvaluation = tempEntity
        .newPolicyEvaluation(secondApp.getId(), ReleaseStageType.ID,
            "DashboardTestSecondEvaluation", now.minusDays(1).toDate());
    tempEntity.newPolicyViolation(secondPolicyEvaluation, policy, 10, PolicyThreatCategory.QUALITY, "Group1",
        "Artifact1", "Version1");

    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash-waived", policy.getId(), secondApp.getId());
    tempEntity.newWaiver("hash-waived-2", policy.getId(), secondApp.getId(), "",
        Date.from(seeDate.plus(5, ChronoUnit.DAYS)));
    tempEntity.newWaiver("hash-waived-3", policy.getId(), Organization.ROOT_ORGANIZATION_ID, "",
        Date.from(seeDate.plus(6, ChronoUnit.DAYS)));
    tempEntity.newWaiver("hash-waived-3", policy.getId(), org.getId(), "",
        Date.from(seeDate.plus(7, ChronoUnit.DAYS)));
    tempEntity.newWaiver("hash-waived-4", policy.getId(), firstApp.getId(), "",
        Date.from(seeDate.plus(8, ChronoUnit.DAYS)));
    tempEntity.newWaiver("hash-waived-5", policy.getId(), repository1.getId(), "",
        Date.from(seeDate.plus(9, ChronoUnit.DAYS)));
    tempEntity.newWaiver("hash-waived-6", policy.getId(), repository2.getId(), "",
        Date.from(seeDate.plus(9, ChronoUnit.DAYS)));
    tempEntity.newWaiver(
        "hash-waived-7",
        policy.getId(),
        RepositoryContainer.REPOSITORY_CONTAINER_ID,
        "",
        Date.from(seeDate.plus(12, ChronoUnit.DAYS)),
        Lists.newArrayList(),
        POLICY_WAIVER_REASON_ACKNOWLEDGED_VIOLATION_ID);

    tempEntity.newWaivedPolicyViolation(secondPolicyEvaluation, policy, 3, PolicyThreatCategory.QUALITY,
        ComponentIdentifier.createMavenCoordinates("Group2", "Artifact2", "Version2"), "hash-waived", policyWaiver);

    Policy legacyViolationPolicy = tempEntity.newPolicy(org.getId(), "LegacyViolationTestPolicy");
    tempEntity.newLegacyPolicyViolation(secondPolicyEvaluation, legacyViolationPolicy,
        ComponentIdentifier.createMavenCoordinates("Group3", "ArtifactLegacyViolation", "Version3"),
        "hash-legacy");
  }

  @Before
  public void before() {
    setupData();
    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.waitUntilSpinnersGone();
  }

  @After
  public void after() {
    setNeedsAcknowledgementOfInitialDashboardFilter(null);
    clearFilters();
  }

  public void clearFilters() {
    dashboardFilterDAO.deleteByUsernameAndRealmId(User.ADMIN_USERNAME, InternalRealm.ID);
  }

  /**
   * Age only applies to violations tab and defaults to 'past 30 days'.
   */
  @Test
  public void testAgeFilter() {
    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();
    AgeFilter ageFilter = DashboardFilters.ageFilter();
    ageFilter.shouldBe(hidden);

    refreshOrOpen(DashboardPage.urlToComponents());
    DashboardPage.waitUntilSpinnersGone();
    ageFilter.shouldBe(hidden);

    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    ageFilter.shouldBe(hidden);

    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.waitUntilSpinnersGone();
    ageFilter.shouldBe(visible).shouldHave(text("past 30 days"));
    ageFilter.twisty().click();
    ageFilter.singleSelectList().shouldHave(size(6)).shouldHave(
        texts("past 24 hours", "past 7 days", "past 30 days", "past 90 days", "past 12 months", "all time"));
    ageFilter.past30days().shouldBe(selected);
    ageFilter.past90days().shouldNotBe(selected).click();
    ageFilter.past90days().shouldBe(selected);

    // make sure the results are updated
    DashboardFilters.apply();
    ageFilter.singleSelectList().shouldHave(size(6));

    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.violationsView().results().violations().shouldHave(size(3));
    DashboardPage.expandFilter();
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
  public void testNLevelHierarchy() {
    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();
    NxTreeViewMultiSelect appFilter = DashboardFilters.applicationFilter();
    NxTreeViewMultiSelect orgFilter = DashboardFilters.organizationFilter();
    appFilter.twisty().click();
    orgFilter.twisty().click();

    orgFilter.checkboxItem("Level 1 Org").shouldNotBe(selected);
    orgFilter.checkboxItem("Level 2 Org").shouldNotBe(selected);
    orgFilter.checkboxItem("Level 3 Org").shouldNotBe(selected);
    orgFilter.checkboxItem("Level 4 Org").shouldNotBe(selected);
    orgFilter.checkboxItem("Level 5 Org").shouldNotBe(selected);

    appFilter.checkboxItem("Level 1 App").shouldNotBe(selected);
    appFilter.checkboxItem("Level 2 App").shouldNotBe(selected);
    appFilter.checkboxItem("Level 3 App").shouldNotBe(selected);
    appFilter.checkboxItem("Level 4 App").shouldNotBe(selected);
    appFilter.checkboxItem("Level 5 App").shouldNotBe(selected);

    orgFilter.checkboxItem("Level 1 Org").click();

    orgFilter.checkboxItem("Level 1 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 2 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 3 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 4 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 5 Org").shouldBe(selected);

    appFilter.checkboxItem("Level 1 App").shouldBe(selected);
    appFilter.checkboxItem("Level 2 App").shouldBe(selected);
    appFilter.checkboxItem("Level 3 App").shouldBe(selected);
    appFilter.checkboxItem("Level 4 App").shouldBe(selected);
    appFilter.checkboxItem("Level 5 App").shouldBe(selected);

    orgFilter.checkboxItem("Level 3 Org").click();

    orgFilter.checkboxItem("Level 1 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 2 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 3 Org").shouldNotBe(selected);
    orgFilter.checkboxItem("Level 4 Org").shouldNotBe(selected);
    orgFilter.checkboxItem("Level 5 Org").shouldNotBe(selected);

    appFilter.checkboxItem("Level 1 App").shouldBe(selected);
    appFilter.checkboxItem("Level 2 App").shouldBe(selected);
    appFilter.checkboxItem("Level 3 App").shouldNotBe(selected);
    appFilter.checkboxItem("Level 4 App").shouldNotBe(selected);
    appFilter.checkboxItem("Level 5 App").shouldNotBe(selected);

    orgFilter.checkboxItem("Level 3 Org").click();

    orgFilter.checkboxItem("Level 1 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 2 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 3 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 4 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 5 Org").shouldBe(selected);

    appFilter.checkboxItem("Level 1 App").shouldBe(selected);
    appFilter.checkboxItem("Level 2 App").shouldBe(selected);
    appFilter.checkboxItem("Level 3 App").shouldBe(selected);
    appFilter.checkboxItem("Level 4 App").shouldBe(selected);
    appFilter.checkboxItem("Level 5 App").shouldBe(selected);

    appFilter.checkboxItem("Level 4 App").click();

    orgFilter.checkboxItem("Level 1 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 2 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 3 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 4 Org").shouldNotBe(selected);
    orgFilter.checkboxItem("Level 5 Org").shouldBe(selected);

    appFilter.checkboxItem("Level 1 App").shouldBe(selected);
    appFilter.checkboxItem("Level 2 App").shouldBe(selected);
    appFilter.checkboxItem("Level 3 App").shouldBe(selected);
    appFilter.checkboxItem("Level 4 App").shouldNotBe(selected);
    appFilter.checkboxItem("Level 5 App").shouldBe(selected);

    appFilter.checkboxItem("Level 4 App").click();

    orgFilter.checkboxItem("Level 1 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 2 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 3 Org").shouldBe(selected);
    orgFilter.checkboxItem("Level 4 Org").shouldNotBe(selected);
    orgFilter.checkboxItem("Level 5 Org").shouldBe(selected);

    appFilter.checkboxItem("Level 1 App").shouldBe(selected);
    appFilter.checkboxItem("Level 2 App").shouldBe(selected);
    appFilter.checkboxItem("Level 3 App").shouldBe(selected);
    appFilter.checkboxItem("Level 4 App").shouldBe(selected);
    appFilter.checkboxItem("Level 5 App").shouldBe(selected);
  }

  /**
   * Expiration date filter only appears in waivers tab and defaults to 'all'.
   */
  @Test
  public void testExpirationDateFilter() {
    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();
    ExpirationDateFilter expirationDateFilter = DashboardFilters.expirationDateFilter();
    expirationDateFilter.shouldBe(hidden);

    refreshOrOpen(DashboardPage.urlToComponents());
    DashboardPage.waitUntilSpinnersGone();
    expirationDateFilter.shouldBe(hidden);

    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.waitUntilSpinnersGone();
    expirationDateFilter.shouldBe(hidden);

    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    expirationDateFilter.shouldBe(visible).shouldHave(text("all"));
    expirationDateFilter.twisty().click();
    expirationDateFilter.singleSelectList().shouldHave(size(8)).shouldHave(
        texts("all", "auto", "in 24 hours", "in 7 days", "in 30 days", "in 90 days", "in over 90 days", "never"));
    expirationDateFilter.all().shouldBe(selected);
    expirationDateFilter.in24hours().shouldNotBe(selected).click();
    expirationDateFilter.in24hours().shouldBe(selected);
    expirationDateFilter.in7days().shouldNotBe(selected).click();
    expirationDateFilter.in7days().shouldBe(selected);
    expirationDateFilter.in30days().shouldNotBe(selected).click();
    expirationDateFilter.in30days().shouldBe(selected);
    expirationDateFilter.in90days().shouldNotBe(selected).click();
    expirationDateFilter.in90days().shouldBe(selected);
    expirationDateFilter.inOver90days().shouldNotBe(selected).click();
    expirationDateFilter.inOver90days().shouldBe(selected);
    expirationDateFilter.never().shouldNotBe(selected).click();
    expirationDateFilter.never().shouldBe(selected);

    // check waiver results change
    DashboardFilters.apply();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.waiversView().results().waivers().shouldHave(size(7));

    expirationDateFilter.in30days().shouldNotBe(selected).click();
    DashboardFilters.apply();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.waiversView().results().waivers().shouldHave(size(4));

    expirationDateFilter.all().shouldNotBe(selected).click();
    DashboardFilters.apply();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.waiversView().results().waivers().shouldHave(size(8));

    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();
    expirationDateFilter.twisty().click();
    expirationDateFilter.in30days().shouldNotBe(selected).click();

    // check that revert button restores previously applied value
    DashboardFilters.revertButton().shouldNotBe(DISABLED).click();
    expirationDateFilter.all().shouldBe(selected);

    DashboardFilters.saveButton();
    expirationDateFilter.twisty().click();
    expirationDateFilter.singleSelectList().forEach(selenideElement -> selenideElement.shouldBe(hidden));
  }

  @Test
  public void testFiltersWithNoPermissions() {
    createUser();
    logout();
    login();
    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.expandFilter();
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
  public void testFilters_defaultState() {
    DashboardPage.expandFilter();
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    assertDefaultFilterState();
    assertAgeFilterDefaultState();

    // assert no filtering is done
    DashboardPage.violationsView().results().violations().shouldHave(size(3));

    DashboardFilters.applyButton().shouldBe(DISABLED).hover();
    Tooltip.get().shouldHave(NO_CHANGES_MESSAGE);
    DashboardPage.violationsView().results().mask().shouldBe(hidden);
  }

  @Test
  public void testFilters_updateCountersWhenValuesAreSet() {
    DashboardPage.expandFilter();

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
  }

  @Test
  public void testFilters_shouldRevertFilters() {
    DashboardPage.expandFilter();
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();

    // first set some filters and assert them before revert
    setSomeFilterValues();
    assertNewCounterState();

    DashboardFilters.revertButton().click();

    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    assertDefaultFilterState();
    DashboardPage.violationsView().results().mask().shouldBe(hidden);
  }

  @Test
  public void testFilters_shouldPersistFilterChanges() {
    DashboardPage.expandFilter();

    setSomeFilterValues();
    DashboardFilters.apply();
    DashboardPage.violationsView().results().mask().shouldBe(hidden);

    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();
    assertNewCounterState();
    DashboardPage.violationsView().results().mask().shouldBe(hidden);

    // assert stored filter
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filter = dashboardFilterDAO
        .getByUsernameAndRealmId("admin", InternalRealm.ID);

    assertThat(filter.get(0).getFilter().replace("\r\n", "\n"))
        .isEqualTo("{\n" +
            "  \"minPolicyThreatLevel\" : 2,\n" +
            "  \"maxPolicyThreatLevel\" : 7,\n" +
            "  \"applicationFilters\" : [ \"" + firstApp.getId() + "\" ],\n" +
            "  \"organizationFilters\" : [ ],\n" +
            "  \"tagFilters\" : [ \"" + firstAppCategory1.getId() + "\" ],\n" +
            "  \"policyThreatCategoryFilters\" : [ \"QUALITY\" ],\n" +
            "  \"stageTypeFilters\" : [ \"release\" ],\n" +
            "  \"maxDaysOld\" : 30,\n" +
            "  \"policyViolationStates\" : [ \"OPEN\", \"WAIVED\", \"LEGACY_VIOLATION\" ],\n" +
            "  \"expirationDate\" : \"ALL\",\n" +
            "  \"repositoryFilters\" : [ ],\n" +
            "  \"policyWaiverReasonIds\" : [ ]\n" +
            "}");

    // assert applied filters
    DashboardPage.violationsView().results().violations().shouldHave(size(1));
    ViolationTile violation = DashboardPage.violationsView().results().firstViolation();
    violation.threatNumber().shouldHave(text("2"));
    violation.policy().shouldHave(text("DashboardTestPolicy"));
    violation.application().shouldHave(text("DashboardTestAppOne"));
  }

  @Test
  public void testFilters_shouldPersistWaiverReasonFilterChanges() {
    // navigate to the policy violation page
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();

    // make sure we expand and check the acknowledged violation waiver reason under waiver reasons
    DashboardPage.expandFilter();
    DashboardFilters.iqPolicyWaiverReasonFilter().click();
    DashboardFilters.iqPolicyWaiverReasonFilter().checkboxItem(2).click();

    // apply the filters
    DashboardFilters.apply();
    DashboardPage.waiversView().mask().shouldBe(hidden);

    // assert stored filter
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filter = dashboardFilterDAO
        .getByUsernameAndRealmId("admin", InternalRealm.ID);

    assertThat(filter.get(0).getFilter().replace("\r\n", "\n"))
        .isEqualTo("{\n" +
            "  \"minPolicyThreatLevel\" : 2,\n" +
            "  \"maxPolicyThreatLevel\" : 10,\n" +
            "  \"applicationFilters\" : [ ],\n" +
            "  \"organizationFilters\" : [ ],\n" +
            "  \"tagFilters\" : [ ],\n" +
            "  \"policyThreatCategoryFilters\" : [ ],\n" +
            "  \"stageTypeFilters\" : [ ],\n" +
            "  \"maxDaysOld\" : 30,\n" +
            "  \"policyViolationStates\" : [ \"OPEN\" ],\n" +
            "  \"expirationDate\" : \"ALL\",\n" +
            "  \"repositoryFilters\" : [ ],\n" +
            "  \"policyWaiverReasonIds\" : [ \"" + POLICY_WAIVER_REASON_ACKNOWLEDGED_VIOLATION_ID + "\" ]\n" +
            "}");

    // refresh the page and wait for it to fully re-load
    refresh();
    DashboardPage.dashboardContainer().shouldBe(visible);
    DashboardPage.waitUntilSpinnersGone();

    // re-expand the filter and make sure it's still checked
    DashboardPage.expandFilter();
    DashboardFilters.iqPolicyWaiverReasonFilter().click();
    DashboardFilters.iqPolicyWaiverReasonFilter().checkboxItem(2).input().shouldBe(checked);

    // should have filtered down to one waiver which has this reason set
    DashboardPage.waiversView().results().waivers().shouldHave(size(1));
    final var waiver = DashboardPage.waiversView().results().firstWaiver();
    waiver.threatNumber().shouldHave(text("5"));
  }

  @Test
  public void testFilters_shouldShowTooltipsWhenHoveringOnAppCategoryFilter() {
    DashboardPage.expandFilter();
    DashboardFilters.applicationCategoryFilter().twisty().click();

    DashboardFilters.applicationCategoryFilter().getFilterCheckboxAt(0).hover();
    Tooltip.get().shouldNotBe(visible);

    DashboardFilters.applicationCategoryFilter().noCategory().hover();
    Tooltip.get().shouldNotBe(visible);

    DashboardFilters.applicationCategoryFilter().getFilterCheckboxAt(2).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("in DashboardTest"));

    DashboardFilters.applicationCategoryFilter().getFilterCheckboxAt(3).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("in DashboardTest"));
  }

  @Test
  public void testFilters_shouldFilterUncategorizedApplications() {
    DashboardPage.expandFilter();
    DashboardFilters.applicationCategoryFilter().twisty().click();

    // select no category option
    DashboardFilters.applicationCategoryFilter().noCategory().click();
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.apply();

    DashboardPage.violationsView().results().violations().shouldHave(size(1));
    ViolationTile firstViolation = DashboardPage.violationsView().results().firstViolation();
    firstViolation.threatNumber().shouldHave(text("10"));
    firstViolation.policy().shouldHave(text("DashboardTestPolicy"));
    firstViolation.application().shouldHave(text("DashboardTestAppTwo"));
    firstViolation.component().shouldHave(text("Group1 : Artifact1 : Version1"));

    // check component tab
    DashboardPage.componentsTab().click();
    DashboardPage.componentsView().results().components().shouldHave(size(1));

    // check application tab
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHave(size(1));

    // check waivers tab
    DashboardPage.waiversTab().click();
    DashboardPage.waiversView().results().waivers().shouldHave(size(4));
  }

  @Test
  public void testFilters_shouldFilterByApplication() {
    DashboardPage.expandFilter();

    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.apply();

    ViolationTile violation = DashboardPage.violationsView().results().firstViolation();
    violation.threatNumber().shouldHave(text("2"));
    violation.policy().shouldHave(text("DashboardTestPolicy"));
    violation.application().shouldHave(text("DashboardTestAppOne"));
  }

  @Test
  public void testFilters_filterByPolicyViolations() {
    DashboardPage.expandFilter();

    // this sets some other filters - should help testing feature with multiple other filters
    setSomeFilterValues();

    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().allItems().shouldBe(selected).click();
    DashboardFilters.policyViolationStateFilter().twisty().click();

    DashboardFilters.apply();

    DashboardPage.violationsTab().click();
    DashboardPage.violationsView().results().violations().shouldHave(size(1));
    DashboardPage.violationsView().results().firstViolation().component()
        .shouldHave(text("Group1 : Artifact1 : Version1"));

    DashboardPage.componentsTab().click();
    DashboardPage.componentsView().results().components().shouldHave(size(1));

    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHave(size(1));
    DashboardPage.componentsTab().click();
  }

  @Test
  public void testFilters_shouldFilterWaivedPolicyViolations() {
    DashboardPage.expandFilter();

    // this sets some other filters - should help testing feature with multiple other filters
    setSomeFilterValues();

    // filter WAIVED only
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().allItems().click();
    DashboardFilters.policyViolationStateFilter().waived().click();
    DashboardFilters.policyViolationStateFilter().twisty().click();

    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().checkboxItem(1).click();
    DashboardFilters.applicationFilter().twisty().click();

    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.applicationCategoryFilter().checkboxItem(1).click();

    DashboardFilters.apply();
    DashboardPage.componentsView().resultsMask().shouldBe(hidden);

    // components tab should have only waived component
    DashboardPage.componentsTab().click();
    DashboardPage.componentsView().results().components().shouldHave(size(1));
    DashboardPage.componentsView().results().firstComponent().shouldHave(text("Artifact2"));

    // violations tab should have only waived violation
    DashboardPage.violationsTab().click();
    DashboardPage.violationsView().results().violations().shouldHave(size(1));
    ViolationTile waivedViolation = DashboardPage.violationsView().results().firstViolation();
    waivedViolation.component().shouldHave(text("Artifact2"));

    // applications tab should have only waived violation app
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHave(size(1));
    DashboardPage.applicationsView().results().firstApplication().shouldHave(text("DashboardTestAppTwo"));
  }

  @Test
  public void testFilters_shouldFilterLegacyViolations() {
    DashboardPage.componentsTab().click();
    DashboardPage.expandFilter();

    selectDefaultFilter();
    // wait until the drawer is fully closed before proceeding to the next step
    DashboardPage.waitForDrawerAnimation();

    DashboardPage.expandFilter();

    // filter Legacy Violation only
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyViolationStateFilter().open().click();
    DashboardFilters.policyViolationStateFilter().legacyViolation().click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardPage.componentsView().resultsMask().shouldBe(visible);

    DashboardFilters.apply();
    DashboardPage.componentsView().resultsMask().shouldBe(hidden);

    // components tab should have only legacy component
    DashboardPage.componentsView().results().components().shouldHave(size(1));
    DashboardPage.componentsView().results().firstComponent().shouldHave(text("ArtifactLegacyViolation"));

    // violations tab should have only legacy violation
    DashboardPage.violationsTab().click();
    DashboardPage.violationsView().results().violations().shouldHave(size(1));
    ViolationTile legacyViolation = DashboardPage.violationsView().results().firstViolation();
    legacyViolation.component().shouldHave(text("ArtifactLegacyViolation"));

    // applications tab should have only legacy violation app
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHave(size(1));
    DashboardPage.applicationsView().results().firstApplication().shouldHave(text("DashboardTestAppTwo"));
  }

  @Test
  public void testFilterSingleAppWithMultipleCategories() {
    DashboardPage.expandFilter();
    CategoryFilter categoryFilter = DashboardFilters.applicationCategoryFilter();
    categoryFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("3"));
    categoryFilter.twisty().shouldBe(visible).click();
    categoryFilter.multiSelectList().shouldHave(size(4));
    categoryFilter.checkboxItem(3).shouldNotBe(selected).click();
    categoryFilter.checkboxItem(4).shouldNotBe(selected).click();
    categoryFilter.counter().shouldBe(ACTIVE).shouldHave(text("2 of 3"));

    DashboardFilters.apply();
    DashboardPage.applicationsTab().click();
    DashboardPage.applicationsView().results().applications().shouldHave(size(1));
  }

  @Test
  public void testFilterOutAllResults() {
    DashboardPage.expandFilter();
    // filter only for security policy type
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.policyTypeFilter().security().click();
    DashboardFilters.policyTypeFilter().twisty().click();
    DashboardFilters.apply();
    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();
    // verify policy filter counter
    DashboardFilters.policyTypeFilter().counter().shouldBe(ACTIVE).shouldHave(text("1 of 4"));
    // verify no violations row shown
    DashboardPage.violationsView().results().violations().shouldHave(size(0));
    // verify no data message
    DashboardPage.violationsView().results().noDataMessage().shouldBe(visible)
        .shouldHave(text("No data available in the last 30 days given the applied filters and permissions."));
  }

  @Test
  public void testApplyChangesToDefaultFilter() {
    DashboardPage.expandFilter();
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);

    setSomeFilterValues();
    DashboardFilters.apply();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);

    // check that the 'dirty' asterisk remains after reload
    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);
  }

  @Test
  public void testSaveInitialFilter() {
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(hidden);
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default")).click();
    // no saved filters
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    manage.dropdownMenu().options().shouldHave(size(1));
    manage.dropdownMenu().defaultFilterOption().shouldHave(text("Default"));
    manage.dropdownMenu().emptyListMessage().shouldBe(visible).shouldHave(text("No saved filters"));

    manage.openMenuButton().click();

    manage.dropdownMenu().shouldNotBe(visible);

    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);

    // save initial filter
    saveFilter("Initial", null, false, true);

    manage.openMenuButton().click();
    manage.dropdownMenu().options().shouldHave(size(2));
    manage.dropdownMenu().option(1).shouldHave(text("Initial"));

    eyesWatcher.eyesCheck("Initial filter saved");
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
  }

  @Test
  public void testOverwriteFilter() {
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default")).click();
    saveFilter("Initial", null, false, true);
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    overwriteFilter("Initial");
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
  }

  @Test
  public void testSaveFilterWithUnsavedChanges() {
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default")).click();
    setSomeFilterValues();
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
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

    DashboardFilters.apply();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);
    DashboardFilters.saveButton().shouldNotBe(DISABLED);

    DashboardFilters.closeButton().shouldNotBe(DISABLED).click();

    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default"));
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(visible);

    // check that the 'dirty' asterisk remains after reload
    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(visible);
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default")).click();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);
  }

  @Test
  public void testLoadSavedFilter() {
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default")).click();
    saveFilter("Initial", null, false, true);
    setSomeFilterValues();
    DashboardFilters.apply();
    saveFilter("New Filter", "Initial", false, false);
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("New Filter"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);

    manage.openMenuButton().click();
    manage.dropdownMenu().options().shouldHave(size(3));
    manage.dropdownMenu().option(2).shouldHave(text("New Filter"));

    DashboardFilters.closeButton().click();
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("New Filter"));
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(hidden);

    // load other filter
    ViolationsResults table = DashboardPage.violationsView().results();
    table.violations().shouldHave(size(1));
    DashboardPage.expandFilter();
    manage.openMenuButton().click();
    manage.dropdownMenu().option(1).selectFilterButton().click();
    manage.dropdownMenu().shouldBe(hidden);

    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Initial"));
    DashboardPage.expandFilter();
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    assertDefaultFilterState();
    table.violations().shouldHave(size(3));
    DashboardFilters.closeButton().click();
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Initial"));
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(hidden);

    // check that refreshing doesn't change anything
    refresh();
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.filterToggleDirtyAsterisk().shouldBe(hidden);
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Initial")).click();
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
  }

  @Test
  public void testSaveFilterWithExistingName() {
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default")).click();
    saveFilter("Initial", null, false, true);
    saveFilter("New Filter", "Initial", false, false);
    setSomeFilterValues();
    DashboardFilters.apply();
    saveFilter("Initial", "New Filter", true, true);
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Initial"));
    manage.selectedFilterDirtyAsterisk().shouldBe(hidden);
    ViolationsResults table = DashboardPage.violationsView().results();
    table.violations().shouldHave(size(1));
  }

  @Test
  public void testSaveFilterWithDefaultName() {
    DashboardPage.filterToggle().shouldBe(visible).shouldHave(text("Default")).click();
    setSomeFilterValues();
    DashboardFilters.apply();
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);
    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    SaveFilterDialog saveDialog = DashboardFilters.saveFilterDialog();
    saveDialog.shouldBe(visible).saveAsRadio().click();
    saveDialog.nameInput().val("Default");
    saveDialog.saveButton().click();
    FormUtils.getAlertElement(saveDialog)
        .shouldHave(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Can not overwrite Default filter"));
  }

  @Test
  public void testDeleteSavedFilter() {
    DashboardPage.expandFilter();
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
    DashboardPage.expandFilter();
    manage.openMenuButton().click();
    manage.dropdownMenu().shouldBe(visible);
    manage.dropdownMenu().options().shouldHave(size(3));
    manage.dropdownMenu().option(2).shouldHave(text("Do not delete")).shouldBe(SELECTED_SAVED_FILTER_OPTION);
    manage.dropdownMenu().option(1).shouldHave(text("Delete")).deleteFilterButton().click();
    // delete filter button shouldn't close the filters dropdown
    manage.dropdownMenu().shouldBe(visible);
    eyesWatcher.eyesCheck();
    deleteFilterDialog.shouldBe(visible).confirmation()
        .shouldHave(exactText("You are about to delete \"Delete\" filter. This action can not be undone."));
    // document click while delete dialog is open shouldn't close the filters dropdown
    // Issue in ManageFiltersDropdown: dropdown closes when clicking Delete modal. See CLM-34681
    deleteFilterDialog.confirmation().click();
    manage.dropdownMenu().shouldNotBe(visible);
    deleteFilterDialog.cancelButton().click();

    // delete filter - continue
    manage.openMenuButton().click();
    manage.dropdownMenu().option(1).deleteFilterButton().click();
    deleteFilterDialog.shouldBe(visible);
    deleteFilterDialog.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteFilterDialog.shouldBe(hidden);
    manage.dropdownMenu().shouldNotBe(visible);

    // verify delete
    manage.openMenuButton().click();
    manage.dropdownMenu().options().shouldHave(size(2));
    manage.dropdownMenu().option(1).shouldHave(text("Do not delete"));

    DashboardFilterDAO dashboardFilterDAO = this.dashboardFilterDAO;
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

    DashboardPage.expandFilter();
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
    manage.openMenuButton().click();
    manage.selectedFilterLabel().shouldHave(text("Default"));
    manage.selectedFilterDirtyAsterisk().shouldBe(visible);
    manage.dropdownMenu().defaultFilterOption().shouldBe(SELECTED_SAVED_FILTER_OPTION);

    // verify that applied filter is no longer based on the deleted one
    DashboardFilterDAO dashboardFilterDAO = this.dashboardFilterDAO;
    DashboardFilter filter = dashboardFilterDAO.getByUsernameAndRealmIdAndName("admin", InternalRealm.ID, "");
    assertThat(filter.getBasedOnFilterName()).isNull();
  }

  @Test
  public void testNeedsAcknowledgement() {
    setNeedsAcknowledgementOfInitialDashboardFilter(true);
    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.waitUntilSpinnersGone();

    DashboardFilters.filterContainer().shouldBe(visible);

    DashboardFilters.closeButton().shouldBe(visible, DISABLED).click();
    DashboardFilters.closeButton().shouldBe(visible); // click should have no effect
    DashboardFilters.closeButton().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Please apply a filter"));

    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    manage.selectedFilterLabel().shouldHave(exactText("Default"));
    DashboardPage.needsAcknowledgementMessage().shouldBe(visible)
        .shouldHave(text(DashboardPage.NEEDS_ACKNOWLEDGEMENT_MESSAGE));
    DashboardPage.violationsView().results().violations().shouldHave(size(0));

    DashboardPage.componentsTab().click();
    DashboardPage.needsAcknowledgementMessage().shouldBe(visible)
        .shouldHave(text(DashboardPage.NEEDS_ACKNOWLEDGEMENT_MESSAGE));
    DashboardPage.componentsView().results().components().shouldHave(size(0));

    DashboardPage.applicationsTab().click();
    DashboardPage.needsAcknowledgementMessage().shouldBe(visible)
        .shouldHave(text(DashboardPage.NEEDS_ACKNOWLEDGEMENT_MESSAGE));
    DashboardPage.applicationsView().results().applications().shouldHave(size(0));

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
    DashboardPage.expandFilter();
    String filterName = "Saved Filter";
    saveFilter(filterName, null, false, false);
    DashboardFilterDAO dashboardFilterDAO = this.dashboardFilterDAO;
    List<com.sonatype.insight.brain.model.filter.DashboardFilter> filters =
        dashboardFilterDAO.getByUsernameAndRealmId("admin", InternalRealm.ID);

    assertThat(filters).hasSize(2); // one for the active and named
    assertThat(filters.get(0).isAcknowledged()).isFalse();
    assertThat(filters.get(1).isAcknowledged()).isFalse();

    setNeedsAcknowledgementOfInitialDashboardFilter(true);
    refreshOrOpen(DashboardPage.urlToViolations());
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.expandFilter();
    assertNeedsAcknowledgementPostFilterState(filterName);
  }

  @Test
  public void testOrgFilterIncludesNewApplications() {
    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    // filter by Org
    DashboardFilters.organizationFilter().twisty().click();
    DashboardFilters.organizationFilter().checkboxItem("DashboardTest").click();
    DashboardFilters.apply();

    ApplicationsResults results = DashboardPage.applicationsView().results();
    results.applications().shouldHave(size(2));

    // add new App to same Org
    Application thirdApp = tempEntity.newApplication("DashboardTestAppThree", "DashboardTestAppThree", org.getId());
    PolicyEvaluation appThreePolicyEvaluation = tempEntity.newPolicyEvaluation(thirdApp.getId(), BuildStageType.ID,
        "DashboardTestEvaluationForAppThree", DateTime.now().minusDays(1).toDate());
    tempEntity.newPolicyViolation(appThreePolicyEvaluation, policy, 5, PolicyThreatCategory.LICENSE, "Group1",
        "Artifact1", "Version1", "hash", FailActionType.ID);

    // new App should be included in results
    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.waitUntilSpinnersGone();
    results.applications().shouldHave(size(3));
    eyesWatcher.eyesCheck();

    applicationDAO.delete(thirdApp);
    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.waitUntilSpinnersGone();
    results.applications().shouldHave(size(2));
  }

  @Test
  public void testWaiverAppFilter_specificApplicationIsSelected() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    // select a specific application to filter
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().multiSelectList().shouldHave(size(8));
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.apply();

    DashboardPage.waiversView().results().waivers().shouldHave(size(3));
    List<WaiverTile> allWaivers = DashboardPage.waiversView().results().allWaivers();

    assertThat(
        containsWaiverWithScope(allWaivers, firstApp))
        .isTrue();

    assertThat(
        containsWaiverWithScope(allWaivers, secondApp))
        .as("When filtering waives for first application," +
            "It should not contain waivers on second application.")
        .isFalse();

    // check for a different application
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.applicationFilter().checkboxItem(3).click();
    DashboardFilters.apply();
    DashboardPage.waiversView().results().waivers().shouldHave(size(4));
    
    assertThat(
        containsWaiverWithScope(allWaivers, secondApp))
        .as("It should contain waivers on second application.")
        .isTrue();

    assertThat(
        containsWaiverWithScope(allWaivers, firstApp))
        .as("When filtering waivers for second application, " +
            "it should not contain waivers on first application.")
        .isFalse();
  }

  @Test
  public void testWaiverAppFilter_allOptionSelected() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    // all waivers should be listed without filter
    DashboardPage.waiversView().results().waivers().shouldHave(size(8));

    // select All applications filter option
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().checkboxItem(1).click();
    DashboardFilters.apply();

    // all the waivers should be present after filtering.
    DashboardPage.waiversView().results().waivers().shouldHave(size(5));
  }

  @Test
  public void testWaiverAppCategoryFilter_allOptionSelected() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    // all waivers should be listed without filter
    DashboardPage.waiversView().results().waivers().shouldHave(size(8));

    // select All applications category filter option
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.applicationCategoryFilter().allItems().click();
    DashboardFilters.apply();

    // all the waivers should be present after filtering.
    DashboardPage.waiversView().results().waivers().shouldHave(size(5));
  }

  @Test
  public void testWaiverAppCategoryFilter_noCategoryOptionSelected() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    // all waivers should be listed without filter
    DashboardPage.waiversView().results().waivers().shouldHave(size(8));

    // select no category filter option
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.applicationCategoryFilter().noCategory().click();
    DashboardFilters.apply();

    DashboardPage.waiversView().results().waivers().shouldHave(size(4));

    List<WaiverTile> allWaivers = DashboardPage.waiversView().results().allWaivers();
    assertThat(
        containsWaiverWithScope(allWaivers, firstApp))
        .as("It should not contain first app as it has been tagged")
        .isFalse();

    assertThat(containsWaiverWithScope(allWaivers, secondApp))
        .as("It should contain second app as it has not been tagged")
        .isTrue();
  }

  @Test
  public void testWaiverAppCategoryFilter_specificOptionSelected() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.dashboardContainer().shouldBe(visible);
    DashboardPage.expandFilter();

    // all waivers should be listed without filter
    DashboardPage.waiversView().results().waivers().shouldHave(size(8));

    // select no category filter option
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.applicationCategoryFilter().checkboxItem(3).click();
    DashboardFilters.apply();

    DashboardPage.waiversView().results().waivers().shouldHave(size(3));

    List<WaiverTile> allWaivers = DashboardPage.waiversView().results().allWaivers();
    assertThat(
        containsWaiverWithScope(allWaivers, firstApp))
        .as("It should contain first app as it has been tagged by selected category")
        .isTrue();

    assertThat(
        containsWaiverWithScope(allWaivers, rootOrg))
        .as("It should contain root org as it is the parent")
        .isTrue();

    assertThat(
        containsWaiverWithScope(allWaivers, org))
        .as("It should contain org as it is the parent")
        .isTrue();

    assertThat(containsWaiverWithScope(allWaivers, secondApp))
        .as("It should not contain second app as it has not been tagged by selected category")
        .isFalse();
  }

  @Test
  public void testWaiverAppFilter_orderEntriesByExpiryDate() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    // select a specific application to filter
    DashboardFilters.applicationFilter().twisty().click();
    DashboardFilters.applicationFilter().checkboxItem(2).click();
    DashboardFilters.apply();

    WaiverTile firstWaiver = DashboardPage.waiversView().results().firstWaiver();
    firstWaiver.scope().shouldHave(text(rootOrg.getName()));
    WaiverTile lastWaiver = DashboardPage.waiversView().results().lastWaiver();
    lastWaiver.scope().shouldHave(text(getWaiverDashboardTableScopeName(firstApp)));
  }

  @Test
  public void testWaiverAppCategoryFilter_orderEntriesByExpiryDate() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    // all waivers should be listed without filter
    DashboardPage.waiversView().results().waivers().shouldHave(size(8));

    // assert that waivers are by default sorted by expiry date
    WaiverTile firstWaiver = DashboardPage.waiversView().results().firstWaiver();
    firstWaiver.scope().shouldHave(text(getWaiverDashboardTableScopeName(secondApp)));

    WaiverTile secondWaiver = DashboardPage.waiversView().results().waiver(1);
    secondWaiver.scope().shouldHave(text(getWaiverDashboardTableScopeName(rootOrg)));

    WaiverTile lastWaiver = DashboardPage.waiversView().results().lastWaiver();
    lastWaiver.scope().shouldHave(text(getWaiverDashboardTableScopeName(secondApp)));

    // filter by a category
    DashboardFilters.applicationCategoryFilter().twisty().click();
    DashboardFilters.applicationCategoryFilter().checkboxItem(3).click();
    DashboardFilters.apply();

    DashboardPage.waiversView().results().waivers().shouldHave(size(3));

    // assert items order after filtering.
    firstWaiver = DashboardPage.waiversView().results().firstWaiver();
    firstWaiver.scope().shouldHave(text(rootOrg.getName()));
    lastWaiver = DashboardPage.waiversView().results().lastWaiver();
    lastWaiver.scope().shouldHave(text(getWaiverDashboardTableScopeName(firstApp)));
  }

  @Test
  public void testWaiverOrgFilter_allOptionSelected() {
    testWaiverOrgFilter(null, 6);
  }

  @Test
  public void testWaiverOrgFilter_childOrgSelected() {
    testWaiverOrgFilter("DashboardTest", 6);
  }

  @Test
  public void testWaiverOrgFilter_parentOrgSelected() {
    testWaiverOrgFilter("ParentOrgTest", 6);
  }

  private void testWaiverOrgFilter(String orgName, int waiverAmountExpectation) {
    tempEntity.newWaiver(policy.getId(), parentOrg.getId());

    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    // all waivers should be listed without filter
    DashboardPage.waiversView().results().waivers().shouldHave(size(9));

    // select parent organizations filter option
    DashboardFilters.organizationFilter().twisty().click();
    if (orgName == null) {
      DashboardFilters.organizationFilter().allItems().click();
    }
    else {
      DashboardFilters.organizationFilter().checkboxItem(orgName).click();
    }
    DashboardFilters.apply();

    // all the waivers should be present after filtering.
    DashboardPage.waiversView().results().waivers().shouldHave(size(waiverAmountExpectation));
  }

  private boolean containsWaiverWithScope(List<WaiverTile> waivers, Owner owner) {
    if (CollectionUtils.isEmpty(waivers)) {
      return false;
    }

    Predicate<WaiverTile> ownerPredicate = waiverTile -> waiverTile.scope()
        .getText()
        .toLowerCase()
        .contains(getWaiverDashboardTableScopeName(owner));

    // root org and repository container display name is not lower cased
    Predicate<WaiverTile> rootOrgOrRepoContainerPredicate = waiverTile -> waiverTile.scope()
        .getText()
        .contains(getWaiverDashboardTableScopeName(owner));

    if (isOwnerRootOrgOrRepoContainer(owner)) {
      return waivers.stream().anyMatch(rootOrgOrRepoContainerPredicate);
    }

    return waivers.stream().anyMatch(ownerPredicate);
  }

  private String getWaiverDashboardTableScopeName(Owner owner) {
    if (isOwnerRootOrgOrRepoContainer(owner)) {
      return owner.getName();
    }
    return owner.getType().toString() + " - " + owner.getName().toLowerCase();
  }

  private boolean isOwnerRootOrgOrRepoContainer(final Owner owner) {
    return Objects.equals(owner.getId(), Organization.ROOT_ORGANIZATION_ID) ||
        Objects.equals(owner.getId(), RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testWaiverReposFilterIncludesRootOrgAndAllRepositories() {
    refreshOrOpen(DashboardPage.urlToWaivers());
    DashboardPage.waitUntilSpinnersGone();
    DashboardPage.expandFilter();

    DashboardPage.waiversView().results().waivers().shouldHave(size(8));

    WaiverTile firstWaiver = DashboardPage.waiversView().results().firstWaiver();
    firstWaiver.scope().shouldHave(text(secondApp.getType().toString() + " - " + secondApp.getName()));
    WaiverTile lastWaiver = DashboardPage.waiversView().results().lastWaiver();
    lastWaiver.scope().shouldHave(text(secondApp.getType().toString() + " - " + secondApp.getName()));

    DashboardFilters.repositoryFilter().twisty().click();
    DashboardFilters.repositoryFilter().allItems().click();
    DashboardFilters.apply();

    DashboardPage.waiversView().results().waivers().shouldHave(size(4));
    Owner rootOrgAsOwner = orgDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    List<WaiverTile> waiverTiles = DashboardPage.waiversView().results().allWaivers();
    assertThat(containsWaiverWithScope(waiverTiles, rootOrgAsOwner)).isTrue();
    assertThat(containsWaiverWithScope(waiverTiles, repository1)).isTrue();
    assertThat(containsWaiverWithScope(waiverTiles, repository2)).isTrue();
    assertThat(containsWaiverWithScope(waiverTiles, RepositoryContainer.SINGLETON)).isTrue();

    DashboardFilters.repositoryFilter().allItems().click();
    DashboardFilters.repositoryFilter().checkboxItem(2).click();
    DashboardFilters.apply();

    DashboardPage.waiversView().results().waivers().shouldHave(size(3));
    waiverTiles = DashboardPage.waiversView().results().allWaivers();
    assertThat(containsWaiverWithScope(waiverTiles, rootOrgAsOwner)).isTrue();
    assertThat(containsWaiverWithScope(waiverTiles, repository1)).isTrue();
    assertThat(containsWaiverWithScope(waiverTiles, repository2)).isFalse();
    assertThat(containsWaiverWithScope(waiverTiles, RepositoryContainer.SINGLETON)).isTrue();

    DashboardFilters.repositoryFilter().checkboxItem(2).click();
    DashboardFilters.repositoryFilter().checkboxItem(3).click();
    DashboardFilters.apply();
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.waiversView().results().waivers().shouldHave(size(3));
    waiverTiles = DashboardPage.waiversView().results().allWaivers();
    assertThat(containsWaiverWithScope(waiverTiles, rootOrgAsOwner)).isTrue();
    assertThat(containsWaiverWithScope(waiverTiles, repository1)).isFalse();
    assertThat(containsWaiverWithScope(waiverTiles, repository2)).isTrue();
    assertThat(containsWaiverWithScope(waiverTiles, RepositoryContainer.SINGLETON)).isTrue();
  }

  @Test
  public void testNoResultsShownForEmptyOrg() {
    DashboardPage.expandFilter();
    // filter by Empty Org
    DashboardFilters.organizationFilter().twisty().click();
    DashboardFilters.organizationFilter().checkboxItem(3).click();
    DashboardFilters.apply();

    ViolationsResults violationsResults = DashboardPage.violationsView().results();
    violationsResults.violations().shouldHave(size(0));
    violationsResults.noDataMessage().shouldBe(visible);

    refreshOrOpen(DashboardPage.urlToApplications());
    DashboardPage.waitUntilSpinnersGone();
    ApplicationsResults applicationsResults = DashboardPage.applicationsView().results();
    applicationsResults.applications().shouldHave(size(0));
    applicationsResults.noDataMessage().shouldBe(visible);

    refreshOrOpen(DashboardPage.urlToComponents());
    DashboardPage.waitUntilSpinnersGone();
    ComponentsResults componentsResults = DashboardPage.componentsView().results();
    componentsResults.components().shouldHave(size(0));
    componentsResults.noDataMessage().shouldBe(visible);
  }

  // Due to issues with ManageFiltersDropdown, this test is disabled. See CLM-34681
  @Ignore
  @Test
  public void testEscEvents() {
    // filter container is open
    DashboardPage.expandFilter();
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // filters are dirty - Esc should not close filterContainer
    DashboardPage.expandFilter();
    setSomeFilterValues();
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldBe(visible);
    DashboardFilters.revertButton().click();
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    DashboardPage.expandFilter();
    DashboardFilters.manageFiltersDropdown().openMenuButton().click();
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldBe(visible);
    pressEscape();
    DashboardFilters.manageFiltersDropdown().dropdownMenu().shouldNotBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // filter container, dropdown and delete modal are open
    DashboardPage.expandFilter();
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
    DashboardPage.expandFilter();
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
    DashboardPage.expandFilter();
    DashboardFilters.saveButton().shouldNotBe(disabled).click();
    SaveFilterDialog saveDialog = DashboardFilters.saveFilterDialog();
    saveDialog.shouldBe(visible);
    pressEscape();
    saveDialog.shouldNotBe(visible);
    DashboardFilters.filterContainer().shouldBe(visible);
    pressEscape();
    DashboardFilters.filterContainer().shouldNotBe(visible);
  }

  // Due to issues with ManageFiltersDropdown, this test is disabled. See CLM-34681
  @Ignore
  @Test
  public void testOffClickEvents() {
    // clicking within filter panel should not close it
    DashboardPage.expandFilter();
    DashboardFilters.applyButton().shouldBe(visible).click();
    DashboardFilters.filterContainer().shouldBe(visible);

    // clicking outside of filter panel should close it
    DashboardPage.violationsTab().click();
    DashboardFilters.filterContainer().shouldNotBe(visible);

    // clicking within filters dropdown should not close it
    ManageFiltersDropdown manage = DashboardFilters.manageFiltersDropdown();
    DashboardPage.expandFilter();
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
    DashboardPage.expandFilter();
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

    DashboardFilters.filterContainer().shouldNotBe(visible);
    DashboardPage.expandFilter();
    manage.openMenuButton().shouldBe(visible).click();
    manage.dropdownMenu().shouldBe(visible);
    manage.dropdownMenu().option(1).deleteFilterButton().shouldBe(visible).click();

    DashboardFilters.deleteFilterDialog().continueButton().click();
    manage.dropdownMenu().shouldBe(visible);
  }

  @Test
  public void testDisplayModalOverFilter() {
    DashboardPage.expandFilter();
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
    DashboardPage.violationsView().results().violations().shouldHave(size(3));
    ViolationTile violation = DashboardPage.violationsView().results().firstViolation();
    violation.threatNumber().shouldHave(text("10"));
    violation.policy().shouldHave(text("DashboardTestPolicy"));
    violation.application().shouldHave(text("DashboardTestAppTwo"));

    DashboardPage.componentsTab().click();
    DashboardPage.needsAcknowledgementMessage().shouldBe(hidden);
    DashboardPage.componentsView().results().components().shouldHave(size(1));
    DashboardPage.componentsView().results().firstComponent().shouldHave(text("Artifact1"));

    DashboardPage.applicationsTab().click();
    DashboardPage.needsAcknowledgementMessage().shouldBe(hidden);
    DashboardPage.applicationsView().results().applications().shouldHave(size(2));
    DashboardPage.applicationsView().results().firstApplication().shouldHave(text("DashboardTestAppTwo"));

    // go back to violations so refreshOrOpen calls work properly
    DashboardPage.violationsTab().click();
  }

  /**
   * Save the filter under the given name. Executes and asserts the "Save As" workflow of the Save Filter modal. Does
   * not test the "Overwrite" workflow
   *
   * @param existingExpected Is there expected to be an existing filter with a matching name
   * @param useVisualTesting determines whether or not to visually validate when saving filters
   */
  private void saveFilter(
      String filterName,
      String existingFilterName,
      boolean existingExpected,
      boolean useVisualTesting)
  {
    DashboardFilters.saveButton().shouldNotBe(disabled).scrollTo().click();
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

    saveDialog.saveButton().shouldHave(text("Save"));
    saveDialog.overwriteRadio().shouldNotBe(selected);
    saveDialog.saveAsRadio().shouldBe(selected);
    saveDialog.nameInput().shouldBe(empty).shouldBe(visible);

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
    // Verify if the filter was closed and opened again. This is due to a race condition when clicking the save button
    if (!DashboardFilters.filterContainer().is(visible)) {
      DashboardPage.expandFilter();
      DashboardFilters.filterContainer().shouldBe(visible);
    }
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
    DashboardFilters.policyViolationStateFilter().legacyViolation().click();
    DashboardFilters.policyViolationStateFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().twisty().click();
    DashboardFilters.policyThreatLevelFilter().slider().setValues(2, 7);
    DashboardFilters.policyThreatLevelFilter().twisty().click();
  }

  private void assertDefaultFilterState() {
    NxTreeViewMultiSelect appFilter = DashboardFilters.applicationFilter();

    appFilter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("7"));
    appFilter.multiSelectList().filter(visible).shouldBe(CollectionCondition.empty);
    appFilter.twisty().shouldBe(visible).click();
    appFilter.multiSelectList().filter(visible).shouldHave(size(8));
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
    ageFilter.singleSelectList().shouldHave(size(6));
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
    policyViolationStateFilter.legacyViolation().shouldNotBe(selected).label().shouldHave(text("Legacy"));
    policyViolationStateFilter.twisty().click();
  }

  private void assertNewCounterState() {
    DashboardFilters.applicationFilter().counter().shouldHave(cssClass("nx-counter--active"))
        .shouldHave(text("1 of 7"));
    DashboardFilters.applicationCategoryFilter().counter().shouldBe(ACTIVE).shouldHave(text("1 of 3"));
    DashboardFilters.stageFilter().counter().shouldBe(ACTIVE).shouldHave(text("1 of 5"));
    DashboardFilters.policyTypeFilter().counter().shouldBe(ACTIVE).shouldHave(text("1 of 4"));
    DashboardFilters.policyViolationStateFilter().counter().shouldBe(ACTIVE).shouldHave(text("3 of 3"));
    DashboardFilters.policyThreatLevelFilter().counter().shouldHave(text("2 – 7"));
  }

  private void assertFilterDisabled(NxTreeViewMultiSelect filter, String filterType) {
    filter.counter().shouldBe(visible, not(ACTIVE)).shouldHave(text("0"));

    filter.shouldHave(cssClass("nx-collapsible-items--disabled"));
    filter.multiSelectList().filter(visible).shouldBe(CollectionCondition.empty);
    filter.twisty().shouldBe(visible).hover();
    filter.multiSelectList().filter(visible).shouldBe(CollectionCondition.empty);

    Tooltip.get().shouldBe(visible).shouldHave(text("There are no " + filterType + " to filter"));
  }

  private void pressEscape() {
    Selenide.actions().sendKeys(Keys.ESCAPE).perform();
  }

  private void setNeedsAcknowledgementOfInitialDashboardFilter(Boolean needsAcknowledgementOfInitialDashboardFilter) {
    ApiConfigurationService configurationService =
        testCLMServer.getCLMServer().getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationNoAuthz(
        SystemConfigurationProperty.NEEDS_ACKNOWLEDGEMENT_OF_INITIAL_DASHBOARD_FILTER,
        needsAcknowledgementOfInitialDashboardFilter);
  }
}
