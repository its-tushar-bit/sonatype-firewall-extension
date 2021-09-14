/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { RecommendedVersions } from '../../../../../main/frontend/componentDetails/overview/riskRemediation/RecommendedVersions';

describe('RecommendedVersionsComponent', () => {
  let minimalProps, getMounted;

  beforeEach(function () {
    minimalProps = {
      actualVersion: '2.4.9',
      stageId: 'build',
      remediation: [],
    };

    getMounted = enzymeUtils.getMountedComponent(RecommendedVersions, minimalProps);
  });

  it('renders a component', () => {
    expect(getMounted()).toExist();
  });

  it("Title is 'Recommended Versions'", () => {
    const remediation = [];
    const component = getMounted({
      remediation: remediation,
    });

    expect(component).toHaveProp('remediation', remediation);
    expect(component).toHaveProp('actualVersion', '2.4.9');
    expect(component).toHaveProp('stageId', 'build');

    const title = component.find('.nx-tile-header__title');
    expect(title).not.toBeNull();
    expect(title).toHaveText('Recommended Versions');
  });

  it('with one component list if no remediation array is sent', () => {
    const remediation = [];
    const component = getMounted({
      remediation: remediation,
    });
    const content = component.find('.nx-tile-content');
    expect(content).not.toBeNull();

    const listElements = content.find('.nx-list__item');
    expect(listElements.length).toBe(1);
    const element = listElements.at(0);
    expect(element).not.toBeNull();
    const subText = element.find('.nx-list__subtext');
    expect(subText).toHaveText('No recommended versions are available for the current component');
  });

  it('with two component list remediation array is sent', () => {
    const remediation = {
      versionChanges: [
        {
          type: 'next-no-violations',
          data: {
            component: {
              packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.10?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'spring-boot-jarmode-layertools',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'org.springframework.boot',
                  version: '2.4.10',
                },
              },
              displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.10',
            },
          },
        },
        {
          type: 'next-non-failing',
          data: {
            component: {
              packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.9?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'spring-boot-jarmode-layertools',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'org.springframework.boot',
                  version: '2.4.9',
                },
              },
              displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.9',
            },
          },
        },
        {
          type: 'next-non-failing-with-dependencies',
          data: {
            component: {
              packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.9?type=jar',
              hash: null,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'spring-boot-jarmode-layertools',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'org.springframework.boot',
                  version: '2.4.9',
                },
              },
              displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.9',
            },
          },
        },
      ],
    };
    const component = getMounted({
      remediation: remediation,
    });
    expect(component).toHaveProp('remediation', remediation);
    expect(component).toHaveProp('actualVersion', '2.4.9');
    expect(component).toHaveProp('stageId', 'build');

    const content = component.find('.nx-tile-content');
    expect(content).not.toBeNull();

    const listElements = content.find('.nx-list__item');
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
