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
      loadManageWaiversDataMock,
      setWaiverToDeleteMock,
      store,
      state,
      vdom;

  beforeEach(function() {
    loadManageWaiversDataMock = jasmine.createSpy('loadManageWaiversData').and.returnValue({
      type: 'LOAD_MANAGE_WAIVERS_DATA'
    });

    setWaiverToDeleteMock = jasmine.createSpy('setWaiverToDelete').and.returnValue({
      type: 'SET_WAIVER_TO_DELETE'
    });

    ListWaiversPageContainer =
        require('inject-loader!../../../main/frontend/waivers/ListWaiversPageContainer')({
          './waiverActions': {
            loadManageWaiversData: loadManageWaiversDataMock,
            setWaiverToDelete: setWaiverToDeleteMock
          }
        }).default;

    state = {
      violation: {
        activeWaivers: [],
        expiredWaivers: [],
        violationDetails: {}
      },
      router: {
        currentParams: { violationId: 'foo' }
      },
      manageWaivers: {
        loadingManageWaiversData: false,
        loadManageWaiversDataError: 'test error',
        hasPermissionForAppWaivers: false
      },
      deleteWaiver: {
        waiverToDelete: { waiverId: 'foo' }
      }
    };

    store = configureStore()(() => state);
    vdom = <ListWaiversPageContainer store={store}/>;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('activeWaivers', []);
    expect(wrapper).toHaveProp('expiredWaivers', []);
    expect(wrapper).toHaveProp('loadingManageWaiversData', false);
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('violationDetails', {});
    expect(wrapper).toHaveProp('loadManageWaiversDataError', 'test error');
    expect(wrapper).toHaveProp('hasPermissionForAppWaivers', false);
    state = {
      ...state,
      violation: {
        violationDetails: {
          id: 'bar'
        }
      },
      manageWaivers: {
        loadingManageWaiversData: true,
        loadManageWaiversDataError: null,
        hasPermissionForAppWaivers: true
      }
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loadingManageWaiversData', true);
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('violationDetails', { id: 'bar' });
    expect(wrapper).toHaveProp('loadManageWaiversDataError', null);
    expect(wrapper).toHaveProp('hasPermissionForAppWaivers', true);
    expect(wrapper).toHaveProp('waiverToDelete', { waiverId: 'foo' });
  });

  it('maps action creators to props', function() {
    const wrapper = shallow(vdom).dive();
    const loadMAnageWaiversDataActionCreator = wrapper.prop('loadManageWaiversData');
    const setWaiverToDeleteActionCreator = wrapper.prop('setWaiverToDelete');

    expect(loadMAnageWaiversDataActionCreator).toEqual(jasmine.any(Function));
    expect(setWaiverToDeleteActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);

    loadMAnageWaiversDataActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_MANAGE_WAIVERS_DATA' }]);

    setWaiverToDeleteActionCreator();
    expect(store.getActions()).toEqual([
      { type: 'LOAD_MANAGE_WAIVERS_DATA' },
      { type: 'SET_WAIVER_TO_DELETE' }
    ]);
  });

  it('renders ListWaiversPage component', function() {
    const listWaiversPageComponent = shallow(vdom).find(ListWaiversPage);
    expect(listWaiversPageComponent).toExist();
    expect(listWaiversPageComponent).toHaveProp('violationId', 'foo');
  });
});
