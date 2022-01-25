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
  let QuarantinedComponentContainer, loadQuarantineReportDataSpy, store, state, vdom;

  beforeEach(function () {
    loadQuarantineReportDataSpy = jasmine.createSpy('loadQuarantineReportDataSpy').and.returnValue({
      type: 'QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED',
    });

    QuarantinedComponentContainer = require('inject-loader!../../../main/frontend/quarantinedComponentReport/QuarantinedComponentContainer')(
      {
        './quarantinedComponentReportActions': {
          loadQuarantineReportData: loadQuarantineReportDataSpy,
        },
      }
    ).default;

    state = {
      loadError: 'this is the error',
      violationsLoadError: null,
      violationsLoading: false,
      router: {
        currentParams: {
          token: 'token',
        },
      },
      quarantinedComponentReport: {
        viewState: {
          loadError: null,
          componentOverviewLoading: false,
          componentOverview: {},
          violationsLoadError: null,
          violationsLoading: false,
          violations: { activePolicyViolations: [] },
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <QuarantinedComponentContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loadError', null);
    expect(wrapper).toHaveProp('violationsLoadError', null);

    state = {
      ...state,
      quarantinedComponentReport: {
        ...state.quarantinedComponentReport,
        viewState: {
          ...state.quarantinedComponentReport.viewState,
          loadError: 'error',
          componentOverviewLoading: true,
          componentOverview: {},
        },
      },
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loadError', 'error');
    expect(wrapper).toHaveProp('violationsLoadError', null);
  });

  it('maps action creators to props', function () {
    const wrapper = shallow(vdom).dive(),
      loadQuarantineReportDataActionCreator = wrapper.prop('loadQuarantineReportData');

    expect(loadQuarantineReportDataActionCreator).toEqual(jasmine.any(Function));
    expect(store.getActions()).toEqual([]);

    loadQuarantineReportDataActionCreator('token');
    expect(store.getActions()).toEqual([{ type: 'QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED' }]);
  });

  it('renders QuarantinedComponentReport component', function () {
    const quarantinedComponentReport = shallow(vdom).find(QuarantinedComponentReport);

    expect(quarantinedComponentReport).toExist();
    expect(quarantinedComponentReport).toHaveProp('loadError', null);
    expect(quarantinedComponentReport).toHaveProp('violationsLoadError', null);
  });
});
