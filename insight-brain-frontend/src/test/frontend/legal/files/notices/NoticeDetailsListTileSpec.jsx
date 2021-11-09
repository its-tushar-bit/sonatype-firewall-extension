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
    hash: 'hash1',
  };

  beforeEach(function () {
    let $state = jasmine.createSpyObj('$state', ['get', 'href']);
    $state.get.and.callFake((stateName) => stateName);
    $state.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });
    minimalProps.$state = $state;

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

  it('renders the given notice files links by hash', function () {
    const wrapper = getShallowComponent();
    let noticeFileLikns = wrapper.find('a.nx-list__link');

    let noticeFileLink = noticeFileLikns.at(0);
    expect(noticeFileLink).toHaveProp(
      'href',
      'legal.componentNoticeDetails.noticeDetails-{"hash":"hash1","noticeIndex":0}'
    );

    noticeFileLink = noticeFileLikns.at(1);
    expect(noticeFileLink).toHaveProp(
      'href',
      'legal.componentNoticeDetails.noticeDetails-{"hash":"hash1","noticeIndex":1}'
    );
  });

  it('renders the given notice files links by component identifier', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      componentIdentifier: 'testComponentIdentifier',
      hash: undefined,
    });
    let noticeFileLikns = wrapper.find('a.nx-list__link');

    let noticeFileLink = noticeFileLikns.at(0);
    expect(noticeFileLink).toHaveProp(
      'href',
      'legal.noticeFilesByComponentIdentifier.noticeDetails' +
        '-{"noticeIndex":0,"componentIdentifier":"testComponentIdentifier"}'
    );

    noticeFileLink = noticeFileLikns.at(1);
    expect(noticeFileLink).toHaveProp(
      'href',
      'legal.noticeFilesByComponentIdentifier.noticeDetails' +
        '-{"noticeIndex":1,"componentIdentifier":"testComponentIdentifier"}'
    );
  });
});
