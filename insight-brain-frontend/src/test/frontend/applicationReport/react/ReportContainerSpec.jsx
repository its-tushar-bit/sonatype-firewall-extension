/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

describe('ReportContainer component', function () {
  let ReportContainer, loadMetadataActionMock, state, store, vdom;

  beforeEach(function () {
    loadMetadataActionMock = jasmine.createSpy('loadReportMetadata').and.returnValue({ type: 'LOAD_DATA' });
    ReportContainer = require('inject-loader!../../../../main/frontend/applicationReport/react/ReportContainer')({
      '../applicationReportActions': {
        loadReportMetadata: loadMetadataActionMock,
      },
    }).default;

    state = {
      applicationReport: {
        publicId: 'appId',
        scanId: 'scanId',
        metadata: { reportTitle: 'title' },
        selectedReport: { reportVersion: 5 },
        exactValueFilters: { matchState: 'unknown' },
        pendingLoads: new Set(),
        loadError: null,
      },
      router: {
        currentParams: {
          publicId: 'appId',
          scanId: 'scanId',
          unknownjs: true,
          embeddable: true,
          policyViolationId: 'CVE-123',
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <ReportContainer store={store} />;
  });

  it('maps the state "ReportPage" to ReportContainer props', () => {
    store.dispatch({ type: 'ANY_ACTION' });
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('publicId', 'appId');
    expect(wrapper).toHaveProp('scanId', 'scanId');
    expect(wrapper).toHaveProp('unknownjs', true);
    expect(wrapper).toHaveProp('embeddable', true);
    expect(wrapper).toHaveProp('policyViolationId', 'CVE-123');
    expect(wrapper).toHaveProp('metadata', { reportTitle: 'title' });
    expect(wrapper).toHaveProp('selectedReport', { reportVersion: 5 });
    expect(wrapper).toHaveProp('loadError', null);
    expect(wrapper).toHaveProp('exactValueFilters', { matchState: 'unknown' });
  });

  it('maps the loading flag as true if the metadata is not defined', function () {
    state.applicationReport.metadata = null;
    store.dispatch({ type: 'ANY_ACTION' });
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('loading', true);
  });

  it('maps the loading flag as true if the pendingLoads set is not empty', function () {
    state.applicationReport.pendingLoads = new Set(['asdf']);
    store.dispatch({ type: 'ANY_ACTION' });
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('loading', true);
  });

  it('maps the loading flag as false if the pendingLoads set is empty and metadata is present', function () {
    store.dispatch({ type: 'ANY_ACTION' });
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('loading', false);
  });

  it('maps the loading flag as false if the loadError is set', function () {
    state.applicationReport.pendingLoads = new Set(['asdf']);
    state.applicationReport.metadata = null;
    state.applicationReport.loadError = 'foobar';
    store.dispatch({ type: 'ANY_ACTION' });
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('loading', false);
  });
});
