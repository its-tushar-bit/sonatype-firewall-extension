/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { always } from 'ramda';

import * as enzymeUtils from '../enzymeUtils';
import ViolationDetailsTile from '../../../main/frontend/violation/ViolationDetailsTile';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import SecurityVulnerabilityDetailsTile
  from '../../../main/frontend/violation/SecurityVulnerabilityDetailsTile';
import PolicyViolationConstraintInfoTile from '../../../main/frontend/violation/PolicyViolationConstraintInfoTile';

// MaximizedContainer must be mocked because it depends on an angular service
function MaximizedContainer({ children }) {
  return <div>{children}</div>;
}

MaximizedContainer.propTypes = {
  children: PropTypes.node
};

describe('ViolationPage', function() {
  let minimalProps,
      loadViolationSpy,
      fetchStageTypesSpy,
      stateGoSpy,
      ViolationPage,
      getShallowComponent,
      getMountedComponent;

  beforeEach(function() {
    ViolationPage =
        require('inject-loader!../../../main/frontend/violation/ViolationPage')({
          '../react/MaximizedContainer': MaximizedContainer
        }).default;

    loadViolationSpy = jasmine.createSpy('loadViolation');
    fetchStageTypesSpy = jasmine.createSpy('fetchStageTypes');
    stateGoSpy = jasmine.createSpy('stateGo');

    minimalProps = {
      $state: {
        params: { id: 'foo' },
        get: always({
          data: {
            title: 'asdf'
          }
        }),
        href: always('qwerty')
      },
      loadViolation: loadViolationSpy,
      fetchStageTypes: fetchStageTypesSpy,
      stateGo: stateGoSpy,
      loading: false
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ViolationPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ViolationPage, minimalProps);
  });

  it('renders a MaximizedContainer with the "violation-page" id', function() {
    const component = getShallowComponent();

    expect(component).toMatchSelector(MaximizedContainer);
    expect(component).toMatchSelector('#violation-page');
  });

  it('renders a LoadWrapper within the page', function() {
    expect(getShallowComponent().find(LoadWrapper)).toExist();
  });

  it('sets the LoadWrapper\'s loading flag based on the loading, violationDetails, and stageTypes props', function() {
    const getLoadWrapper = props => getShallowComponent(props).find(LoadWrapper);

    expect(getLoadWrapper()).toHaveProp('loading', true);
    expect(getLoadWrapper({ violationDetails: {} })).toHaveProp('loading', true);
    expect(getLoadWrapper({ stageTypes: [] })).toHaveProp('loading', true);
    expect(getLoadWrapper({ violationDetails: {}, stageTypes: [] })).toHaveProp('loading', false);
    expect(getLoadWrapper({ violationDetails: {}, stageTypes: [], loading: true })).toHaveProp('loading', true);
  });

  it('sets the LoadWrapper\'s error from the violationDetailsError and stageTypesError props', function() {
    const getLoadWrapper = props => getShallowComponent(props).find(LoadWrapper);

    expect(getLoadWrapper()).toHaveProp('error', undefined);
    expect(getLoadWrapper({ violationDetailsError: 'foo' })).toHaveProp('error', 'foo');
    expect(getLoadWrapper({ stageTypesError: 'foo' })).toHaveProp('error', 'foo');
    expect(getLoadWrapper({ violationDetailsError: 'foo', stageTypesError: 'bar' })).toHaveProp('error', 'foo');
  });

  it('calls loadViolation with the $state id param, and fetchStageTypes with the `dashboard` param, on first load',
      function() {
        getMountedComponent();

        expect(loadViolationSpy).toHaveBeenCalledWith('foo');
        expect(fetchStageTypesSpy).toHaveBeenCalledWith('dashboard');
      }
  );

  it('calls loadViolation whenever the $state id param changes', function() {
    const component = getMountedComponent();

    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
    expect(loadViolationSpy).not.toHaveBeenCalledWith('bar');

    component.setProps({ $state: { ...minimalProps.$state, params: { id: 'bar' } } });

    expect(loadViolationSpy).toHaveBeenCalledWith('bar');
  });

  it('renders a ViolationDetailsTile within the LoadWrapper with $state, stageTypes, violationDetails & stateGo',
      function() {
        const violationDetails = {},
            stageTypes = {},
            stateGo = () => {},
            component = getShallowComponent({ violationDetails, stageTypes, stateGo }),
            tile = component.find(LoadWrapper).find(ViolationDetailsTile);

        expect(tile).toExist();
        expect(tile.prop('$state')).toBe(minimalProps.$state);
        expect(tile.prop('violationDetails')).toBe(violationDetails);
        expect(tile.prop('stageTypes')).toBe(stageTypes);
        expect(tile.prop('stateGo')).toBe(stateGo);
      }
  );

  it('renders a PolicyViolationConstraintInfoTile within the LoadWrapper with correct props', function() {
    const violationDetails = { constraintViolations: 'constraintViolations' };
    const tile = getShallowComponent({ violationDetails })
        .find(LoadWrapper).find(PolicyViolationConstraintInfoTile);

    expect(tile).toExist();
    expect(tile.prop('constraintViolations')).toBe('constraintViolations');
  });

  it('renders a SecurityVulnerabilityDetailsTile with correct props if it\'s a security vulnerability', function() {
    const violationDetails = {
      policyThreatCategory: 'security'
    };
    const vulnerabilityDetails = { foo: 'bar' };
    const tile = getShallowComponent({
      vulnerabilityDetails,
      violationDetails,
      vulnerabilityDetailsError: 'Test Error',
      vulnerabilityDetailsLoading: true
    }).find(LoadWrapper).find(SecurityVulnerabilityDetailsTile);

    expect(tile).toExist();
    expect(tile.prop('vulnerabilityDetails')).toBe(vulnerabilityDetails);
    expect(tile.prop('vulnerabilityDetailsError')).toBe('Test Error');
    expect(tile.prop('vulnerabilityDetailsLoading')).toBe(true);
  });

  it('does not render a SecurityVulnerabilityDetailsTile if it\'s not a security vulnerability', function() {
    const violationDetails = {
      policyThreatCategory: 'license'
    };
    const vulnerabilityDetails = { foo: 'bar' };
    const tile = getShallowComponent({
      vulnerabilityDetails,
      violationDetails,
      vulnerabilityDetailsError: 'Test Error',
      vulnerabilityDetailsLoading: true
    }).find(LoadWrapper).find(SecurityVulnerabilityDetailsTile);

    expect(tile).not.toExist();
  });
});
