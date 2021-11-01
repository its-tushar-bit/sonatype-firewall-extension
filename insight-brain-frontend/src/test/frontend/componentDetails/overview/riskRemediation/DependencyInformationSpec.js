/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { DependencyInformation } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/DependencyInformation';
import * as enzymeUtils from '../../../enzymeUtils';

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

    const title = component.find('.nx-grid-header__title');
    expect(title).not.toBeNull();
    expect(title).toHaveText('Dependency Information');

    const content = component.find('.nx-list');
    expect(content).not.toBeNull();
    const contentParagraph = component.find('p');
    expect(contentParagraph).toHaveText(
      'This dependency was brought in by the component(s) listed below. Clicking on a component will take you to its Component Details Page.'
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

    const content = component.find('AncestorsList');
    expect(content).not.toBeNull();

    const listElements = content.find('li');
    expect(listElements.length).toBe(1);
    const element = listElements.at(0);
    expect(element).toHaveText('org.springframework.data : spring-data-rest-hal-explorer : 3.4.11');
  });

  it('The Tile contains a list with three ancestor links', () => {
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
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'spring-boot-actuator-autoconfigure',
          },
        },
        hash: '502f98a535313e13cf18',
        derivedComponentName: 'org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9',
      },
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jackson-module-parameter-names',
          },
        },
        hash: '502f98a535313e13cf18',
        derivedComponentName: 'com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4',
      },
    ];
    const component = getMounted({
      ancestors,
    });
    expect(component).not.toBeNull();
    expect(component).toHaveProp('ancestors', ancestors);
    expect(component).toHaveProp('routeName', 'applicationReport.componentDetails.overview');

    const content = component.find('AncestorsList');
    expect(content).not.toBeNull();

    const listElements = content.find('li');
    expect(listElements.length).toBe(3);
    const element = listElements.at(0);
    expect(element).toHaveText('org.springframework.data : spring-data-rest-hal-explorer : 3.4.11');
    const element1 = listElements.at(1);
    expect(element1).toHaveText('org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9');
    const element2 = listElements.at(2);
    expect(element2).toHaveText('com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4');
  });

  it('The Tile contains a list with three ancestor links and a show more link', () => {
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
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'spring-boot-actuator-autoconfigure',
          },
        },
        hash: '502f98a535313e13cf18',
        derivedComponentName: 'org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9',
      },
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jackson-module-parameter-names',
          },
        },
        hash: '502f98a535313e13cf18',
        derivedComponentName: 'com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4',
      },
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jackson-datatype-jdk8',
          },
        },
        hash: '502f98a535313e13cf18',
        derivedComponentName: 'com.fasterxml.jackson.datatype : jackson-datatype-jdk8 : 2.11.4',
      },
    ];
    const component = getMounted({
      ancestors,
      expanded: false,
    });
    expect(component).not.toBeNull();
    expect(component).toHaveProp('ancestors', ancestors);
    expect(component).toHaveProp('routeName', 'applicationReport.componentDetails.overview');

    const content = component.find('AncestorsList');
    expect(content).not.toBeNull();

    const listElements = content.find('li');
    expect(listElements.length).toBe(3);
    const element = listElements.at(0);
    expect(element).toHaveText('org.springframework.data : spring-data-rest-hal-explorer : 3.4.11');
    const element1 = listElements.at(1);
    expect(element1).toHaveText('org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9');
    const element2 = listElements.at(2);
    expect(element2).toHaveText('com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4');
    const showMore = content.find('.iq-toggle-list').last();
    expect(showMore).toHaveText('Show more');
  });

  it('The Tile contains a list with four ancestor links and a show less link', () => {
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
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'spring-boot-actuator-autoconfigure',
          },
        },
        hash: '502f98a535313e13cf18',
        derivedComponentName: 'org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9',
      },
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jackson-module-parameter-names',
          },
        },
        hash: '502f98a535313e13cf18',
        derivedComponentName: 'com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4',
      },
      {
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'jackson-datatype-jdk8',
          },
        },
        hash: '502f98a535313e13cf18',
        derivedComponentName: 'com.fasterxml.jackson.datatype : jackson-datatype-jdk8 : 2.11.4',
      },
    ];
    const component = getMounted({
      ancestors: ancestors,
      expanded: true,
    });
    expect(component).not.toBeNull();
    expect(component).toHaveProp('ancestors', ancestors);
    expect(component).toHaveProp('routeName', 'applicationReport.componentDetails.overview');

    const content = component.find('AncestorsList');
    expect(content).not.toBeNull();

    const listElements = content.find('li');
    expect(listElements.length).toBe(4);
    const element = listElements.at(0);
    expect(element).toHaveText('org.springframework.data : spring-data-rest-hal-explorer : 3.4.11');
    const element1 = listElements.at(1);
    expect(element1).toHaveText('org.springframework.boot : spring-boot-actuator-autoconfigure : 2.4.9');
    const element2 = listElements.at(2);
    expect(element2).toHaveText('com.fasterxml.jackson.module : jackson-module-parameter-names : 2.11.4');
    const element3 = listElements.at(3);
    expect(element3).toHaveText('com.fasterxml.jackson.datatype : jackson-datatype-jdk8 : 2.11.4');
    const showLess = content.find('.iq-toggle-list').last();
    expect(showLess).toHaveText('Show less');
  });
});
