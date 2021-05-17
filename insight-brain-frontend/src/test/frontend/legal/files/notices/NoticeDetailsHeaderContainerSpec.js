/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import { noticeState } from './noticeCommonState';
import NoticeDetailsHeader from '../../../../../main/frontend/legal/files/notices/NoticeDetailsHeader';
import { mergeDeepRight } from 'ramda';

describe('NoticeDetailsHeaderContainer', function () {
  let store, state, vdom, NoticeDetailsHeaderContainer, loadComponentAndNoticeDetailsMock;

  beforeEach(function () {
    state = noticeState;
    loadComponentAndNoticeDetailsMock = jasmine
      .createSpy('loadComponentAndNoticeDetails')
      .and.returnValue({ type: 'FOO' });
    NoticeDetailsHeaderContainer = require('inject-loader!../../../../../main/frontend/legal/files/notices/NoticeDetailsHeaderContainer')(
      {
        './componentNoticeDetailsActions': {
          loadComponentAndNoticeDetails: loadComponentAndNoticeDetailsMock,
        },
      }
    ).default;

    store = configureStore()(() => state);
    vdom = <NoticeDetailsHeaderContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).not.toHaveProp('stageTypeId');
    expect(wrapper).toHaveProp('noticeIndex', '0');
  });

  it('maps the state slice to props with stageTypeId routing', () => {
    store = configureStore()(() => mergeDeepRight(state, { router: { currentParams: { stageTypeId: 'build' } } }));
    vdom = <NoticeDetailsHeaderContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('stageTypeId', 'build');
    expect(wrapper).toHaveProp('noticeIndex', '0');
  });

  it('correctly maps the action creators to the NoticeDetailsHeaderContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const loadComponentAndNoticeDetailsCreator = wrapper.prop('loadComponentAndNoticeDetails');

    expect(loadComponentAndNoticeDetailsCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadComponentAndNoticeDetailsCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
  });

  it('renders NoticeDetailsHeader component', function () {
    const noticeDetailsHeader = shallow(vdom).find(NoticeDetailsHeader);
    expect(noticeDetailsHeader).toExist();
  });

  it('handles route switch when current state has changed', () => {
    state = {
      ...noticeState,
      router: {
        currentState: { name: 'ComponentOverview' },
        currentParams: { hash: 'fooHash', applicationPublicId: 'appId' },
        prevParams: {
          hash: 'fooHash',
          ownerType: 'organization',
          ownerId: 'org',
          noticeIndex: '0',
        },
        prevState: { name: 'componentNoticeDetails.noticeDetails' },
      },
    };

    store = configureStore()(() => state);
    vdom = <NoticeDetailsHeaderContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('noticeIndex', '0');
  });
});
