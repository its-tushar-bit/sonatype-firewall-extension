/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import VersionGraphExplorer from '../../../../../main/frontend/componentDetails/overview/VersionGraphExplorer/VersionGraphExplorer';
import { RiskRemediation } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/RiskRemediation';

describe('ComponentDetailsOverviewRiskRemediation', () => {
  let minimalProps, getShallow, getMounted;

  beforeEach(function () {
    minimalProps = {
      directDependency: false,
      ancestors: [
        {
          componentIdentifier: {
            format: 'maven',
            coordinates: {
              artifactId: 'spring-data-rest-hal-explorer',
            },
          },
          hash: '502f98a535313e13cf18',
          derivedComponentName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
        },
      ],
      routeName: 'applicationReport.componentDetails.overview',
      requestVersionGraphData: jasmine.createSpy('requestVersionGraphData'),
      versionExplorerData: {
        loading: false,
        loadError: null,
        data: null,
      },
    };

    getShallow = enzymeUtils.getShallowComponent(RiskRemediation, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(RiskRemediation, minimalProps);
  });

  it('renders dependency information tile if it is not a direct dependency', () => {
    const component = getShallow(),
      dependencyInfoTile = component.find('.iq-dependency-information');

    expect(dependencyInfoTile).not.toBeNull();
    const ancestorsList = dependencyInfoTile.find('li');
    expect(ancestorsList.length).toBe(1);
  });

  it('does not render dependency information tile if it is a direct dependency', () => {
    const component = getShallow({ directDependency: true }),
      dependencyInfoTile = component.find('.iq-dependency-information');

    expect(dependencyInfoTile.length).toBe(0);
  });

  it('calls the requestVersionGraphData method when mounted and VersionGraphExplorer not to exists', () => {
    const component = getMounted().find(VersionGraphExplorer);
    expect(component).not.toExist();
    expect(minimalProps.requestVersionGraphData).toHaveBeenCalledTimes(1);
  });

  it('renders the VersionGraphExplorer', () => {
    const data = {
      version: '123',
      versions: {
        version: '2.0.4',
        versions: [],
      },
    };
    const component = getShallow({
      versionExplorerData: {
        loading: false,
        loadError: null,
        data: data,
      },
    }).find(VersionGraphExplorer);
    expect(component).toExist();
    expect(component).toHaveProp('data', data);
  });
});
