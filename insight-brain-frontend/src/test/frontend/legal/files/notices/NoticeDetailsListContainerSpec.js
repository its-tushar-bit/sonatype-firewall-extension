/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import { noticeState } from './noticeCommonState';
import NoticeDetailsList from '../../../../../main/frontend/legal/files/notices/NoticeDetailsList';
import { mergeDeepRight } from 'ramda';

describe('NoticeDetailsListContainer', function () {
  let store, state, vdom, NoticeDetailsListContainer;

  beforeEach(function () {
    state = noticeState;
    NoticeDetailsListContainer = require('inject-loader!../../../../../main/frontend/legal/files/notices/NoticeDetailsListContainer')(
      {}
    ).default;

    store = configureStore()(() => state);
    vdom = <NoticeDetailsListContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).not.toHaveProp('stageTypeId');
    expect(wrapper).toHaveProp('noticeIndex', '0');
  });

  it('maps the state slice to props with stageTypeId routing', () => {
    store = configureStore()(() =>
      mergeDeepRight(state, {
        router: {
          currentParams: {
            ownerType: 'organization',
            ownerId: 'org',
            stageTypeId: 'build',
            noticeIndex: '0',
          },
        },
      })
    );
    vdom = <NoticeDetailsListContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('stageTypeId', 'build');
    expect(wrapper).toHaveProp('noticeIndex', '0');
  });

  it('renders NoticeDetailsList component', function () {
    const noticeDetailsList = shallow(vdom).find(NoticeDetailsList);
    expect(noticeDetailsList).toExist();
  });

  it('handles route switch when current state has changed', () => {
    state = {
      ...noticeState,
      router: {
        currentState: { name: 'ComponentOverview' },
        currentParams: { hash: 'fooHash', applicationPublicId: 'appId', componentIdentifier: 'fooComponentIdentifier' },
        prevParams: {
          hash: 'fooHash',
          componentIdentifier: 'fooComponentIdentifier',
          ownerType: 'organization',
          ownerId: 'org',
          noticeIndex: '0',
        },
        prevState: { name: 'componentNoticeDetails.noticeDetails' },
      },
    };

    store = configureStore()(() => state);
    vdom = <NoticeDetailsListContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('componentIdentifier', 'fooComponentIdentifier');
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('noticeIndex', '0');
  });
});
