/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxErrorAlert } from '@sonatype/react-shared-components';

import DashboardFilterHeader from
  '../../../../../main/frontend/dashboard/filter/dashboardFilter/DashboardFilterHeader';
import * as enzymeUtils from '../../../enzymeUtils';

describe('DashboardFilter header', function() {
  const getShallowComponent = enzymeUtils.getShallowComponent(DashboardFilterHeader, {});

  it('renders a section with the header classes', function() {
    const fullFilter = getShallowComponent(),
        header = fullFilter.find('.dashboard-filter-header');

    expect(header).toExist();
  });

  it('renders an error alert if the loading failed', function() {
    const props = { loadErrorFilterName: 'a filter' },
        fullFilter = getShallowComponent(props),
        errorAlert = fullFilter.find(NxErrorAlert);

    expect(fullFilter).toExist();
    expect(errorAlert).toExist();
    expect(errorAlert.text()).toContain(`Failed to load ${props.loadErrorFilterName}`);
  });

  it('renders the selected filter name if loaded correctly', function() {
    const props = { appliedFilterName: 'some filter'},
        fullFilter = getShallowComponent(props),
        filterName = fullFilter.find('.dashboard-filter-name');

    expect(filterName).toExist();
    expect(filterName.text()).toContain(props.appliedFilterName);
  });

  it('renders an asterisk if the filters are dirty', function() {
    const props = {
          appliedFilterName: 'some filter',
          showDirtyAsterisk: true
        },
        fullFilter = getShallowComponent(props),
        filterAsterisk = fullFilter.find('.dashboard-filter-dirty-asterisk');

    expect(filterAsterisk).toExist();
    expect(filterAsterisk.text()).toContain('*');
  });

  it('does not renders an asterisk if the filters are not dirty', function() {
    const props = {
          appliedFilterName: 'some filter'
        },
        fullFilter = getShallowComponent(props),
        filterAsterisk = fullFilter.find('.dashboard-filter-dirty-asterisk');

    expect(filterAsterisk).not.toExist();
  });
});
