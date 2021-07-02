/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import RequestWaiveTransitiveViolationsPopover from '../../../main/frontend/violation/RequestWaiveTransitiveViolationsPopover';

describe('RequestWaiveTransitiveViolationsPopoverContainer', function () {
  let store, state, vdom, RequestWaiveTransitiveViolationsPopoverContainer, spyToggleRequestWaiveTransitiveViolations;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          scanId: 'someScanId',
          hash: 'someHash',
        },
      },
      transitiveViolations: {
        availableScopes: 'someAvailableScopes',
        componentTransitivePolicyViolations: 'someComponentTransitivePolicyViolations',
      },
    };
    spyToggleRequestWaiveTransitiveViolations = jasmine
      .createSpy('spyToggleRequestWaiveTransitiveViolations')
      .and.returnValue({ type: 'BAR' });
    RequestWaiveTransitiveViolationsPopoverContainer = require('inject-loader!../../../main/frontend/violation/RequestWaiveTransitiveViolationsPopoverContainer')(
      {
        './transitiveViolationsActions': {
          toggleRequestWaiveTransitiveViolations: spyToggleRequestWaiveTransitiveViolations,
        },
      }
    ).default;
    store = configureStore()(() => state);
    vdom = <RequestWaiveTransitiveViolationsPopoverContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('scanId', 'someScanId');
    expect(wrapper).toHaveProp('hash', 'someHash');
    expect(wrapper).toHaveProp('availableScopes', 'someAvailableScopes');
    expect(wrapper).toHaveProp('componentTransitivePolicyViolations', 'someComponentTransitivePolicyViolations');
  });

  it('correctly maps the action creators to the WaiveTransitiveViolationsPopoverContainer props', function () {
    const wrapper = shallow(vdom).dive();

    const toggleRequestWaiveTransitiveViolationsActionCreator = wrapper.prop('toggleRequestWaiveTransitiveViolations');
    expect(toggleRequestWaiveTransitiveViolationsActionCreator).toEqual(jasmine.any(Function));
    toggleRequestWaiveTransitiveViolationsActionCreator('test');
    expect(store.getActions()[0]).toEqual({ type: 'BAR' });
  });

  it('renders the RequestWaiveTransitiveViolationsPopover component', function () {
    const requestWaiveTransitiveViolationsPopover = shallow(vdom).find(RequestWaiveTransitiveViolationsPopover);
    expect(requestWaiveTransitiveViolationsPopover).toExist();
  });
});
