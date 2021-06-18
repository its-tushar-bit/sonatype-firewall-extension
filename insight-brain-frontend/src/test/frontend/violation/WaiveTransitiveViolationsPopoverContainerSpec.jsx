/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import WaiveTransitiveViolationsPopover from '../../../main/frontend/violation/WaiveTransitiveViolationsPopover';

describe('WaiveTransitiveViolationsPopoverContainer', function () {
  let store,
    state,
    vdom,
    WaiveTransitiveViolationsPopoverContainer,
    spyToggleWaiveTransitiveViolations,
    spySetScope,
    spySetExpiration,
    spySetComments,
    spyCancel,
    spySave;

  beforeEach(function () {
    state = {
      transitiveViolations: {
        availableScopes: 'someAvailableScopes',
        componentTransitivePolicyViolations: 'someComponentTransitivePolicyViolations',
      },
      waiveTransitiveViolations: {
        scope: 'someScope',
        expiration: 'someExpiration',
        comments: 'someComments',
        submitMaskState: 'someSubmitMaskState',
        saveError: 'someSaveError',
      },
    };
    spyToggleWaiveTransitiveViolations = jasmine
      .createSpy('spyToggleWaiveTransitiveViolations')
      .and.returnValue({ type: 'BAR1' });
    spySetScope = jasmine.createSpy('spySetScope').and.returnValue({ type: 'BAR2' });
    spySetExpiration = jasmine.createSpy('spySetExpiration').and.returnValue({ type: 'BAR3' });
    spySetComments = jasmine.createSpy('spySetComments').and.returnValue({ type: 'BAR4' });
    spyCancel = jasmine.createSpy('spyCancel').and.returnValue({ type: 'BAR5' });
    spySave = jasmine.createSpy('spySave').and.returnValue({ type: 'BAR6' });
    WaiveTransitiveViolationsPopoverContainer = require('inject-loader!../../../main/frontend/violation/WaiveTransitiveViolationsPopoverContainer')(
      {
        './transitiveViolationsActions': {
          toggleWaiveTransitiveViolations: spyToggleWaiveTransitiveViolations,
        },
        './waiveTransitiveViolationsRedux': {
          actions: {
            setScope: spySetScope,
            setExpiration: spySetExpiration,
            setComments: spySetComments,
            cancel: spyCancel,
            save: spySave,
          },
        },
      }
    ).default;
    store = configureStore()(() => state);
    vdom = <WaiveTransitiveViolationsPopoverContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('availableScopes', 'someAvailableScopes');
    expect(wrapper).toHaveProp('componentTransitivePolicyViolations', 'someComponentTransitivePolicyViolations');
    expect(wrapper).toHaveProp('scope', 'someScope');
    expect(wrapper).toHaveProp('expiration', 'someExpiration');
    expect(wrapper).toHaveProp('comments', 'someComments');
    expect(wrapper).toHaveProp('submitMaskState', 'someSubmitMaskState');
    expect(wrapper).toHaveProp('saveError', 'someSaveError');
  });

  it('correctly maps the action creators to the WaiveTransitiveViolationsPopoverContainer props', function () {
    const wrapper = shallow(vdom).dive();

    const toggleWaiveTransitiveViolationsActionCreator = wrapper.prop('toggleWaiveTransitiveViolations');
    expect(toggleWaiveTransitiveViolationsActionCreator).toEqual(jasmine.any(Function));
    toggleWaiveTransitiveViolationsActionCreator('test');
    expect(store.getActions()[0]).toEqual({ type: 'BAR1' });

    const setScopeActionCreator = wrapper.prop('setScope');
    expect(setScopeActionCreator).toEqual(jasmine.any(Function));
    setScopeActionCreator('test');
    expect(store.getActions()[1]).toEqual({ type: 'BAR2' });

    const setExpirationActionCreator = wrapper.prop('setExpiration');
    expect(setExpirationActionCreator).toEqual(jasmine.any(Function));
    setExpirationActionCreator('test');
    expect(store.getActions()[2]).toEqual({ type: 'BAR3' });

    const setCommentsActionCreator = wrapper.prop('setComments');
    expect(setCommentsActionCreator).toEqual(jasmine.any(Function));
    setCommentsActionCreator('test');
    expect(store.getActions()[3]).toEqual({ type: 'BAR4' });

    const cancelActionCreator = wrapper.prop('cancel');
    expect(cancelActionCreator).toEqual(jasmine.any(Function));
    cancelActionCreator('test');
    expect(store.getActions()[4]).toEqual({ type: 'BAR5' });

    const saveActionCreator = wrapper.prop('save');
    expect(saveActionCreator).toEqual(jasmine.any(Function));
    saveActionCreator('test');
    expect(store.getActions()[5]).toEqual({ type: 'BAR6' });
  });

  it('renders the WaiveTransitiveViolationsPopover component', function () {
    const waiveTransitiveViolationsPopover = shallow(vdom).find(WaiveTransitiveViolationsPopover);
    expect(waiveTransitiveViolationsPopover).toExist();
  });
});
