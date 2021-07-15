/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';

import PolicyViolationsTable from '../../../../main/frontend/componentDetails/violations/PolicyViolationsTable';
import PolicyViolationsTableRow from '../../../../main/frontend/componentDetails/violations/PolicyViolationsTableRow';

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
      retryHandler: jasmine.createSpy('loadPolicyViolationsInformation'),
    };

    getShallow = enzymeUtils.getShallowComponent(PolicyViolationsTable, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(PolicyViolationsTable, minimalProps);
  });

  it('renders an NxTable with headers', () => {
    const component = getShallow(),
      table = component.find(NxTable),
      tHeader = table.find(NxTableHead),
      headerRow = tHeader.find(NxTableRow),
      headers = headerRow.find(NxTableCell);

    expect(headers.length).toEqual(5);
    expect(headers.at(0)).toHaveProp('children', 'Threat');
    expect(headers.at(1)).toHaveProp('children', 'Policy/Action');
    expect(headers.at(2)).toHaveProp('children', 'Constraint Name');
    expect(headers.at(3)).toHaveProp('children', 'Condition');
    expect(headers.at(4)).not.toHaveProp('children');
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

      expect(body).toHaveProp('retryHandler', minimalProps.retryHandler);
    });

    it('creates a PolicyViolationsTableRow per violation in the received prop', () => {
      const multipleViolations = [
        {
          policyViolationId: 'policyViolationId',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
          actions: [],
          constraints: [],
        },
        {
          policyViolationId: 'policyViolationId2',
          policyThreatLevel: 10,
          policyName: 'Security-Blocker',
          actions: [],
          constraints: [],
        },
      ];
      const component = getShallow({ violations: multipleViolations });
      const table = component.find(NxTable);
      const tBody = table.find(NxTableBody);
      const rows = tBody.find(PolicyViolationsTableRow);

      expect(rows.length).toEqual(2);
      expect(rows.at(0).key()).toBe('policyViolationId');
      expect(rows.at(1).key()).toBe('policyViolationId2');
    });
  });
});
