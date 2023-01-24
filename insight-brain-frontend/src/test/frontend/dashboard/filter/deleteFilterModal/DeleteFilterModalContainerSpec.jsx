/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import configureStore from 'redux-mock-store';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';

describe('DeleteFilterModalContainer', function () {
  let store, DeleteFilterModalContainer, DeleteFilterModalMock;

  beforeEach(function () {
    DeleteFilterModalMock = jasmine
      .createSpy('DeleteFilterModalMock')
      .and.returnValue(<div>DeleteFilterModalMock</div>);

    DeleteFilterModalContainer = require('inject-loader!../../../../../main/frontend/dashboard/' +
      'filter/deleteFilterModal/DeleteFilterModalContainer')({
      './DeleteFilterModal': DeleteFilterModalMock,
    }).default;

    const state = {
      manageFilters: {
        filterToDelete: 'filter1',
        deleteFilterError: 'error123',
        deleteFilterMaskState: true,
      },
    };
    store = configureStore()(() => state);
  });

  it('passes the correct properties to DeleteFilterModal from mapStateToPops and mapDispatchToProps', function () {
    const wrapper = mount(
      <Provider store={store}>
        <DeleteFilterModalContainer />
      </Provider>
    );
    const modalProps = wrapper.find(DeleteFilterModalMock).props();
    expect(modalProps).toEqual(
      jasmine.objectContaining({
        filterToDelete: 'filter1',
        deleteFilterError: 'error123',
        deleteFilterMaskState: true,
        deleteFilter: jasmine.any(Function),
        hideDeleteFilterModal: jasmine.any(Function),
      })
    );
  });
});
