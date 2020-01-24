/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

describe('ViolationPageContainer', function() {

  let ViolationPageContainer,
      loadViolationActionMock,
      state,
      store,
      vdom,
      mock$State;

  beforeEach(function() {

    loadViolationActionMock = jasmine.createSpy('load').and.returnValue({ type: 'LOAD_VIOLATION' });

    ViolationPageContainer =
        require('inject-loader!../../../main/frontend/violation/ViolationPageContainer')({
          './violationPageActions': {
            loadViolation: loadViolationActionMock
          }
        }).default;

    state = {
      violationPage: {
        loading: false,
        error: null
      }
    };

    mock$State = { params: { id: 'foo' } };

    store = configureStore()(() => state);
    vdom = <ViolationPageContainer store={store} $state={mock$State}/>;
  });

  it('maps the state slice ("violationPage") to ViolationPageContainer props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('error', null);

    state = {
      violationPage: {
        loading: true,
        error: 'foo'
      }
    };

    // force state update
    store.dispatch({ type: 'BLAH' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', true);
    expect(wrapper).toHaveProp('error', 'foo');
  });

  it('maps action creators to ViolationPageContainer props', function() {
    const wrapper = shallow(vdom).dive();

    const loadViolationActionCreator = wrapper.prop('loadViolation');
    expect(loadViolationActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadViolationActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_VIOLATION' }]);
  });
});
