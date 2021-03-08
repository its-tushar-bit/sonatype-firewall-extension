/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import configureStore from 'redux-mock-store';
import {mount} from 'enzyme';
import {Provider} from 'react-redux';

describe('DeleteLegalFilterModalContainer', function() {

  let store,
      DeleteLegalFilterModalContainer,
      DeleteFilterModalMock;

  beforeEach(function() {
    DeleteFilterModalMock = jasmine.createSpy('DeleteFilterModalMock')
        .and.returnValue(<div>DeleteFilterModalMock</div>);

    DeleteLegalFilterModalContainer = require(
        'inject-loader!../../../../../main/frontend/legal/dashboard/filter/DeleteLegalFilterModalContainer'
    )({
      '../../../dashboard/filter/deleteFilterModal/DeleteFilterModal': DeleteFilterModalMock
    }).default;

    const state = {
      manageLegalFilters: {
        filterToDelete: 'filter1',
        deleteFilterError: 'error123',
        deleteFilterSaving: true,
        deleteFilterSuccess: true
      }
    };
    store = configureStore()(() => state);
  });

  it('passes the correct properties to DeleteFilterModal from mapStateToPops and mapDispatchToProps', function() {
    const wrapper = mount(
      <Provider store={store}>
        <DeleteLegalFilterModalContainer/>
      </Provider>
    );
    const modalProps = wrapper.find(DeleteFilterModalMock).props();
    expect(modalProps).toEqual(jasmine.objectContaining({
      filterToDelete: 'filter1',
      deleteFilterError: 'error123',
      deleteFilterSaving: true,
      deleteFilterSuccess: true,
      deleteFilter: jasmine.any(Function),
      hideDeleteFilterModal: jasmine.any(Function)
    }));
  });
});
