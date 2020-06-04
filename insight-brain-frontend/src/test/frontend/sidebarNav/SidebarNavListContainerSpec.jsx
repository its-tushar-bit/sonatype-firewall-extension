/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

describe('SidebarNavListContainer', function() {

  let SidebarNavListContainer,
      loadSidebarNavMock,
      gotoNewVulnerabilityMock,
      state,
      store,
      vdom,
      mock$State;

  beforeEach(function() {

    loadSidebarNavMock = jasmine.createSpy('loadSidebarNav').and.returnValue({ type: 'LOAD_LEFT_NAV' });
    gotoNewVulnerabilityMock = jasmine.createSpy('gotoNewVulnerability')
        .and.returnValue({ type: 'GOTO_NEW_VULNERABILITY' });

    SidebarNavListContainer =
        require('inject-loader!../../../main/frontend/sidebarNav/SidebarNavListContainer')({
          './sidebarNavListActions': {
            loadSidebarNav: loadSidebarNavMock,
            gotoNewVulnerability: gotoNewVulnerabilityMock
          }
        }).default;

    state = {
      sidebarNavList: {
        loading: false,
        error: null,
        data: [{
          policyViolationId: 'idFromStateData'
        }]
      },
      router: {
        currentState: {
          name: 'sidebarView.violation'
        },
        prevState: {
          name: ''
        }
      },
      violationPage: {
        violationDetails: {
          policyViolationId: 'idFromDetailsPage'
        }
      }
    };

    mock$State = { params: { id: 'foo' } };

    store = configureStore()(() => state);
    vdom = <SidebarNavListContainer store={store} $state={mock$State}/>;
  });

  it('maps the state slice ("sidebarNavList") to SidebarNavListContainer props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('error', null);

    state = {
      ...state,
      sidebarNavList: {
        loading: true,
        error: 'foo',
        backButtonStateName: 'foo.bar.baz',
        contentType: 'violations',
        data: [{foo: 'bar' }]
      }
    };

    // force state update
    store.dispatch({ type: 'BLAH' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', true);
    expect(wrapper).toHaveProp('error', 'foo');
    expect(wrapper).toHaveProp('contentType', 'violations');
    expect(wrapper).toHaveProp('backButtonStateName', 'foo.bar.baz');
    expect(wrapper).toHaveProp('data', [{foo: 'bar' }]);
  });

  it('sets data from violationDetails if contentType is not defined and stateName is sidebarView.violation', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('error', null);

    state = {
      ...state,
      sidebarNavList: {
        loading: true,
        error: 'foo',
        backButtonStateName: 'foo.bar.baz',
        contentType: undefined,
        data: [{foo: 'bar' }]
      }
    };

    // force state update
    store.dispatch({ type: 'BLAH' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('data', [{ policyViolationId: 'idFromDetailsPage' }]);
    expect(wrapper).toHaveProp('contentType', 'violations');
    expect(wrapper).toHaveProp('backButtonStateName', 'dashboard.overview.violations');
    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('error', null);
  });

  it('maps action creators to SidebarNavListContainer props', function() {
    const wrapper = shallow(vdom).dive(),
        loadSidebarNavCreator = wrapper.prop('loadSidebarNav'),
        gotoNewVulnerabilityCreator = wrapper.prop('gotoNewVulnerability');

    expect(loadSidebarNavCreator).toEqual(jasmine.any(Function));
    expect(gotoNewVulnerabilityCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadSidebarNavCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_LEFT_NAV' }]);

    gotoNewVulnerabilityCreator();

    expect(store.getActions()).toEqual([{ type: 'LOAD_LEFT_NAV' }, { type: 'GOTO_NEW_VULNERABILITY' }]);
  });

  it('sets the scrollToSelection prop according on the previous state', function() {
    // coming from different state
    expect(shallow(vdom).dive().prop('scrollToSelection')).toEqual(true);

    // coming from the same state
    state.router.prevState.name = state.router.currentState.name;
    expect(shallow(vdom).dive().prop('scrollToSelection')).toEqual(false);
  });
});
