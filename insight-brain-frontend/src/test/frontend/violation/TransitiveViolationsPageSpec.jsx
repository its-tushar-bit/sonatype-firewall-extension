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
    spyLoadReportMetadata,
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
    spyLoadReportMetadata = jasmine.createSpy('spyLoadReportMetadata');
    spyLoadTransitiveViolations = jasmine.createSpy('spyLoadTransitiveViolations');
    spySetSortingParameters = jasmine.createSpy('spySetSortingParameters');
    spySetFilteringParameters = jasmine.createSpy('spySetFilteringParameters');
    minimalProps = {
      ownerType: 'someOwnerType',
      ownerId: 'someOwnerId',
      scanId: 'someScanId',
      hash: 'someHash',
      $state: spy$State,
      availableScopes: {
        loading: false,
        error: null,
        data: [
          { id: 'appId', name: 'app' },
          { id: 'orgId', name: 'org' },
          { id: 'ROOT_ORGANIZATION_ID', name: 'Root Organization' },
        ],
      },
      reportMetadata: {
        loading: false,
        error: null,
        data: {
          reportTime: 1622466767823,
          reportTitle: 'Build Report',
          stageId: 'Build',
        },
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
        data: {
          displayName: 'someDisplayName',
          isInnerSource: false,
          violations: [],
          displayedViolations: [],
        },
      },
      loadAvailableScopes: spyLoadAvailableScopes,
      loadReportMetadata: spyLoadReportMetadata,
      loadTransitiveViolations: spyLoadTransitiveViolations,
      setSortingParameters: spySetSortingParameters,
      setFilteringParameters: spySetFilteringParameters,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(TransitiveViolationsPage, minimalProps);
  });

  it(
    'loads the available scopes, report metadata, and transitive policy violations using the given ' + 'properties',
    function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} />);
      expect(spyLoadAvailableScopes).toHaveBeenCalledWith('someOwnerType', 'someOwnerId');
      expect(spyLoadReportMetadata).toHaveBeenCalledWith('someOwnerId', 'someScanId');
      expect(spyLoadTransitiveViolations).toHaveBeenCalledWith(
        'someOwnerType',
        'someOwnerId',
        'someScanId',
        'someHash'
      );
      component.unmount();
    }
  );

  describe('does not load the available scopes, report metadata, and transitive policy violations if', function () {
    it('has no ownerType', function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} ownerType={undefined} />);
      expect(spyLoadAvailableScopes).not.toHaveBeenCalled();
      expect(spyLoadReportMetadata).not.toHaveBeenCalled();
      expect(spyLoadTransitiveViolations).not.toHaveBeenCalled();
      component.unmount();
    });

    it('has no ownerId', function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} ownerId={undefined} />);
      expect(spyLoadAvailableScopes).not.toHaveBeenCalled();
      expect(spyLoadReportMetadata).not.toHaveBeenCalled();
      expect(spyLoadTransitiveViolations).not.toHaveBeenCalled();
      component.unmount();
    });

    it('has no scanId', function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} scanId={undefined} />);
      expect(spyLoadAvailableScopes).not.toHaveBeenCalled();
      expect(spyLoadReportMetadata).not.toHaveBeenCalled();
      expect(spyLoadTransitiveViolations).not.toHaveBeenCalled();
      component.unmount();
    });

    it('has no hash', function () {
      const component = mount(<TransitiveViolationsPage {...minimalProps} hash={undefined} />);
      expect(spyLoadAvailableScopes).not.toHaveBeenCalled();
      expect(spyLoadReportMetadata).not.toHaveBeenCalled();
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

  it('is loading if report metadata is loading', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      reportMetadata: { ...minimalProps.reportMetadata, loading: true },
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

  it(
    'is not loading if available scopes, report metadata, and component transitive policy violations are not ' +
      'loading',
    function () {
      const wrapper = getShallowComponent();
      expect(wrapper.find(LoadWrapper)).toHaveProp('loading', false);
    }
  );

  it('has an error if available scopes has an error', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      availableScopes: { ...minimalProps.availableScopes, error: 'someError' },
    });
    expect(wrapper.find(LoadWrapper)).toHaveProp('error', 'someError');
  });

  it('has an error if report metadata has an error', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      reportMetadata: { ...minimalProps.reportMetadata, error: 'someError' },
    });
    expect(wrapper.find(LoadWrapper)).toHaveProp('error', 'someError');
  });

  it('has an error if component transitive policy violations has an error', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      componentTransitivePolicyViolations: { ...minimalProps.componentTransitivePolicyViolations, error: 'someError' },
    });
    expect(wrapper.find(LoadWrapper)).toHaveProp('error', 'someError');
  });

  it(
    'has no error if available scopes, report metadata, and component transitive policy violations have no ' + 'errors',
    function () {
      const wrapper = getShallowComponent();
      expect(wrapper.find(LoadWrapper)).toHaveProp('error', null);
    }
  );

  describe('the back button', function () {
    it('links to the app report if ownerId with scanId is requested', function () {
      const wrapper = getShallowComponent();
      const backButton = wrapper.find(NxBackButton);
      expect(backButton).toExist();
      expect(backButton).toHaveProp(
        'href',
        'applicationReport.policy-{"publicId":"someOwnerId","scanId":"someScanId"}'
      );
      expect(spy$State.href).toHaveBeenCalled();
    });
  });

  describe('the InnerSourceTag', function () {
    it('is shown if the queried component is InnerSource', function () {
      const component = mount(
        <TransitiveViolationsPage
          {...{
            ...minimalProps,
            componentTransitivePolicyViolations: {
              ...minimalProps.componentTransitivePolicyViolations,
              data: {
                ...minimalProps.componentTransitivePolicyViolations.data,
                isInnerSource: true,
              },
            },
          }}
        />
      );
      const tags = component.find('.nx-tag');
      console.log(tags);
      expect(tags.length).toBe(1);
      expect(tags.childAt(0)).toHaveText('InnerSource');
    });

    it('is not shown if the queried component is not InnerSource', function () {
      const wrapper = getShallowComponent();
      const tags = wrapper.find('.nx-tag');
      expect(tags.length).toBe(0);
    });
  });
});
