/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';

import { axiosMockAdapter, render, screen, within } from 'TestRoot/SpecUtil';
import {
  ages,
  defaultMaxDaysOld,
  expirationDates,
  policyTypes,
  policyViolationStates,
  uncategorizedCategory,
} from 'MainRoot/dashboard/filter/staticFilterEntries';
import DashboardFilterContainer from 'MainRoot/dashboard/filter/dashboardFilter/DashboardFilterContainer';
import { fireEvent } from '@testing-library/react';
import { getDashboardFilters } from 'MainRoot/util/CLMLocation';
import defaultFilter from 'MainRoot/dashboard/filter/defaultFilter';

describe('DashboardFilter', () => {
  const rootOrganizationId = 'ROOT_ORGANIZATION_ID';
  const group1Id = 'group-id-1';

  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
  });

  it('renders waiver reason filter when enabled', async () => {
    renderComponent(
      getMinimalReduxState({
        dashboardFilter: getFilterState({ showPolicyWaiverReasonFilter: true }),
      })
    );

    const reasonFilter = getAndAssertReasonFilterExists();

    expect(within(reasonFilter).getAllByRole('menuitemcheckbox').length).toEqual(4);

    expect(within(reasonFilter).getByLabelText('all/none')).toBeVisible();
    expect(within(reasonFilter).getByLabelText('REASON-1')).toBeVisible();
    expect(within(reasonFilter).getByLabelText('REASON-2')).toBeVisible();
    expect(within(reasonFilter).getByLabelText('(No reason provided)')).toBeVisible();
  });

  it('does not render waiver reason filter when disabled', async () => {
    renderComponent(
      getMinimalReduxState({
        dashboardFilter: getFilterState({ showPolicyWaiverReasonFilter: false }),
      })
    );

    let filters = within(getFilter()).getAllByRole('group');
    expect(filters.length).toBe(5);
  });

  it('shows reasons as checked when persisted', () => {
    renderComponentWithAReasonChecked('some-reason-id-2');

    const reasonFilter = getAndAssertReasonFilterExists();

    expect(within(reasonFilter).getAllByRole('menuitemcheckbox').length).toEqual(4);

    const reason1 = within(reasonFilter).getByLabelText('REASON-1');
    expect(reason1).toBeVisible();
    expect(reason1).not.toHaveAttribute('checked');

    const reason2 = within(reasonFilter).getByLabelText('REASON-2');
    expect(reason2).toBeVisible();
    expect(reason2).toHaveAttribute('checked');
  });

  it('includes policyViolationReasonIds when applying filters', () => {
    const applyMatcher = mockDashboardFilterUpdateRequest();

    renderComponentWithAReasonChecked('some-reason-id-1');

    const footer = getFooter();
    const [, , applyButton] = within(footer).getAllByRole('button');
    fireEvent.click(applyButton);

    expect(applyMatcher.history.put.length).toBe(1);
    expect(applyMatcher.history.put[0].data).toContain('"policyWaiverReasonIds":["some-reason-id-1"]');
  });

  function renderComponentWithAReasonChecked(reasonId) {
    // render with a reason id checked
    return renderComponent({
      dashboardFilter: getFilterState({
        filtersAreDirty: true,
        showPolicyWaiverReasonFilter: true,
        selected: getSelected({ policyWaiverReasonIds: new Set([reasonId]) }),
      }),
    });
  }

  function renderComponent(preloadedStateOverrides = {}) {
    const preloadedState = {
      ...getMinimalReduxState(),
      ...preloadedStateOverrides,
    };

    return render(<DashboardFilterContainer />, { preloadedState });
  }

  function getMinimalReduxState(overrides = {}) {
    return {
      dashboardFilter: getFilterState(),
      orgsAndPolicies: getOrgsAndPoliciesState(),
      waivers: getWaiversState(),
      ...overrides,
    };
  }

  function getFilterState(overrides = {}) {
    return {
      selected: getSelected(),
      applications: [
        {
          id: 'app-1-id',
          publicId: 'App1',
          name: 'App1',
          organizationId: 'org-1-id',
          organizationName: 'Org1',
        },
      ],
      repositories: [
        {
          fullName: 'maven-central - 12345-67890',
          id: 'repo123',
          publicId: 'maven-central - 12345',
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
      loadFilter: jest.fn(),
      loading: false,
      savedFilters: getSavedFilters(),
      applyDefaultFilter: jest.fn(),
      applySavedFilter: jest.fn(),
      applyFilter: jest.fn(),
      setDisplaySaveFilterModal: jest.fn(),
      revert: jest.fn(),
      selectFilterToDelete: jest.fn(),
      applyFilterCancelled: jest.fn(),
      toggleAppsAndOrgs: jest.fn(),
      ...overrides,
    };
  }

  function getOrgsAndPoliciesState(ownerSideNavOverrides = {}) {
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
        },
      },
      ...ownerSideNavOverrides,
    };
  }

  function getSavedFilters() {
    return [
      {
        name: 'foo',
      },
      {
        name: 'bar',
      },
    ];
  }

  function getSelected(overrides = {}) {
    return {
      ...defaultFilter,
      ...overrides,
    };
  }

  function getWaiversState() {
    return {
      waiverReasons: {
        data: [
          { id: 'some-reason-id-1', type: 'system', reasonText: 'REASON-1' },
          { id: 'some-reason-id-2', type: 'system', reasonText: 'REASON-2' },
        ],
      },
    };
  }

  function mockDashboardFilterUpdateRequest() {
    return axiosMock.onPut(getDashboardFilters()).reply(200, {
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
      policyWaiverReasonIds: ['some-reason-id-1'],
    });
  }

  function getFilter() {
    return screen.getByRole('complementary');
  }

  function getFooter() {
    const filter = getFilter();
    return filter.children[2];
  }

  function getAndAssertReasonFilterExists() {
    const filters = within(getFilter()).getAllByRole('group');
    const reasonFilter = filters[5];
    within(reasonFilter).getByText('Reason');

    return reasonFilter;
  }
});
