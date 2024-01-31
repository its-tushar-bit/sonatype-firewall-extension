/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';

import PolicyViolationsTable from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsTable';
import PolicyViolationsTableRow from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsTableRow';
import ComponentWaiversPopover from 'MainRoot/componentDetails/ViolationsTableTile/componentWaivers/ComponentWaiversPopover';

describe('PolicyViolationsTable', () => {
  let minimalProps, getShallow, getMounted;

  beforeEach(function () {
    minimalProps = {
      violations: [
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
          actions: [],
          constraints: [],
        },
      ],
      loading: false,
      error: null,
      loadPolicyViolationsInformation: jasmine.createSpy('loadPolicyViolationsInformation'),
      toggleShowViolationsDetailPopover: jasmine.createSpy('toggleShowViolationsDetailPopover'),
      resetViolationDetails: jasmine.createSpy('resetViolationDetails'),
      setSelectedPolicyViolationId: () => {},
      showViolationsDetailPopover: false,
      showComponentWaiversPopover: false,
      componentName: 'componentName',
      waivers: [],
      toggleComponentWaiversPopover: () => {},
      waiverToDelete: null,
      setWaiverToDelete: () => {},
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolationsTable, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolationsTable, minimalProps);
  });

  it('calls loadPolicyViolationsInformation when the component mounts', () => {
    const component = getMounted();
    expect(minimalProps.loadPolicyViolationsInformation).toHaveBeenCalled();
    component.unmount();
  });

  it('renders an NxTable with headers', () => {
    const component = getShallow(),
      table = component.find(NxTable),
      tHeader = table.find(NxTableHead),
      headerRow = tHeader.find(NxTableRow),
      headers = headerRow.find(NxTableCell);

    expect(headers.length).toEqual(6);
    expect(headers.at(0)).toHaveProp('children', 'Threat');
    expect(headers.at(1)).toHaveProp('children', 'Policy/Action');
    expect(headers.at(2)).toHaveProp('children', 'Constraint Name');
    expect(headers.at(3)).toHaveProp('children', 'Condition');
    expect(headers.at(4)).not.toHaveProp('children');
    expect(headers.at(5)).not.toHaveProp('children');
  });

  describe('Table body', () => {
    it('displays an empty message when there are no violations to show', () => {
      const component = getMounted({ violations: [] });
      const body = component.find(NxTableBody);
      const tRow = body.find(NxTableRow);
      const tCell = tRow.find(NxTableCell);
      expect(tCell).toHaveText('No policy violations');
    });

    it('sets isLoading in the table body with the received loading flag', () => {
      let component = getShallow({ loading: true });
      let body = component.find(NxTableBody);
      expect(body).toHaveProp('isLoading', true);

      component = getShallow({ loading: false });
      body = component.find(NxTableBody);
      expect(body).toHaveProp('isLoading', false);
    });

    it('sets the error prop on the table body with the received error prop', () => {
      let component = getShallow({ error: 'some err' });
      let body = component.find(NxTableBody);
      expect(body).toHaveProp('error', 'some err');

      component = getShallow();
      body = component.find(NxTableBody).dive();
      expect(body).not.toHaveProp('error');
    });

    it('sets the retryHandler prop on the table body with the received retryHandler prop', () => {
      const component = getShallow(),
        body = component.find(NxTableBody);

      expect(body).toHaveProp('retryHandler', minimalProps.loadPolicyViolationsInformation);
    });

    it('creates a PolicyViolationsTableRow per sorted violation', () => {
      const multipleViolations = [
        {
          policyViolationId: 'policyViolationId3',
          policyThreatLevel: 3,
          policyName: 'Security-Low',
          actions: [],
          constraints: [],
          waived: false,
        },
        {
          policyViolationId: 'waivedPolicyViolationId3',
          policyThreatLevel: 3,
          policyName: 'Security-Low',
          actions: [],
          constraints: [],
          waived: true,
        },
        {
          policyViolationId: 'waivedPolicyViolationId10',
          policyThreatLevel: 10,
          policyName: 'Security-Low',
          actions: [],
          constraints: [],
          waived: true,
        },
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
          actions: [],
          constraints: [],
          waived: false,
        },
        {
          policyViolationId: 'policyViolationId7',
          policyThreatLevel: 7,
          policyName: 'Security-Critical',
          actions: [],
          constraints: [],
          waived: false,
        },
      ];
      const component = getShallow({ violations: multipleViolations });
      const table = component.find(NxTable);
      const tBody = table.find(NxTableBody);
      const rows = tBody.find(PolicyViolationsTableRow);

      expect(rows.length).toEqual(5);
      // violations are sorted by threat level
      expect(rows.at(0).key()).toBe('policyViolationId');
      expect(rows.at(1).key()).toBe('policyViolationId7');
      expect(rows.at(2).key()).toBe('policyViolationId3');
      expect(rows.at(3).key()).toBe('waivedPolicyViolationId10');
      expect(rows.at(4).key()).toBe('waivedPolicyViolationId3');
    });
  });

  describe('ComponentWaiversPopover', () => {
    it('is not rendered if policy violations information is loading', () => {
      const component = getShallow({ loading: true });
      const popover = component.find(ComponentWaiversPopover);

      expect(popover).not.toExist();
    });

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

  describe('PolicyViolationDetailsPopover', () => {
    it('setShowViolationsDetailPopover is called on row click', () => {
      const setViolationsDetailRowClicked = jasmine.createSpy('setViolationsDetailRowClicked');
      const toggleShowViolationsDetailPopover = jasmine.createSpy('toggleShowViolationsDetailPopover');
      const violation = {
        policyViolationId: 'policyViolationId3',
        policyThreatLevel: 3,
        policyName: 'Security-Low',
        actions: [],
        constraints: [],
        waived: false,
      };
      const component = getMounted({
        violations: [violation],
        toggleShowViolationsDetailPopover,
        setViolationsDetailRowClicked,
      });
      const table = component.find(NxTable);
      const tBody = table.find(NxTableBody);
      const rows = tBody.find(PolicyViolationsTableRow);

      rows.at(0).simulate('click');

      expect(setViolationsDetailRowClicked).toHaveBeenCalledTimes(1);
      expect(toggleShowViolationsDetailPopover).toHaveBeenCalledTimes(1);
    });
  });
});
