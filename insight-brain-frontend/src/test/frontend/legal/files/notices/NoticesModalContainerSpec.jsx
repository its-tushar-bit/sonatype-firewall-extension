/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import NoticesModal from '../../../../../main/frontend/legal/files/notices/NoticesModal';

describe('NoticesModalContainer', function () {
  let store,
    state,
    vdom,
    NoticesModalContainer,
    cancelNoticesModalSpy,
    setNoticeContentSpy,
    setNoticeStatusSpy,
    addNoticeSpy,
    setNoticesScopeSpy,
    saveNoticesSpy;

  beforeEach(function () {
    state = {
      advancedLegal: {
        component: {
          component: {
            licenseLegalData: {
              originalComponentNoticesScopeOwnerId: 'originalComponentNoticesScopeOwnerId',
              componentNoticesScopeOwnerId: 'componentNoticesScopeOwnerId',
              noticeFiles: 'noticeFiles',
              noticesError: 'noticesError',
              saveNoticesSubmitMask: 'saveNoticesSubmitMask',
              obligations: [
                {
                  name: 'Inclusion of Notice',
                  status: 'FLAGGED',
                },
                {
                  name: 'Something else',
                },
              ],
            },
          },
        },
        availableScopes: 'availableScopes',
      },
    };
    cancelNoticesModalSpy = jasmine.createSpy().and.returnValue({ type: 'cancelNoticesModalSpy' });
    setNoticeContentSpy = jasmine.createSpy().and.returnValue({ type: 'setNoticeContentSpy' });
    setNoticeStatusSpy = jasmine.createSpy().and.returnValue({ type: 'setNoticeStatusSpy' });
    addNoticeSpy = jasmine.createSpy().and.returnValue({ type: 'addNoticeSpy' });
    setNoticesScopeSpy = jasmine.createSpy().and.returnValue({ type: 'setNoticesScopeSpy' });
    saveNoticesSpy = jasmine.createSpy().and.returnValue({ type: 'saveNoticesSpy' });
    NoticesModalContainer = require('inject-loader!../../../../../main/frontend/legal/files/notices/NoticesModalContainer')(
      {
        '../advancedLegalFileActions': {
          cancelNoticesModal: cancelNoticesModalSpy,
          setNoticeContent: setNoticeContentSpy,
          setNoticeStatus: setNoticeStatusSpy,
          addNotice: addNoticeSpy,
          setNoticesScope: setNoticesScopeSpy,
          saveNotices: saveNoticesSpy,
        },
      }
    ).default;
    store = configureStore()(() => state);
    vdom = <NoticesModalContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('scope', 'componentNoticesScopeOwnerId');
    expect(wrapper).toHaveProp('originalScope', 'originalComponentNoticesScopeOwnerId');
    expect(wrapper).toHaveProp('availableScopes', 'availableScopes');
    expect(wrapper).toHaveProp('notices', 'noticeFiles');
    expect(wrapper).toHaveProp('error', 'noticesError');
    expect(wrapper).toHaveProp('submitMaskState', 'saveNoticesSubmitMask');
  });

  it('correctly maps the action creators to the NoticesModalContainer props', function () {
    const wrapper = shallow(vdom).dive();
    expect(wrapper.prop('cancelNoticesModal')()).toEqual({
      type: 'cancelNoticesModalSpy',
    });
    expect(wrapper.prop('setNoticeContent')()).toEqual({
      type: 'setNoticeContentSpy',
    });
    expect(wrapper.prop('setNoticeStatus')()).toEqual({
      type: 'setNoticeStatusSpy',
    });
    expect(wrapper.prop('addNotice')()).toEqual({ type: 'addNoticeSpy' });
    expect(wrapper.prop('setNoticesScope')()).toEqual({
      type: 'setNoticesScopeSpy',
    });
    expect(wrapper.prop('saveNotices')()).toEqual({ type: 'saveNoticesSpy' });
  });

  it('renders the NoticesModal component', function () {
    const noticesModal = shallow(vdom).find(NoticesModal);
    expect(noticesModal).toExist();
  });
});
