/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import ComponentCopyrightDetailsPage from '../../../../main/frontend/legal/copyright/ComponentCopyrightDetailsPage';

describe('ComponentCopyrightDetailsContainer', function() {
  let store,
      state,
      vdom,
      ComponentCopyrightDetailsContainer,
      loadComponentAndCopyrightDetailsMock,
      loadCopyrightContextsMock,
      unloadCopyrightContextsMock,
      loadFilePathsOnPageUpdateMock;

  beforeEach(function() {
    state = {
      advancedLegal: {
        component: {
          component: {
            licenseLegalData: {
              copyrights: [
                {originalContentHash: 'hash1', content: 'content1'},
                {originalContentHash: 'hash2', content: 'content2'},
                {originalContentHash: null, content: 'content3'}
              ]
            }
          },
          licenseLegalMetadata: 'licenseLegalMetadata',
          loading: 'loading',
          error: 'error'
        },
        availableScopes: {
          loading: false,
          error: null,
          values: []
        }
      },
      componentCopyrightDetails: {
        selectedCopyright: 'selectedCopyright',
        filePathsPage: 1,
        loadingCopyrightDetails: 'loadingCopyrightDetails',
        loadingFilePaths: 'loadingFilePaths',
        loadingCopyrightContext: 'loadingCopyrightContext',
        errorCopyrightFileCounts: 'errorCopyrightFileCounts',
        errorCopyrightContext: 'errorCopyrightContext',
        errorFilePaths: 'errorFilePaths',
        filePaths: ['path1', 'path2'],
        totalFileMatches: 2,
        copyrightContexts: ['context1', 'context2'],
        copyrightFileCounts: {'path1': 1, 'path2': 2}
      },
      router: {
        currentParams: {hash: 'fooHash', ownerType: 'organization', ownerId: 'org', copyrightIndex: '12'}
      },
      copyrightOverrides: {
        showEditCopyrightOverrideModal: false
      }
    };
    loadComponentAndCopyrightDetailsMock = jasmine
        .createSpy('loadComponentAndCopyrightDetails').and.returnValue({ type: 'FOO' });
    loadCopyrightContextsMock = jasmine
        .createSpy('loadCopyrightContexts').and.returnValue({ type: 'BAR' });
    unloadCopyrightContextsMock = jasmine
        .createSpy('unloadCopyrightContexts').and.returnValue({type: 'BAZ'});
    loadFilePathsOnPageUpdateMock = jasmine
        .createSpy('loadFilePathsOnPageUpdate').and.returnValue({type: 'QUX'});
    ComponentCopyrightDetailsContainer =
      require('inject-loader!../../../../main/frontend/legal/copyright/ComponentCopyrightDetailsContainer')({
        './componentCopyrightDetailsActions': {
          loadComponentAndCopyrightDetails: loadComponentAndCopyrightDetailsMock,
          loadCopyrightContexts: loadCopyrightContextsMock,
          unloadCopyrightContexts: unloadCopyrightContextsMock,
          loadFilePathsOnPageUpdate: loadFilePathsOnPageUpdateMock
        }
      }).default;

    store = configureStore()(() => state);
    vdom = <ComponentCopyrightDetailsContainer store={store}/>;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('component', {
      licenseLegalData: {
        copyrights: [
          {originalContentHash: 'hash1', content: 'content1'},
          {originalContentHash: 'hash2', content: 'content2'},
          {originalContentHash: null, content: 'content3'}
        ]
      }
    });
    expect(wrapper).toHaveProp('loading', 'loading');
    expect(wrapper).toHaveProp('error', 'error');
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('copyrightIndex', '12');
    expect(wrapper).toHaveProp('componentCopyrightDetails', {
      selectedCopyright: 'selectedCopyright',
      filePathsPage: 1,
      loadingCopyrightDetails: 'loadingCopyrightDetails',
      loadingFilePaths: 'loadingFilePaths',
      loadingCopyrightContext: 'loadingCopyrightContext',
      errorCopyrightFileCounts: 'errorCopyrightFileCounts',
      errorCopyrightContext: 'errorCopyrightContext',
      errorFilePaths: 'errorFilePaths',
      filePaths: ['path1', 'path2'],
      totalFileMatches: 2,
      copyrightContexts: ['context1', 'context2'],
      copyrightFileCounts: {'path1': 1, 'path2': 2}
    });
  });

  it('correctly maps the action creators to the ComponentLegalOverviewContainer props', function() {
    const wrapper = shallow(vdom).dive();
    const loadComponentAndCopyrightDetailsCreator = wrapper.prop('loadComponentAndCopyrightDetails');
    const loadCopyrightContextsCreator = wrapper.prop('loadCopyrightContexts');
    const unloadCopyrightContextsCreator = wrapper.prop('unloadCopyrightContexts');
    const loadFilePathsOnPageUpdateCreator = wrapper.prop('loadFilePathsOnPageUpdate');

    expect(loadComponentAndCopyrightDetailsCreator).toEqual(jasmine.any(Function));
    expect(loadCopyrightContextsCreator).toEqual(jasmine.any(Function));
    expect(unloadCopyrightContextsCreator).toEqual(jasmine.any(Function));
    expect(loadFilePathsOnPageUpdateCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadComponentAndCopyrightDetailsCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
    loadCopyrightContextsCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }]);
    unloadCopyrightContextsCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }, {type: 'BAZ'}]);
    loadFilePathsOnPageUpdateCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }, {type: 'BAZ'}, {type: 'QUX'}]);
  });

  it('renders ComponentCopyrightDetailsPage component', function() {
    const componentCopyrightDetailsPage = shallow(vdom).find(ComponentCopyrightDetailsPage);
    expect(componentCopyrightDetailsPage).toExist();
  });
});
