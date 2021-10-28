/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from 'TestRoot/enzymeUtils';
import CopyrightDetailsHeader from 'MainRoot/legal/copyright/CopyrightDetailsHeader';
import { copyrightCommonState } from './copyrightCommonState';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import * as legalUtilities from 'MainRoot/legal/legalUtility';

describe('CopyrightDetailsHeader', function () {
  let getShallowComponent, loadComponentAndCopyrightDetailsMock, setDisplayCopyrightOverrideModalMock;

  const minimalProps = {
    ...copyrightCommonState,
    $state: {
      get: () => '',
      href: () => '',
    },
    ownerType: 'testOwner',
    ownerId: 'testId',
    stageTypeId: 'testStage',
    hash: 'testHash',
    loadComponentAndCopyrightDetails: loadComponentAndCopyrightDetailsMock,
    setDisplayCopyrightOverrideModal: setDisplayCopyrightOverrideModalMock,
  };
  getShallowComponent = enzymeUtils.getShallowComponent(CopyrightDetailsHeader, minimalProps);

  beforeEach(() => {
    loadComponentAndCopyrightDetailsMock = jasmine
      .createSpy('loadComponentAndCopyrightDetails')
      .and.returnValue({ type: 'FOO' });
    setDisplayCopyrightOverrideModalMock = jasmine
      .createSpy('setDisplayCopyrightOverrideModal')
      .and.returnValue({ type: 'FOO' });
    spyOn(legalUtilities, 'backToComponentOverviewUrl').and.returnValue('some-href');
  });

  it('renders a MenuBarBackButton with correct href prop', function () {
    const wrapper = getShallowComponent();
    const menuBarBackButton = wrapper.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', 'some-href');
    expect(legalUtilities.backToComponentOverviewUrl).toHaveBeenCalledWith(
      minimalProps.$state,
      minimalProps.ownerType,
      minimalProps.ownerId,
      minimalProps.stageTypeId,
      minimalProps.hash
    );
  });
});
