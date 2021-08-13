/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxButton } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../enzymeUtils';
import PolicyViolations from '../../../../main/frontend/componentDetails/violations/PolicyViolations';
import PolicyViolationsTable from '../../../../main/frontend/componentDetails/violations/PolicyViolationsTable';
import PolicyViolationDetailsPopover from '../../../../main/frontend/componentDetails/violations/PolicyViolationDetailsPopover';
import ComponentWaiversPopover from '../../../../main/frontend/componentDetails/violations/componentWaivers/ComponentWaiversPopover';

describe('PolicyViolations', () => {
  let minimalProps, getShallow, getMounted;

  beforeEach(function () {
    minimalProps = {
      waivers: [],
      violations: [
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
          actions: [],
          constraints: [],
        },
      ],
      componentName: 'componentName',
      loadPolicyViolationsInformation: jasmine.createSpy('loadPolicyViolationsInformation'),
      loadError: null,
      loading: false,
      goToWaivers: jasmine.createSpy('goToWaivers'),
      showComponentWaiversPopover: false,
      toggleComponentWaiversPopover: jasmine.createSpy('toggleComponentWaiversPopover'),
      setWaiverToDelete: jasmine.createSpy('setWaiverToDelete'),
      selectedViolationId: '',
      setShowViolationsDetail: jasmine.createSpy('setShowViolationsDetail'),
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolations, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolations, minimalProps);
  });

  describe('ComponentWaiversPopover', () => {
    it('is rendered if showComponentWaiversPopover is true', () => {
      let component = getShallow({ showComponentWaiversPopover: false });
      let popover = component.find(ComponentWaiversPopover);
      expect(popover).not.toExist();

      component = getShallow({ showComponentWaiversPopover: true });
      popover = component.find(ComponentWaiversPopover);
      expect(popover).toExist();
      expect(popover).toHaveProp('componentName', minimalProps.componentName);
      expect(popover).toHaveProp('toggleComponentWaiversPopover', minimalProps.toggleComponentWaiversPopover);
      expect(popover).toHaveProp('waivers', minimalProps.waivers);
      expect(popover).toHaveProp('setWaiverToDelete', minimalProps.setWaiverToDelete);
      expect(popover).toHaveProp('waiverToDelete', minimalProps.waiverToDelete);
    });
  });

  describe('loadPolicyViolationsInformation action', () => {
    it('calls loadPolicyViolationsInformation when the component renders', () => {
      getMounted();
      expect(minimalProps.loadPolicyViolationsInformation).toHaveBeenCalled();
    });
  });

  describe('View All Component Waivers button', () => {
    it('is rendered', () => {
      const component = getShallow();
      const button = component.find(NxButton);

      expect(button).toHaveProp('id', 'component-details-view-waivers');
      expect(button).toHaveProp('variant', 'tertiary');
      expect(button).toHaveProp('onClick', minimalProps.toggleComponentWaiversPopover);
      expect(button).toHaveText('View All Component Waivers');
    });

    it('calls `toggleComponentWaiversPopover` when clicked', () => {
      const component = getShallow();
      const button = component.find(NxButton);

      button.simulate('click');
      expect(minimalProps.toggleComponentWaiversPopover).toHaveBeenCalled();
    });
  });

  describe('renders a PolicyViolationsTable', () => {
    it('renders an PolicyViolationsTable component passing the appropriate props', () => {
      let violationsTable;
      violationsTable = getShallow().find(PolicyViolationsTable);

      expect(violationsTable).toExist();
      expect(violationsTable).toHaveProp('violations', minimalProps.violations);
      expect(violationsTable).toHaveProp('loading', false);
      expect(violationsTable).toHaveProp('error', null);
      expect(violationsTable).toHaveProp('retryHandler', minimalProps.loadPolicyViolationsInformation);

      violationsTable = getShallow({ loading: true }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('loading', true);

      violationsTable = getShallow({ loadError: 'some error' }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('error', 'some error');
    });

    it('orders the violations by policyThreatLevel to pass them to the table', () => {
      const originViolations = [
        {
          policyViolationId: 'policyViolationId3',
          policyThreatLevel: 3,
          policyName: 'Security-Low',
        },
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
        },
        {
          policyViolationId: 'policyViolationId7',
          policyThreatLevel: 7,
          policyName: 'Security-Critical',
        },
      ];

      const expectedViolationsInOrder = [
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
        },
        {
          policyViolationId: 'policyViolationId7',
          policyThreatLevel: 7,
          policyName: 'Security-Critical',
        },
        {
          policyViolationId: 'policyViolationId3',
          policyThreatLevel: 3,
          policyName: 'Security-Low',
        },
      ];

      const violationsTable = getShallow({ violations: originViolations }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('violations', expectedViolationsInOrder);
    });
  });

  describe('renders a PolicyViolationDetailsPopover', () => {
    it('renders a PolicyViolationDetailsPopover component when the flag is active', () => {
      let violationsDetail;
      violationsDetail = getShallow().find(PolicyViolationDetailsPopover);
      expect(violationsDetail).not.toExist();

      violationsDetail = getShallow({ selectedViolationId: 'ViolationId' }).find(PolicyViolationDetailsPopover);
      expect(violationsDetail).toExist();
    });
  });
});
