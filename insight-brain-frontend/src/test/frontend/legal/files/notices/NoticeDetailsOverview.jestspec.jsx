/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import NoticeDetailsOverview from '../../../../../main/frontend/legal/files/notices/NoticeDetailsOverview';

describe('NoticeDetailsOverview component', function () {
  let getShallowComponent;

  const minimalProps = {
    component: {
      licenseLegalData: {
        noticeFiles: [
          {
            content: 'you must include notice for this fake notice file',
          },
          {
            relPath: '/test/sub/notice.txt',
            content: 'Apache Royale bla bla bla',
          },
        ],
      },
    },
    availableScopes: {
      values: [
        { id: 'org', publicId: 'org', type: 'organization' },
        {
          id: 'ROOT_ORGANIZATION_ID',
          publicId: 'ROOT_ORGANIZATION_ID',
          type: 'organization',
        },
      ],
    },
    $state: {
      get: () => '',
      href: () => '',
    },
    componentNoticeDetails: {
      selectedNotice: {
        content: 'you must include notice for this fake notice file',
      },
    },
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(NoticeDetailsOverview, minimalProps);
  });

  it('renders the given notice overview', function () {
    const wrapper = getShallowComponent();
    let items = wrapper.find('.nx-read-only__data');
    expect(items.length).toBe(6);
    expect(items.at(0).text()).toEqual('Excluded');
    expect(items.at(1).text()).toEqual('');
    expect(items.at(2).text()).toEqual('Manually added');
    expect(items.at(3).text()).toEqual('N/A');
    expect(items.at(4).text()).toEqual('');
    expect(items.at(5).text()).toContain('you must include notice for this fake notice file');
  });
});
