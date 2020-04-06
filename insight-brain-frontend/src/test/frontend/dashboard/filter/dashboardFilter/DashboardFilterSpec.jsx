/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mount } from 'enzyme';

import DashboardFilterHeader from
  '../../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilterHeader';
import DashboardFilterFooter from
  '../../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilterFooter';
import {
  ages,
  defaultMaxDaysOld,
  policyTypes,
  policyViolationStates,
  uncategorizedCategory
} from '../../../../../main/frontend/dashboard/filter/staticFilterEntries';
import * as enzymeUtils from '../../../enzymeUtils';
import LoadWrapper from '../../../../../main/frontend/react/LoadWrapper';

describe('DashboardFilter', function() {
  let getShallowComponent, loadFilterSpy, minimalProps, SaveFilterModalContainerMock, DashboardFilter;

  const filterData = {
    organizations: [
      {
        id: '666hell666',
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        name: 'Org1',
        nameLowercaseNoWhitespace: 'org1'
      }
    ],
    applications: [
      {
        id: '777heaven777',
        publicId: 'App1',
        name: 'App1',
        organizationId: '666hell666',
        organizationName: 'Org1'
      }
    ],
    categories: [uncategorizedCategory, { id: 'cat', name: 'Cat' }],
    stages: [
      {id: 'build', name: 'Build'},
      {id: 'stage-release', name: 'Stage Release'},
      {id: 'release', name: 'Release'},
      {id: 'operate', name: 'Operate'}
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
      policyThreatLevels: [2, 10]
    }
  };

  beforeEach(function() {
    loadFilterSpy = jasmine.createSpy('loadFilter');
    minimalProps = {
      ...filterData,
      loadFilter: loadFilterSpy,
      loading: false
    };
    SaveFilterModalContainerMock = jasmine.createSpy('SaveFilterModalContainer')
        .and.returnValue(<div>Save Filter Modal</div>);

    DashboardFilter = require(
        'inject-loader!../../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilter'
    )({
      '../manageFilterMenu/saveFilterModal/SaveFilterModalContainer': SaveFilterModalContainerMock
    }).default;

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardFilter, minimalProps);
  });

  /**
   * This is a functional component so it needs to be mounted
   * in order to test the calls that happen inside the `useEffect` hook.
   */
  it('fires the loadFilter action', function() {
    const component = mount(<DashboardFilter {...minimalProps} />);
    expect(loadFilterSpy).toHaveBeenCalled();
    component.unmount();
  });

  it('renders a DashboardFilterHeader with the correct props', function() {
    const props = {
          appliedFilterName: 'a filter name',
          showDirtyAsterisk: false,
          loadErrorFilterName: 'err filter'
        },
        fullFilter = getShallowComponent(props),
        filterHeader = fullFilter.find(DashboardFilterHeader);

    expect(filterHeader).toExist();
    expect(filterHeader).toHaveProp('appliedFilterName', props.appliedFilterName);
    expect(filterHeader).toHaveProp('showDirtyAsterisk', props.showDirtyAsterisk);
    expect(filterHeader).toHaveProp('loadErrorFilterName', props.loadErrorFilterName);
  });

  it('renders a DashboardFilterFooter with the correct props', function() {
    const props = {
          saveError: 'err',
          filtersAreDirty: true,
          needsAcknowledgement: true,
          setDisplaySaveFilterModal: jasmine.createSpy('setDisplaySaveFilterModal'),
          revert: jasmine.createSpy('revert')
        },
        fullFilter = getShallowComponent(props),
        filterFooter = fullFilter.find(DashboardFilterFooter);

    expect(filterFooter).toExist();
    expect(filterFooter).toHaveProp('saveError', props.saveError);
    expect(filterFooter).toHaveProp('filtersAreDirty', props.filtersAreDirty);
    expect(filterFooter).toHaveProp('needsAcknowledgement', props.needsAcknowledgement);
    expect(filterFooter).toHaveProp('revert', props.revert);
    expect(filterFooter).toHaveProp('setDisplaySaveFilterModal', props.setDisplaySaveFilterModal);
    expect(filterFooter).toHaveProp('onApplyCurrentFilter', jasmine.any(Function));
  });

  describe('DashboardFilter filter contents', function() {
    it('renders the filters if loading is false', function() {
      const toggleAppsAndOrgsSpy = jasmine.createSpy('toggleAppsAndOrgs'),
          toggleFilterSpy = jasmine.createSpy('toggleFilter'),
          filterContent = enzymeUtils.getLoadWrapperChildren(getShallowComponent({
            ...filterData,
            loading: false,
            toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
            toggleFilter: toggleFilterSpy
          })),
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
      const generatedTooltip = categoryFilter.prop('optionTooltipGenerator')(minimalProps.categories[1]);
      expect(generatedTooltip).toBe('Cat');

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

    it('renders a loading loadWrapper if it is loading', function() {
      const fullFilter = getShallowComponent({ loading: true }),
          loadWrapperElement = fullFilter.find(LoadWrapper);

      expect(loadWrapperElement).toHaveProp('loading', true);
    });

    it('passes loadFilter to loadWrapper as its retryHandler prop', function() {
      const fullFilter = getShallowComponent({ loading: true }),
          loadWrapperElement = fullFilter.find(LoadWrapper);

      expect(loadWrapperElement).toHaveProp('loading', true);
      expect(loadWrapperElement).toHaveProp('retryHandler', loadFilterSpy);
    });

    it('renders the age filter based on showAgeFilter prop', function() {
      let fullFilter, filterContent;

      fullFilter = getShallowComponent({
        ...filterData,
        showAgeFilter: true
      });
      filterContent = enzymeUtils.getLoadWrapperChildren(fullFilter);
      expect(filterContent.find('#age-filter')).toExist();

      fullFilter = getShallowComponent({
        ...filterData,
        showAgeFilter: false
      });
      filterContent = enzymeUtils.getLoadWrapperChildren(fullFilter);
      expect(filterContent.find('#age-filter')).not.toExist();
    });
  });

  describe('applyCurrentFilter callback', function() {
    const selectedItems = {
      organizations: new Set(['666hell666']),
      applications: new Set(['777heaven777']),
      policyTypes: new Set(['QUALITY', 'OTHER', 'SECURITY']),
      stages: new Set(['release', 'stage-release', 'build']),
      categories: new Set([null]),
      policyViolationStates: new Set(['OPEN', 'WAIVED']),
      maxDaysOld: 90,
      policyThreatLevels: [3, 6]
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
      maxPolicyThreatLevel: 6
    };

    it('calls applyFilter action', function() {
      const applySpy = jasmine.createSpy('applyFilter'),
          shallowRender = getShallowComponent({
            applyFilter: applySpy,
            selected: selectedItems,
            appliedFilterName: 'foo filter'
          });

      shallowRender.find(DashboardFilterFooter).simulate('applyCurrentFilter');
      expect(applySpy).toHaveBeenCalledWith(expectedJsonFilter, 'foo filter');
    });
  });

  describe('SaveFilterModal', function() {
    it('is rendered when showSaveFilterModal is true', function() {
      const shallowRender = getShallowComponent({
        showSaveFilterModal: true
      });

      expect(shallowRender).toContainReact(<SaveFilterModalContainerMock/>);
    });

    it('is not rendered when showSaveFilterModal is false', function() {
      const shallowRender = getShallowComponent({
        showSaveFilterModal: false
      });

      expect(shallowRender).not.toContainReact(<SaveFilterModalContainerMock/>);
    });
  });
});
