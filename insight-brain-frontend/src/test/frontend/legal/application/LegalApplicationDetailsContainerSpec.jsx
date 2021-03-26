/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import LegalApplicationDetailsPage from '../../../../main/frontend/legal/application/LegalApplicationDetailsPage';

describe('LegalApplicationDetailsContainer', function() {
  let store,
      state,
      vdom,
      LegalApplicationDetailsContainer,
      loadApplicationMock;

  beforeEach(function() {
    state = {
      legalApplicationDetails: {
        application: 'application',
        stageType: 'stageType',
        components: 'components'
      },
      router: {
        currentParams: {
          applicationPublicId: 'appId',
          stageTypeId: 'develop'
        }
      }
    };

    loadApplicationMock = jasmine.createSpy('loadApplication').and.returnValue({ type: 'FOO' });
    LegalApplicationDetailsContainer =
        require('inject-loader!../../../../main/frontend/legal/application/LegalApplicationDetailsContainer')({
          './legalApplicationDetailsActions': {
            loadApplication: loadApplicationMock
          }
        }).default;

    store = configureStore()(() => state);
    vdom = <LegalApplicationDetailsContainer store={store}/>;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('application', 'application');
    expect(wrapper).toHaveProp('stageType', 'stageType');
    expect(wrapper).toHaveProp('components', 'components');
    expect(wrapper).toHaveProp('applicationPublicId', 'appId');
    expect(wrapper).toHaveProp('stageTypeId', 'develop');
  });

  it('correctly maps the action creators to the LegalApplicationDetailsContainer props', function() {
    const wrapper = shallow(vdom).dive();
    const loadApplicationActionCreator = wrapper.prop('loadApplication');
    expect(loadApplicationActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadApplicationActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
  });

  it('renders LegalApplicationDetailsPage component', function() {
    const legalApplicationDetailsPage = shallow(vdom).find(LegalApplicationDetailsPage);
    expect(legalApplicationDetailsPage).toExist();
  });
});
