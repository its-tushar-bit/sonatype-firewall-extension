/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { DependencyInformation } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/DependencyInformation';

describe('DependencyInformation', () => {
  let minimalProps, getMounted;

  beforeEach(function () {
    minimalProps = {
      ancestors: [],
      routeName: 'applicationReport.componentDetails.overview',
    };

    getMounted = enzymeUtils.getMountedComponent(DependencyInformation, minimalProps);
  });

  it('renders a component', () => {
    expect(getMounted()).toExist();
  });

  it('header title and content should contain proper wording', () => {
    const component = getMounted();
    expect(component).not.toBeNull();

    const title = component.find('.nx-tile-header__title');
    expect(title).not.toBeNull();
    expect(title).toHaveText('Dependency Information');

    const content = component.find('.nx-tile-content');
    expect(content).not.toBeNull();
    const contentParagraph = component.find('p');
    expect(contentParagraph).toHaveText(
      'This dependency was brought in by the listed component(s). Clicking the component will take you to the associated component detail page'
    );
  });

  it('The Tile contains a list with one ancestor link', () => {
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

    const content = component.find('.nx-tile-content');
    expect(content).not.toBeNull();

    const listElements = content.find('li');
    expect(listElements.length).toBe(1);
    const element = listElements.at(0);
    expect(element).toHaveText('org.springframework.data : spring-data-rest-hal-explorer : 3.4.11');
  });
});
