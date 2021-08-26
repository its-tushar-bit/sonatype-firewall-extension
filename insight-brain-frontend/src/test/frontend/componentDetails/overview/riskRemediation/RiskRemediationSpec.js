/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { RiskRemediation } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/RiskRemediation';

describe('ComponentDetailsOverviewRiskRemediation', () => {
  let minimalProps, getMounted;

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
    };

    getMounted = enzymeUtils.getMountedComponent(RiskRemediation, minimalProps);
  });

  it('renders dependency information tile if it is not a direct dependency', () => {
    const component = getMounted(),
      dependencyInfoTile = component.find('.iq-dependency-information');

    expect(dependencyInfoTile).not.toBeNull();
    const ancestorsList = dependencyInfoTile.find('li');
    expect(ancestorsList.length).toBe(1);
  });

  it('does not render dependency information tile if it is a direct dependency', () => {
    const component = getMounted({ directDependency: true }),
      dependencyInfoTile = component.find('.iq-dependency-information');

    expect(dependencyInfoTile.length).toBe(0);
  });
});
