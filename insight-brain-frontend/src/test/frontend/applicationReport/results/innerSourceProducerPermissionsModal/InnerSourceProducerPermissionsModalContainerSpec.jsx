/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { mount, shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import { pathSet } from '../../../../../main/frontend/util/jsUtil';

describe('InnerSourceProducerPermissionsModalContainer', function () {
  let InnerSourceProducerPermissionsModalContainer, onCloseMock, state, store, vdom;

  beforeEach(function () {
    onCloseMock = jasmine.createSpy('onClose').and.returnValue({ type: 'CLOSE_MODAL' });
    InnerSourceProducerPermissionsModalContainer = require('inject-loader!../../../../../main/frontend/applicationReport/results/cipModal/cipTabPanel/innerSourceProducerPermissionsModal/InnerSourceProducerPermissionsModalContainer')(
      {
        '../../../../applicationReportActions': {
          closeInnerSourceProducerPermissionsModal: onCloseMock,
        },
      }
    ).default;

    state = {
      applicationReport: {
        selectedComponent: {
          showInnerSourceProducerPermissionsModal: true,
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <InnerSourceProducerPermissionsModalContainer store={store} applicationName="someName" />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('showModal', true);
  });

  it('correctly maps the action creators to the InnerSourceProducerPermissionsModal props', function () {
    const wrapper = shallow(vdom).dive();
    const closeInnerSourceProducerPermissionsModalCreator = wrapper.prop('onClose');
    expect(closeInnerSourceProducerPermissionsModalCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    closeInnerSourceProducerPermissionsModalCreator();
    expect(store.getActions()).toEqual([{ type: 'CLOSE_MODAL' }]);
  });

  it('returns null if showModal is false', function () {
    state = pathSet(
      ['applicationReport', 'selectedComponent', 'showInnerSourceProducerPermissionsModal'],
      false,
      state
    );
    store = configureStore()(() => state);
    vdom = <InnerSourceProducerPermissionsModalContainer store={store} applicationName="someName" />;
    const wrapper = mount(vdom);
    expect(wrapper).toBeEmptyRender();
  });

  it('returns null if applicationName is not present', function () {
    vdom = <InnerSourceProducerPermissionsModalContainer store={store} />;
    const wrapper = mount(vdom);
    expect(wrapper).toBeEmptyRender();
  });
});
