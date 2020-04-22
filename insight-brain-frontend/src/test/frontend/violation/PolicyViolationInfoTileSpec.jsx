/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import PolicyViolationInfoTile from '../../../main/frontend/violation/PolicyViolationInfoTile';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import {NxVulnerabilityDetails} from '@sonatype/react-shared-components';

describe('PolicyViolationInfoTile', function() {
  let minimalProps, getShallowComponent;

  beforeEach(function() {
    minimalProps = {
      violationDetails: {
        constraintViolations: [{
          constraintName: 'test constraint',
          reasons: [
            {reason: 'reason 1'},
            {reason: 'reason 2'}
          ]
        }]
      }
    };

    getShallowComponent = enzymeUtils.getShallowComponent(PolicyViolationInfoTile, minimalProps);
  });

  it('renders title with policy constraint name', function() {
    const header = getShallowComponent().find('.nx-tile-header .nx-tile-header__title h2');

    expect(header).toHaveText('Policy Constraint - test constraint');
  });

  it('renders violation reasons', function() {
    const reasonListItems = getShallowComponent().find('#policy-violation-reasons .nx-list__item');
    expect(reasonListItems.length).toBe(2);
    expect(reasonListItems.at(0)).toHaveText('reason 1');
    expect(reasonListItems.at(1)).toHaveText('reason 2');
  });

  it('renders LoadWrapper with loading true if vulnerabilityDetailsLoading is true', function() {
    const loadWrapper = getShallowComponent({ vulnerabilityDetailsLoading: true }).find(LoadWrapper);

    expect(loadWrapper).toExist();
    expect(loadWrapper.prop('loading')).toBe(true);
  });

  it('renders LoadWrapper with error prop if vulnerabilityDetailsError is not null', function() {
    const loadWrapper = getShallowComponent({ vulnerabilityDetailsError: 'Errr!'} ).find(LoadWrapper);

    expect(loadWrapper).toExist();
    expect(loadWrapper.prop('error')).toBe('Errr!');
  });

  it('renders LoadWrapper with loading false if vulnerabilityDetailsLoading is false', function() {
    const loadWrapper = getShallowComponent({ vulnerabilityDetailsLoading: false }).find(LoadWrapper);

    expect(loadWrapper).toExist();
    expect(loadWrapper.prop('loading')).toBe(false);
  });

  it('renders NxVulnerabilityDetails within the LoadWrapper with the vulnerabilityDetails', function() {
    const vulnerabilityDetails = {},
        vulnerabilityDetailsComponent = getShallowComponent({vulnerabilityDetails})
            .find(LoadWrapper).find(NxVulnerabilityDetails);

    expect(vulnerabilityDetailsComponent).toExist();
    expect(vulnerabilityDetailsComponent.prop('vulnerabilityDetails')).toBe(vulnerabilityDetails);
  });

  it('does not render NxVulnerabilityDetails if vulnerabilityDetails is null', function() {
    const vulnerabilityDetailsComponent = getShallowComponent().find(LoadWrapper).find(NxVulnerabilityDetails);
    expect(vulnerabilityDetailsComponent).not.toExist();
  });
});
