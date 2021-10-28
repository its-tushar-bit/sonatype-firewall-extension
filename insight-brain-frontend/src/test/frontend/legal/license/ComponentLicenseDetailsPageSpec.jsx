/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import React from 'react';
import ComponentLicenseDetailsPage from 'MainRoot/legal/license/ComponentLicenseDetailsPage';
import ComponentLicenseOverviewTile from 'MainRoot/legal/license/ComponentLicenseOverviewTile';
import LicenseFullDetailsTile from 'MainRoot/legal/license/LicenseFullDetailsTile';
import LicenseList from 'MainRoot/legal/license/LicenseList';
import { mount } from 'enzyme/build';
import { licenseState } from './licenseCommonState';
import LicensesModalContainer from 'MainRoot/legal/license/LicensesModalContainer';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import * as legalUtilities from 'MainRoot/legal/legalUtility';

describe('ComponentLicenseDetailsPage', function () {
  let minimalProps, loadComponentAndLicenseDetailsSpy, setShowLicensesModalSpy, getShallowComponent, $state;

  beforeEach(function () {
    loadComponentAndLicenseDetailsSpy = jasmine.createSpy('loadComponentAndLicenseDetails');
    setShowLicensesModalSpy = jasmine.createSpy('setShowLicensesModal');
    $state = jasmine.createSpyObj('$state', ['get', 'href']);
    $state.get.and.callFake((stateName) => stateName);
    $state.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });

    const availableScopes = {
      values: [
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          label: 'Organization',
        },
      ],
    };

    minimalProps = {
      ...licenseState,
      availableScopes,
      setShowLicensesModal: setShowLicensesModalSpy,
      $state,
      licenseIndex: 1,
      loadComponentAndLicenseDetails: loadComponentAndLicenseDetailsSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentLicenseDetailsPage, minimalProps);
  });

  it('loads the expected data', function () {
    const component = mount(<ComponentLicenseDetailsPage {...minimalProps} loading={true} />);
    expect(loadComponentAndLicenseDetailsSpy).toHaveBeenCalledWith(
      'organization',
      'org',
      'fooHash',
      1,
      'fooComponentIdentifier'
    );
    component.unmount();
  });

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
      minimalProps.hash,
      'fooComponentIdentifier'
    );
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders the ComponentLicenseOverviewTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(ComponentLicenseOverviewTile)).toExist();
  });

  it('renders the LicenseList', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicenseList)).toExist();
  });

  it('renders the LicenseFullDetailsTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicenseFullDetailsTile)).toExist();
  });

  it('renders a LicensesModalContainer if showLicensesModal is true', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicensesModalContainer)).not.toExist();
  });

  it('does not render a LicensesModalContainer if showLicensesModal is false', function () {
    const props = {
      component: {
        ...licenseState.component,
        licenseLegalData: {
          showLicensesModal: true,
          ...licenseState.component.licenseLegalData,
        },
      },
    };
    const wrapper = getShallowComponent(props);
    expect(wrapper.find(LicensesModalContainer)).toExist();
  });

  it('calls setShowLicensesModal', function () {
    const wrapper = getShallowComponent();
    const editButton = wrapper.find('#edit-licenses');
    expect(setShowLicensesModalSpy).not.toHaveBeenCalled();
    editButton.simulate('click');
    expect(setShowLicensesModalSpy).toHaveBeenCalledWith(true);
  });
});
