/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { uncategorizedCategory } from '../../../../../main/frontend/dashboard/filter/staticFilterEntries';
import * as enzymeUtils from '../../../enzymeUtils';
import LegalDashboardFilterFooter from '../../../../../main/frontend/legal/dashboard/filter/LegalDashboardFilterFooter';
import LoadWrapper from '../../../../../main/frontend/react/LoadWrapper';
import { mount } from 'enzyme/build';
import React from 'react';
import { NxErrorAlert } from '@sonatype/react-shared-components';
import ManageFiltersDropdown from '../../../../../main/frontend/dashboard/filter/manageFiltersDropdown/ManageFiltersDropdown';

describe('LegalDashboardFilter', function () {
  let getShallowComponent,
    getMountedComponent,
    loadFilterSpy,
    minimalProps,
    SaveLegalFilterModalContainerMock,
    DeleteLegalFilterModalContainerMock,
    LegalDashboardFilter;

  const filterData = {
    organizations: [
      {
        id: '1234',
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        name: 'Org1',
        nameLowercaseNoWhitespace: 'org1',
      },
    ],
    applications: [
      {
        id: '456',
        publicId: 'App1',
        name: 'App1',
        organizationId: '123',
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
    progressOptions: [
      {
        id: 'NOT_STARTED',
        name: 'Unreviewed',
      },
      {
        id: 'OPEN',
        name: 'In Progress or Completed',
      },
    ],
    selected: {
      organizations: new Set(),
      applications: new Set(),
      categories: new Set(),
      stages: new Set(),
      progressOptions: new Set(),
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
      toggleFiltersDropdown: jasmine.createSpy('toggleFiltersDropdown'),
      handleDocumentClick: jasmine.createSpy('handleDocumentClick'),
    };

    SaveLegalFilterModalContainerMock = jasmine
      .createSpy('SaveFilterModalContainer')
      .and.returnValue(<div>Save Filter Modal</div>);

    DeleteLegalFilterModalContainerMock = jasmine
      .createSpy('DeleteFilterModalContainer')
      .and.returnValue(<div>Delete Filter Modal</div>);

    LegalDashboardFilter = require('inject-loader!../../../../../main/frontend/legal/dashboard/filter/LegalDashboardFilter')(
      {
        './SaveLegalFilterModalContainer': SaveLegalFilterModalContainerMock,
        './DeleteLegalFilterModalContainer': DeleteLegalFilterModalContainerMock,
      }
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardFilter, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(LegalDashboardFilter, minimalProps);
  });

  it('fires the loadFilter action', function () {
    const component = mount(<LegalDashboardFilter {...minimalProps} />);
    expect(loadFilterSpy).toHaveBeenCalled();
    component.unmount();
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
        header = shallowRender.find('.dashboard-filter-header');

      expect(header.childAt(0)).toMatchSelector('h3.nx-h3');
      expect(header.childAt(0)).toHaveText('Filter');

      expect(header.childAt(1).find(ManageFiltersDropdown)).toExist();
    });

    it('does not render ManageFiltersDropdown if loading', function () {
      const props = {
          appliedFilterName: 'some filter',
          showDirtyAsterisk: true,
          loading: true,
        },
        mountedRender = getMountedComponent(props),
        header = mountedRender.find('.dashboard-filter-header');

      expect(header.find('.iq-manage-filters-dropdown')).not.toExist();
    });

    it('does not render ManageFiltersDropdown if loadError', function () {
      const props = {
          appliedFilterName: 'some filter',
          showDirtyAsterisk: true,
          loadError: 'Error',
        },
        mountedRender = getMountedComponent(props),
        header = mountedRender.find('.dashboard-filter-header');

      expect(header.find('.iq-manage-filters-dropdown')).not.toExist();
    });
  });

  it('renders a LegalDashboardFilterFooter with the correct props', function () {
    const props = {
        applyFilterError: 'err',
        filtersAreDirty: true,
        needsAcknowledgement: true,
        setDisplaySaveFilterModal: jasmine.createSpy('setDisplaySaveFilterModal'),
        revert: jasmine.createSpy('revert'),
        applyFilterCancelled: () => {},
      },
      fullFilter = getShallowComponent(props),
      filterFooter = fullFilter.find(LegalDashboardFilterFooter);

    expect(filterFooter).toExist();
    expect(filterFooter).toHaveProp('applyFilterError', props.applyFilterError);
    expect(filterFooter).toHaveProp('filtersAreDirty', props.filtersAreDirty);
    expect(filterFooter).toHaveProp('needsAcknowledgement', props.needsAcknowledgement);
    expect(filterFooter).toHaveProp('revert', props.revert);
    expect(filterFooter).toHaveProp('setDisplaySaveFilterModal', props.setDisplaySaveFilterModal);
    expect(filterFooter).toHaveProp('onApplyCurrentFilter', jasmine.any(Function));
    expect(filterFooter).toHaveProp('onCancelApplyFilter', jasmine.any(Function));
  });

  describe('LegalDashboardFilter filter contents', function () {
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
        orgAppFilter = filterContent.find('#legal-org-app-filters'),
        categoryFilter = filterContent.find('#legal-category-filter'),
        stageFilter = filterContent.find('#legal-stage-filter'),
        progressOptionsFilter = filterContent.find('#legal-progress-options-filter');

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

      expect(progressOptionsFilter).toHaveProp('options', minimalProps.progressOptions);
      expect(progressOptionsFilter).toHaveProp('selectedIds', minimalProps.selected.stages);
      expect(progressOptionsFilter).toHaveProp('onChange');
      const selectedProgressOptions = ['build'];
      progressOptionsFilter.simulate('change', selectedStages);
      expect(toggleFilterSpy).toHaveBeenCalledWith('progressOptions', selectedProgressOptions);
    });

    it('renders a loading loadWrapper if it is loading', function () {
      const fullFilter = getShallowComponent({ loading: true }),
        loadWrapperElement = fullFilter.find(LoadWrapper);

      expect(loadWrapperElement).toHaveProp('loading', true);
    });

    it('passes loadFilter to loadWrapper as its retryHandler prop', function () {
      const fullFilter = getShallowComponent({ loading: true }),
        loadWrapperElement = fullFilter.find(LoadWrapper);

      expect(loadWrapperElement).toHaveProp('loading', true);
      expect(loadWrapperElement).toHaveProp('retryHandler', loadFilterSpy);
    });
  });

  describe('applyCurrentFilter callback', function () {
    const selectedItems = {
      organizations: new Set(['org1']),
      applications: new Set(['app1']),
      stages: new Set(['release', 'stage-release', 'build']),
      categories: new Set([null]),
      progressOptions: new Set(['NOT_REVIEWED']),
    };

    const expectedJsonFilter = {
      organizationFilters: ['org1'],
      applicationFilters: ['app1'],
      categoryFilters: [null],
      stageTypeFilters: ['release', 'stage-release', 'build'],
      progressOptionsFilters: ['NOT_REVIEWED'],
    };

    it('calls applyFilter action', function () {
      const applySpy = jasmine.createSpy('applyFilter'),
        shallowRender = getShallowComponent({
          applyFilter: applySpy,
          selected: selectedItems,
          appliedFilterName: 'foo filter',
        });

      shallowRender.find(LegalDashboardFilterFooter).simulate('applyCurrentFilter');
      expect(applySpy).toHaveBeenCalledWith(expectedJsonFilter, 'foo filter');
    });
  });

  describe('SaveFilterModal', function () {
    it('is rendered when showSaveFilterModal is true', function () {
      const shallowRender = getShallowComponent({
        showSaveFilterModal: true,
      });

      expect(shallowRender).toContainReact(<SaveLegalFilterModalContainerMock />);
    });

    it('is not rendered when showSaveFilterModal is false', function () {
      const shallowRender = getShallowComponent({
        showSaveFilterModal: false,
      });

      expect(shallowRender).not.toContainReact(<SaveLegalFilterModalContainerMock />);
    });
  });
});
