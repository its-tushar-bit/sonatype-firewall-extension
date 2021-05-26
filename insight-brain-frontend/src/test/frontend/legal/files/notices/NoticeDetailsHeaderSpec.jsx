/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import NoticeDetailsHeader from '../../../../../main/frontend/legal/files/notices/NoticeDetailsHeader';
import NoticesModalContainer from '../../../../../main/frontend/legal/files/notices/NoticesModalContainer';

describe('NoticeDetailsHeader component', function () {
  let getShallowComponent, setShowNoticesModalMock;

  setShowNoticesModalMock = jasmine.createSpy('setShowNoticesModal').and.returnValue({ type: 'FOO' });

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
    setShowNoticesModal: setShowNoticesModalMock,
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(NoticeDetailsHeader, minimalProps);
  });

  it('renders the given notice header', function () {
    const wrapper = getShallowComponent();
    let buttonContainer = wrapper.find('#edit-notices');
    expect(buttonContainer.length).toBe(1);
    const editButton = buttonContainer.dive();
    expect(editButton).toHaveClassName('nx-btn--tertiary');
    editButton.simulate('click');
    expect(setShowNoticesModalMock).toHaveBeenCalledWith(true);
  });

  it('renders the given notice modal when true', function () {
    const wrapper = getShallowComponent({ showNoticesModal: true });
    let modalContainer = wrapper.find(NoticesModalContainer);
    expect(modalContainer).toExist();
  });

  it('does not render the given notice modal when false', function () {
    const wrapper = getShallowComponent({ showNoticesModal: false });
    let modalContainer = wrapper.find(NoticesModalContainer);
    expect(modalContainer).not.toExist();
  });
});
