/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

import DeleteWaiverModal from '../../../../main/frontend/waivers/deleteWaiverModal/DeleteWaiverModal';

describe('DeleteWaiverModalContainer', function () {
  let DeleteWaiverModalContainer, deleteWaiverMock, hideDeleteWaiverModalMock, state, store, vdom;

  beforeEach(function () {
    deleteWaiverMock = jasmine.createSpy('deleteWaiver').and.returnValue({
      type: 'DELETE_WAIVER',
    });
    hideDeleteWaiverModalMock = jasmine.createSpy('hideDeleteWaiverModal').and.returnValue({
      type: 'HIDE_DELETE_WAIVER_MODAL',
    });

    DeleteWaiverModalContainer = require('inject-loader!../../../../main/frontend/waivers/deleteWaiverModal/DeleteWaiverModalContainer')(
      {
        '../waiverActions': {
          deleteWaiver: deleteWaiverMock,
          hideDeleteWaiverModal: hideDeleteWaiverModalMock,
        },
      }
    ).default;

    state = {
      deleteWaiver: {
        deleteWaiverSaving: null,
        deleteWaiverError: null,
        waiverToDelete: { waiverId: 'foo' },
      },
    };

    store = configureStore()(() => state);
    vdom = <DeleteWaiverModalContainer store={store} />;
  });

  it('maps the state slice to props', function () {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('deleteWaiverSaving', null);
    expect(wrapper).toHaveProp('deleteWaiverError', null);
    expect(wrapper).toHaveProp('waiverToDelete', { waiverId: 'foo' });
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive(),
      deleteWaiverActionCreator = wrapper.prop('deleteWaiver'),
      hideDeleteWaiverModalActionCreator = wrapper.prop('hideDeleteWaiverModal');

    expect(deleteWaiverActionCreator).toEqual(jasmine.any(Function));
    expect(hideDeleteWaiverModalActionCreator).toEqual(jasmine.any(Function));
    expect(store.getActions()).toEqual([]);

    deleteWaiverActionCreator();
    expect(store.getActions()).toEqual([{ type: 'DELETE_WAIVER' }]);

    hideDeleteWaiverModalActionCreator();
    expect(store.getActions()).toEqual([{ type: 'DELETE_WAIVER' }, { type: 'HIDE_DELETE_WAIVER_MODAL' }]);
  });

  it('renders DeleteWaiverModal component', function () {
    const deleteWaiverModalComponent = shallow(vdom).find(DeleteWaiverModal);
    expect(deleteWaiverModalComponent).toExist();
    expect(deleteWaiverModalComponent).toHaveProp('waiverToDelete', {
      waiverId: 'foo',
    });
  });
});
