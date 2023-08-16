/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'jest-enzyme';
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import ListWaiversPage from '../../../main/frontend/waivers/ListWaiversPage';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import ListWaiversPageContainer from 'MainRoot/waivers/ListWaiversPageContainer';

jest.mock('MainRoot/waivers/waiverActions', () => ({
  loadManageWaiversData: () => ({ type: 'LOAD_MANAGE_WAIVERS_DATA' }),
  setWaiverToDelete: () => ({ type: 'SET_WAIVER_TO_DELETE' }),
}));

describe('ListWaiversPageContainer', function () {
  let store, state, vdom;

  beforeEach(function () {
    state = {
      violation: {
        activeWaivers: [],
        expiredWaivers: [],
        violationDetails: {},
      },
      router: {
        currentParams: { violationId: 'foo' },
      },
      manageWaivers: {
        loadingManageWaiversData: false,
        loadManageWaiversDataError: 'test error',
        hasPermissionForAppWaivers: false,
      },
      deleteWaiver: {
        waiverToDelete: { waiverId: 'foo' },
      },
      firewall: {
        componentDetailsPage: {
          showManageWaiverPage: false,
          componentDetails: {
            matchState: 'exact',
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6',
              },
            },
          },
        },
      },
    };

    jest.spyOn(routerSelectors, 'selectIsFirewall').mockReturnValue(false);

    store = configureStore()(() => state);
    vdom = <ListWaiversPageContainer store={store} />;
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
          id: 'bar',
        },
      },
      manageWaivers: {
        loadingManageWaiversData: true,
        loadManageWaiversDataError: null,
        hasPermissionForAppWaivers: true,
      },
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loadingManageWaiversData', true);
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('violationDetails', { id: 'bar' });
    expect(wrapper).toHaveProp('loadManageWaiversDataError', null);
    expect(wrapper).toHaveProp('hasPermissionForAppWaivers', true);
    expect(wrapper).toHaveProp('waiverToDelete', { waiverId: 'foo' });
    expect(wrapper).toHaveProp('showManageWaiverPage', false);
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive();
    const loadMAnageWaiversDataActionCreator = wrapper.prop('loadManageWaiversData');
    const setWaiverToDeleteActionCreator = wrapper.prop('setWaiverToDelete');

    expect(loadMAnageWaiversDataActionCreator).toEqual(expect.any(Function));
    expect(setWaiverToDeleteActionCreator).toEqual(expect.any(Function));

    expect(store.getActions()).toEqual([]);

    loadMAnageWaiversDataActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_MANAGE_WAIVERS_DATA' }]);

    setWaiverToDeleteActionCreator();
    expect(store.getActions()).toEqual([{ type: 'LOAD_MANAGE_WAIVERS_DATA' }, { type: 'SET_WAIVER_TO_DELETE' }]);
  });

  it('renders ListWaiversPage component', function () {
    const listWaiversPageComponent = shallow(vdom).find(ListWaiversPage);
    expect(listWaiversPageComponent).toExist();
    expect(listWaiversPageComponent).toHaveProp('violationId', 'foo');
  });
});
