/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import LegalDashboardFilter from '../../../../../main/frontend/legal/dashboard/filter/LegalDashboardFilter';

describe('LegalDashboardFilterContainerSpec', function () {
  let store,
    state,
    vdom,
    LegalDashboardFilterContainer,
    loadFilterMock,
    manageFilterActionMock;

  beforeEach(function () {
    state = {
      manageLegalFilters: {
        appliedFilterName: 'appliedFilterName',
        showDirtyAsterisk: 'showDirtyAsterisk',
        showSaveFilterModal: 'showSaveFilterModal',
        savedFilters: 'savedFilters',
        filtersDropdownOpen: 'filtersDropdownOpen',
      },
      legalDashboardFilter: {
        foo: 'bar',
      },
    };

    loadFilterMock = jasmine
      .createSpy('loadFilter')
      .and.returnValue({ type: 'FOO' });
    manageFilterActionMock = jasmine
      .createSpy('manageFilterAction')
      .and.returnValue({ type: 'FOO2' });
    LegalDashboardFilterContainer = require('inject-loader!../../../../../main/frontend/legal/dashboard/filter/LegalDashboardFilterContainer')(
      {
        './legalDashboardFilterActions': {
          loadFilter: loadFilterMock,
        },
        './manageLegalFiltersActions': {
          manageFilterAction: manageFilterActionMock,
        },
      }
    ).default;

    store = configureStore()(() => state);
    vdom = <LegalDashboardFilterContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('appliedFilterName', 'appliedFilterName');
    expect(wrapper).toHaveProp('showDirtyAsterisk', 'showDirtyAsterisk');
    expect(wrapper).toHaveProp('showSaveFilterModal', 'showSaveFilterModal');
    expect(wrapper).toHaveProp('savedFilters', 'savedFilters');
    expect(wrapper).toHaveProp('filtersDropdownOpen', 'filtersDropdownOpen');
    expect(wrapper).toHaveProp('foo', 'bar');
  });

  it('correctly maps the action creators to the LegalDashboardContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const loadFilterActionCreator = wrapper.prop('loadFilter');
    expect(loadFilterActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadFilterActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);

    const manageFilterActionCreator = wrapper.prop('manageFilterAction');
    expect(manageFilterActionCreator).toEqual(jasmine.any(Function));

    manageFilterActionCreator('test');
    expect(store.getActions()[1]).toEqual({ type: 'FOO2' });
  });

  it('renders LegalDashboardFilter component', function () {
    const legalDashboardFilter = shallow(vdom).find(LegalDashboardFilter);
    expect(legalDashboardFilter).toExist();
  });
});
