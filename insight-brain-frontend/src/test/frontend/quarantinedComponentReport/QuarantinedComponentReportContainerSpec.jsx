/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import QuarantinedComponentReport from 'MainRoot/quarantinedComponentReport/QuarantinedComponentReport';

describe('QuarantinedComponentContainer', function () {
  let QuarantinedComponentContainer, loadComponentMock, store, state, vdom;

  beforeEach(function () {
    loadComponentMock = jasmine.createSpy('loadComponentMock').and.returnValue({
      type: 'QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED',
    });

    QuarantinedComponentContainer = require('inject-loader!../../../main/frontend/quarantinedComponentReport/QuarantinedComponentContainer')(
      {
        './quarantinedComponentReportActions': {
          loadComponent: loadComponentMock,
        },
      }
    ).default;

    state = {
      loadError: 'this is the error',
      router: {
        currentParams: {
          token: 'token',
        },
      },
      quarantinedComponentReport: {
        viewState: {
          dataLoading: false,
          loadError: null,
          repositoryComponentId: '',
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <QuarantinedComponentContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('repositoryComponentId', '');
    expect(wrapper).toHaveProp('loadError', null);
    expect(wrapper).toHaveProp('dataLoading', false);

    state = {
      ...state,
      quarantinedComponentReport: {
        ...state.quarantinedComponentReport,
        viewState: {
          ...state.quarantinedComponentReport.viewState,
          dataLoading: true,
          loadError: 'error',
          repositoryComponentId: 'repComId',
        },
      },
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('dataLoading', true);
    expect(wrapper).toHaveProp('loadError', 'error');
    expect(wrapper).toHaveProp('repositoryComponentId', 'repComId');
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive(),
      loadComponentActionCreator = wrapper.prop('loadComponent');

    expect(loadComponentActionCreator).toEqual(jasmine.any(Function));
    expect(store.getActions()).toEqual([]);

    loadComponentActionCreator('token');
    expect(store.getActions()).toEqual([{ type: 'QUARANTINED_COMPONENT_REPORT_LOAD_COMPONENT_REQUESTED' }]);
  });

  it('renders QuarantinedComponentReport component', function () {
    const quarantinedComponentReport = shallow(vdom).find(QuarantinedComponentReport);

    expect(quarantinedComponentReport).toExist();
    expect(quarantinedComponentReport).toHaveProp('loadError', null);
  });
});
