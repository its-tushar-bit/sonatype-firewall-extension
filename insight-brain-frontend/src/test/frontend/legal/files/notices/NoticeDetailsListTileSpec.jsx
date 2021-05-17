/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import NoticeDetailsList from '../../../../../main/frontend/legal/files/notices/NoticeDetailsList';

describe('NoticeDetailsList component', function () {
  let getShallowComponent;

  const minimalProps = {
    component: {
      licenseLegalData: {
        noticeFiles: [
          {
            relPath: '/test/NOTICE',
            content: 'you must include notice for this fake notice file',
            status: 'enabled',
          },
          {
            relPath: '/test/sub/notice.txt',
            content: 'Apache Royale bla bla bla',
            status: 'disabled',
          },
        ],
      },
    },
    $state: {
      get: () => '',
      href: () => '',
    },
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(NoticeDetailsList, minimalProps);
  });

  it('renders the given notices', function () {
    const wrapper = getShallowComponent();
    let noticeTexts = wrapper.find('div.nx-list__text');
    let noticeTextStatus = wrapper.find('div.nx-list__subtext');
    expect(noticeTexts.length).toBe(2);
    expect(noticeTexts.at(0).text()).toContain('/test/NOTICE');
    expect(noticeTextStatus.at(0).text()).toContain('Included in attribution report');
    expect(noticeTexts.at(1).text()).toContain('/test/sub/notice.txt');
    expect(noticeTextStatus.at(1).text()).toContain('Excluded from the report');
  });
});
