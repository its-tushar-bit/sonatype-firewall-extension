/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import ListWaiversPage from '../../../main/frontend/waivers/ListWaiversPage';

describe('ListWaiversPageContainer', function() {
  let ListWaiversPageContainer,
      loadViolationMock,
      store,
      state,
      vdom;

  beforeEach(function() {
    loadViolationMock = jasmine.createSpy('loadViolation').and.returnValue({
      type: 'LOAD_VIOLATION'
    });

    ListWaiversPageContainer =
        require('inject-loader!../../../main/frontend/waivers/ListWaiversPageContainer')({
          '../violation/violationPageActions': {
            loadViolation: loadViolationMock
          }
        }).default;

    state = {
      violationPage: {
        activeWaivers: [],
        expiredWaivers: [],
        loading: false,
        violationDetails: {},
        violationDetailsError: {}
      },
      router: {
        currentParams: { violationId: 'foo' }
      }
    };

    store = configureStore()(() => state);
    vdom = <ListWaiversPageContainer store={store}/>;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('activeWaivers', []);
    expect(wrapper).toHaveProp('expiredWaivers', []);
    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('violationDetails', {});
    expect(wrapper).toHaveProp('violationDetailsError', {});
    state = {
      ...state,
      violationPage: {
        loading: true,
        violationDetails: {
          id: 'bar'
        }
      }
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', true);
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('violationDetails', { id: 'bar' });
  });

  it('maps action creators to props', function() {
    const wrapper = shallow(vdom).dive();
    const loadViolationActionCreator = wrapper.prop('loadViolation');

    expect(loadViolationActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadViolationActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_VIOLATION' }]);
  });

  it('renders ListWaiversPage component', function() {
    const listWaiversPageComponent = shallow(vdom).find(ListWaiversPage);
    expect(listWaiversPageComponent).toExist();
    expect(listWaiversPageComponent).toHaveProp('violationId', 'foo');
  });
});
