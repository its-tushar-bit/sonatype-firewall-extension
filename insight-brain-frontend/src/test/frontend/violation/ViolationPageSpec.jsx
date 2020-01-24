/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { always } from 'ramda';

import * as enzymeUtils from '../enzymeUtils';
import ViolationPage from '../../../main/frontend/violation/ViolationPage';
import BackButton from '../../../main/frontend/react/BackButton';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';

// MaximizedContainer must be mocked because it depends on an angular service
function MaximizedContainer({ children }) {
  return <div>{children}</div>;
}

describe('ViolationPage', function() {
  let minimalProps,
      loadViolationSpy,
      ViolationPage,
      getShallowComponent,
      getMountedComponent;

  beforeEach(function() {
    ViolationPage =
        require('inject-loader!../../../main/frontend/violation/ViolationPage')({
          '../react/MaximizedContainer': MaximizedContainer
        }).default;

    loadViolationSpy =jasmine.createSpy('loadViolation');

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

  it('renders a LoadWrapper within the nx-page-main using the specified loading flag', function() {
    expect(getShallowComponent().find('.nx-page-main').find(LoadWrapper)).toHaveProp('loading', false);
    expect(getShallowComponent({ loading: true }).find(LoadWrapper)).toHaveProp('loading', true);
  });

  it('calls loadViolation with the $state id param on first load', function() {
    getMountedComponent();

    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
  });

  it('calls loadViolation whenever the $state id param changes', function() {
    const component = getMountedComponent();

    expect(loadViolationSpy).toHaveBeenCalledWith('foo');
    expect(loadViolationSpy).not.toHaveBeenCalledWith('bar');

    component.setProps({ $state: { ...minimalProps.$state, params: { id: 'bar' } } });

    expect(loadViolationSpy).toHaveBeenCalledWith('bar');
  });
});
