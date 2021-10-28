/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from 'TestRoot/enzymeUtils';
import LicenseFileDetailsHeader from 'MainRoot/legal/files/licenses/LicenseFilesDetailsHeader';
import LicenseFilesModalContainer from 'MainRoot/legal/files/licenses/LicenseFilesModalContainer';
import { licenseFilesState } from './licenseCommonState';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import * as legalUtilities from 'MainRoot/legal/legalUtility';

describe('LicenseDetailsHeader', function () {
  let getShallowComponent, setShowLicenseFilesModalMock;

  setShowLicenseFilesModalMock = jasmine.createSpy('setShowLicenseFilesModal').and.returnValue({ type: 'FOO' });

  const minimalProps = {
    ...licenseFilesState,
    $state: {
      get: () => '',
      href: () => '',
    },
    ownerType: 'testOwner',
    ownerId: 'testId',
    stageTypeId: 'testStage',
    hash: 'testHash',
    setShowLicenseFilesModal: setShowLicenseFilesModalMock,
  };
  getShallowComponent = enzymeUtils.getShallowComponent(LicenseFileDetailsHeader, minimalProps);

  it('renders a MenuBarBackButton with correct href prop', function () {
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
      minimalProps.hash
    );
  });

  it('renders the given license file header', function () {
    const wrapper = getShallowComponent();
    let buttonContainer = wrapper.find('#edit-license-files');
    expect(buttonContainer.length).toBe(1);
    const editButton = buttonContainer.dive();
    expect(editButton).toHaveClassName('nx-btn--tertiary');
    editButton.simulate('click');
    expect(setShowLicenseFilesModalMock).toHaveBeenCalledWith(true);
  });

  it('renders the given license files modal when true', function () {
    const wrapper = getShallowComponent({ showLicenseFilesModal: true });
    let modalContainer = wrapper.find(LicenseFilesModalContainer);
    expect(modalContainer).toExist();
  });

  it('does not render the given license files modal when false', function () {
    const wrapper = getShallowComponent({ showLicenseFilesModal: false });
    let modalContainer = wrapper.find(LicenseFilesModalContainer);
    expect(modalContainer).not.toExist();
  });
});
