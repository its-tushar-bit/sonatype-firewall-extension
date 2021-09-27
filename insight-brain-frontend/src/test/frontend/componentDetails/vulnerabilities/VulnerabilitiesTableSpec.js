/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';

import VulnerabilitiesTable from '../../../../main/frontend/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTable';
import VulnerabilitiesTableRow from '../../../../main/frontend/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTableRow';

describe('VulnerabilitiesTable', () => {
  let minimalProps, getShallow;

  beforeEach(function () {
    const loadVulnerabilities = jasmine.createSpy('loadVulnerabilities');
    minimalProps = {
      vulnerabilities: {
        data: null,
        loading: false,
        error: null,
      },
      loadVulnerabilities,
    };
    getShallow = enzymeUtils.getShallowComponent(VulnerabilitiesTable, minimalProps);
  });

  it('renders an NxTable with headers', () => {
    const component = getShallow(),
      table = component.find(NxTable),
      tHeader = table.find(NxTableHead),
      headerRow = tHeader.find(NxTableRow),
      headers = headerRow.find(NxTableCell);

    expect(headers.length).toEqual(4);
    expect(headers.at(0)).toHaveProp('children', 'CVSS');
    expect(headers.at(1)).toHaveProp('children', 'Problem Code');
    expect(headers.at(2)).toHaveProp('children', 'Status');
    expect(headers.at(3)).not.toHaveProp('children');
  });

  describe('Table body', () => {
    it('sets isLoading in the table body with the received loading flag', () => {
      let component = getShallow({
        vulnerabilities: {
          data: null,
          loading: true,
          error: null,
        },
      });
      let body = component.find(NxTableBody);
      expect(body).toHaveProp('isLoading', true);

      component = getShallow();
      body = component.find(NxTableBody);
      expect(body).toHaveProp('isLoading', false);
    });

    it('sets the error prop on the table body with the received error prop', () => {
      let component = getShallow({
        vulnerabilities: {
          data: null,
          loading: true,
          error: 'some error',
        },
      });
      let body = component.find(NxTableBody);
      expect(body).toHaveProp('error', 'some error');

      component = getShallow();
      body = component.find(NxTableBody).dive();
      expect(body).not.toHaveProp('error');
    });

    it('sets the retryHandler prop on the table body with the received retryHandler prop', () => {
      const component = getShallow(),
        body = component.find(NxTableBody);

      expect(body).toHaveProp('retryHandler', minimalProps.loadVulnerabilities);
    });

    it('creates a VulnerabilitiesTableRowSpec per vulnerability in the received prop', () => {
      const vulnerabilities = {
        data: [
          {
            refId: 'ID1',
            severity: 8,
            status: 'status 1',
          },
          {
            refId: 'ID2',
            severity: 9.2,
            status: 'status 2',
          },
        ],
        loading: true,
        error: 'some error',
      };
      const component = getShallow({
        vulnerabilities,
      });
      const table = component.find(NxTable);
      const tBody = table.find(NxTableBody);
      const rows = tBody.find(VulnerabilitiesTableRow);

      expect(rows.length).toEqual(2);
      expect(rows.at(0).key()).toBe('ID1');
      expect(rows.at(1).key()).toBe('ID2');
    });
  });
});
