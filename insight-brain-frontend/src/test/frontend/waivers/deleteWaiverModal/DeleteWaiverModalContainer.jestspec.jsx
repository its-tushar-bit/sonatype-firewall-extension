/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'jest-enzyme';
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

import DeleteWaiverModal from '../../../../main/frontend/waivers/deleteWaiverModal/DeleteWaiverModal';
import DeleteWaiverModalContainer from 'MainRoot/waivers/deleteWaiverModal/DeleteWaiverModalContainer';

jest.mock('MainRoot/waivers/waiverActions', () => ({
  deleteWaiver: () => ({ type: 'DELETE_WAIVER' }),
  hideDeleteWaiverModal: () => ({ type: 'HIDE_DELETE_WAIVER_MODAL' }),
}));

describe('DeleteWaiverModalContainer', function () {
  let state, store, vdom;

  beforeEach(function () {
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

    expect(deleteWaiverActionCreator).toEqual(expect.any(Function));
    expect(hideDeleteWaiverModalActionCreator).toEqual(expect.any(Function));
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
