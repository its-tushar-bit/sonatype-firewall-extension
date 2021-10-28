/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxLoadWrapper } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';

import VulnerabilitiesTableTile from 'MainRoot/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTableTile';
import VulnerabilitiesTable from 'MainRoot/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTable';
import { VulnerabilityDetailsPopoverContainer } from 'MainRoot/componentDetails/VulnerabilitiesTableTile/VulnerabilityDetailsPopoverContainer';

describe('VulnerabilitiesTableTile', () => {
  let minimalProps, tableProps, getShallow;
  jasmine.createSpy('loadVulnerabilities');
  const toggleVulnerabilityPopoverWithEffects = jasmine.createSpy('toggleVulnerabilityPopoverWithEffects');

  beforeEach(() => {
    tableProps = {
      vulnerabilities: {
        data: ['item1', 'item2'],
        loading: false,
        error: null,
      },
      loadVulnerabilities: () => {},
      toggleVulnerabilityPopoverWithEffects,
    };
    minimalProps = {
      ...tableProps,
      isLoadingComponentDetails: false,
      componentDetailsLoadError: null,
      loadComponentDetails: () => {},
    };
    getShallow = enzymeUtils.getShallowComponent(VulnerabilitiesTableTile, minimalProps);
  });

  it('renders the title correctly', () => {
    const component = getShallow();
    const title = component.find('#component-details-vulnerabilities-title');
    expect(title).toHaveText('Vulnerabilities');
  });

  it('renders a loading indicator if component details are loading', () => {
    const component = getShallow({ isLoadingComponentDetails: true });
    const loadWrapper = component.find(NxLoadWrapper);

    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', minimalProps.componentDetailsLoadError);
    expect(loadWrapper).toHaveProp('retryHandler', minimalProps.loadComponentDetails);
  });

  it('renders a VulnerabilityDetailsPopoverContainer', () => {
    const component = getShallow();
    const popover = component.find(VulnerabilityDetailsPopoverContainer);

    expect(popover).toExist();
  });

  it('renders the VulnerabilitiesTable if component details are not loading', () => {
    const component = getShallow();
    const loadWrapper = component.find(NxLoadWrapper);
    const table = loadWrapper.dive().find(VulnerabilitiesTable);

    expect(table).toExist();
    expect(table).toHaveProp(tableProps);
  });
});
