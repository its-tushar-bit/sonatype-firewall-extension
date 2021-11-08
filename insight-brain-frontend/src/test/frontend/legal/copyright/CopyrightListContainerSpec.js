/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import { copyrightState } from './copyrightCommonState';
import CopyrightList from '../../../../main/frontend/legal/copyright/CopyrightList';
import { copyrightDetailsStateNameSuffix } from '../../../../main/frontend/legal/copyright/copyrightDetailsUtils';

describe('CopyrightListContainer', function () {
  let store, state, vdom, CopyrightListContainer;

  beforeEach(function () {
    state = copyrightState;
    CopyrightListContainer = require('inject-loader!../../../../main/frontend/legal/copyright/CopyrightListContainer')(
      {}
    ).default;

    store = configureStore()(() => state);
    vdom = <CopyrightListContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('component', {
      licenseLegalData: {
        copyrights: [
          { originalContentHash: 'hash1', content: 'content1' },
          { originalContentHash: 'hash2', content: 'content2' },
          { originalContentHash: null, content: 'content3' },
        ],
      },
    });
    expect(wrapper).toHaveProp('loading', 'loading');
    expect(wrapper).toHaveProp('error', 'error');
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('componentIdentifier', 'fooComponentIdentifier');
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
      copyrightFileCounts: { path1: 1, path2: 2 },
    });
  });

  it('renders CopyrightList component', function () {
    const copyrightList = shallow(vdom).find(CopyrightList);
    expect(copyrightList).toExist();
  });

  it('handles route switch when current state has changed', () => {
    state = {
      ...copyrightState,
      router: {
        currentState: { name: 'ComponentOverview' },
        currentParams: {
          hash: 'fooHash',
          componentIdentifier: 'fooComponentIdentifier',
          applicationPublicId: 'appId',
        },
        prevParams: {
          hash: 'fooHash',
          componentIdentifier: 'fooComponentIdentifier',
          ownerType: 'organization',
          ownerId: 'org',
          copyrightIndex: '12',
        },
        prevState: { name: copyrightDetailsStateNameSuffix },
      },
    };

    store = configureStore()(() => state);
    vdom = <CopyrightListContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('componentIdentifier', 'fooComponentIdentifier');
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('copyrightIndex', '12');
  });
});
