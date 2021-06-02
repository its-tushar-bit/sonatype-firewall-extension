/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../enzymeUtils';
import TransitiveViolationsPage from '../../../main/frontend/violation/TransitiveViolationsPage';
import { mount } from 'enzyme';
import React from 'react';
import { NxBackButton } from '@sonatype/react-shared-components';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';

describe('TransitiveViolationsPage', function () {
  let minimalProps,
    spy$State,
    spyLoadAvailableScopes,
    spyLoadTransitiveViolations,
    spySetSortingParameters,
    spySetFilteringParameters,
    getShallowComponent;

  beforeEach(function () {
    spy$State = jasmine.createSpyObj('$state', ['get', 'href']);
    spy$State.get.and.callFake((stateName) => stateName);
    spy$State.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });
    spyLoadAvailableScopes = jasmine.createSpy('spyLoadAvailableScopes');
    spyLoadTransitiveViolations = jasmine.createSpy('spyLoadTransitiveViolations');
    spySetSortingParameters = jasmine.createSpy('spySetSortingParameters');
    spySetFilteringParameters = jasmine.createSpy('spySetFilteringParameters');
    minimalProps = {
      ownerType: 'someOwnerType',
      ownerId: 'someOwnerId',
      stageTypeId: 'someStageTypeId',
      hash: 'someHash',
      scanId: 'someScanId',
      $state: spy$State,
      availableScopes: {
        loading: false,
        error: null,
        values: [
          {
            id: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
            label: 'Organization',
          },
        ],
      },
      componentTransitivePolicyViolations: {
        loading: false,
        error: null,
        sortConfiguration: {
          key: 'threatLevel',
          dir: 'desc',
        },
        filterConfiguration: {
          policyName: '',
          displayName: '',
        },
        displayName: 'someDisplayName',
        isInnerSource: false,
        violations: [],
      },
      loadAvailableScopes: spyLoadAvailableScopes,
      loadTransitiveViolations: spyLoadTransitiveViolations,
      setSortingParameters: spySetSortingParameters,
      setFilteringParameters: spySetFilteringParameters,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(TransitiveViolationsPage, minimalProps);
  });

  it('loads the available scopes and transitive policy violations using the given properties', function () {
    const component = mount(<TransitiveViolationsPage {...minimalProps} />);
    expect(spyLoadAvailableScopes).toHaveBeenCalledWith('someOwnerType', 'someOwnerId');
    expect(spyLoadTransitiveViolations).toHaveBeenCalledWith(
      'someOwnerType',
      'someOwnerId',
      'someStageTypeId',
      'someHash'
    );
    component.unmount();
  });

  describe('does not load the available scopes and transitive policy violations if', function () {
    it('has no ownerType', function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} ownerType={undefined} />);
      expect(spyLoadAvailableScopes).not.toHaveBeenCalled();
      expect(spyLoadTransitiveViolations).not.toHaveBeenCalled();
      component.unmount();
    });

    it('has no ownerId', function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} ownerId={undefined} />);
      expect(spyLoadAvailableScopes).not.toHaveBeenCalled();
      expect(spyLoadTransitiveViolations).not.toHaveBeenCalled();
      component.unmount();
    });

    it('has no stageTypeId', function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} stageTypeId={undefined} />);
      expect(spyLoadAvailableScopes).not.toHaveBeenCalled();
      expect(spyLoadTransitiveViolations).not.toHaveBeenCalled();
      component.unmount();
    });

    it('has no hash', function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} hash={undefined} />);
      expect(spyLoadAvailableScopes).not.toHaveBeenCalled();
      expect(spyLoadTransitiveViolations).not.toHaveBeenCalled();
      component.unmount();
    });
  });

  it('is loading if available scopes is loading', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      availableScopes: { ...minimalProps.availableScopes, loading: true },
    });
    expect(wrapper.find(LoadWrapper)).toHaveProp('loading', true);
  });

  it('is loading if component transitive policy violations is loading', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      componentTransitivePolicyViolations: { ...minimalProps.componentTransitivePolicyViolations, loading: true },
    });
    expect(wrapper.find(LoadWrapper)).toHaveProp('loading', true);
  });

  it('is not loading if available scopes and component transitive policy violations are not loading', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LoadWrapper)).toHaveProp('loading', false);
  });

  it('has an error if available scopes has an error', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      availableScopes: { ...minimalProps.availableScopes, error: 'someError' },
    });
    expect(wrapper.find(LoadWrapper)).toHaveProp('error', 'someError');
  });

  it('has an error if component transitive policy violations is loading', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      componentTransitivePolicyViolations: { ...minimalProps.componentTransitivePolicyViolations, error: 'someError' },
    });
    expect(wrapper.find(LoadWrapper)).toHaveProp('error', 'someError');
  });

  it('has no error if available scopes and component transitive policy violations have no errors', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LoadWrapper)).toHaveProp('error', null);
  });

  describe('the back button', function () {
    it('links to the app report if app with scan is requested', function () {
      const wrapper = getShallowComponent({ ...minimalProps, ownerType: 'application' });
      const backButton = wrapper.find(NxBackButton);
      expect(backButton).toExist();
      expect(backButton).toHaveProp(
        'href',
        'applicationReport.policy-{"publicId":"someOwnerId","scanId":"someScanId"}'
      );
      expect(spy$State.href).toHaveBeenCalled();
    });

    it('links to the latest app report if app without scan is requested', function () {
      const wrapper = getShallowComponent({ ...minimalProps, ownerType: 'application', scanId: undefined });
      const backButton = wrapper.find(NxBackButton);
      expect(backButton).toExist();
      expect(backButton).toHaveProp('href', '/ui/links/application/someOwnerId/latestReport/someStageTypeId');
    });

    it('links to the specific component if a non app is requested with hash', function () {
      const wrapper = getShallowComponent();
      const backButton = wrapper.find(NxBackButton);
      expect(backButton).toExist();
      expect(backButton).toHaveProp('href', 'dashboard.component-{"hash":"someHash"}');
    });

    it('links to the components if a non app is requested without hash', function () {
      const wrapper = getShallowComponent({ hash: undefined });
      const backButton = wrapper.find(NxBackButton);
      expect(backButton).toExist();
      expect(backButton).toHaveProp('href', 'dashboard.component');
    });
  });

  describe('the InnerSourceTag', function () {
    it('is shown if the queried component is InnerSource', function () {
      const wrapper = getShallowComponent({
        ...minimalProps,
        componentTransitivePolicyViolations: {
          ...minimalProps.componentTransitivePolicyViolations,
          isInnerSource: true,
        },
      });
      const innerSourceTag = wrapper.find('#iq-transitive-violations-page-is-inner-source');
      expect(innerSourceTag).toExist();
      expect(innerSourceTag.childAt(0)).toHaveText('InnerSource');
    });

    it('is not shown if the queried component is not InnerSource', function () {
      const wrapper = getShallowComponent();
      const innerSourceTag = wrapper.find('#iq-transitive-violations-page-is-inner-source');
      expect(innerSourceTag).not.toExist();
    });
  });
});
