/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxErrorAlert } from '@sonatype/react-shared-components';

import DashboardFilterFooter from '../../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilterFooter';
import {
  ages,
  defaultMaxDaysOld,
  policyTypes,
  policyViolationStates,
  uncategorizedCategory,
} from '../../../../../main/frontend/dashboard/filter/staticFilterEntries';
import * as enzymeUtils from '../../../enzymeUtils';
import LoadWrapper from '../../../../../main/frontend/react/LoadWrapper';
import ManageFiltersDropdown from '../../../../../main/frontend/dashboard/filter/manageFiltersDropdown/ManageFiltersDropdown';
import DeleteFilterModalContainer from '../../../../../main/frontend/dashboard/filter/deleteFilterModal/DeleteFilterModalContainer';
import { IqPopoverHeader } from '../../../../../main/frontend/react/IqPopover';

describe('DashboardFilter', function () {
  let getShallowComponent, loadFilterSpy, minimalProps, SaveFilterModalContainerMock, DashboardFilter;

  const filterData = {
    organizations: [
      {
        id: '666hell666',
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        name: 'Org1',
        nameLowercaseNoWhitespace: 'org1',
      },
    ],
    applications: [
      {
        id: '777heaven777',
        publicId: 'App1',
        name: 'App1',
        organizationId: '666hell666',
        organizationName: 'Org1',
      },
    ],
    categories: [uncategorizedCategory, { id: 'cat', name: 'Cat', owner: 'Org1' }],
    stages: [
      { id: 'build', name: 'Build' },
      { id: 'stage-release', name: 'Stage Release' },
      { id: 'release', name: 'Release' },
      { id: 'operate', name: 'Operate' },
    ],
    ages: [...ages],
    policyTypes: [...policyTypes],
    policyViolationStates: [...policyViolationStates],
    selected: {
      organizations: new Set(),
      applications: new Set(),
      categories: new Set(),
      stages: new Set(),
      policyTypes: new Set(),
      policyViolationStates: new Set(),
      maxDaysOld: defaultMaxDaysOld,
      policyThreatLevels: [2, 10],
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
      applyDefaultFilter: jasmine.createSpy('applyDefaultFilter'),
      applySavedFilter: jasmine.createSpy('applySavedFilter'),
    };
    SaveFilterModalContainerMock = jasmine
      .createSpy('SaveFilterModalContainer')
      .and.returnValue(<div>Save Filter Modal</div>);

    DashboardFilter = require('inject-loader!../../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilter')(
      {
        '../saveFilterModal/SaveFilterModalContainer': SaveFilterModalContainerMock,
      }
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardFilter, minimalProps);
  });

  describe('apply named filter error', function () {
    it('is rendered within the header if loadErrorFilterName is not null', function () {
      const props = { loadErrorFilterName: 'filter 1234' },
        shallowRender = getShallowComponent(props),
        header = shallowRender.find('.dashboard-filter-header');

      expect(header).toContainReact(<NxErrorAlert>Failed to load filter 1234</NxErrorAlert>);
    });

    it('is not rendered if loadErrorFilterName is null', function () {
      const props = { loadErrorFilterName: null },
        shallowRender = getShallowComponent(props),
        error = shallowRender.find('.nx-alert');
      expect(error).not.toExist();
    });
  });

  describe('filter header', function () {
    it('renders ManageFiltersDropdown outside of header element', function () {
      const props = {
          appliedFilterName: 'some filter',
          showDirtyAsterisk: true,
        },
        shallowRender = getShallowComponent(props),
        header = shallowRender.find(IqPopoverHeader);

      expect(header).toHaveProp('headerTitle', 'Filter');

      expect(header.dive().childAt(1)).toContainReact(
        <ManageFiltersDropdown
          appliedFilterName="some filter"
          showDirtyAsterisk={true}
          savedFilters={savedFilters}
          applyDefaultFilter={minimalProps.applyDefaultFilter}
          applySavedFilter={minimalProps.applySavedFilter}
          DeleteFilterModal={DeleteFilterModalContainer}
        />
      );
    });

    it('does not render ManageFiltersDropdown if loading', function () {
      const props = {
          appliedFilterName: 'some filter',
          showDirtyAsterisk: true,
          loading: true,
        },
        shallowRender = getShallowComponent(props),
        header = shallowRender.find('.dashboard-filter-header');

      expect(header).not.toContainMatchingElement(ManageFiltersDropdown);
    });

    it('does not render ManageFiltersDropdown if loadError', function () {
      const props = {
          appliedFilterName: 'some filter',
          showDirtyAsterisk: true,
          loadError: 'Error',
        },
        shallowRender = getShallowComponent(props),
        header = shallowRender.find('.dashboard-filter-header');

      expect(header).not.toContainMatchingElement(ManageFiltersDropdown);
    });
  });

  it('renders a DashboardFilterFooter with the correct props', function () {
    const props = {
        applyFilterError: 'Error',
        filtersAreDirty: true,
        needsAcknowledgement: true,
        setDisplaySaveFilterModal: jasmine.createSpy('setDisplaySaveFilterModal'),
        revert: jasmine.createSpy('revert'),
        applyFilterCancelled: () => {},
      },
      fullFilter = getShallowComponent(props),
      filterFooter = fullFilter.find(DashboardFilterFooter);

    expect(filterFooter).toExist();
    expect(filterFooter).toHaveProp('applyFilterError', props.applyFilterError);
    expect(filterFooter).toHaveProp('filtersAreDirty', props.filtersAreDirty);
    expect(filterFooter).toHaveProp('needsAcknowledgement', props.needsAcknowledgement);
    expect(filterFooter).toHaveProp('revert', props.revert);
    expect(filterFooter).toHaveProp('setDisplaySaveFilterModal', props.setDisplaySaveFilterModal);
    expect(filterFooter).toHaveProp('onApplyCurrentFilter', jasmine.any(Function));
    expect(filterFooter).toHaveProp('onCancelApplyFilter', jasmine.any(Function));
  });

  describe('DashboardFilter filter contents', function () {
    it('renders the filters if loading is false', function () {
      const toggleAppsAndOrgsSpy = jasmine.createSpy('toggleAppsAndOrgs'),
        toggleFilterSpy = jasmine.createSpy('toggleFilter'),
        filterContent = enzymeUtils.getLoadWrapperChildren(
          getShallowComponent({
            ...filterData,
            loading: false,
            toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
            toggleFilter: toggleFilterSpy,
          })
        ),
        orgAppFilter = filterContent.find('#org-app-filters'),
        categoryFilter = filterContent.find('#category-filter'),
        stageFilter = filterContent.find('#stage-filter'),
        policyTypeFilter = filterContent.find('#policy-type-filter'),
        policyViolationStateFilter = filterContent.find('#policy-violation-state-filter'),
        threatLevelFiler = filterContent.find('#threat-level-filter');

      expect(orgAppFilter).toHaveProp('organizations', minimalProps.organizations);
      expect(orgAppFilter).toHaveProp('applications', minimalProps.applications);
      expect(orgAppFilter).toHaveProp('selectedApplications', minimalProps.selected.applications);
      expect(orgAppFilter).toHaveProp('selectedOrganizations', minimalProps.selected.organizations);
      expect(orgAppFilter).toHaveProp('onChange', toggleAppsAndOrgsSpy);
      orgAppFilter.simulate('change');
      expect(toggleAppsAndOrgsSpy).toHaveBeenCalled();

      expect(categoryFilter).toHaveProp('options', minimalProps.categories);
      expect(categoryFilter).toHaveProp('selectedIds', minimalProps.selected.categories);
      expect(categoryFilter).toHaveProp('onChange');
      expect(categoryFilter).toHaveProp('optionTooltipGenerator');
      const selectedCategories = [null];
      categoryFilter.simulate('change', selectedCategories);
      expect(toggleFilterSpy).toHaveBeenCalledWith('categories', selectedCategories);

      const noTooltipForUncategorizedApplications = categoryFilter.prop('optionTooltipGenerator')(
        minimalProps.categories[0]
      );
      const generatedTooltip = categoryFilter.prop('optionTooltipGenerator')(minimalProps.categories[1]);
      expect(noTooltipForUncategorizedApplications).toBe('');
      expect(generatedTooltip).toBe('in Org1');

      expect(stageFilter).toHaveProp('options', minimalProps.stages);
      expect(stageFilter).toHaveProp('selectedIds', minimalProps.selected.stages);
      expect(stageFilter).toHaveProp('onChange');
      const selectedStages = ['build'];
      stageFilter.simulate('change', selectedStages);
      expect(toggleFilterSpy).toHaveBeenCalledWith('stages', selectedStages);

      expect(policyTypeFilter).toHaveProp('options', minimalProps.policyTypes);
      expect(policyTypeFilter).toHaveProp('selectedIds', minimalProps.selected.policyTypes);
      expect(policyTypeFilter).toHaveProp('onChange');
      const selectedPolicyTypes = ['SECURITY'];
      policyTypeFilter.simulate('change', selectedPolicyTypes);
      expect(toggleFilterSpy).toHaveBeenCalledWith('policyTypes', selectedPolicyTypes);

      expect(policyViolationStateFilter).toHaveProp('options', minimalProps.policyViolationStates);
      expect(policyViolationStateFilter).toHaveProp('selectedIds', minimalProps.selected.policyViolationStates);
      expect(policyViolationStateFilter).toHaveProp('onChange');
      const selectedPolicyViolationStates = ['OPEN', 'WAIVED'];
      policyViolationStateFilter.simulate('change', selectedPolicyViolationStates);
      expect(toggleFilterSpy).toHaveBeenCalledWith('policyViolationStates', selectedPolicyViolationStates);

      expect(threatLevelFiler).toHaveProp('value', minimalProps.selected.policyThreatLevels);
      expect(threatLevelFiler).toHaveProp('onChange');
      const selectedThreatLevels = [5, 8];
      threatLevelFiler.simulate('change', selectedThreatLevels);
      expect(toggleFilterSpy).toHaveBeenCalledWith('policyThreatLevels', selectedThreatLevels);
    });

    it('renders a loading loadWrapper if it is loading', function () {
      const fullFilter = getShallowComponent({ loading: true }),
        loadWrapperElement = fullFilter.find(LoadWrapper);

      expect(loadWrapperElement).toHaveProp('loading', true);
    });

    it('passes retryHandler to LoadWrapper that calls loadFilter with no args', function () {
      const fullFilter = getShallowComponent({ loading: true }),
        loadWrapperElement = fullFilter.find(LoadWrapper);

      expect(loadWrapperElement).toHaveProp('loading', true);
      loadWrapperElement.prop('retryHandler')();
      expect(loadFilterSpy).toHaveBeenCalled();
      expect(loadFilterSpy.calls.count()).toEqual(1);
      expect(loadFilterSpy.calls.argsFor(0)).toEqual([]);
    });

    it('renders the age filter based on showAgeFilter prop', function () {
      let fullFilter, filterContent;

      fullFilter = getShallowComponent({
        ...filterData,
        showAgeFilter: true,
      });
      filterContent = enzymeUtils.getLoadWrapperChildren(fullFilter);
      expect(filterContent.find('#age-filter')).toExist();

      fullFilter = getShallowComponent({
        ...filterData,
        showAgeFilter: false,
      });
      filterContent = enzymeUtils.getLoadWrapperChildren(fullFilter);
      expect(filterContent.find('#age-filter')).not.toExist();
    });
  });

  describe('applyCurrentFilter callback', function () {
    const selectedItems = {
      organizations: new Set(['666hell666']),
      applications: new Set(['777heaven777']),
      policyTypes: new Set(['QUALITY', 'OTHER', 'SECURITY']),
      stages: new Set(['release', 'stage-release', 'build']),
      categories: new Set([null]),
      policyViolationStates: new Set(['OPEN', 'WAIVED']),
      maxDaysOld: 90,
      policyThreatLevels: [3, 6],
    };
    const expectedJsonFilter = {
      organizationFilters: ['666hell666'],
      applicationFilters: ['777heaven777'],
      policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
      stageTypeFilters: ['release', 'stage-release', 'build'],
      tagFilters: [null],
      policyViolationStates: ['OPEN', 'WAIVED'],
      maxDaysOld: 90,
      minPolicyThreatLevel: 3,
      maxPolicyThreatLevel: 6,
    };

    it('calls applyFilter action', function () {
      const applySpy = jasmine.createSpy('applyFilter'),
        shallowRender = getShallowComponent({
          applyFilter: applySpy,
          selected: selectedItems,
          appliedFilterName: 'foo filter',
        });

      shallowRender.find(DashboardFilterFooter).simulate('applyCurrentFilter');
      expect(applySpy).toHaveBeenCalledWith(expectedJsonFilter, 'foo filter');
    });
  });

  describe('SaveFilterModal', function () {
    it('is rendered when showSaveFilterModal is true', function () {
      const shallowRender = getShallowComponent({
        showSaveFilterModal: true,
      });

      expect(shallowRender).toContainReact(<SaveFilterModalContainerMock />);
    });

    it('is not rendered when showSaveFilterModal is false', function () {
      const shallowRender = getShallowComponent({
        showSaveFilterModal: false,
      });

      expect(shallowRender).not.toContainReact(<SaveFilterModalContainerMock />);
    });
  });

  describe('Close button', function () {
    it('is not disabled and closes sidebar when filtersAreDirty and needsAcknowledgement are false', function () {
      const toggleFilterSidebarSpy = jasmine.createSpy('toggleFilterSidebar'),
        shallowRender = getShallowComponent({
          toggleFilterSidebar: toggleFilterSidebarSpy,
        });

      const header = shallowRender.find(IqPopoverHeader).dive();
      const closeButton = header.find('#dashboard-filter-close-btn');
      expect(closeButton).not.toHaveClassName('disabled');
      closeButton.simulate('click');
      expect(toggleFilterSidebarSpy).toHaveBeenCalledWith(false);
    });

    it('sets the tooltip to "Close" when filtersAreDirty and needsAcknowledgement are false', function () {
      const shallowRender = getShallowComponent();

      const header = shallowRender.find(IqPopoverHeader).dive();
      const btn = header.find('#dashboard-filter-close-btn');
      expect(btn).toHaveProp('title', 'Close');
    });

    describe('when filtersAreDirty', function () {
      it('is disabled', function () {
        const toggleFilterSidebarSpy = jasmine.createSpy('toggleFilterSidebar'),
          shallowRender = getShallowComponent({
            toggleFilterSidebar: toggleFilterSidebarSpy,
            filtersAreDirty: true,
          });

        const header = shallowRender.find(IqPopoverHeader).dive();
        const closeButton = header.find('#dashboard-filter-close-btn');
        expect(closeButton).toHaveClassName('disabled');
        closeButton.simulate('click');
        expect(toggleFilterSidebarSpy).not.toHaveBeenCalled();
      });

      it('renders tooltip', function () {
        const shallowRender = getShallowComponent({
          filtersAreDirty: true,
        });

        const header = shallowRender.find(IqPopoverHeader).dive();
        const btn = header.find('#dashboard-filter-close-btn');
        expect(btn).toHaveProp('title', 'Please apply or revert filter');
      });
    });

    describe('when needsAcknowledgement', function () {
      it('is disabled', function () {
        const toggleFilterSidebarSpy = jasmine.createSpy('toggleFilterSidebar'),
          shallowRender = getShallowComponent({
            toggleFilterSidebar: toggleFilterSidebarSpy,
            needsAcknowledgement: true,
          });

        const header = shallowRender.find(IqPopoverHeader).dive();
        const closeButton = header.find('#dashboard-filter-close-btn');
        expect(closeButton).toHaveClassName('disabled');
        closeButton.simulate('click');
        expect(toggleFilterSidebarSpy).not.toHaveBeenCalled();
      });

      it('renders tooltip', function () {
        const shallowRender = getShallowComponent({
          needsAcknowledgement: true,
        });

        const header = shallowRender.find(IqPopoverHeader).dive();
        const btn = header.find('#dashboard-filter-close-btn');
        expect(btn).toHaveProp('title', 'Please apply a filter');
      });
    });

    describe('when both needsAcknowledgement and filtersAreDirty', function () {
      it('renders needsAcknowledgement tooltip', function () {
        const shallowRender = getShallowComponent({
          needsAcknowledgement: true,
          filtersAreDirty: true,
        });

        const header = shallowRender.find(IqPopoverHeader).dive();
        const btn = header.find('#dashboard-filter-close-btn');
        expect(btn).toHaveProp('title', 'Please apply a filter');
      });
    });
  });
});
