/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { expandedProgressOptions } from '../../../../../main/frontend/legal/dashboard/legalDashboardConstants';

describe('LegalApplicationDetailsFilter', function () {
  let getShallowComponent, minimalProps, LegalApplicationDetailsFilter;

  const filterData = {
    licenseThreatGroups: ['Liberal', 'Weak Copyleft'],
    selected: {
      licenseThreatGroups: new Set(),
      progressOptions: new Set(),
    },
  };

  beforeEach(function () {
    minimalProps = {
      ...filterData,
      toggleFiltersDropdown: jasmine.createSpy('toggleFiltersDropdown'),
    };

    LegalApplicationDetailsFilter = require('inject-loader!../../../../../main/frontend/legal/application/filter/LegalApplicationDetailsFilter')(
      {}
    ).default;

    getShallowComponent = enzymeUtils.getShallowComponent(LegalApplicationDetailsFilter, minimalProps);
  });

  describe('LegalApplicationDetailsFilter filter contents', function () {
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
