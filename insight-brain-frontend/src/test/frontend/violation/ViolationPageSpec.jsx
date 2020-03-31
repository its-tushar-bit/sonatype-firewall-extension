/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { always } from 'ramda';

import * as enzymeUtils from '../enzymeUtils';
import ViolationPage from '../../../main/frontend/violation/ViolationPage';
import ViolationDetailsTile from '../../../main/frontend/violation/ViolationDetailsTile';
import BackButton from '../../../main/frontend/react/BackButton';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';

// MaximizedContainer must be mocked because it depends on an angular service
function MaximizedContainer({ children }) {
  return <div>{children}</div>;
}

describe('ViolationPage', function() {
  let minimalProps,
      loadViolationSpy,
      fetchStageTypesSpy,
      ViolationPage,
      getShallowComponent,
      getMountedComponent;

  beforeEach(function() {
    ViolationPage =
        require('inject-loader!../../../main/frontend/violation/ViolationPage')({
          '../react/MaximizedContainer': MaximizedContainer
        }).default;

    loadViolationSpy =jasmine.createSpy('loadViolation');
    fetchStageTypesSpy =jasmine.createSpy('fetchStageTypes');

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
      loading: false
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ViolationPage, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ViolationPage, minimalProps);
  });

  it('renders a MaximizedContainer with the "violation-page" id and "nx-root-container" class', function() {
    const component = getShallowComponent();

    expect(component).toMatchSelector(MaximizedContainer);
    expect(component).toMatchSelector('#violation-page.nx-root-container');
  });

  it('renders a BackButton using the supplied $state', function() {
    expect(getShallowComponent().find(BackButton)).toHaveProp('$state', minimalProps.$state);
  });

  it('renders a LoadWrapper within the nx-page-main', function() {
    expect(getShallowComponent().find('.nx-page-main').find(LoadWrapper)).toExist();
  });

  it('sets the LoadWrapper\'s loading flag based on the loading, violationDetails, and stageTypes props', function() {
    const getLoadWrapper = props => getShallowComponent(props).find('.nx-page-main').find(LoadWrapper);

    expect(getLoadWrapper()).toHaveProp('loading', true);
    expect(getLoadWrapper({ violationDetails: {} })).toHaveProp('loading', true);
    expect(getLoadWrapper({ stageTypes: [] })).toHaveProp('loading', true);
    expect(getLoadWrapper({ violationDetails: {}, stageTypes: [] })).toHaveProp('loading', false);
    expect(getLoadWrapper({ violationDetails: {}, stageTypes: [], loading: true })).toHaveProp('loading', true);
  });

  it('sets the LoadWrapper\'s error from the error and stageTypesError props', function() {
    const getLoadWrapper = props => getShallowComponent(props).find('.nx-page-main').find(LoadWrapper);

    expect(getLoadWrapper()).toHaveProp('error', undefined);
    expect(getLoadWrapper({ error: 'foo' })).toHaveProp('error', 'foo');
    expect(getLoadWrapper({ stageTypesError: 'foo' })).toHaveProp('error', 'foo');
    expect(getLoadWrapper({ error: 'foo', stageTypesError: 'bar' })).toHaveProp('error', 'foo');
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

  it('renders a ViolationDetailsTile within the LoadWrapper with the $state, stageTypes, and violationDetails',
      function() {
        const violationDetails = {},
            stageTypes = {},
            tile = getShallowComponent({ violationDetails, stageTypes }).find(LoadWrapper).find(ViolationDetailsTile);

        expect(tile).toExist();
        expect(tile.prop('$state')).toBe(minimalProps.$state);
        expect(tile.prop('violationDetails')).toBe(violationDetails);
        expect(tile.prop('stageTypes')).toBe(stageTypes);
      }
  );
});
