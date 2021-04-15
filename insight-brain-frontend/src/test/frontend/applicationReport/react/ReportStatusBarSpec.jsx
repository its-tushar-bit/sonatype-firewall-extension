/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ReportStatusBar from '../../../../main/frontend/applicationReport/react/ReportStatusBar';

describe('ReportStatusBar component', function () {
  let getShallowComponent;

  beforeEach(function () {
    const minimalProps = {
      selectedReport: {
        knownArtifactCount: 1,
        totalArtifactCount: 2,
        policyComponentCount: 1,
        grandfatheredPolicyViolationCount: 0,
        criticalViolationCount: 1,
        severeViolationCount: 2,
        moderateViolationCount: 3,
        nonLowViolationCount: 0,
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ReportStatusBar, minimalProps);
  });

  it('renders a tile', function () {
    const shallowComponent = getShallowComponent();
    expect(shallowComponent).toMatchSelector('.nx-tile');
  });

  it('renders a div for threats', function () {
    const shallowComponent = getShallowComponent();
    const indicator = shallowComponent.find('.iq-threat-indicators');
    const critical = shallowComponent.find('.iq-threat-indicator.critical');
    const severe = shallowComponent.find('.iq-threat-indicator.severe');
    const moderate = shallowComponent.find('.iq-threat-indicator.moderate');

    expect(indicator).toExist();
    expect(critical).toExist();
    expect(critical).toHaveText('1');
    expect(severe).toExist();
    expect(severe).toHaveText('2');
    expect(moderate).toExist();
    expect(moderate).toHaveText('3');
  });

  it('renders a div for violations counts', function () {
    const shallowComponent = getShallowComponent();
    const indicator = shallowComponent.find('.iq-threat-indicators');
    const captionText = indicator.find('.iq-caption').find('h3');
    const captionSubtext = indicator.find('.iq-caption').find('p');

    expect(indicator).toExist();
    expect(captionText).toHaveText('0 VIOLATIONS');
    expect(captionSubtext).toHaveText('Affecting 1 component');
  });

  it('renders a div for coverage', function () {
    const shallowComponent = getShallowComponent();
    const indicator = shallowComponent.find('.iq-coverage-indicator');
    const captionText = indicator.find('.iq-caption').find('h3');
    const captionSubtext = indicator.find('.iq-caption').find('p');

    expect(indicator).toExist();
    expect(captionText).toHaveText('2 COMPONENTS');
    expect(captionSubtext).toHaveText('50% of all components identified');
  });

  it('renders a div for grandfathering', function () {
    const shallowComponent = getShallowComponent();
    const indicator = shallowComponent.find('.iq-grandfathering-indicator');
    const captionText = indicator.find('.iq-caption').find('h3');

    expect(indicator).toExist();
    expect(captionText).toHaveText('0 Grandfathered');
  });
});
