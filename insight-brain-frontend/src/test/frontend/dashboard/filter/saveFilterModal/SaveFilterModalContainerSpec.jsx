/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import configureStore from 'redux-mock-store';
import {mount} from 'enzyme';
import {Provider} from 'react-redux';

describe('SaveFilterModalContainer', function() {

  let store,
      state,
      mockDashboardFilterActions,
      mockManageFiltersActions,
      mockMessages,
      SaveFilterModalContainer,
      MockSaveFilterModalContent,
      vdom;

  const mockSaveFilters = ['filter1', 'filter2'];
  const mockSaveFilterError = 'saveFilterError';
  const mockErrorMessage = 'mockErrorMessage';

  beforeEach(function() {
    const mockDisplaySaveFilterModal = jasmine.createSpy('setDisplaySaveFilterModal');

    MockSaveFilterModalContent = jasmine.createSpy('mockSaveFilterModalContentPage')
        .and.returnValue(<div>mockSaveFilterModalContentPage</div>);

    mockDashboardFilterActions = {
      setDisplaySaveFilterModal: mockDisplaySaveFilterModal
    };

    mockManageFiltersActions = {
      saveFilter: jasmine.createSpy('saveFilter')
    };
    mockMessages = {
      getHttpErrorMessage: jasmine.createSpy('getHttpErrorMessage').and.returnValue(mockErrorMessage)
    };

    SaveFilterModalContainer = require(
        'inject-loader!../../../../../main/frontend/dashboard/' +
        'filter/saveFilterModal/SaveFilterModalContainer'
    )({
      './SaveFilterModalContent': MockSaveFilterModalContent,
      '../../../util/CommonServices': { Messages: mockMessages }
    }).default;

    state = {
      manageFilters: {
        savedFilters: mockSaveFilters,
        appliedFilterName: 'appliedFilterName',
        saveFilterSaving: true,
        saveFilterSuccess: false,
        saveFilterError: mockSaveFilterError
      }
    };

    store = configureStore()(() => state);
    vdom = (
      <Provider store={store}>
        <SaveFilterModalContainer
            dashboardFilterActions={mockDashboardFilterActions}
            manageFiltersActions={mockManageFiltersActions}
            Messages={mockMessages}
        />
      </Provider>
    );
  });

  it('passes the correct properties to SaveFilterModalContent from mapStateToPops and mapDispatchToProps', function() {
    const wrapper = mount(vdom);
    const modalContent = wrapper.find(MockSaveFilterModalContent).props();
    expect(modalContent.savedFilters).toEqual(mockSaveFilters);
    expect(modalContent.appliedFilterName).toEqual('appliedFilterName');
    expect(modalContent.saveFilterSaving).toEqual(true);
    expect(modalContent.saveFilterSuccess).toEqual(false);
    expect(modalContent.setDisplaySaveFilterModal).toEqual(jasmine.any(Function));
    expect(modalContent.saveFilter).toEqual(jasmine.any(Function));
    expect(modalContent.saveError).toEqual(mockErrorMessage);
    expect(mockMessages.getHttpErrorMessage).toHaveBeenCalledWith(mockSaveFilterError);
  });
});
