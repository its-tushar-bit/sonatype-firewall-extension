/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

describe('ReportContainer component', function() {

  let ReportContainer,
      loadMetadataActionMock,
      state,
      store,
      vdom;

  beforeEach(function() {

    loadMetadataActionMock = jasmine.createSpy('loadReportMetadata').and.returnValue({ type: 'LOAD_DATA' });
    ReportContainer =
        require('inject-loader!../../../../main/frontend/applicationReport/react/ReportContainer')({
          '../applicationReportActions': {
            loadReportMetadata: loadMetadataActionMock
          }
        }).default;

    state = {
      applicationReport: {
        publicId: 'appId',
        scanId: 'scanId',
        metadata: { reportTitle: 'title' },
        selectedReport: { reportVersion: 5 }
      },
      router: {
        currentParams: {
          publicId: 'appId',
          scanId: 'scanId',
          unknownjs: true,
          embeddable: true,
          policyViolationId: 'CVE-123'
        }
      }
    };

    store = configureStore()(() => state);
    vdom = (
      <ReportContainer store={store}/>
    );
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
  });

});
