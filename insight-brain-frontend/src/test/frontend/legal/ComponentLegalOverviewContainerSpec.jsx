/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import ComponentLegalOverviewPage from '../../../main/frontend/legal/ComponentLegalOverviewPage';

describe('ComponentLegalOverviewContainer', function() {
  let store,
      state,
      vdom,
      ComponentLegalOverviewContainer,
      loadComponentActionMock;

  beforeEach(function() {
    state = {
      advancedLegal: {
        component: {
          component: 'component',
          licenseLegalMetadata: 'licenseLegalMetadata',
          loading: 'loading',
          error: 'error'
        }
      },
      router: {
        currentParams: { hash: 'fooHash' }
      }
    };

    loadComponentActionMock = jasmine.createSpy('loadComponent').and.returnValue({ type: 'FOO' });
    ComponentLegalOverviewContainer =
        require('inject-loader!../../../main/frontend/legal/ComponentLegalOverviewContainer')({
          '../advancedLegal/advancedLegalActions': {
            loadComponent: loadComponentActionMock
          }
        }).default;

    store = configureStore()(() => state);
    vdom = <ComponentLegalOverviewContainer store={store}/>;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('component', 'component');
    expect(wrapper).toHaveProp('licenseLegalMetadata', 'licenseLegalMetadata');
    expect(wrapper).toHaveProp('loading', 'loading');
    expect(wrapper).toHaveProp('error', 'error');
    expect(wrapper).toHaveProp('hash', 'fooHash');
  });

  it('correctly maps the action creators to the ComponentLegalOverviewContainer props', function() {
    const wrapper = shallow(vdom).dive();
    const loadComponentActionCreator = wrapper.prop('loadComponent');
    expect(loadComponentActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadComponentActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
  });

  it('renders ComponentLegalOverviewPage component', function() {
    const componentLegalOverviewPage = shallow(vdom).find(ComponentLegalOverviewPage);
    expect(componentLegalOverviewPage).toExist();
  });
});
