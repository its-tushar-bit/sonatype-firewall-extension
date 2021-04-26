/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import LegalApplicationDetailsFilter from '../../../../../main/frontend/legal/application/filter/LegalApplicationDetailsFilter';

describe('LegalApplicationDetailsFilterContainer', function () {
  let store,
    state,
    vdom,
    LegalApplicationDetailsFilterContainer,
    toggleFilterMock,
    updateComponentNameFilterMock,
    updateLicenseNameFilterMock;

  beforeEach(function () {
    state = {
      legalApplicationDetails: {
        foo: 'bar',
      },
    };

    toggleFilterMock = jasmine.createSpy('toggleFilter').and.returnValue({ type: 'FOO' });
    updateComponentNameFilterMock = jasmine.createSpy('updateComponentNameFilter').and.returnValue({ type: 'BAR' });
    updateLicenseNameFilterMock = jasmine.createSpy('updateLicenseNameFilter').and.returnValue({ type: 'BAZ' });

    LegalApplicationDetailsFilterContainer = require('inject-loader!../../../../../main/frontend/legal/application/filter' +
      '/LegalApplicationDetailsFilterContainer')({
      './legalApplicationDetailsFilterActions': {
        toggleFilter: toggleFilterMock,
        updateComponentNameFilter: updateComponentNameFilterMock,
        updateLicenseNameFilter: updateLicenseNameFilterMock,
      },
    }).default;

    store = configureStore()(() => state);
    vdom = <LegalApplicationDetailsFilterContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('foo', 'bar');
  });

  it('correctly maps the action creators to props', function () {
    const wrapper = shallow(vdom).dive();
    const loadFilterActionCreator = wrapper.prop('toggleFilter');
    const updateComponentFilterCreator = wrapper.prop('updateComponentNameFilter');
    const updateLicenseFilterCreator = wrapper.prop('updateLicenseNameFilter');

    expect(loadFilterActionCreator).toEqual(jasmine.any(Function));
    expect(updateComponentFilterCreator).toEqual(jasmine.any(Function));
    expect(updateLicenseFilterCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadFilterActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
    updateComponentFilterCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }]);
    updateLicenseFilterCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }, { type: 'BAZ' }]);
  });

  it('renders LegalApplicationDetailsFilter component', function () {
    const legalApplicationDetailsFilter = shallow(vdom).find(LegalApplicationDetailsFilter);
    expect(legalApplicationDetailsFilter).toExist();
  });
});
