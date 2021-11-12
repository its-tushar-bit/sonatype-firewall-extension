/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from 'TestRoot/enzymeUtils';
import NoticeDetailsHeader from 'MainRoot/legal/files/notices/NoticeDetailsHeader';
import NoticesModalContainer from 'MainRoot/legal/files/notices/NoticesModalContainer';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import * as legalUtilities from 'MainRoot/legal/legalUtility';

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
    ownerType: 'testOwner',
    ownerId: 'testId',
    stageTypeId: 'testStage',
    hash: 'testHash',
    setShowNoticesModal: setShowNoticesModalMock,
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(NoticeDetailsHeader, minimalProps);
  });

  it('renders a MenuBarBackButton with component hash url', function () {
    spyOn(legalUtilities, 'backToComponentOverviewUrl').and.returnValue('some-href');
    const wrapper = getShallowComponent();
    const menuBarBackButton = wrapper.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', 'some-href');
    expect(legalUtilities.backToComponentOverviewUrl).toHaveBeenCalledWith(
      minimalProps.$state,
      minimalProps.ownerType,
      minimalProps.ownerId,
      minimalProps.stageTypeId,
      minimalProps.hash,
      undefined
    );
  });

  it('renders a MenuBarBackButton with component identifier url', function () {
    const newMinimalProps = { ...minimalProps, componentIdentifier: '{dummyComponentIdentifier: "identifier"}' };
    getShallowComponent = enzymeUtils.getShallowComponent(NoticeDetailsHeader, newMinimalProps);
    spyOn(legalUtilities, 'backToComponentOverviewUrl').and.returnValue('some-href');
    const wrapper = getShallowComponent();
    const menuBarBackButton = wrapper.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', 'some-href');
    expect(legalUtilities.backToComponentOverviewUrl).toHaveBeenCalledWith(
      minimalProps.$state,
      minimalProps.ownerType,
      minimalProps.ownerId,
      minimalProps.stageTypeId,
      minimalProps.hash,
      newMinimalProps.componentIdentifier
    );
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
