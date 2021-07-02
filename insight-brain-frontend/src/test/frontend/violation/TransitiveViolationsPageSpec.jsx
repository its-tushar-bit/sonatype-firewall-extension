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
import WaiveTransitiveViolationsPopoverContainer from '../../../main/frontend/violation/WaiveTransitiveViolationsPopoverContainer';
import RequestWaiveTransitiveViolationsPopoverContainer from '../../../main/frontend/violation/RequestWaiveTransitiveViolationsPopoverContainer';

describe('TransitiveViolationsPage', function () {
  let minimalProps,
    spy$State,
    spyLoadAvailableScopes,
    spyLoadReportMetadata,
    spyLoadTransitiveViolations,
    spySetSortingParameters,
    spySetFilteringParameters,
    spyToggleRequestWaiveTransitiveViolations,
    spyToggleWaiveTransitiveViolations,
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
    spyToggleRequestWaiveTransitiveViolations = jasmine.createSpy('spyToggleRequestWaiveTransitiveViolations');
    spyToggleWaiveTransitiveViolations = jasmine.createSpy('spyToggleWaiveTransitiveViolations');
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
      isRequestWaiveTransitiveViolationsOpen: false,
      isWaiveTransitiveViolationsOpen: false,
      loadAvailableScopes: spyLoadAvailableScopes,
      loadReportMetadata: spyLoadReportMetadata,
      loadTransitiveViolations: spyLoadTransitiveViolations,
      setSortingParameters: spySetSortingParameters,
      setFilteringParameters: spySetFilteringParameters,
      toggleRequestWaiveTransitiveViolations: spyToggleRequestWaiveTransitiveViolations,
      toggleWaiveTransitiveViolations: spyToggleWaiveTransitiveViolations,
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

  it('shows the component display name as the title', function () {
    const component = mount(<TransitiveViolationsPage {...minimalProps} />);
    const title = component.find('#transitive-violations-page-title');
    expect(title.at(0)).toHaveText('someDisplayName');
  });

  it('does not show the waive transitive violations popover when isWaiveTransitiveViolationsOpen is false', function () {
    const wrapper = getShallowComponent();
    const waiveTransitiveViolationsPopover = wrapper.find(WaiveTransitiveViolationsPopoverContainer);
    expect(waiveTransitiveViolationsPopover).not.toExist();
  });

  it('shows the waive transitive violations popover when isWaiveTransitiveViolationsOpen is true', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      isWaiveTransitiveViolationsOpen: true,
    });
    const waiveTransitiveViolationsPopover = wrapper.find(WaiveTransitiveViolationsPopoverContainer);
    expect(waiveTransitiveViolationsPopover).toExist();
  });

  it('calls toggleWaiveTransitiveViolations when clicking the waive transitive violations button', function () {
    const component = getShallowComponent();
    const waiveTransitiveViolationsButton = component.find('#transitive-violations-page-waive');
    waiveTransitiveViolationsButton.simulate('click');
    expect(spyToggleWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('disables the waive transitive violations button if there are no transitive violations', function () {
    const component = getShallowComponent();
    const waiveTransitiveViolationsButton = component.find('#transitive-violations-page-waive');
    expect(waiveTransitiveViolationsButton).toHaveProp('disabled', true);
  });

  it('enables the waive transitive violations button if there is at least 1 transitive violation', function () {
    const component = getShallowComponent({
      ...minimalProps,
      componentTransitivePolicyViolations: {
        ...minimalProps.componentTransitivePolicyViolations,
        data: {
          ...minimalProps.componentTransitivePolicyViolations.data,
          violations: [{}],
        },
      },
    });
    const waiveTransitiveViolationsButton = component.find('#transitive-violations-page-waive');
    expect(waiveTransitiveViolationsButton).toHaveProp('disabled', false);
  });

  it('does not show the request waive transitive violations popover when isRequestWaiveTransitiveViolationsOpen is false', function () {
    const wrapper = getShallowComponent();
    const requestWaiveTransitiveViolationsPopover = wrapper.find(RequestWaiveTransitiveViolationsPopoverContainer);
    expect(requestWaiveTransitiveViolationsPopover).not.toExist();
  });

  it('shows the request waive transitive violations popover when isRequestWaiveTransitiveViolationsOpen is true', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      isRequestWaiveTransitiveViolationsOpen: true,
    });
    const requestWaiveTransitiveViolationsPopover = wrapper.find(RequestWaiveTransitiveViolationsPopoverContainer);
    expect(requestWaiveTransitiveViolationsPopover).toExist();
  });

  it('calls toggleRequestWaiveTransitiveViolations when clicking the request waiver button', function () {
    const component = getShallowComponent();
    const requestWaiveTransitiveViolationsButton = component.find('#transitive-violations-page-request-waive');
    requestWaiveTransitiveViolationsButton.simulate('click');
    expect(spyToggleRequestWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('disables the request waive transitive violations button if there are no transitive violations', function () {
    const component = getShallowComponent();
    const requestWaiveTransitiveViolationsButton = component.find('#transitive-violations-page-request-waive');
    expect(requestWaiveTransitiveViolationsButton).toHaveProp('disabled', true);
  });

  it('enables the request waive transitive violations button if there is at least 1 transitive violation', function () {
    const component = getShallowComponent({
      ...minimalProps,
      componentTransitivePolicyViolations: {
        ...minimalProps.componentTransitivePolicyViolations,
        data: {
          ...minimalProps.componentTransitivePolicyViolations.data,
          violations: [{}],
        },
      },
    });
    const requestWaiveTransitiveViolationsButton = component.find('#transitive-violations-page-request-waive');
    expect(requestWaiveTransitiveViolationsButton).toHaveProp('disabled', false);
  });
});
