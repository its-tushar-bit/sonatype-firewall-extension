/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import NoticeTextsTile from '../../../../../main/frontend/legal/files/notices/NoticeTextsTile';

describe('NoticeTextsTileContainer', function () {
  let store, state, vdom, NoticeTextsTileContainer, setShowNoticesModalSpy;

  beforeEach(function () {
    state = {
      advancedLegal: {
        component: {
          component: {
            licenseLegalData: {
              noticeFiles: 'noticeFiles',
              showNoticesModal: 'showNoticesModal',
            },
          },
        },
      },
    };
    setShowNoticesModalSpy = jasmine.createSpy().and.returnValue({ type: 'setShowNoticesModalSpy' });

    NoticeTextsTileContainer = require('inject-loader!../../../../../main/frontend/legal/files/notices/NoticeTextsTileContainer')(
      {
        '../advancedLegalFileActions': {
          setShowNoticesModal: setShowNoticesModalSpy,
        },
      }
    ).default;
    store = configureStore()(() => state);
    vdom = <NoticeTextsTileContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('noticeFiles', 'noticeFiles');
    expect(wrapper).toHaveProp('showNoticesModal', 'showNoticesModal');
  });

  it('correctly maps the action creators to the NoticeTextsTileContainer props', function () {
    const wrapper = shallow(vdom).dive();
    expect(wrapper.prop('setShowNoticesModal')()).toEqual({
      type: 'setShowNoticesModalSpy',
    });
  });

  it('renders the NoticeTextsTile component', function () {
    const noticeTextsTile = shallow(vdom).find(NoticeTextsTile);
    expect(noticeTextsTile).toExist();
  });
});
