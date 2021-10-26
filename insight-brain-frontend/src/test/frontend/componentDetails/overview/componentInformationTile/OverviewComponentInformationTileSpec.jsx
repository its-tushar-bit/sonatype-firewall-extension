/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';

import OverviewComponentInformation from 'MainRoot/componentDetails/overview/componentInformationTile/OverviewComponentInformation';
import OverviewComponentInformationTile from 'MainRoot/componentDetails/overview/componentInformationTile/OverviewComponentInformationTile';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

describe('OverviewComponentInformationTile', () => {
  let minimalProps, overviewInfoProps, getShallow, getMounted;

  beforeEach(function () {
    overviewInfoProps = {
      componentInformation: {
        displayName: {
          parts: [{ field: 'Name', value: 'componentname' }],
        },
        matchState: 'unknown',
        pathnames: ['componentPath'],
      },
      toggleShowOccurrencesPopover: () => {},
      similarMatches: [],
      toggleShowSimilarMatches: () => {},
    };
    minimalProps = {
      ...overviewInfoProps,
      loading: false,
      loadError: null,
      loadReport: jasmine.createSpy('loadReport'),
    };

    getShallow = enzymeUtils.getShallowComponent(OverviewComponentInformationTile, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(OverviewComponentInformationTile, minimalProps);
  });

  it('calls `loadReport` when mounted', () => {
    const component = getMounted({ loading: true });
    expect(minimalProps.loadReport).toHaveBeenCalledTimes(1);
    component.unmount();
  });

  it('renders a loading indicator if component details are loading', () => {
    const component = getShallow({ loading: true });
    const loadWrapper = component.find(NxLoadWrapper);

    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', minimalProps.loadError);
    expect(loadWrapper).toHaveProp('retryHandler', minimalProps.loadReport);
  });

  it('renders an OverviewComponentInformation if component details are not loading', () => {
    const component = getShallow();
    const loadWrapper = component.find(NxLoadWrapper);
    const overviewComponentInformation = loadWrapper.dive().find(OverviewComponentInformation);

    expect(overviewComponentInformation).toHaveProp(overviewInfoProps);
  });
});
