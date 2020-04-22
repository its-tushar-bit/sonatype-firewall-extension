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
      fetchStageTypesMock,
      state,
      store,
      vdom,
      mock$State;

  beforeEach(function() {

    loadViolationActionMock = jasmine.createSpy('loadViolation').and.returnValue({ type: 'LOAD_VIOLATION' });
    fetchStageTypesMock = jasmine.createSpy('fetchStageTypes').and.returnValue({ type: 'FETCH_STAGE_TYPES' });

    ViolationPageContainer =
        require('inject-loader!../../../main/frontend/violation/ViolationPageContainer')({
          './violationPageActions': {
            loadViolation: loadViolationActionMock
          },
          '../stages/stagesActions': {
            fetchStageTypes: fetchStageTypesMock
          }
        }).default;

    state = {
      violationPage: {
        loading: false,
        violationDetailsError: null
      },
      stages: {
        dashboard: {
          stageTypes: null,
          error: null
        }
      }
    };

    mock$State = { params: { id: 'foo' } };

    store = configureStore()(() => state);
    vdom = <ViolationPageContainer store={store} $state={mock$State}/>;
  });

  it('maps the state slice ("violationPage") to ViolationPageContainer props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('violationDetailsError', null);

    state = {
      ...state,
      violationPage: {
        loading: true,
        violationDetailsError: 'foo'
      }
    };

    // force state update
    store.dispatch({ type: 'BLAH' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', true);
    expect(wrapper).toHaveProp('violationDetailsError', 'foo');
  });

  it('maps the stageTypes and error props from the dashboard stages to `stageTypes` and `stageTypesError`', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('stageTypes', null);
    expect(wrapper).toHaveProp('stageTypesError', null);

    state = {
      ...state,
      stages: {
        dashboard: {
          stageTypes: [],
          error: 'foo'
        }
      }
    };

    // force state update
    store.dispatch({ type: 'BLAH' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('stageTypes', []);
    expect(wrapper).toHaveProp('stageTypesError', 'foo');
  });

  it('maps action creators to ViolationPageContainer props', function() {
    const wrapper = shallow(vdom).dive(),
        loadViolationActionCreator = wrapper.prop('loadViolation'),
        fetchStageTypesActionCreator = wrapper.prop('fetchStageTypes');

    expect(loadViolationActionCreator).toEqual(jasmine.any(Function));
    expect(fetchStageTypesActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadViolationActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_VIOLATION' }]);

    fetchStageTypesActionCreator();

    expect(store.getActions()).toEqual([{ type: 'LOAD_VIOLATION' }, { type: 'FETCH_STAGE_TYPES' }]);
  });
});
