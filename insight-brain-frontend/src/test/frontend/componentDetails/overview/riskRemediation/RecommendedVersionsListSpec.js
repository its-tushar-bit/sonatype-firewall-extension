/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { RecommendedVersionsList } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/RecommendedVersionsList';

describe('RecommendedVersionsList', () => {
  let minimalProps, getMounted;

  beforeEach(function () {
    minimalProps = {
      actualVersion: '2.4.9',
    };

    getMounted = enzymeUtils.getMountedComponent(RecommendedVersionsList, minimalProps);
  });

  it('renders a component', () => {
    const versionChanges = [
      {
        id: 'no-versions-available',
        text: 'No recommended versions are available for the current component',
      },
    ];
    const component = getMounted({
      versionChanges: versionChanges,
    });
    expect(component).toExist();
  });

  it('with one component list if no remediation array is sent', () => {
    const versionChanges = [
      {
        id: 'no-versions-available',
        text: 'No recommended versions are available for the current component',
      },
    ];
    const component = getMounted({
      versionChanges: versionChanges,
    });
    expect(component).toHaveProp('versionChanges', versionChanges);
    expect(component).toHaveProp('actualVersion', '2.4.9');

    const listElements = component.find('.nx-list__item');
    expect(listElements.length).toBe(1);
    const element = listElements.at(0);
    expect(element).not.toBeNull();
    const subText = element.find('.nx-list__subtext');
    expect(subText).toHaveText('No recommended versions are available for the current component');
  });

  it('with two component list if remediation array is sent', () => {
    const versionChanges = [
      {
        id: 'next-no-violation-version-link',
        text: 'Next version with no policy violation',
        type: 'next-no-violations',
        version: '2.4.10',
      },
      {
        id: 'next-no-fail-dependencies-version',
        text: "The current version doesn't cause Build failure for this component and its dependencies",
        type: 'next-non-failing-with-dependencies',
        version: '2.4.9',
      },
    ];
    const component = getMounted({
      versionChanges: versionChanges,
    });
    expect(component).toHaveProp('versionChanges', versionChanges);
    expect(component).toHaveProp('actualVersion', '2.4.9');

    const listElements = component.find('.nx-list__item');
    expect(listElements.length).toBe(2);

    let element = listElements.at(0);
    expect(element).not.toBeNull();
    let text = element.find('.nx-list__text');
    expect(text).toHaveText('Upgrade to 2.4.10');
    let subText = element.find('.nx-list__subtext');
    expect(subText).toHaveText('Next version with no policy violation');
    let button = element.find('.nx-btn');
    expect(button).toHaveText('Compare');

    element = listElements.at(1);
    expect(element).not.toBeNull();
    subText = element.find('.nx-list__subtext');
    expect(subText).toHaveText(
      "The current version doesn't cause Build failure for this component and its dependencies"
    );
  });
});
