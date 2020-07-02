/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

import AddWaiverPage from '../../../main/frontend/waivers/AddWaiverPage';

describe('AddWaiverPageContainer', function() {
  let AddWaiverPageContainer,
      addWaiverMock,
      store,
      state,
      vdom;

  beforeEach(function() {
    addWaiverMock = jasmine.createSpy('addWaiver').and.returnValue({ type: 'ADD_WAIVER' });

    AddWaiverPageContainer =
        require('inject-loader!../../../main/frontend/waivers/AddWaiverPageContainer')({
          './addWaiverActions': {
            addWaiver: addWaiverMock
          }
        }).default;

    state = {
      router: {
        currentParams: 'foo'
      }
    };

    store = configureStore()(() => state);
    vdom = <AddWaiverPageContainer store={store}/>;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('stateParams', 'foo');

    state = {
      router: {
        currentParams: 'bar'
      }
    };
    store.dispatch({ type: 'FOO' });
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('stateParams', 'bar');
  });

  it('maps action creators to props', function() {
    const wrapper = shallow(vdom).dive(),
        addWaiver = wrapper.prop('addWaiver');

    expect(addWaiver).toEqual(jasmine.any(Function));
    expect(store.getActions()).toEqual([]);

    addWaiver();
    expect(store.getActions()).toEqual([{ type: 'ADD_WAIVER' }]);
  });

  it('renders AddWaiverPage component', function() {
    const addWaiverPageComponent = shallow(vdom).find(AddWaiverPage);
    expect(addWaiverPageComponent).toExist();
  });
});
