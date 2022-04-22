/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../enzymeUtils';
import { mount } from 'enzyme/build';
import { NxWarningAlert } from '@sonatype/react-shared-components';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import ComponentOverviewTile from 'MainRoot/legal/ComponentOverviewTile';
import LicenseDetailsTile from 'MainRoot/legal/LicenseDetailsTile';
import CopyrightStatementsTile from 'MainRoot/legal/copyright/CopyrightStatementsTile';
import ComponentLegalOverviewPage from 'MainRoot/legal/ComponentLegalOverviewPage';
import LicenseObligationsTileContainer from 'MainRoot/legal/obligation/LicenseObligationsTileContainer';
import NoticeTextsTile from 'MainRoot/legal/files/notices/NoticeTextsTile';
import LicenseFilesTile from 'MainRoot/legal/files/licenses/LicenseFilesTile';
import OriginalSourcesTile from 'MainRoot/legal/originalSources/OriginalSourcesTile';

describe('ComponentLegalOverviewPage', function () {
  let minimalProps,
    loadComponentSpy,
    loadComponentByComponentIdentifierSpy,
    loadAvailableScopesSpy,
    getShallowComponent,
    spy$State;

  beforeEach(function () {
    loadComponentSpy = jasmine.createSpy('loadComponent');
    loadComponentByComponentIdentifierSpy = jasmine.createSpy('loadComponentByComponentIdentifier');
    loadAvailableScopesSpy = jasmine.createSpy('loadAvailableScopes');
    spy$State = jasmine.createSpyObj('$state', ['get', 'href']);
    spy$State.get.and.callFake((stateName) => stateName);
    spy$State.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });

    const licenseLegalMetadata = [
      {
        licenseId: 'license1',
        licenseName: 'license1',
        obligations: [
          {
            name: 'obligation 1',
            obligationTexts: ['text1', 'text2'],
          },
          {
            name: 'obligation 2',
            obligationTexts: ['text3', 'text4'],
          },
        ],
      },
      {
        licenseId: 'license2',
        licenseName: 'license2',
        obligations: [
          {
            name: 'obligation 2',
            obligationTexts: ['text5', 'text6'],
          },
          {
            name: 'obligation 3',
            obligationTexts: ['text7', 'text8'],
          },
        ],
      },
      {
        licenseId: 'multilicense',
        licenseName: 'multilicense',
        obligations: null,
      },
    ];

    const obligations = [
      {
        name: 'obligation 1',
        status: 'OPEN',
        comment: null,
      },
      {
        name: 'obligation 2',
        status: 'IGNORED',
        comment: 'comment',
      },
      {
        name: 'obligation 3',
        status: 'FULFILLED',
        comment: null,
      },
    ];

    const availableScopes = {
      values: [
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          label: 'Organization',
        },
      ],
    };

    const component = {
      licenseLegalData: {
        effectiveLicenses: [],
      },
    };

    minimalProps = {
      loadComponent: loadComponentSpy,
      loadComponentByComponentIdentifier: loadComponentByComponentIdentifierSpy,
      loadAvailableScopes: loadAvailableScopesSpy,
      $state: spy$State,
      licenseLegalMetadata,
      obligations,
      availableScopes,
      component,
      ecosystem: 'maven',
      hash: '1e48256a2341047e7d72',
      prevState: {
        name: 'prevState.test',
        url: 'prevstate/test',
      },
      prevParams: {
        testParam: 'testParam',
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentLegalOverviewPage, minimalProps);
  });

  it('loads the expected data using the root organization id', function () {
    const component = mount(<ComponentLegalOverviewPage {...minimalProps} loading={true} />);
    expect(loadComponentSpy).toHaveBeenCalledWith('organization', 'ROOT_ORGANIZATION_ID', '1e48256a2341047e7d72');
    expect(loadAvailableScopesSpy).toHaveBeenCalledWith('organization', 'ROOT_ORGANIZATION_ID');
    component.unmount();
  });

  it('loads the expected data using the organization id', function () {
    const component = mount(
      <ComponentLegalOverviewPage {...{ ...minimalProps, organizationId: 'orgId' }} loading={true} />
    );
    expect(loadComponentSpy).toHaveBeenCalledWith('organization', 'orgId', '1e48256a2341047e7d72');
    expect(loadAvailableScopesSpy).toHaveBeenCalledWith('organization', 'orgId');
    component.unmount();
  });

  it('loads the expected data using the application public id', function () {
    const component = mount(
      <ComponentLegalOverviewPage {...{ ...minimalProps, applicationPublicId: 'appId' }} loading={true} />
    );
    expect(loadComponentSpy).toHaveBeenCalledWith('application', 'appId', '1e48256a2341047e7d72');
    expect(loadAvailableScopesSpy).toHaveBeenCalledWith('application', 'appId');
    component.unmount();
  });

  it('does not load the data if there is no hash', function () {
    const component = getShallowComponent(
      <ComponentLegalOverviewPage loadComponent={loadComponentSpy} loading={true} $state={spy$State} />
    );
    expect(loadComponentSpy).not.toHaveBeenCalled();
    expect(loadAvailableScopesSpy).not.toHaveBeenCalled();
    component.unmount();
  });

  it('loads the data if there is a component identifier', function () {
    const component = mount(
      <ComponentLegalOverviewPage
        {...{ ...minimalProps, hash: undefined, componentIdentifier: 'componentIdentifier' }}
        loading={true}
      />
    );
    expect(loadComponentSpy).not.toHaveBeenCalled();
    expect(loadComponentByComponentIdentifierSpy).toHaveBeenCalled();
    expect(loadAvailableScopesSpy).toHaveBeenCalled();
    component.unmount();
  });

  it('loads the data by hash if there is both a hash and a component identifier', function () {
    const component = mount(
      <ComponentLegalOverviewPage {...{ ...minimalProps, componentIdentifier: 'componentIdentifier' }} loading={true} />
    );
    expect(loadComponentSpy).toHaveBeenCalledWith('organization', 'ROOT_ORGANIZATION_ID', '1e48256a2341047e7d72');
    expect(loadComponentByComponentIdentifierSpy).not.toHaveBeenCalled();
    expect(loadAvailableScopesSpy).toHaveBeenCalled();
    component.unmount();
  });

  it('renders a MenuBarBackButton to go to the app details page when using app public id and stage type id', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      applicationPublicId: 'appId',
      stageTypeId: 'stage',
    });

    const menuBarBackButton = wrapper.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp(
      'href',
      'legal.applicationDetails-{"applicationPublicId":"appId","stageTypeId":"stage"}'
    );
    expect(spy$State.href).toHaveBeenCalled();
  });

  it('renders a MenuBarBackButton to go to the real prev page when using app public id but no stage type id', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      applicationPublicId: 'appId',
    });

    const menuBarBackButton = wrapper.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', 'prevState.test-{"testParam":"testParam"}');
    expect(spy$State.href).toHaveBeenCalled();
  });

  it('renders a MenuBarBackButton to go to the real prev page when is neither using stage type id nor app public id', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      stageTypeId: 'stage',
    });
    const menuBarBackButton = wrapper.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', 'prevState.test-{"testParam":"testParam"}');
    expect(spy$State.href).toHaveBeenCalled();
  });

  it('renders a MenuBarBackButton to go to the real prev page when using stage type id but no app public id', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      stageTypeId: 'stage',
    });

    const menuBarBackButton = wrapper.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', 'prevState.test-{"testParam":"testParam"}');
    expect(spy$State.href).toHaveBeenCalled();
  });

  it('renders a MenuBarBackButton to go to the real prev page when not using stage type id and app public id', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      organizationId: 'orgId',
    });

    const menuBarBackButton = wrapper.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', 'prevState.test-{"testParam":"testParam"}');
    expect(spy$State.href).toHaveBeenCalled();
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders the ComponentOverviewTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(ComponentOverviewTile)).toExist();
  });

  it('renders the CopyrightStatementsTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(CopyrightStatementsTile)).toExist();
  });

  it('renders the OriginalSourcesTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(OriginalSourcesTile)).toExist();
  });

  it('renders the LicenseDetailsTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicenseDetailsTile)).toExist();
  });

  it('renders the LicenseObligationsTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicenseObligationsTileContainer)).toExist();
  });

  it('renders the LicenseTextsTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicenseFilesTile)).toExist();
  });

  it('renders the NoticeTextsTile', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NoticeTextsTile)).toExist();
  });

  it('renders a warning alert when the packages ecosystem is not supported', function () {
    const customMinimalProps = { ...minimalProps, ecosystem: 'NotSupportedEcosystem' };
    const component = enzymeUtils.getShallowComponent(ComponentLegalOverviewPage, customMinimalProps)();
    expect(component.find(NxWarningAlert)).toExist();
  });

  it('does not render a warning alert when the packages ecosystem is supported', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxWarningAlert)).not.toExist();
  });
});
