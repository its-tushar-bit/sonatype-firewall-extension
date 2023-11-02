/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import NoticeTextsTile from '../../../../../main/frontend/legal/files/notices/NoticeTextsTile';
import { NxButton, NxFontAwesomeIcon, NxAccordion, NxTextLink } from '@sonatype/react-shared-components';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';

describe('NoticeTextsTile', function () {
  let getShallowComponent, minimalProps, setShowNoticesModalSpy, $state;

  beforeEach(function () {
    setShowNoticesModalSpy = jasmine.createSpy('setShowNoticesModalSpy');
    $state = jasmine.createSpyObj('$state', ['get', 'href']);
    $state.get.and.callFake((stateName) => stateName);
    $state.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });

    minimalProps = {
      setShowNoticesModal: setShowNoticesModalSpy,
      noticeFiles: [
        {
          originalStatus: 'enabled',
          originalContent: 'notice content 1',
          content: 'notice content 1',
          relPath: 'path1/notice.txt',
        },
        {
          originalStatus: 'enabled',
          originalContent: 'notice content 2',
          content: 'notice content 2',
        },
        {
          originalStatus: 'disabled',
          originalContent: 'notice content 3',
          content: 'notice content 3',
        },
      ],
      showNoticesModal: false,
      hash: 'testHash',
      $state: $state,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(NoticeTextsTile, minimalProps);
  });

  it('renders a header with label `Notice Files`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxAccordion.Title)).toHaveText('Notice Files');
  });

  it('renders the given notices', function () {
    const wrapper = getShallowComponent();
    const notices = wrapper.find('.legal-file');
    expect(notices.length).toBe(2);
    expect(notices.at(0).find('.legal-file-path')).toHaveText('path1/notice.txt');
    expect(notices.at(0).find('blockquote')).toHaveText('notice content 1');
    expect(notices.at(1).find('.legal-file-path')).toHaveText('');
    expect(notices.at(1).find('blockquote')).toHaveText('notice content 2');
  });

  it('renders none found if there are no notices', function () {
    const wrapper = getShallowComponent({ noticeFiles: [] });
    const content = wrapper.find(NxAccordion).shallow();
    expect(content.find('.nx-accordion__content')).toHaveText('None found');
  });

  it('renders an add button if there are no notices', function () {
    const wrapper = getShallowComponent({ noticeFiles: [] });
    const button = wrapper.find(NxButton);
    expect(button.find(NxFontAwesomeIcon).at(0).prop('icon')).toEqual(faPlus);
    expect(button.find('span').at(0)).toHaveText('Add');
  });

  it('renders an edit button if there is at least one notice', function () {
    const wrapper = getShallowComponent();
    const button = wrapper.find(NxButton);
    expect(button.find(NxFontAwesomeIcon).at(0).prop('icon')).toEqual(faPen);
    expect(button.find('span').at(0)).toHaveText('Edit');
  });

  it('shows the notices modal when clicking the add/edit button', function () {
    const wrapper = getShallowComponent();
    const button = wrapper.find(NxButton);
    button.simulate('click');
    expect(setShowNoticesModalSpy).toHaveBeenCalledWith(true);
  });

  it('renders the given notice file links by hash', function () {
    const wrapper = getShallowComponent();
    let noticeFileLinks = wrapper.find('#legal-file-section-view-more-details').find(NxTextLink);

    let noticeFileLink = noticeFileLinks.at(0);
    expect(noticeFileLink).toHaveProp(
      'href',
      'legal.componentNoticeDetails.noticeDetails-{"hash":"testHash","noticeIndex":0}'
    );

    noticeFileLink = noticeFileLinks.at(1);
    expect(noticeFileLink).toHaveProp(
      'href',
      'legal.componentNoticeDetails.noticeDetails-{"hash":"testHash","noticeIndex":1}'
    );
  });

  it('renders the given notice file links by component identifier', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      hash: undefined,
      componentIdentifier: 'testComponentIdentifier',
    });
    let noticeFileLinks = wrapper.find('#legal-file-section-view-more-details').find(NxTextLink);

    let noticeFileLink = noticeFileLinks.at(0);
    expect(noticeFileLink).toHaveProp(
      'href',
      'legal.noticeFilesByComponentIdentifier.noticeDetails' +
        '-{"noticeIndex":0,"componentIdentifier":"testComponentIdentifier"}'
    );

    noticeFileLink = noticeFileLinks.at(1);
    expect(noticeFileLink).toHaveProp(
      'href',
      'legal.noticeFilesByComponentIdentifier.noticeDetails' +
        '-{"noticeIndex":1,"componentIdentifier":"testComponentIdentifier"}'
    );
  });
});
