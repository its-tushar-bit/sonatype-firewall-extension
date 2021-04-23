/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import React from 'react';
import { NxBackButton } from '@sonatype/react-shared-components';
import ComponentLicenseDetailsPage from '../../../../main/frontend/legal/license/ComponentLicenseDetailsPage';
import ComponentLicenseOverviewTile from '../../../../main/frontend/legal/license/ComponentLicenseOverviewTile';
import LicenseFullDetailsTile from '../../../../main/frontend/legal/license/LicenseFullDetailsTile';
import LicenseList from '../../../../main/frontend/legal/license/LicenseList';
import { mount } from 'enzyme/build';
import { licenseState } from './licenseCommonState';

describe('ComponentLicenseDetailsPage', function () {
  let minimalProps, loadComponentAndLicenseDetailsSpy, getShallowComponent, $state;

  beforeEach(function () {
    loadComponentAndLicenseDetailsSpy = jasmine.createSpy('loadComponentAndLicenseDetails');
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
      $state,
      licenseIndex: 1,
      loadComponentAndLicenseDetails: loadComponentAndLicenseDetailsSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentLicenseDetailsPage, minimalProps);
  });

  it('loads the expected data', function () {
    const component = mount(<ComponentLicenseDetailsPage {...minimalProps} loading={true} />);
    expect(loadComponentAndLicenseDetailsSpy).toHaveBeenCalledWith('organization', 'org', 'fooHash', 1);
    component.unmount();
  });

  it('renders a NxBackButton to go to the component overview  page', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      applicationPublicId: 'appId',
      stageTypeId: 'stage',
    });

    const backButton = wrapper.find(NxBackButton);
    expect(backButton).toExist();
    expect(backButton).toHaveProp(
      'href',
      'legal.organizationComponentOverview-{"organizationId":"org","hash":"fooHash"}'
    );
    expect($state.href).toHaveBeenCalled();
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
});
