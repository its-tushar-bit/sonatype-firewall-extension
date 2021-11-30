/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

import RequestWaiverPage from '../../../main/frontend/waivers/RequestWaiverPage';

describe('RequestWaiverPageContainer', function () {
  let RequestWaiverPageContainer, loadViolationMock, store, state, vdom;

  beforeEach(function () {
    loadViolationMock = jasmine.createSpy('loadViolation').and.returnValue({ type: 'LOAD_VIOLATION' });

    RequestWaiverPageContainer = require('inject-loader!../../../main/frontend/waivers/RequestWaiverPageContainer')({
      './../violation/violationActions': {
        loadViolation: loadViolationMock,
      },
    }).default;

    state = {
      violation: {
        loading: false,
        violationDetailsError: '',
        violationDetails: {},
      },
      router: {
        currentParams: {
          violationId: 'foo',
        },
        prevState: {
          name: 'prevStateName',
        },
        prevParams: {
          publicId: 'publicId',
          scanId: 'scanId',
          hash: 'hash',
          sidebarReference: 'sidebarReference',
          type: 'type',
        },
      },
    };

    store = configureStore()(() => state);
    vdom = <RequestWaiverPageContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', false);
    expect(wrapper).toHaveProp('violationDetailsError', '');
    expect(wrapper).toHaveProp('violationDetails', {});
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('name', 'prevStateName');
    expect(wrapper).toHaveProp('prevParams', {
      publicId: 'publicId',
      scanId: 'scanId',
      hash: 'hash',
      sidebarReference: 'sidebarReference',
      type: 'type',
    });
    state = {
      ...state,
      violation: {
        loading: true,
        violationDetails: {
          id: 'bar',
        },
      },
    };
    wrapper = shallow(vdom).dive();

    expect(wrapper).toHaveProp('loading', true);
    expect(wrapper).toHaveProp('violationId', 'foo');
    expect(wrapper).toHaveProp('violationDetails', { id: 'bar' });
  });

  it('renders RequestWaiverPage component', function () {
    const requestWaiverPageComponent = shallow(vdom).find(RequestWaiverPage);
    expect(requestWaiverPageComponent).toExist();
    expect(requestWaiverPageComponent).toHaveProp('violationId', 'foo');
  });
});
