/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import { copyrightState } from './copyrightCommonState';
import CopyrightDetailsContents from '../../../../main/frontend/legal/copyright/CopyrightDetailsContents';

describe('CopyrightDetailsContentsContainer', function() {
  let store,
      state,
      vdom,
      CopyrightDetailsContentsContainer,
      loadCopyrightContextsMock,
      unloadCopyrightContextsMock,
      loadFilePathsOnPageUpdateMock;

  beforeEach(function() {
    state = copyrightState;
    loadCopyrightContextsMock = jasmine
        .createSpy('loadCopyrightContexts').and.returnValue({ type: 'FOO' });
    unloadCopyrightContextsMock = jasmine
        .createSpy('unloadCopyrightContexts').and.returnValue({type: 'BAR'});
    loadFilePathsOnPageUpdateMock = jasmine
        .createSpy('loadFilePathsOnPageUpdate').and.returnValue({type: 'BAZ'});
    CopyrightDetailsContentsContainer =
      require('inject-loader!../../../../main/frontend/legal/copyright/CopyrightDetailsContentsContainer')({
        './componentCopyrightDetailsActions': {
          loadCopyrightContexts: loadCopyrightContextsMock,
          unloadCopyrightContexts: unloadCopyrightContextsMock,
          loadFilePathsOnPageUpdate: loadFilePathsOnPageUpdateMock
        }
      }).default;

    store = configureStore()(() => state);
    vdom = <CopyrightDetailsContentsContainer store={store}/>;
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
    const loadCopyrightContextsCreator = wrapper.prop('loadCopyrightContexts');
    const unloadCopyrightContextsCreator = wrapper.prop('unloadCopyrightContexts');
    const loadFilePathsOnPageUpdateCreator = wrapper.prop('loadFilePathsOnPageUpdate');

    expect(loadCopyrightContextsCreator).toEqual(jasmine.any(Function));
    expect(unloadCopyrightContextsCreator).toEqual(jasmine.any(Function));
    expect(loadFilePathsOnPageUpdateCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadCopyrightContextsCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
    unloadCopyrightContextsCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }]);
    loadFilePathsOnPageUpdateCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }, { type: 'BAR' }, {type: 'BAZ'}]);
  });

  it('renders CopyrightDetailsContents component', function() {
    const copyrightDetailsContents = shallow(vdom).find(CopyrightDetailsContents);
    expect(copyrightDetailsContents).toExist();
  });
});
