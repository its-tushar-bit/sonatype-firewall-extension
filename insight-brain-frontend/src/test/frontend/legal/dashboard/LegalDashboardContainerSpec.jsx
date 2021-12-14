/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import LegalDashboardPage from '../../../../main/frontend/legal/dashboard/LegalDashboardPage';

describe('LegalDashboardContainerSpec', function () {
  let store, state, vdom, LegalDashboardContainer, loadResultsMock, stateGoMock;

  beforeEach(function () {
    state = {
      legalDashboard: {
        applications: 'applications',
        components: 'components',
        loading: 'loading',
        loadError: 'loadError',
      },
      legalDashboardFilter: {
        filtersAreDirty: false,
        filterSidebarOpen: false,
        loading: false,
      },
      manageLegalFilters: {
        appliedFilterName: 'appliedFilterName',
        showDirtyAsterisk: 'showDirtyAsterisk',
      },
      router: {
        currentParams: {},
      },
    };

    loadResultsMock = jasmine.createSpy('loadResults').and.returnValue({ type: 'FOO' });
    stateGoMock = jasmine.createSpy('stateGo').and.returnValue({ type: 'BAR' });

    LegalDashboardContainer = require('inject-loader!../../../../main/frontend/legal/dashboard/LegalDashboardContainer')(
      {
        './legalDashboardActions': {
          loadResults: loadResultsMock,
        },
        '../../reduxUiRouter/routerActions': {
          stateGo: stateGoMock,
        },
      }
    ).default;

    store = configureStore()(() => state);
    vdom = <LegalDashboardContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('applications', 'applications');
    expect(wrapper).toHaveProp('components', 'components');
    expect(wrapper).toHaveProp('loading', 'loading');
    expect(wrapper).toHaveProp('loadError', 'loadError');
    expect(wrapper).toHaveProp('loadError', 'loadError');
    expect(wrapper).toHaveProp('filtersAreDirty', false);
    expect(wrapper).toHaveProp('filterLoading', false);
    expect(wrapper).toHaveProp('filterSidebarOpen', false);
    expect(wrapper).toHaveProp('appliedFilterName', 'appliedFilterName');
    expect(wrapper).toHaveProp('showDirtyAsterisk', 'showDirtyAsterisk');
  });

  it('correctly maps the action creators to the LegalDashboardContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const loadResultsActionCreator = wrapper.prop('loadResults');
    expect(loadResultsActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadResultsActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);

    const stateGoActionCreator = wrapper.prop('stateGo');
    expect(stateGoActionCreator).toEqual(jasmine.any(Function));
    stateGoActionCreator('test');
    expect(store.getActions()[1]).toEqual({ type: 'BAR' });
  });

  it('renders LegalDashboardPage component', function () {
    const legalDashboardPage = shallow(vdom).find(LegalDashboardPage);
    expect(legalDashboardPage).toExist();
  });
});
