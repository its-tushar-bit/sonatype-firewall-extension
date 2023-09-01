/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import {
  ages,
  defaultMaxDaysOld,
  policyTypes,
  policyViolationStates,
  uncategorizedCategory,
  expirationDates,
} from 'MainRoot/dashboard/filter/staticFilterEntries';
import { render, screen, within, fireEvent } from 'TestRoot/SpecUtil';

describe('DashboardFilter', function () {
  let renderComponent, loadFilterSpy, minimalProps, SaveFilterModalContainerMock, DashboardFilter;

  const filterData = {
    applications: [
      {
        id: '777heaven777',
        publicId: 'App1',
        name: 'App1',
        organizationId: '666hell666',
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
    selected: {
      organizations: new Set(),
      applications: new Set(),
      repositories: new Set(),
      categories: new Set(),
      stages: new Set(),
      policyTypes: new Set(),
      policyViolationStates: new Set(),
      maxDaysOld: defaultMaxDaysOld,
      policyThreatLevels: [2, 10],
      expirationDate: 'ALL',
    },
  };

  const savedFilters = [
    {
      name: 'foo',
    },
    {
      name: 'bar',
    },
  ];

  beforeEach(function () {
    loadFilterSpy = jasmine.createSpy('loadFilter');
    minimalProps = {
      ...filterData,
      loadFilter: loadFilterSpy,
      loading: false,
      savedFilters,
      ownersMap: {
        ROOT_ORGANIZATION_ID: {
          type: 'organization',
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          synthetic: true,
          parentOrganizationId: null,
          applicationIds: null,
          subOrgs: 1,
          totalApps: 1,
          organizationIds: ['666hell666'],
        },
        '666hell666': {
          type: 'organization',
          id: '666hell666',
          name: 'Org1',
          synthetic: false,
          parentOrganizationId: 'ROOT_ORGANIZATION_ID',
          applicationIds: ['App1'],
          subOrgs: 0,
          totalApps: 1,
          organizationIds: [],
        },
      },
      topParentOrganizationId: 'ROOT_ORGANIZATION_ID',
      applyDefaultFilter: jasmine.createSpy('applyDefaultFilter'),
      applySavedFilter: jasmine.createSpy('applySavedFilter'),
    };
    SaveFilterModalContainerMock = jasmine
      .createSpy('SaveFilterModalContainer')
      .and.returnValue(<div>Save Filter Modal</div>);

    DashboardFilter = require('inject-loader!../../../../../main/frontend/dashboard/filter/dashboardFilter/' +
      'DashboardFilter')({ '../saveFilterModal/SaveFilterModalContainer': SaveFilterModalContainerMock }).default;

    renderComponent = (props) => render(<DashboardFilter {...minimalProps} {...props} />);
  });

  function getFilter() {
    return screen.getByRole('complementary');
  }

  function getHeader() {
    const filter = getFilter();
    return filter.children[0];
  }

  function getFooter() {
    const filter = getFilter();
    return filter.children[2];
  }

  describe('apply named filter error', function () {
    it('is rendered within the header if loadErrorFilterName is not null', function () {
      const props = { loadErrorFilterName: 'filter 1234' };
      renderComponent(props);
      const error = screen.getByRole('alert');

      expect(error).toHaveTextContent('Failed to load filter 1234');
    });

    it('is not rendered if loadErrorFilterName is null', function () {
      const props = { loadErrorFilterName: null };
      renderComponent(props);
      const findError = () => {
        return screen.getByRole('alert');
      };

      expect(findError).toThrowError();
    });
  });

  describe('filter header', function () {
    const findFilterDropdown = () => {
      within(getHeader().children[1]).getByRole('button');
    };

    it('renders ManageFiltersDropdown outside of header element', function () {
      const props = {
        appliedFilterName: 'some filter',
        showDirtyAsterisk: true,
      };
      renderComponent(props);
      const header = getHeader();

      expect(header).toHaveTextContent('Filter');

      const dropdown = within(header.children[1]).getByRole('button');
      expect(dropdown).toHaveTextContent('*some filter');
    });

    it('does not render ManageFiltersDropdown if loading', function () {
      const props = {
        appliedFilterName: 'some filter',
        showDirtyAsterisk: true,
        loading: true,
      };
      renderComponent(props);
      const filter = getFilter();

      const loading = within(filter).getByText('Loading…');
      expect(loading).toBeVisible();
      expect(findFilterDropdown).toThrowError();
    });

    it('does not render ManageFiltersDropdown if loadError', function () {
      const props = {
        appliedFilterName: 'some filter',
        showDirtyAsterisk: true,
        loadError: 'Error',
      };
      renderComponent(props);

      expect(findFilterDropdown).toThrowError();
    });
  });

  it('renders a DashboardFilterFooter with the correct props', function () {
    const props = {
      applyFilterError: 'err',
      filtersAreDirty: true,
      needsAcknowledgement: true,
      setDisplaySaveFilterModal: jasmine.createSpy('setDisplaySaveFilterModal'),
      revert: jasmine.createSpy('revert'),
      applyFilter: jasmine.createSpy('applyFilter'),
      applyFilterCancelled: jasmine.createSpy('applyFilterCancel'),
    };
    renderComponent(props);
    const footer = getFooter();
    const error = within(footer).getByRole('alert');

    expect(error).toBeVisible();
    expect(error).toHaveTextContent('err');

    const [cancel, retry] = within(error).getAllByRole('button');
    fireEvent.click(cancel);
    expect(props.applyFilterCancelled).toHaveBeenCalled();
    fireEvent.click(retry);
    expect(props.applyFilter).toHaveBeenCalled();
  });

  describe('DashboardFilter filter contents', function () {
    let toggleAppsAndOrgsSpy, toggleFilterSpy;

    function getInnerFilters() {
      return getFilter().children[1].children[0].children;
    }

    beforeEach(() => {
      toggleAppsAndOrgsSpy = jasmine.createSpy('toggleAppsAndOrgs');
      toggleFilterSpy = jasmine.createSpy('toggleFilter');
    });

    it('renders the organization and application filter when loading prop is false', function () {
      renderComponent({
        ...filterData,
        loading: false,
        toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
        toggleFilter: toggleFilterSpy,
      });

      const [orgAppFilter] = getInnerFilters();

      const [orgGroup, appGroup] = within(orgAppFilter).getAllByRole('menu');
      expect(orgGroup.children[0]).toHaveTextContent('all/none');
      expect(orgGroup.children[1]).toHaveTextContent('Org1');
      expect(appGroup.children[0]).toHaveTextContent('all/none');
      expect(appGroup.children[1]).toHaveTextContent('App1');
      fireEvent.click(orgGroup.children[0]);
      fireEvent.click(orgGroup.children[1]);
      fireEvent.click(appGroup.children[0]);
      fireEvent.click(appGroup.children[1]);
      expect(toggleAppsAndOrgsSpy).toHaveBeenCalledTimes(4);
    });

    it('renders the category filter when loading prop is false', function () {
      renderComponent({
        ...filterData,
        loading: false,
        toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
        toggleFilter: toggleFilterSpy,
      });

      const [, categoryFilter] = getInnerFilters();

      const categoryGroup = within(categoryFilter).getByRole('menu');
      expect(categoryGroup.children[0]).toHaveTextContent('all/none');
      expect(categoryGroup.children[1]).toHaveTextContent('uncategorized applications');
      expect(categoryGroup.children[2]).toHaveTextContent('Cat');
      fireEvent.click(categoryGroup.children[0]);
      fireEvent.click(categoryGroup.children[1]);
      fireEvent.click(categoryGroup.children[2]);
      expect(toggleFilterSpy).toHaveBeenCalledTimes(3);
    });

    it('renders the stage filter when loading prop is false', function () {
      renderComponent({
        ...filterData,
        showStagesFilter: true,
        loading: false,
        toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
        toggleFilter: toggleFilterSpy,
      });

      const [, , stageFilter] = getInnerFilters();

      const stagesGroup = within(stageFilter).getByRole('menu');
      expect(stagesGroup.children[0]).toHaveTextContent('all/none');
      expect(stagesGroup.children[1]).toHaveTextContent('Build');
      expect(stagesGroup.children[2]).toHaveTextContent('Stage Release');
      expect(stagesGroup.children[3]).toHaveTextContent('Release');
      expect(stagesGroup.children[4]).toHaveTextContent('Operate');
      fireEvent.click(stagesGroup.children[0]);
      fireEvent.click(stagesGroup.children[1]);
      fireEvent.click(stagesGroup.children[2]);
      fireEvent.click(stagesGroup.children[3]);
      fireEvent.click(stagesGroup.children[4]);
      expect(toggleFilterSpy).toHaveBeenCalledTimes(5);
    });

    it('renders the policy type filter when loading prop is false', function () {
      renderComponent({
        ...filterData,
        loading: false,
        toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
        toggleFilter: toggleFilterSpy,
      });

      const [, , policyTypeFilter] = getInnerFilters();

      const policyTypeGroup = within(policyTypeFilter).getByRole('menu');
      expect(policyTypeGroup.children[0]).toHaveTextContent('all/none');
      expect(policyTypeGroup.children[1]).toHaveTextContent('Security');
      expect(policyTypeGroup.children[2]).toHaveTextContent('License');
      expect(policyTypeGroup.children[3]).toHaveTextContent('Quality');
      expect(policyTypeGroup.children[4]).toHaveTextContent('Other');
      fireEvent.click(policyTypeGroup.children[0]);
      fireEvent.click(policyTypeGroup.children[1]);
      fireEvent.click(policyTypeGroup.children[2]);
      fireEvent.click(policyTypeGroup.children[3]);
      fireEvent.click(policyTypeGroup.children[4]);
      expect(toggleFilterSpy).toHaveBeenCalledTimes(5);
    });

    it('renders the policy violation state filter when loading prop is false', function () {
      renderComponent({
        ...filterData,
        showViolationStateFilter: true,
        loading: false,
        toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
        toggleFilter: toggleFilterSpy,
      });

      const [, , , policyViolationStateFilter] = getInnerFilters();

      const policyViolationStateGroup = within(policyViolationStateFilter).getByRole('menu');
      expect(policyViolationStateGroup.children[0]).toHaveTextContent('all/none');
      expect(policyViolationStateGroup.children[1]).toHaveTextContent('Open');
      expect(policyViolationStateGroup.children[2]).toHaveTextContent('Waived');
      expect(policyViolationStateGroup.children[3]).toHaveTextContent('Legacy');
      fireEvent.click(policyViolationStateGroup.children[0]);
      fireEvent.click(policyViolationStateGroup.children[1]);
      fireEvent.click(policyViolationStateGroup.children[2]);
      fireEvent.click(policyViolationStateGroup.children[3]);
      expect(toggleFilterSpy).toHaveBeenCalledTimes(4);
    });

    it('renders the threat level filter when loading prop is false', function () {
      renderComponent({
        ...filterData,
        loading: false,
        toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
        toggleFilter: toggleFilterSpy,
      });

      const [, , , threatLevelFiler] = getInnerFilters();

      const [minSlider, maxSlider] = within(threatLevelFiler).getAllByRole('slider');
      expect(minSlider).toHaveTextContent('2');
      expect(maxSlider).toHaveTextContent('10');
    });

    it('renders the repositories filter when loading prop is false', function () {
      renderComponent({
        ...filterData,
        repositories: [
          { fullName: 'foo - 12345-67890', id: '1', name: 'foo - 12345' },
          { fullName: 'bar - 12345-67890', id: '2', name: 'bar - 12345' },
          { fullName: 'foobar - 12345-67890', id: '3', name: 'foobar - 12345' },
          { fullName: 'test - 12345-67890', id: '4', name: 'test - 12345' },
        ],
        showRepositoriesFilter: true,
        loading: false,
        toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
        toggleFilter: toggleFilterSpy,
      });

      const [, repositoriesFilter] = getInnerFilters();

      const allNone = within(repositoriesFilter).getByRole('menuitemcheckbox', { name: 'all/none' });
      const repo1 = within(repositoriesFilter).getByRole('menuitemcheckbox', { name: 'foo - 12345' });
      const repo2 = within(repositoriesFilter).getByRole('menuitemcheckbox', { name: 'bar - 12345' });
      const repo3 = within(repositoriesFilter).getByRole('menuitemcheckbox', { name: 'foobar - 12345' });
      const repo4 = within(repositoriesFilter).getByRole('menuitemcheckbox', { name: 'test - 12345' });

      expect(allNone).toBeVisible();
      expect(repo1).toBeVisible();
      expect(repo2).toBeVisible();
      expect(repo3).toBeVisible();
      expect(repo4).toBeVisible();

      fireEvent.click(allNone);
      fireEvent.click(repo1);
      fireEvent.click(repo2);
      fireEvent.click(repo3);
      fireEvent.click(repo4);

      expect(toggleFilterSpy).toHaveBeenCalledTimes(5);
    });

    it('renders a loading loadWrapper if it is loading', function () {
      renderComponent({ loading: true });
      const loading = screen.getByText('Loading…');

      expect(loading).toBeVisible();
    });

    it('renders the age filter based on showAgeFilter prop', function () {
      const { unmount } = renderComponent({
        ...filterData,
        showAgeFilter: true,
      });
      let filters = within(getFilter()).getAllByRole('group');
      expect(filters.length).toBe(6);
      expect(filters[4].id).toBe('age-filter');
      unmount();

      renderComponent({
        ...filterData,
        showAgeFilter: false,
      });
      filters = within(getFilter()).getAllByRole('menu');
      expect(filters.length).toBe(4);
    });

    it('renders the expiration date filter based on showExpirationDateFilter prop', function () {
      const selectExpirationDateSpy = jasmine.createSpy('selectExpirationDate');
      const { unmount } = renderComponent({
        ...filterData,
        selectExpirationDate: selectExpirationDateSpy,
        showExpirationDateFilter: true,
      });
      let filters = within(getFilter()).getAllByRole('group');
      expect(filters.length).toBe(6);
      expect(filters[4].id).toBe('expiration-date-filter');
      const expirationDatesGroup = within(filters[4]).getByRole('menu');
      expect(expirationDatesGroup.children.length).toBe(7);
      expect(expirationDatesGroup.children[0]).toHaveTextContent('all');
      expect(expirationDatesGroup.children[1]).toHaveTextContent('in 24 hours');
      expect(expirationDatesGroup.children[2]).toHaveTextContent('in 7 days');
      expect(expirationDatesGroup.children[3]).toHaveTextContent('in 30 days');
      expect(expirationDatesGroup.children[4]).toHaveTextContent('in 90 days');
      expect(expirationDatesGroup.children[5]).toHaveTextContent('in over 90 days');
      expect(expirationDatesGroup.children[6]).toHaveTextContent('never');
      fireEvent.click(expirationDatesGroup.children[1]);
      expect(selectExpirationDateSpy).toHaveBeenCalledWith('IN_24_HOURS');
      fireEvent.click(expirationDatesGroup.children[2]);
      expect(selectExpirationDateSpy).toHaveBeenCalledWith('IN_7_DAYS');
      fireEvent.click(expirationDatesGroup.children[3]);
      expect(selectExpirationDateSpy).toHaveBeenCalledWith('IN_30_DAYS');
      fireEvent.click(expirationDatesGroup.children[4]);
      expect(selectExpirationDateSpy).toHaveBeenCalledWith('IN_90_DAYS');
      fireEvent.click(expirationDatesGroup.children[5]);
      expect(selectExpirationDateSpy).toHaveBeenCalledWith('IN_OVER_90_DAYS');
      fireEvent.click(expirationDatesGroup.children[6]);
      expect(selectExpirationDateSpy).toHaveBeenCalledWith('NEVER');
      expect(selectExpirationDateSpy).toHaveBeenCalledTimes(6);
      unmount();

      renderComponent({
        ...filterData,
        showExpirationDateFilter: false,
      });
      filters = within(getFilter()).getAllByRole('menu');
      expect(filters.length).toBe(4);
    });
  });

  describe('applyCurrentFilter callback', function () {
    const selectedItems = {
      organizations: new Set(['666hell666']),
      applications: new Set(['777heaven777']),
      repositories: new Set(['repo123']),
      policyTypes: new Set(['QUALITY', 'OTHER', 'SECURITY']),
      stages: new Set(['release', 'stage-release', 'build']),
      categories: new Set([null]),
      policyViolationStates: new Set(['OPEN', 'WAIVED']),
      maxDaysOld: 90,
      policyThreatLevels: [3, 6],
      expirationDate: 'ALL',
    };
    const expectedJsonFilter = {
      organizationFilters: ['666hell666'],
      applicationFilters: ['777heaven777'],
      repositoryFilters: ['repo123'],
      policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
      stageTypeFilters: ['release', 'stage-release', 'build'],
      tagFilters: [null],
      policyViolationStates: ['OPEN', 'WAIVED'],
      maxDaysOld: 90,
      minPolicyThreatLevel: 3,
      maxPolicyThreatLevel: 6,
      expirationDate: 'ALL',
    };

    it('calls applyFilter action', function () {
      const applySpy = jasmine.createSpy('applyFilter');
      renderComponent({
        applyFilter: applySpy,
        selected: selectedItems,
        appliedFilterName: 'foo filter',
        filtersAreDirty: true,
      });
      const footer = getFooter();
      const [, , apply] = within(footer).getAllByRole('button');
      fireEvent.click(apply);
      expect(applySpy).toHaveBeenCalledWith(expectedJsonFilter, 'foo filter');
    });
  });

  describe('SaveFilterModal', function () {
    it('is rendered when showSaveFilterModal is true', function () {
      renderComponent({
        showSaveFilterModal: true,
      });
      const filter = getFilter();

      expect(filter).toHaveTextContent('Save Filter Modal');
    });

    it('is not rendered when showSaveFilterModal is false', function () {
      renderComponent({
        showSaveFilterModal: false,
      });
      const filter = getFilter();

      expect(filter).not.toHaveTextContent('Save Filter Modal');
    });
  });

  describe('Close button', function () {
    it('is not disabled and closes sidebar when filtersAreDirty and needsAcknowledgement are false', function () {
      const toggleFilterSidebarSpy = jasmine.createSpy('toggleFilterSidebar');
      renderComponent({
        toggleFilterSidebar: toggleFilterSidebarSpy,
      });
      const header = getHeader();

      const [close] = within(header).getAllByRole('button');
      fireEvent.click(close);
      expect(toggleFilterSidebarSpy).toHaveBeenCalledWith(false);
    });

    it('sets the tooltip to "Close" when filtersAreDirty and needsAcknowledgement are false', async function () {
      SpecUtil.requestIdleCallbackInvokeImmediate();
      renderComponent();
      const header = getHeader();

      fireEvent.mouseEnter(within(header).getAllByRole('button')[0]);
      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toHaveTextContent('Close');
    });

    describe('when filtersAreDirty', function () {
      it('is disabled', function () {
        const toggleFilterSidebarSpy = jasmine.createSpy('toggleFilterSidebar');
        renderComponent({
          toggleFilterSidebar: toggleFilterSidebarSpy,
          filtersAreDirty: true,
        });
        const header = getHeader();

        const [close] = within(header).getAllByRole('button');
        expect(close).toHaveClassName('disabled');
        fireEvent.click(close);
        expect(toggleFilterSidebarSpy).not.toHaveBeenCalled();
      });

      it('renders tooltip', async function () {
        SpecUtil.requestIdleCallbackInvokeImmediate();
        renderComponent({
          filtersAreDirty: true,
        });
        const header = getHeader();

        fireEvent.mouseEnter(within(header).getAllByRole('button')[0]);
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Please apply or revert filter');
      });
    });

    describe('when needsAcknowledgement', function () {
      it('is disabled', function () {
        const toggleFilterSidebarSpy = jasmine.createSpy('toggleFilterSidebar');
        renderComponent({
          toggleFilterSidebar: toggleFilterSidebarSpy,
          needsAcknowledgement: true,
        });
        const header = getHeader();

        const [close] = within(header).getAllByRole('button');
        expect(close).toHaveClassName('disabled');
        fireEvent.click(close);
        expect(toggleFilterSidebarSpy).not.toHaveBeenCalled();
      });

      it('renders tooltip', async function () {
        SpecUtil.requestIdleCallbackInvokeImmediate();
        renderComponent({
          needsAcknowledgement: true,
        });
        const header = getHeader();

        fireEvent.mouseEnter(within(header).getAllByRole('button')[0]);
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Please apply a filter');
      });
    });

    describe('when both needsAcknowledgement and filtersAreDirty', function () {
      it('renders needsAcknowledgement tooltip', async function () {
        SpecUtil.requestIdleCallbackInvokeImmediate();
        renderComponent({
          needsAcknowledgement: true,
          filtersAreDirty: true,
        });
        const header = getHeader();

        fireEvent.mouseEnter(within(header).getAllByRole('button')[0]);
        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Please apply a filter');
      });
    });
  });
});
