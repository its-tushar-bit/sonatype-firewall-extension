/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mount } from 'enzyme';

import { uncategorizedCategory } from '../../../../../main/frontend/dashboard/filter/staticFilterEntries';
import * as enzymeUtils from '../../../enzymeUtils';
import LegalDashboardFilterFooter from '../../../../../main/frontend/legal/dashboard/filter/LegalDashboardFilterFooter';
import LoadWrapper from '../../../../../main/frontend/react/LoadWrapper';

describe('LegalDashboardFilter', function() {
  let getShallowComponent, loadFilterSpy, minimalProps, LegalDashboardFilter;

  const filterData = {
    organizations: [
      {
        id: '1234',
        parentOrganizationId: 'ROOT_ORGANIZATION_ID',
        name: 'Org1',
        nameLowercaseNoWhitespace: 'org1'
      }
    ],
    applications: [
      {
        id: '456',
        publicId: 'App1',
        name: 'App1',
        organizationId: '123',
        organizationName: 'Org1'
      }
    ],
    categories: [uncategorizedCategory, { id: 'cat', name: 'Cat', owner: 'Org1' }],
    stages: [
      {id: 'build', name: 'Build'},
      {id: 'stage-release', name: 'Stage Release'},
      {id: 'release', name: 'Release'},
      {id: 'operate', name: 'Operate'}
    ],
    selected: {
      organizations: new Set(),
      applications: new Set(),
      categories: new Set(),
      stages: new Set()
    }
  };

  beforeEach(function() {
    loadFilterSpy = jasmine.createSpy('loadFilter');
    minimalProps = {
      ...filterData,
      loadFilter: loadFilterSpy,
      loading: false,
      applyDefaultFilter: jasmine.createSpy('applyDefaultFilter'),
      applySavedFilter: jasmine.createSpy('applySavedFilter'),
      toggleFiltersDropdown: jasmine.createSpy('toggleFiltersDropdown'),
      handleDocumentClick: jasmine.createSpy('handleDocumentClick')
    };

    LegalDashboardFilter = require(
        'inject-loader!../../../../../main/frontend/legal/dashboard/filter/LegalDashboardFilter'
    )({
    }).default;

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardFilter, minimalProps);
  });

  it('fires the loadFilter action', function() {
    const component = mount(<LegalDashboardFilter {...minimalProps} />);
    expect(loadFilterSpy).toHaveBeenCalled();
    component.unmount();
  });

  it('renders a LegalDashboardFilterFooter with the correct props', function() {
    const props = {
          applyFilterError: 'err',
          filtersAreDirty: true,
          needsAcknowledgement: true,
          setDisplaySaveFilterModal: jasmine.createSpy('setDisplaySaveFilterModal'),
          revert: jasmine.createSpy('revert'),
          applyFilterCancelled: () => {}
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

  describe('LegalDashboardFilter filter contents', function() {
    it('renders the filters if loading is false', function() {
      const toggleAppsAndOrgsSpy = jasmine.createSpy('toggleAppsAndOrgs'),
          toggleFilterSpy = jasmine.createSpy('toggleFilter'),
          filterContent = enzymeUtils.getLoadWrapperChildren(getShallowComponent({
            ...filterData,
            loading: false,
            toggleAppsAndOrgs: toggleAppsAndOrgsSpy,
            toggleFilter: toggleFilterSpy
          })),
          orgAppFilter = filterContent.find('#legal-org-app-filters'),
          categoryFilter = filterContent.find('#legal-category-filter'),
          stageFilter = filterContent.find('#legal-stage-filter');

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

      const noTooltipForUncategorizedApplications =
          categoryFilter.prop('optionTooltipGenerator')(minimalProps.categories[0]);
      const generatedTooltip = categoryFilter.prop('optionTooltipGenerator')(minimalProps.categories[1]);
      expect(noTooltipForUncategorizedApplications).toBe('');
      expect(generatedTooltip).toBe('in Org1');

      expect(stageFilter).toHaveProp('options', minimalProps.stages);
      expect(stageFilter).toHaveProp('selectedIds', minimalProps.selected.stages);
      expect(stageFilter).toHaveProp('onChange');
      const selectedStages = ['build'];
      stageFilter.simulate('change', selectedStages);
      expect(toggleFilterSpy).toHaveBeenCalledWith('stages', selectedStages);
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
  });
});
