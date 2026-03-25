/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import {
  axiosMockAdapter,
  render,
  screen,
  within,
  fireEvent,
  waitFor,
  waitForElementToBeRemoved,
  setupPortalContainer,
} from 'TestRoot/SpecUtil';
import {
  ages,
  defaultMaxDaysOld,
  expirationDates,
  policyTypes,
  policyViolationStates,
  uncategorizedCategory,
} from 'MainRoot/dashboard/filter/staticFilterEntries';
import DashboardFilter from 'MainRoot/dashboard/filter/dashboardFilter/DashboardFilter';
import {
  getApplicationsUrl,
  getApplicationTagsUrl,
  getDashboardDeleteFilterUrl,
  getDashboardFilters,
  getDashboardSavedFilters,
  getDashboardStageUrl,
  getOrganizationsUrl,
  getOwnerListUrl,
  getRepositoriesUrl,
} from 'MainRoot/util/CLMLocation';
import defaultFilter from 'MainRoot/dashboard/filter/defaultFilter';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('DashboardFilter', () => {
  const rootOrganizationId = 'ROOT_ORGANIZATION_ID';
  const group1Id = 'group-id-1';

  let axiosMock, selectIsAutoWaiversSpy;

  beforeAll(() => setupPortalContainer());

  beforeEach(() => {
    selectIsAutoWaiversSpy = jest.spyOn(productFeaturesSelectors, 'selectIsAutoWaiversEnabled').mockReturnValue(true);
    axiosMock = axiosMockAdapter();
  });

  describe('filters conditional rendering', () => {
    it('renders only filters that are not conditional', async () => {
      renderComponent();
      await waitFor(() => expect(screen.queryByText('Loading...')).not.toBeInTheDocument());

      const filterContainer = getFilter();

      const filters = within(filterContainer).getAllByRole('group');
      expect(filters.length).toBe(5);
      expect(filters[0]).toHaveTextContent('Organizations');
      expect(filters[1]).toHaveTextContent('Applications');
      expect(filters[2]).toHaveTextContent('Application Categories');
      expect(filters[3]).toHaveTextContent('Policy Types');
      expect(filters[4]).toHaveTextContent('Policy Threat Level');
    });

    it('renders repositories filter', async () => {
      renderComponent({
        dashboardFilter: getFilterState({ showRepositoriesFilter: true }),
      });
      let filters = within(getFilter()).getAllByRole('group');
      expect(filters.length).toBe(6);
      expect(filters[0]).toHaveTextContent('Organizations');
      expect(filters[1]).toHaveTextContent('Applications');
      expect(filters[2]).toHaveTextContent('Repositories');
      expect(filters[3]).toHaveTextContent('Application Categories');
      expect(filters[4]).toHaveTextContent('Policy Types');
      expect(filters[5]).toHaveTextContent('Policy Threat Level');
    });

    it('renders stages filter', async () => {
      renderComponent({
        dashboardFilter: getFilterState({ showStagesFilter: true }),
      });
      let filters = within(getFilter()).getAllByRole('group');
      expect(filters.length).toBe(6);
      expect(filters[0]).toHaveTextContent('Organizations');
      expect(filters[1]).toHaveTextContent('Applications');
      expect(filters[2]).toHaveTextContent('Application Categories');
      expect(filters[3]).toHaveTextContent('Stages');
      expect(filters[4]).toHaveTextContent('Policy Types');
      expect(filters[5]).toHaveTextContent('Policy Threat Level');
    });

    it('renders violation state filter', async () => {
      renderComponent({
        dashboardFilter: getFilterState({ showViolationStateFilter: true }),
      });
      let filters = within(getFilter()).getAllByRole('group');
      expect(filters.length).toBe(6);
      expect(filters[0]).toHaveTextContent('Organizations');
      expect(filters[1]).toHaveTextContent('Applications');
      expect(filters[2]).toHaveTextContent('Application Categories');
      expect(filters[3]).toHaveTextContent('Policy Types');
      expect(filters[4]).toHaveTextContent('Violation State');
      expect(filters[5]).toHaveTextContent('Policy Threat Level');
    });

    it('renders expiration date filter', async () => {
      renderComponent({
        dashboardFilter: getFilterState({ showExpirationDateFilter: true }),
      });
      let filters = within(getFilter()).getAllByRole('group');
      expect(filters.length).toBe(6);
      expect(filters[0]).toHaveTextContent('Organizations');
      expect(filters[1]).toHaveTextContent('Applications');
      expect(filters[2]).toHaveTextContent('Application Categories');
      expect(filters[3]).toHaveTextContent('Policy Types');
      expect(filters[4]).toHaveTextContent('Expiration Date');
      expect(filters[5]).toHaveTextContent('Policy Threat Level');
    });

    it('renders age filter', async () => {
      renderComponent({
        dashboardFilter: getFilterState({ showAgeFilter: true }),
      });
      let filters = within(getFilter()).getAllByRole('group');
      expect(filters.length).toBe(6);
      expect(filters[0]).toHaveTextContent('Organizations');
      expect(filters[1]).toHaveTextContent('Applications');
      expect(filters[2]).toHaveTextContent('Application Categories');
      expect(filters[3]).toHaveTextContent('Policy Types');
      expect(filters[4]).toHaveTextContent('Age');
      expect(filters[5]).toHaveTextContent('Policy Threat Level');
    });

    it('renders policy waiver reason filter', async () => {
      renderComponent({
        dashboardFilter: getFilterState({ showPolicyWaiverReasonFilter: true }),
      });
      let filters = within(getFilter()).getAllByRole('group');
      expect(filters.length).toBe(6);
      expect(filters[0]).toHaveTextContent('Organizations');
      expect(filters[1]).toHaveTextContent('Applications');
      expect(filters[2]).toHaveTextContent('Application Categories');
      expect(filters[3]).toHaveTextContent('Policy Types');
      expect(filters[4]).toHaveTextContent('Policy Threat Level');
      expect(filters[5]).toHaveTextContent('Reason');
    });
  });

  it('shows loading spinner when loading', () => {
    renderComponent({ dashboardFilter: getFilterState({ loading: true }) });
    expect(screen.getByText('Loading…')).toBeVisible();
    expect(screen.queryByRole('group')).toBeNull();
    expect(screen.queryByRole('alert')).toBeNull();
    expect(screen.queryByRole('button', { name: 'foo' })).toBeNull();
  });

  it('shows error when loadingError and reloads on retry', async () => {
    renderComponent({ dashboardFilter: getFilterState({ loadError: 'some error' }) });
    expect(screen.getByRole('alert')).toHaveTextContent('some error');
    mockLoadFilter();
    const retryButton = screen.getByRole('button', { name: 'Retry' });
    fireEvent.click(retryButton);
    expect(screen.getByText('Loading…')).toBeVisible();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  describe('saved filters', () => {
    it('shows saved filters in dropdown', async () => {
      renderComponent();
      const header = getHeader();
      const filterDropdown = header.querySelector('.nx-dropdown__toggle');
      fireEvent.click(filterDropdown);

      const dropdownContent = header.querySelector('.nx-dropdown-menu');
      expect(dropdownContent).toBeInTheDocument();

      const filterOptions = within(dropdownContent).getAllByRole('button');
      // The result is 5 because the delete icon is also a button
      expect(filterOptions.length).toBe(5);
      expect(filterOptions[0]).toHaveTextContent('Default');
      expect(filterOptions[1]).toHaveTextContent('foo');
      expect(filterOptions[3]).toHaveTextContent('bar');
    });

    it('deletes a saved filter', async () => {
      renderComponent();
      const header = getHeader();
      const filterDropdown = header.querySelector('.nx-dropdown__toggle');
      fireEvent.click(filterDropdown);

      let filterOptions = header.querySelector('.nx-dropdown-menu').querySelectorAll('button');
      // The result is 5 because the delete icon is also a button
      expect(filterOptions.length).toBe(5);
      expect(filterOptions[0]).toHaveTextContent('Default');
      expect(filterOptions[1]).toHaveTextContent('foo');
      expect(filterOptions[3]).toHaveTextContent('bar');

      await deleteFilter('foo');

      await waitFor(() => {
        filterOptions = header.querySelector('.nx-dropdown-menu').querySelectorAll('button');
        expect(filterOptions.length).toBe(3);
      });
      expect(filterOptions[0]).toHaveTextContent('Default');
      expect(filterOptions[1]).toHaveTextContent('bar');
    });

    it('loads a saved filter', async () => {
      const applyMatcher = axiosMock.onPut(getDashboardFilters()).reply(200);

      renderComponent();
      selectFilter('foo');
      expect(applyMatcher.history.put.length).toBe(1);
      expect(applyMatcher.history.put[0].data).toContain('"basedOnFilterName":"foo"');
    });

    it('loads the default filter', async () => {
      const applyMatcher = axiosMock.onPut(getDashboardFilters()).reply(200);

      renderComponent();
      selectFilter('Default');
      expect(applyMatcher.history.put.length).toBe(1);
      expect(applyMatcher.history.put[0].data).toContain('"basedOnFilterName":null');
    });

    it('shows error message when apply named filter fails', async () => {
      axiosMock.onPut(getDashboardFilters()).reply(500, 'some error');
      renderComponent();
      selectFilter('foo');
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('some error');
    });

    it('shows error message when apply default filter fails', async () => {
      axiosMock.onPut(getDashboardFilters()).reply(500, 'some error');
      renderComponent();
      selectFilter('Default');
      const errorAlert = await screen.findByRole('alert');
      expect(errorAlert).toBeVisible();
      expect(errorAlert).toHaveTextContent('some error');
    });
  });

  describe('filters modification', () => {
    it('includes policyViolationReasonIds when applying filters', () => {
      const applyMatcher = axiosMock.onPut(getDashboardFilters()).reply(200, {
        organizationFilters: [group1Id],
        applicationFilters: ['app-1-id'],
        repositoryFilters: ['repo123'],
        policyThreatCategoryFilters: ['QUALITY'],
        stageTypeFilters: ['release'],
        tagFilters: ['cat'],
        policyViolationStates: ['OPEN', 'LEGACY_VIOLATION'],
        maxDaysOld: 90,
        minPolicyThreatLevel: 2,
        maxPolicyThreatLevel: 10,
        expirationDate: 'IN_90_DAYS',
        policyWaiverReasonIds: ['some-reason-id-1'],
      });

      renderComponent({
        dashboardFilter: getFilterState({
          showRepositoriesFilter: true,
          showStagesFilter: true,
          showPolicyWaiverReasonFilter: true,
          showViolationStateFilter: true,
          showExpirationDateFilter: true,
          showAgeFilter: true,
        }),
      });
      const organizationsFilter = getAndAssertFilterExists(0, 'Organizations');
      const applicationsFilter = getAndAssertFilterExists(1, 'Applications');
      const repositoriesFilter = getAndAssertFilterExists(2, 'Repositories');
      const categoriesFilter = getAndAssertFilterExists(3, 'Application Categories');
      const stagesFilter = getAndAssertFilterExists(4, 'Stages');
      const policyTypesFilter = getAndAssertFilterExists(5, 'Policy Types');
      const violationStateFilter = getAndAssertFilterExists(6, 'Violation State');
      const expirationDateFilter = getAndAssertFilterExists(7, 'Expiration Date');
      const ageFilter = getAndAssertFilterExists(8, 'Age');
      const reasonFilter = getAndAssertFilterExists(10, 'Reason');

      fireEvent.click(within(organizationsFilter).getByRole('button', { name: /Organizations/ }));
      const organization = within(organizationsFilter).getByLabelText('group-1');
      fireEvent.click(organization);

      fireEvent.click(within(applicationsFilter).getByRole('button', { name: /Applications/ }));
      const application = within(applicationsFilter).getByLabelText('App1');
      fireEvent.click(application);

      fireEvent.click(within(repositoriesFilter).getByRole('button', { name: /Repositories/ }));
      const repository = within(repositoriesFilter).getByLabelText('maven-central - 12345-67890');
      fireEvent.click(repository);

      fireEvent.click(within(categoriesFilter).getByRole('button', { name: /Categories/ }));
      const category = within(categoriesFilter).getByLabelText('Cat');
      fireEvent.click(category);

      fireEvent.click(within(stagesFilter).getByRole('button', { name: /Stages/ }));
      const stage = within(stagesFilter).getByLabelText('Release');
      fireEvent.click(stage);

      fireEvent.click(within(policyTypesFilter).getByRole('button', { name: /Policy Type/ }));
      const policyType = within(policyTypesFilter).getByLabelText('Quality');
      fireEvent.click(policyType);

      fireEvent.click(within(violationStateFilter).getByRole('button', { name: /Violation State/ }));
      const violationState = within(violationStateFilter).getByLabelText('Legacy');
      fireEvent.click(violationState);

      fireEvent.click(within(expirationDateFilter).getByRole('button', { name: /Expiration Date/ }));
      const expirationDate = within(expirationDateFilter).getByLabelText('in 90 days');
      fireEvent.click(expirationDate);

      fireEvent.click(within(ageFilter).getByRole('button', { name: /Age/ }));
      const age = within(ageFilter).getByLabelText('past 90 days');
      fireEvent.click(age);

      fireEvent.click(within(reasonFilter).getByRole('button', { name: /Reason/ }));
      const reason = within(reasonFilter).getByLabelText('REASON-1');
      fireEvent.click(reason);

      const footer = getFooter();
      const [, , applyButton] = within(footer).getAllByRole('button');
      fireEvent.click(applyButton);

      expect(applyMatcher.history.put.length).toBe(1);
      expect(applyMatcher.history.put[0].data).toContain(
        //eslint-disable-next-line
        '{"filter":{"organizationFilters":["group-id-1"],"applicationFilters":["app-1-id"],"repositoryFilters":["repo123"],"policyThreatCategoryFilters":["QUALITY"],"stageTypeFilters":["release"],"tagFilters":["cat"],"policyViolationStates":["OPEN","LEGACY_VIOLATION"],"maxDaysOld":90,"minPolicyThreatLevel":2,"maxPolicyThreatLevel":10,"expirationDate":"IN_90_DAYS","policyWaiverReasonIds":["some-reason-id-1"]}}'
      );
    });

    it('shows preloaded values for filters as checked when persisted', () => {
      selectIsAutoWaiversSpy.mockReturnValue(false);
      renderComponent({
        dashboardFilter: getFilterState({
          showRepositoriesFilter: true,
          showStagesFilter: true,
          showViolationStateFilter: true,
          showExpirationDateFilter: true,
          showAgeFilter: true,
          showPolicyWaiverReasonFilter: true,
          selected: getSelected({
            organizations: new Set([group1Id]),
            applications: new Set(['app-1-id']),
            repositories: new Set(['repo123']),
            categories: new Set(['cat']),
            stages: new Set(['release']),
            policyTypes: new Set(['QUALITY']),
            policyViolationStates: new Set(['OPEN', 'LEGACY_VIOLATION']),
            maxDaysOld: 90,
            expirationDate: 'IN_90_DAYS',
            policyThreatLevels: [2, 10],
            policyWaiverReasonIds: new Set(['some-reason-id-1']),
          }),
        }),
      });
      const organizationsFilter = getAndAssertFilterExists(0, 'Organizations');
      const applicationsFilter = getAndAssertFilterExists(1, 'Applications');
      const repositoriesFilter = getAndAssertFilterExists(2, 'Repositories');
      const categoriesFilter = getAndAssertFilterExists(3, 'Application Categories');
      const stagesFilter = getAndAssertFilterExists(4, 'Stages');
      const policyTypesFilter = getAndAssertFilterExists(5, 'Policy Types');
      const violationStateFilter = getAndAssertFilterExists(6, 'Violation State');
      const expirationDateFilter = getAndAssertFilterExists(7, 'Expiration Date');
      const ageFilter = getAndAssertFilterExists(8, 'Age');
      const reasonFilter = getAndAssertFilterExists(10, 'Reason');

      fireEvent.click(within(organizationsFilter).getByRole('button', { name: /Organizations/ }));
      const organizations = within(organizationsFilter).getAllByRole('menuitemcheckbox');
      expect(organizations.length).toBe(2);

      const organization = within(organizationsFilter).getByLabelText('group-1');
      expect(organization).toBeVisible();
      expect(organization).toHaveAttribute('checked');

      fireEvent.click(within(applicationsFilter).getByRole('button', { name: /Applications/ }));
      const applications = within(applicationsFilter).getAllByRole('menuitemcheckbox');
      expect(applications.length).toBe(3);

      const application = within(applicationsFilter).getByLabelText('App1');
      expect(application).toBeVisible();
      expect(application).toHaveAttribute('checked');

      fireEvent.click(within(repositoriesFilter).getByRole('button', { name: /Repositories/ }));
      const repositories = within(repositoriesFilter).getAllByRole('menuitemcheckbox');
      expect(repositories.length).toBe(3);

      const repository = within(repositoriesFilter).getByLabelText('maven-central - 12345-67890');
      expect(repository).toBeVisible();
      expect(repository).toHaveAttribute('checked');

      fireEvent.click(within(categoriesFilter).getByRole('button', { name: /Categories/ }));
      const categories = within(categoriesFilter).getAllByRole('menuitemcheckbox');
      expect(categories.length).toBe(3);

      const category = within(categoriesFilter).getByLabelText('Cat');
      expect(category).toBeVisible();
      expect(category).toHaveAttribute('checked');

      fireEvent.click(within(stagesFilter).getByRole('button', { name: /Stages/ }));
      const stages = within(stagesFilter).getAllByRole('menuitemcheckbox');
      expect(stages.length).toBe(5);

      const stage = within(stagesFilter).getByLabelText('Release');
      expect(stage).toBeVisible();
      expect(stage).toHaveAttribute('checked');

      fireEvent.click(within(policyTypesFilter).getByRole('button', { name: /Policy Types/ }));
      const policyTypes = within(policyTypesFilter).getAllByRole('menuitemcheckbox');
      expect(policyTypes.length).toBe(5);

      const policyType = within(policyTypesFilter).getByLabelText('Quality');
      expect(policyType).toBeVisible();
      expect(policyType).toHaveAttribute('checked');

      fireEvent.click(within(violationStateFilter).getByRole('button', { name: /Violation State/ }));
      const violationStates = within(violationStateFilter).getAllByRole('menuitemcheckbox');
      expect(violationStates.length).toBe(4);

      const violationState1 = within(violationStateFilter).getByLabelText('Open');
      expect(violationState1).toBeVisible();
      expect(violationState1).toHaveAttribute('checked');

      const violationState2 = within(violationStateFilter).getByLabelText('Legacy');
      expect(violationState2).toBeVisible();
      expect(violationState2).toHaveAttribute('checked');

      fireEvent.click(within(expirationDateFilter).getByRole('button', { name: /Expiration Date/ }));
      const expirationDates = within(expirationDateFilter).getAllByRole('menuitemradio');
      expect(expirationDates.length).toBe(7);

      const expirationDate = within(expirationDateFilter).getByLabelText('in 90 days');
      expect(expirationDate).toBeVisible();
      expect(expirationDate).toHaveAttribute('checked');

      fireEvent.click(within(ageFilter).getByRole('button', { name: /Age/ }));
      const age = within(ageFilter).getByLabelText('past 90 days');
      expect(age).toBeVisible();
      expect(age).toHaveAttribute('checked');

      fireEvent.click(within(reasonFilter).getByRole('button', { name: /Reason/ }));
      const reasons = within(reasonFilter).getAllByRole('menuitemcheckbox');
      expect(reasons.length).toBe(4);

      const reason = within(reasonFilter).getByLabelText('REASON-1');
      expect(reason).toBeVisible();
      expect(reason).toHaveAttribute('checked');
    });

    it('shows preloaded values for filters as checked when persisted with auto-waivers feature flag on', () => {
      renderComponent({
        dashboardFilter: getFilterState({
          showRepositoriesFilter: true,
          showStagesFilter: true,
          showViolationStateFilter: true,
          showExpirationDateFilter: true,
          showAgeFilter: true,
          showPolicyWaiverReasonFilter: true,
          selected: getSelected({
            organizations: new Set([group1Id]),
            applications: new Set(['app-1-id']),
            repositories: new Set(['repo123']),
            categories: new Set(['cat']),
            stages: new Set(['release']),
            policyTypes: new Set(['QUALITY']),
            policyViolationStates: new Set(['OPEN', 'LEGACY_VIOLATION']),
            maxDaysOld: 90,
            expirationDate: 'IN_90_DAYS',
            policyThreatLevels: [2, 10],
            policyWaiverReasonIds: new Set(['some-reason-id-1']),
          }),
        }),
      });
      const organizationsFilter = getAndAssertFilterExists(0, 'Organizations');
      const applicationsFilter = getAndAssertFilterExists(1, 'Applications');
      const repositoriesFilter = getAndAssertFilterExists(2, 'Repositories');
      const categoriesFilter = getAndAssertFilterExists(3, 'Application Categories');
      const stagesFilter = getAndAssertFilterExists(4, 'Stages');
      const policyTypesFilter = getAndAssertFilterExists(5, 'Policy Types');
      const violationStateFilter = getAndAssertFilterExists(6, 'Violation State');
      const expirationDateFilter = getAndAssertFilterExists(7, 'Expiration Date');
      const ageFilter = getAndAssertFilterExists(8, 'Age');
      const reasonFilter = getAndAssertFilterExists(10, 'Reason');

      fireEvent.click(within(organizationsFilter).getByRole('button', { name: /Organizations/ }));
      const organizations = within(organizationsFilter).getAllByRole('menuitemcheckbox');
      expect(organizations.length).toBe(2);

      const organization = within(organizationsFilter).getByLabelText('group-1');
      expect(organization).toBeVisible();
      expect(organization).toHaveAttribute('checked');

      fireEvent.click(within(applicationsFilter).getByRole('button', { name: /Applications/ }));
      const applications = within(applicationsFilter).getAllByRole('menuitemcheckbox');
      expect(applications.length).toBe(3);

      const application = within(applicationsFilter).getByLabelText('App1');
      expect(application).toBeVisible();
      expect(application).toHaveAttribute('checked');

      fireEvent.click(within(repositoriesFilter).getByRole('button', { name: /Repositories/ }));
      const repositories = within(repositoriesFilter).getAllByRole('menuitemcheckbox');
      expect(repositories.length).toBe(3);

      const repository = within(repositoriesFilter).getByLabelText('maven-central - 12345-67890');
      expect(repository).toBeVisible();
      expect(repository).toHaveAttribute('checked');

      fireEvent.click(within(categoriesFilter).getByRole('button', { name: /Categories/ }));
      const categories = within(categoriesFilter).getAllByRole('menuitemcheckbox');
      expect(categories.length).toBe(3);

      const category = within(categoriesFilter).getByLabelText('Cat');
      expect(category).toBeVisible();
      expect(category).toHaveAttribute('checked');

      fireEvent.click(within(stagesFilter).getByRole('button', { name: /Stages/ }));
      const stages = within(stagesFilter).getAllByRole('menuitemcheckbox');
      expect(stages.length).toBe(5);

      const stage = within(stagesFilter).getByLabelText('Release');
      expect(stage).toBeVisible();
      expect(stage).toHaveAttribute('checked');

      fireEvent.click(within(policyTypesFilter).getByRole('button', { name: /Policy Types/ }));
      const policyTypes = within(policyTypesFilter).getAllByRole('menuitemcheckbox');
      expect(policyTypes.length).toBe(5);

      const policyType = within(policyTypesFilter).getByLabelText('Quality');
      expect(policyType).toBeVisible();
      expect(policyType).toHaveAttribute('checked');

      fireEvent.click(within(violationStateFilter).getByRole('button', { name: /Violation State/ }));
      const violationStates = within(violationStateFilter).getAllByRole('menuitemcheckbox');
      expect(violationStates.length).toBe(4);

      const violationState1 = within(violationStateFilter).getByLabelText('Open');
      expect(violationState1).toBeVisible();
      expect(violationState1).toHaveAttribute('checked');

      const violationState2 = within(violationStateFilter).getByLabelText('Legacy');
      expect(violationState2).toBeVisible();
      expect(violationState2).toHaveAttribute('checked');

      fireEvent.click(within(expirationDateFilter).getByRole('button', { name: /Expiration Date/ }));
      const expirationDates = within(expirationDateFilter).getAllByRole('menuitemradio');
      expect(expirationDates.length).toBe(8);

      const expirationDate = within(expirationDateFilter).getByLabelText('in 90 days');
      expect(expirationDate).toBeVisible();
      expect(expirationDate).toHaveAttribute('checked');

      fireEvent.click(within(ageFilter).getByRole('button', { name: /Age/ }));
      const age = within(ageFilter).getByLabelText('past 90 days');
      expect(age).toBeVisible();
      expect(age).toHaveAttribute('checked');

      fireEvent.click(within(reasonFilter).getByRole('button', { name: /Reason/ }));
      const reasons = within(reasonFilter).getAllByRole('menuitemcheckbox');
      expect(reasons.length).toBe(4);

      const reason = within(reasonFilter).getByLabelText('REASON-1');
      expect(reason).toBeVisible();
      expect(reason).toHaveAttribute('checked');
    });
  });

  it('shows error message when apply filter fails', async () => {
    axiosMock.onPut(getDashboardFilters()).reply(500, 'some error');
    renderComponent();
    const orgsFilter = getAndAssertFilterExists(0, 'Organizations');

    fireEvent.click(within(orgsFilter).getByRole('button', { name: /Organizations/ }));

    const org1 = within(orgsFilter).getByLabelText('group-1');
    fireEvent.click(org1);

    const footer = getFooter();
    const [, , applyButton] = within(footer).getAllByRole('button');
    fireEvent.click(applyButton);

    const errorAlert = await screen.findByRole('alert');
    expect(errorAlert).toBeVisible();
    expect(errorAlert).toHaveTextContent('some error');
  });

  const renderComponent = (preloadedStateOverrides = {}) => {
    const preloadedState = {
      ...getMinimalReduxState(),
      ...preloadedStateOverrides,
    };

    render(<DashboardFilter />, { preloadedState });

    const drawer = screen.getByRole('dialog', { hidden: true });
    fireEvent.animationEnd(drawer);
  };

  const getMinimalReduxState = (overrides = {}) => {
    return {
      dashboardFilter: getFilterState(),
      orgsAndPolicies: getOrgsAndPoliciesState(),
      manageFilters: { savedFilters: getSavedFilters() },
      waivers: getWaiversState(),
      ...overrides,
    };
  };

  const getFilterState = (overrides = {}) => {
    return {
      filterSidebarOpen: true,
      selected: getSelected(),
      applications: [
        {
          id: 'app-1-id',
          publicId: 'App1',
          name: 'App1',
          organizationId: 'org-1-id',
          organizationName: 'Org1',
        },
        {
          id: 'app-2-id',
          publicId: 'App2',
          name: 'App2',
          organizationId: 'org-1-id',
          organizationName: 'Org1',
        },
      ],
      repositories: [
        {
          fullName: 'maven-central - 12345-67890',
          name: 'maven-central - 12345-67890',
          id: 'repo123',
          publicId: 'maven-central - 12345',
        },
        {
          fullName: 'maven-not-central - 12345-67890',
          name: 'maven-not-central - 12345-67890',
          id: 'repo456',
          publicId: 'maven-not-central - 12345',
        },
      ],
      categories: [uncategorizedCategory, { id: 'cat', name: 'Cat', owner: 'Org1' }],
      stages: [
        { id: 'build', name: 'Build' },
        { id: 'stage-release', name: 'Stage Release' },
        { id: 'release', name: 'Release' },
        { id: 'operate', name: 'Operate' },
      ],
      ages,
      policyTypes,
      policyViolationStates,
      expirationDates,
      loading: false,
      ...overrides,
    };
  };

  const getOrgsAndPoliciesState = (ownerSideNavOverrides = {}) => {
    return {
      ownerSideNav: {
        topParentOrganizationId: rootOrganizationId,
        ownersMap: {
          [rootOrganizationId]: {
            type: 'organization',
            id: rootOrganizationId,
            name: 'Root Organization',
            synthetic: true,
            parentOrganizationId: null,
            applicationIds: null,
            subOrgs: 1,
            totalApps: 1,
            organizationIds: [group1Id],
          },
          [group1Id]: {
            type: 'organization',
            id: group1Id,
            name: 'group-1',
            synthetic: false,
            parentOrganizationId: rootOrganizationId,
            applicationIds: ['app-1-id'],
            subOrgs: 0,
            totalApps: 1,
            organizationIds: [],
          },
          ['app-1-id']: {
            type: 'application',
            id: 'app-1-id',
            name: 'App1',
            synthetic: false,
            parentOrganizationId: group1Id,
            applicationIds: ['app-1-id'],
          },
        },
      },
      ...ownerSideNavOverrides,
    };
  };

  const getSavedFilters = () => {
    return [
      {
        name: 'foo',
      },
      {
        name: 'bar',
      },
    ];
  };

  const getSelected = (overrides = {}) => {
    return {
      ...defaultFilter,
      ...overrides,
    };
  };

  const getWaiversState = () => {
    return {
      waiverReasons: {
        data: [
          { id: 'some-reason-id-1', type: 'system', reasonText: 'REASON-1' },
          { id: 'some-reason-id-2', type: 'system', reasonText: 'REASON-2' },
        ],
      },
    };
  };

  const getFilter = () => {
    return screen.getByRole('dialog');
  };

  const getFooter = () => {
    const filter = getFilter();
    return filter.children[2];
  };

  const getHeader = () => {
    const filter = getFilter();
    return filter.children[0];
  };

  const getAndAssertFilterExists = (index, text) => {
    // Gets the filter section at index and asserts that it contains the text
    const filters = within(getFilter()).getAllByRole('group');
    const reasonFilter = filters[index];
    within(reasonFilter).getByText(text);
    return reasonFilter;
  };

  const selectFilter = (filterName) => {
    // Selects a saved filter from the dropdown
    const header = getHeader();
    const filterDropdown = header.querySelector('.nx-dropdown__toggle');
    fireEvent.click(filterDropdown);

    const filterOption = Array.from(header.querySelectorAll('.nx-dropdown-menu button')).find(
      (button) => button.textContent === filterName
    );
    fireEvent.click(filterOption);
  };

  const deleteFilter = async (filterName) => {
    // Selects a saved filter from the dropdown
    const deleteRequest = axiosMock.onPost(getDashboardDeleteFilterUrl(filterName)).reply(200);
    axiosMock.onGet(getDashboardSavedFilters()).reply(
      200,
      getSavedFilters().filter((f) => f.name !== filterName)
    );
    const header = getHeader();
    const filterDropdown = header.querySelector('.nx-dropdown__toggle');
    fireEvent.click(filterDropdown);

    const filterOption = Array.from(header.querySelectorAll('.nx-dropdown-menu button')).find(
      (button) => button.textContent === filterName
    );
    const deleteButton = filterOption.nextElementSibling;
    deleteButton.click();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Continue' })).toBeInTheDocument();
    });
    expect(
      screen.getByText(`You are about to delete "${filterName}" filter. This action can not be undone.`)
    ).toBeInTheDocument();
    const continueButton = screen.getByRole('button', { name: 'Continue' });
    fireEvent.click(continueButton);
    expect(deleteRequest.history.post.length).toBe(1);
    await waitForElementToBeRemoved(() => screen.queryByText('Delete Filter'));
  };

  const mockLoadFilter = () => {
    axiosMock.onGet(getApplicationsUrl()).reply(200, [
      {
        id: 'applicationId',
        name: 'app-1-id',
        organizationId: group1Id,
        organizationName: 'group-1',
        publicId: 'app-1-id',
      },
    ]);

    axiosMock.onGet(getOrganizationsUrl()).reply(200, [
      {
        id: rootOrganizationId,
        name: 'Root Organization',
        parentOrganizationId: null,
      },
      {
        id: group1Id,
        name: 'group-1',
        parentOrganizationId: rootOrganizationId,
      },
    ]);

    axiosMock.onGet(getRepositoriesUrl()).reply(200, [
      {
        oldestEvalTimestamp: null,
        managerInstanceId: '72B7AFE9-0FE2FE06-762B1E5B-43258149-63E1C1BB',
        managerName: '72B7AFE9-0FE2FE06-762B1E5B-43258149-63E1C1BB',
        repository: {
          id: '186583fe069447e2a0d26195e7c7d7ab',
          repositoryManagerId: 'f86bbf0ee69742298363a36dc54e8a36',
          publicId: 'sonatype-grid.release',
          repositoryType: 'proxy',
          auditEnabled: false,
          quarantineEnabled: false,
          policyCompliantComponentSelectionEnabled: false,
          namespaceConfusionProtectionEnabled: false,
          format: 'maven2',
          lastManualConfigureTime: null,
        },
      },
    ]);

    axiosMock.onGet(getApplicationTagsUrl()).reply(200, [
      {
        id: 'e5229fffe44343839583846534b38336',
        name: 'Internal',
        description: 'Applications that are used only by your employees',
        organizationId: 'ROOT_ORGANIZATION_ID',
        color: 'dark-green',
      },
    ]);

    axiosMock.onGet(getDashboardFilters()).reply(200, {
      organizationFilters: [],
      applicationFilters: [],
      repositoryFilters: [],
      policyThreatCategoryFilters: [],
      stageTypeFilters: [],
      tagFilters: [],
      policyViolationStates: ['OPEN'],
      maxDaysOld: defaultMaxDaysOld,
      minPolicyThreatLevel: 2,
      maxPolicyThreatLevel: 10,
      expirationDate: 'ALL',
      policyWaiverReasonIds: [],
    });

    axiosMock.onGet(getDashboardStageUrl()).reply(200, [
      {
        stageTypeId: 'source',
        stageName: 'Source',
      },
      {
        stageTypeId: 'build',
        stageName: 'Build',
      },
      {
        stageTypeId: 'stage-release',
        stageName: 'Stage Release',
      },
      {
        stageTypeId: 'release',
        stageName: 'Release',
      },
      {
        stageTypeId: 'operate',
        stageName: 'Operate',
      },
    ]);

    axiosMock.onGet(getDashboardSavedFilters()).reply(200, getSavedFilters());

    axiosMock.onGet(getOwnerListUrl()).reply(200, getOrgsAndPoliciesState.ownerSideNav);
  };
});
