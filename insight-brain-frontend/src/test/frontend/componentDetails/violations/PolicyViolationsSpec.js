/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxButton } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../enzymeUtils';
import PolicyViolations from '../../../../main/frontend/componentDetails/ViolationsTableTile/ViolationsTableTile';
import PolicyViolationsTable from '../../../../main/frontend/componentDetails/ViolationsTableTile/PolicyViolationsTable';
import PolicyViolationDetailsPopover from '../../../../main/frontend/componentDetails/ViolationsTableTile/PolicyViolationDetailsPopover';
import ComponentWaiversPopover from '../../../../main/frontend/componentDetails/ViolationsTableTile/componentWaivers/ComponentWaiversPopover';
import RequestWaiversPopover from '../../../../main/frontend/waivers/requestWaiversPopover/RequestWaiversPopover';
import AddWaiverPopover from '../../../../main/frontend/waivers/addWaiverPopover/AddWaiverPopoverContainer';

describe('PolicyViolations', () => {
  let minimalProps, getShallow, getMounted;

  beforeEach(function () {
    minimalProps = {
      waivers: ['exampleWaiver'],
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
      showViolationsDetailPopover: false,
      toggleShowViolationsDetailPopover: jasmine.createSpy('toggleShowViolationsDetailPopover'),
      showAddWaiverPopover: false,
      toggleAddWaiverPopover: jasmine.createSpy('toggleAddWaiverPopover'),
      showRequestWaiverPopover: false,
      toggleRequestWaiverPopover: jasmine.createSpy('toggleRequestWaiverPopover'),
      hasPermissionToAddWaivers: true,
      setSelectedPolicyViolationId: jasmine.createSpy('setSelectedPolicyViolationId'),
      selectedViolationDetail: { policyViolationId: 'selectedViolationId' },
      title: 'Title',
      showViewAllComponents: true,
      violationType: null,
      setViolationType: jasmine.createSpy('setViolationType'),
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolations, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolations, minimalProps);
  });

  describe('renders title', () => {
    it('renders the title correctly', () => {
      const title = getShallow().find('#violations__tile__title');

      expect(title).toHaveText('Title');
    });
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

  describe('setViolationType action', () => {
    it('calls setViolationType when the component renders', () => {
      getMounted({ violationType: 'test' });
      expect(minimalProps.setViolationType).toHaveBeenCalledWith('test');
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

  describe('showViewAllComponents prop', () => {
    it('Hides All Component Waivers button', () => {
      const component = getShallow({ showViewAllComponents: false });
      const button = component.find(NxButton);

      expect(button).not.toExist();
    });
  });

  describe('renders a PolicyViolationsTable', () => {
    it('renders an PolicyViolationsTable component passing the appropriate props', () => {
      let violationsTable;
      violationsTable = getShallow().find(PolicyViolationsTable);

      expect(violationsTable).toExist();
      expect(violationsTable).toHaveProp('violations', minimalProps.violations);
      expect(violationsTable).toHaveProp('waivers', minimalProps.waivers);
      expect(violationsTable).toHaveProp('loading', false);
      expect(violationsTable).toHaveProp('error', null);
      expect(violationsTable).toHaveProp('retryHandler', minimalProps.loadPolicyViolationsInformation);
      expect(violationsTable).toHaveProp(
        'toggleShowViolationsDetailPopover',
        minimalProps.toggleShowViolationsDetailPopover
      );
      expect(violationsTable).toHaveProp('toggleAddWaiverPopover', minimalProps.toggleAddWaiverPopover);
      expect(violationsTable).toHaveProp('toggleRequestWaiverPopover', minimalProps.toggleRequestWaiverPopover);
      expect(violationsTable).toHaveProp('setSelectedPolicyViolationId', minimalProps.setSelectedPolicyViolationId);
      expect(violationsTable).toHaveProp('hasPermissionToAddWaivers', true);

      violationsTable = getShallow({ loading: true }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('loading', true);

      violationsTable = getShallow({ loadError: 'some error' }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('error', 'some error');

      violationsTable = getShallow({ hasPermissionToAddWaivers: false }).find(PolicyViolationsTable);
      expect(violationsTable).toHaveProp('hasPermissionToAddWaivers', false);
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

      violationsDetail = getShallow({ showViolationsDetailPopover: true }).find(PolicyViolationDetailsPopover);
      expect(violationsDetail).toExist();
    });
  });

  describe('Add waiver popover', () => {
    it('renders the popover when showAddWaiverPopover is true', () => {
      let component = getShallow({ showAddWaiverPopover: false });
      let addWaiverPopover = component.find(AddWaiverPopover);
      expect(addWaiverPopover).not.toExist();

      component = getShallow({ showAddWaiverPopover: true });
      addWaiverPopover = component.find(AddWaiverPopover);

      expect(addWaiverPopover).toExist();
      expect(addWaiverPopover).toHaveProp('onClose', minimalProps.toggleAddWaiverPopover);
    });
  });

  describe('Request waiver popover', () => {
    it('renders the popover when showRequestWaiverPopover is true', () => {
      let component = getShallow({ showRequestWaiverPopover: false });
      let requestWaiverPopover = component.find(RequestWaiversPopover);
      expect(requestWaiverPopover).not.toExist();

      component = getShallow({ showRequestWaiverPopover: true });
      requestWaiverPopover = component.find(RequestWaiversPopover);

      expect(requestWaiverPopover).toExist();
      expect(requestWaiverPopover).toHaveProp('onClose', minimalProps.toggleRequestWaiverPopover);
    });
  });
});
