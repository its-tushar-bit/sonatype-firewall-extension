/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import moment from 'moment';
import ExclusionLogTable from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/ExclusionLogTable';
import ExclusionLogTableRow from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/ExclusionLogTableRow';

describe('Exclusion Log', () => {
  let minimalProps;

  beforeEach(() => {
    minimalProps = {
      exclusions: [
        {
          createTime: moment().subtract(1, 'days').format(),
          threatLevel: 7,
          policyName: 'Security-Medium',
          componentDisplayName: 'com.example:test:1.0.0',
          vulnerabilityIdentifiers: 'SONATYPE-1234',
          autoPolicyWaiverId: 'waiver-id-1',
          autoPolicyWaiverRevocationId: 'revocation-id-1',
        },
        {
          createTime: moment().subtract(2, 'days').format(),
          threatLevel: 5,
          policyName: 'Security-Low',
          componentDisplayName: 'org.example:other:2.0.0',
          vulnerabilityIdentifiers: 'SONATYPE-5678',
          autoPolicyWaiverId: 'waiver-id-1',
          autoPolicyWaiverRevocationId: 'revocation-id-2',
        },
      ],
      refreshTable: jest.fn(),
    };
  });

  describe('ExclusionLogTable', () => {
    const renderTable = (additionalProps = {}) => render(<ExclusionLogTable {...minimalProps} {...additionalProps} />);

    it('renders table headers and rows with data for each exclusion', () => {
      renderTable();

      const rows = screen.getAllByRole('row');

      expect(screen.getByText('Date')).toBeVisible();
      expect(screen.getByText('Threat')).toBeVisible();
      expect(screen.getByText('Policy')).toBeVisible();
      expect(screen.getByText('Component')).toBeVisible();
      expect(screen.getByText('Vulnerability')).toBeVisible();

      expect(rows.length).toEqual(3);

      expect(screen.getByText('Security-Medium')).toBeVisible();
      expect(screen.getByText('com.example:test:1.0.0')).toBeVisible();
      expect(screen.getByText('SONATYPE-1234')).toBeVisible();
      expect(screen.getAllByText('7')).toHaveLength(1);

      expect(screen.getByText('Security-Low')).toBeVisible();
      expect(screen.getByText('org.example:other:2.0.0')).toBeVisible();
      expect(screen.getByText('SONATYPE-5678')).toBeVisible();
      expect(screen.getAllByText('5')).toHaveLength(1);
    });

    it('renders table with empty message when no exclusions', () => {
      renderTable({ exclusions: [] });

      expect(screen.getByText('No exclusions found')).toBeVisible();
      //header and the empty message row
      expect(screen.getAllByRole('row')).toHaveLength(2);
    });
  });

  describe('ExclusionLogTableRow', () => {
    const renderRow = (additionalProps = {}) =>
      render(
        <table>
          <tbody>
            <ExclusionLogTableRow exclusion={minimalProps.exclusions[0]} {...additionalProps} />
          </tbody>
        </table>
      );

    it('renders row with all data fields', () => {
      renderRow();

      // Check all data fields are rendered
      expect(screen.getByText(moment(minimalProps.exclusions[0].createTime).format('YYYY-MM-DD'))).toBeVisible();
      expect(screen.getByText('7')).toBeVisible();
      expect(screen.getByText('Security-Medium')).toBeVisible();
      expect(screen.getByText('com.example:test:1.0.0')).toBeVisible();
      expect(screen.getByText('SONATYPE-1234')).toBeVisible();
    });

    it('opens delete modal on delete button click', async () => {
      renderRow();

      const deleteButton = await screen.findByRole('button', { name: /delete/i });
      fireEvent.click(deleteButton);

      expect(
        await screen.findByText('Click Continue to resume automated waiver eligibility for this violation')
      ).toBeVisible();
    });
  });
});
