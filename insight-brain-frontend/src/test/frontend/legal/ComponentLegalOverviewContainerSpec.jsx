/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import ComponentLegalOverviewPage from '../../../main/frontend/legal/ComponentLegalOverviewPage';

describe('ComponentLegalOverviewContainer', function () {
  let store, state, vdom, ComponentLegalOverviewContainer, loadComponentActionMock, loadAvailableScopesActionMock;

  beforeEach(function () {
    state = {
      advancedLegal: {
        component: {
          component: {
            licenseLegalData: {
              obligations: 'obligations',
            },
          },
          licenseLegalMetadata: 'licenseLegalMetadata',
          loading: 'loading',
          error: 'error',
        },
        availableScopes: {
          loading: false,
          error: null,
          values: [],
        },
      },
      router: {
        currentParams: {
          hash: 'fooHash',
          organizationId: 'organizationId',
          applicationPublicId: 'applicationPublicId',
          stageTypeId: 'stage-type-id',
        },
      },
      copyrightOverrides: {
        showEditCopyrightOverrideModal: false,
      },
    };

    loadComponentActionMock = jasmine.createSpy('loadComponent').and.returnValue({ type: 'FOO' });
    loadAvailableScopesActionMock = jasmine.createSpy('loadAvailableScopes').and.returnValue({ type: 'BAR' });
    ComponentLegalOverviewContainer = require('inject-loader!../../../main/frontend/legal/ComponentLegalOverviewContainer')(
      {
        './advancedLegalActions': {
          loadComponent: loadComponentActionMock,
          loadAvailableScopes: loadAvailableScopesActionMock,
        },
      }
    ).default;

    store = configureStore()(() => state);
    vdom = <ComponentLegalOverviewContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('component', {
      licenseLegalData: {
        obligations: 'obligations',
      },
    });
    expect(wrapper).toHaveProp('licenseLegalMetadata', 'licenseLegalMetadata');
    expect(wrapper).toHaveProp('obligations', 'obligations');
    expect(wrapper).toHaveProp('loading', 'loading');
    expect(wrapper).toHaveProp('error', 'error');
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('organizationId', 'organizationId');
    expect(wrapper).toHaveProp('applicationPublicId', 'applicationPublicId');
    expect(wrapper).toHaveProp('stageTypeId', 'stage-type-id');
    expect(wrapper).toHaveProp('availableScopes', {
      loading: false,
      error: null,
      values: [],
    });
  });

  it('correctly maps the action creators to the ComponentLegalOverviewContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const loadComponentActionCreator = wrapper.prop('loadComponent');
    const loadAvailableScopesActionCreator = wrapper.prop('loadAvailableScopes');
    expect(loadComponentActionCreator).toEqual(jasmine.any(Function));
    expect(loadAvailableScopesActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadComponentActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
    loadAvailableScopesActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }]);
  });

  it('renders ComponentLegalOverviewPage component', function () {
    const componentLegalOverviewPage = shallow(vdom).find(ComponentLegalOverviewPage);
    expect(componentLegalOverviewPage).toExist();
  });
});
