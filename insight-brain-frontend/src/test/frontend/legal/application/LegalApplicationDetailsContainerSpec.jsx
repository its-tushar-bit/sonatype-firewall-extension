/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import LegalApplicationDetailsPage from '../../../../main/frontend/legal/application/LegalApplicationDetailsPage';

describe('LegalApplicationDetailsContainer', function () {
  let store,
    state,
    vdom,
    LegalApplicationDetailsContainer,
    loadApplicationMock,
    stateGoMock,
    updateLegalSortOrderMock,
    updateComponentNameFilterMock,
    updateLicenseNameFilterMock,
    toggleFilterSidebarMock;

  beforeEach(function () {
    state = {
      legalApplicationDetails: {
        application: 'application',
        stageType: 'stageType',
        components: 'components',
        sort: 'sort',
        filterSidebarOpen: 'filterSidebarOpen',
      },
      router: {
        currentParams: {
          applicationPublicId: 'appId',
          stageTypeId: 'develop',
        },
      },
    };

    loadApplicationMock = jasmine.createSpy('loadApplication').and.returnValue({ type: 'FOO' });
    stateGoMock = jasmine.createSpy('stateGo').and.returnValue({ type: 'BAR' });
    updateLegalSortOrderMock = jasmine
      .createSpy('updateLegalSortOrder')
      .and.returnValue({ type: 'updateLegalSortOrder' });
    updateComponentNameFilterMock = jasmine
      .createSpy('updateLegalSortOrder')
      .and.returnValue({ type: 'updateComponentNameFilter' });
    updateLicenseNameFilterMock = jasmine
      .createSpy('updateLegalSortOrder')
      .and.returnValue({ type: 'updateLicenseNameFilter' });
    updateLegalSortOrderMock = jasmine
      .createSpy('updateLegalSortOrder')
      .and.returnValue({ type: 'updateLegalSortOrder' });
    toggleFilterSidebarMock = jasmine.createSpy('toggleFilterSidebar').and.returnValue({ type: 'toggleFilterSidebar' });

    LegalApplicationDetailsContainer = require('inject-loader!../../../../main/frontend/legal/application/LegalApplicationDetailsContainer')(
      {
        './legalApplicationDetailsActions': {
          loadApplication: loadApplicationMock,
        },
        '../../reduxUiRouter/routerActions': {
          stateGo: stateGoMock,
        },
        './filter/legalApplicationDetailsFilterActions': {
          updateComponentNameFilter: updateComponentNameFilterMock,
          updateLicenseNameFilter: updateLicenseNameFilterMock,
          updateLegalSortOrder: updateLegalSortOrderMock,
          toggleFilterSidebar: toggleFilterSidebarMock,
        },
      }
    ).default;

    store = configureStore()(() => state);
    vdom = <LegalApplicationDetailsContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('application', 'application');
    expect(wrapper).toHaveProp('stageType', 'stageType');
    expect(wrapper).toHaveProp('components', 'components');
    expect(wrapper).toHaveProp('applicationPublicId', 'appId');
    expect(wrapper).toHaveProp('stageTypeId', 'develop');
    expect(wrapper).toHaveProp('sort', 'sort');
    expect(wrapper).toHaveProp('filterSidebarOpen', 'filterSidebarOpen');
  });

  it('correctly maps the action creators to the LegalApplicationDetailsContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const loadApplicationActionCreator = wrapper.prop('loadApplication');
    expect(loadApplicationActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadApplicationActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);

    const stateGoActionCreator = wrapper.prop('stateGo');
    expect(stateGoActionCreator).toEqual(jasmine.any(Function));
    stateGoActionCreator('test');
    expect(store.getActions()[1]).toEqual({ type: 'BAR' });

    const updateLegalSortOrderActionCreator = wrapper.prop('updateLegalSortOrder');
    expect(updateLegalSortOrderActionCreator).toEqual(jasmine.any(Function));
    updateLegalSortOrderActionCreator('test');
    expect(store.getActions()[2]).toEqual({ type: 'updateLegalSortOrder' });

    const changeComponentNameFilterActionCreator = wrapper.prop('changeComponentNameFilter');
    expect(changeComponentNameFilterActionCreator).toEqual(jasmine.any(Function));
    changeComponentNameFilterActionCreator('test');
    expect(store.getActions()[3]).toEqual({ type: 'updateComponentNameFilter' });

    const changeLicenseNameFilterActionCreator = wrapper.prop('changeLicenseNameFilter');
    expect(changeLicenseNameFilterActionCreator).toEqual(jasmine.any(Function));
    changeLicenseNameFilterActionCreator('test');
    expect(store.getActions()[4]).toEqual({ type: 'updateLicenseNameFilter' });

    const toggleFilterSidebarActionCreator = wrapper.prop('toggleFilterSidebar');
    expect(toggleFilterSidebarActionCreator).toEqual(jasmine.any(Function));
    toggleFilterSidebarActionCreator('test');
    expect(store.getActions()[5]).toEqual({ type: 'toggleFilterSidebar' });
  });

  it('renders LegalApplicationDetailsPage component', function () {
    const legalApplicationDetailsPage = shallow(vdom).find(LegalApplicationDetailsPage);
    expect(legalApplicationDetailsPage).toExist();
  });
});
