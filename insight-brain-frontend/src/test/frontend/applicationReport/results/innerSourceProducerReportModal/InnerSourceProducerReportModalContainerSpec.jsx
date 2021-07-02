/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { mount, shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import { pathSet } from '../../../../../main/frontend/util/jsUtil';

describe('InnerSourceProducerReportModalContainer', function () {
  let InnerSourceProducerReportModalContainer, onCloseMock, state, store, vdom;

  beforeEach(function () {
    onCloseMock = jasmine.createSpy('onClose').and.returnValue({ type: 'CLOSE_MODAL' });
    InnerSourceProducerReportModalContainer = require('inject-loader!../../../../../main/frontend/applicationReport/results/cipModal/cipTabPanel/innerSourceProducerReportModal/InnerSourceProducerReportModalContainer')(
      {
        '../../../../applicationReportActions': {
          closeInnerSourceProducerReportModal: onCloseMock,
        },
      }
    ).default;

    state = {
      applicationReport: {
        selectedComponent: {
          showInnerSourceProducerReportModal: true,
          latestReport: {
            url: 'someUrl',
          },
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <InnerSourceProducerReportModalContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('showModal', true);
    expect(wrapper).toHaveProp('reportUrl', 'someUrl');
  });

  it('correctly maps the action creators to the InnerSourceProducerReportModalContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const closeInnerSourceProducerReportModalCreator = wrapper.prop('onClose');
    expect(closeInnerSourceProducerReportModalCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    closeInnerSourceProducerReportModalCreator();
    expect(store.getActions()).toEqual([{ type: 'CLOSE_MODAL' }]);
  });

  it('returns null if showModal is false', function () {
    state = pathSet(['applicationReport', 'selectedComponent', 'showInnerSourceProducerReportModal'], false, state);
    store = configureStore()(() => state);
    vdom = <InnerSourceProducerReportModalContainer store={store} />;
    const wrapper = mount(vdom);
    expect(wrapper).toBeEmptyRender();
  });
});
