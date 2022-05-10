/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import configureStore from 'redux-mock-store';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';

describe('SaveFilterModalContainer', function () {
  let store,
    state,
    mockDashboardFilterActions,
    mockManageFiltersActions,
    mockMessages,
    SaveFilterModalContainer,
    MockSaveFilterModalContent,
    vdom;

  const mockSaveFilterError = 'saveFilterError';
  const mockSaveFilterWarning = 'saveFilterWarning';
  const mockErrorMessage = 'mockErrorMessage';

  beforeEach(function () {
    const mockDisplaySaveFilterModal = jasmine.createSpy('setDisplaySaveFilterModal');

    MockSaveFilterModalContent = jasmine
      .createSpy('mockSaveFilterModalContentPage')
      .and.returnValue(<div>mockSaveFilterModalContentPage</div>);

    mockDashboardFilterActions = {
      setDisplaySaveFilterModal: mockDisplaySaveFilterModal,
    };

    mockManageFiltersActions = {
      saveFilter: jasmine.createSpy('saveFilter'),
    };
    mockMessages = {
      getHttpErrorMessage: jasmine.createSpy('getHttpErrorMessage').and.returnValue(mockErrorMessage),
    };

    SaveFilterModalContainer = require('inject-loader!../../../../../main/frontend/dashboard/' +
      'filter/saveFilterModal/SaveFilterModalContainer')({
      './SaveFilterModalContent': MockSaveFilterModalContent,
      '../../../utilAngular/CommonServices': { Messages: mockMessages },
    }).default;

    state = {
      manageFilters: {
        appliedFilterName: 'appliedFilterName',
        saveFilterSaving: true,
        saveFilterSuccess: false,
        saveFilterError: mockSaveFilterError,
        saveFilterWarning: mockSaveFilterWarning,
      },
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

  it('passes the correct properties to SaveFilterModalContent from mapStateToPops and mapDispatchToProps', function () {
    const wrapper = mount(vdom);
    const modalContent = wrapper.find(MockSaveFilterModalContent).props();
    expect(modalContent.appliedFilterName).toEqual('appliedFilterName');
    expect(modalContent.saveFilterSaving).toEqual(true);
    expect(modalContent.saveFilterSuccess).toEqual(false);
    expect(modalContent.saveFilter).toEqual(jasmine.any(Function));
    expect(modalContent.saveError).toEqual(mockErrorMessage);
    expect(modalContent.saveFilterWarning).toEqual(mockSaveFilterWarning);
    expect(mockMessages.getHttpErrorMessage).toHaveBeenCalledWith(mockSaveFilterError);
  });
});
