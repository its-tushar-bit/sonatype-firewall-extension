/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { expandedProgressOptions } from '../../../../../main/frontend/legal/dashboard/legalDashboardConstants';
import { IqPopover } from '../../../../../main/frontend/react/IqPopover';

describe('LegalApplicationDetailsFilter', function () {
  let getShallowComponent, minimalProps, LegalApplicationDetailsFilter;
  const toggleFilterSidebarSpy = jasmine.createSpy('toggleFiltersDropdown');

  const filterData = {
    selected: {
      licenseThreatGroups: new Set(),
      progressOptions: new Set(),
    },
  };

  beforeEach(function () {
    minimalProps = {
      ...filterData,
      components: {
        licenseThreatGroups: ['Liberal', 'Weak Copyleft'],
      },
      toggleFilterSidebar: toggleFilterSidebarSpy,
    };

    LegalApplicationDetailsFilter = require('inject-loader!../../../../../main/frontend/legal/application/filter/LegalApplicationDetailsFilter')(
      {}
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(LegalApplicationDetailsFilter, minimalProps);
  });

  describe('framework', function () {
    it('renders an IqPopover', function () {
      const popover = getShallowComponent().find(IqPopover);
      expect(popover).toExist();
      expect(popover).toHaveProp('onClose', jasmine.any(Function));
      const onCloseFn = popover.prop('onClose');
      onCloseFn();
      expect(toggleFilterSidebarSpy).toHaveBeenCalledWith(false);
    });

    it('renders an IqPopover.Header', function () {
      const popoverHeader = getShallowComponent().find(IqPopover.Header).dive();
      expect(popoverHeader).toExist();
      const filterCloseButton = popoverHeader.find('#legal-dashboard-filter-close-btn');
      expect(filterCloseButton).toExist();
      filterCloseButton.simulate('click');
      expect(toggleFilterSidebarSpy).toHaveBeenCalledWith(false);
    });
  });

  describe('filter contents', function () {
    it('renders the filters', function () {
      const toggleFilterSpy = jasmine.createSpy('toggleFilter'),
        filterContent = getShallowComponent({
          ...filterData,
          toggleFilter: toggleFilterSpy,
        }),
        licenseThreatGroupsFilter = filterContent.find('#legal-license-threat-groups-filter'),
        progressOptionsFilter = filterContent.find('#legal-progress-options-filter');

      const expectedLicenseThreatGroups = [
        { id: 'Liberal', name: 'Liberal' },
        { id: 'Weak Copyleft', name: 'Weak Copyleft' },
      ];

      expect(licenseThreatGroupsFilter).toHaveProp('options', expectedLicenseThreatGroups);
      expect(licenseThreatGroupsFilter).toHaveProp('selectedIds', minimalProps.selected.licenseThreatGroups);
      expect(licenseThreatGroupsFilter).toHaveProp('onChange');
      const selectedLicenseThreatGroups = ['Liberal'];
      licenseThreatGroupsFilter.simulate('change', selectedLicenseThreatGroups);
      expect(toggleFilterSpy).toHaveBeenCalledWith('licenseThreatGroups', selectedLicenseThreatGroups);

      expect(progressOptionsFilter).toHaveProp('options', expandedProgressOptions);
      expect(progressOptionsFilter).toHaveProp('selectedIds', minimalProps.selected.progressOptions);
      expect(progressOptionsFilter).toHaveProp('onChange');
      const selectedProgressOptions = ['build'];
      progressOptionsFilter.simulate('change', selectedProgressOptions);
      expect(toggleFilterSpy).toHaveBeenCalledWith('progressOptions', selectedProgressOptions);
    });
  });
});
