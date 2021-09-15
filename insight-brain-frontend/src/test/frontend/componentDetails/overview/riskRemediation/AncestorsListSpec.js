/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { AncestorsList } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/AncestorsList';

describe('AncestorsList', () => {
  let minimalProps, getMounted;

  beforeEach(function () {
    minimalProps = {
      ancestors: [],
      routeName: 'applicationReport.componentDetails.overview',
    };

    getMounted = enzymeUtils.getMountedComponent(AncestorsList, minimalProps);
  });

  it('renders a component', () => {
    expect(getMounted()).toExist();
  });

  it('returns a list with one ancestor link', () => {
    const ancestors = [
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
    ];
    const component = getMounted({
      ancestors: ancestors,
    });
    expect(component).not.toBeNull();
    expect(component).toHaveProp('ancestors', ancestors);
    expect(component).toHaveProp('routeName', 'applicationReport.componentDetails.overview');
    const listElements = component.find('li');
    expect(listElements.length).toBe(1);
    const element = listElements.at(0);
    expect(element).toHaveText('org.springframework.data : spring-data-rest-hal-explorer : 3.4.11');
  });
});
