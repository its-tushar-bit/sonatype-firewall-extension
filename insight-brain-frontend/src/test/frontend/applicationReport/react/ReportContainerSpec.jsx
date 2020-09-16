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
          './reportActions': {
            loadReportMetadata: loadMetadataActionMock
          }
        }).default;

    state = {
      appReport: {
        appId: 'appId',
        scanId: 'scanId',
        metadataDetails: { title: 'title' }
      },
      router: {
        currentParams: {
          appId: 'appId',
          scanId: 'scanId'
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
    expect(wrapper).toHaveProp('appId', 'appId');
    expect(wrapper).toHaveProp('scanId', 'scanId');
    expect(wrapper).toHaveProp('metadataDetails', { title: 'title' });
  });

});
