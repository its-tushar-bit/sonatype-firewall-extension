/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { AncestorsList } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/AncestorsList';
import { NxTextLink } from '@sonatype/react-shared-components';
import { DependencyTypeTag } from 'MainRoot/react/tag';

describe('AncestorsList', () => {
  let minimalProps, getMounted;

  beforeEach(function () {
    minimalProps = {
      dependencyTreeSubset: [],
      routeName: 'applicationReport.componentDetails.overview',
    };

    getMounted = enzymeUtils.getMountedComponent(AncestorsList, minimalProps);
  });

  it('renders a component', () => {
    expect(getMounted()).toExist();
  });

  it('returns a list with one ancestor link', () => {
    const dependencyTreeSubset = [
      {
        hash: '502f98a535313e13cf18',
        displayName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
      },
    ];
    const component = getMounted({
      dependencyTreeSubset,
    });
    expect(component).not.toBeNull();
    expect(component).toHaveProp('dependencyTreeSubset', dependencyTreeSubset);
    expect(component).toHaveProp('routeName', 'applicationReport.componentDetails.overview');
    const listElements = component.find('li');
    expect(listElements.length).toBe(1);
    const element = listElements.at(0);
    const links = element.find(NxTextLink);
    expect(links.length).toBe(1);
    expect(links.at(0)).toHaveText('org.springframework.data : spring-data-rest-hal-explorer : 3.4.11');
    const tags = element.find(DependencyTypeTag);
    expect(tags.length).toBe(0);
  });

  it('returns a list with one ancestor link with an InnerSource label', () => {
    const dependencyTreeSubset = [
      {
        hash: 'some-innersource-parent-hash',
        displayName: 'innersource-parent',
        isInnerSource: true,
      },
    ];
    const component = getMounted({
      dependencyTreeSubset,
    });
    expect(component).not.toBeNull();
    expect(component).toHaveProp('dependencyTreeSubset', dependencyTreeSubset);
    expect(component).toHaveProp('routeName', 'applicationReport.componentDetails.overview');
    const listElements = component.find('li');
    expect(listElements.length).toBe(1);
    const element = listElements.at(0);
    const links = element.find(NxTextLink);
    expect(links.length).toBe(1);
    expect(links.at(0)).toHaveText('innersource-parent');
    const tags = element.find(DependencyTypeTag);
    expect(tags.length).toBe(1);
    expect(tags.at(0)).toHaveText('InnerSource');
  });
});
