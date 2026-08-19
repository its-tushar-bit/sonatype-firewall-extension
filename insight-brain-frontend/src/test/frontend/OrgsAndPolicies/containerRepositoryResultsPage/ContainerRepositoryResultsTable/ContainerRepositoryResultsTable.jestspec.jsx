/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as R from 'ramda';
import React from 'react';
import userEvent from '@testing-library/user-event';

import { render, screen } from 'TestRoot/SpecUtil';

import ContainerRepositoryResultsTable from 'MainRoot/OrgsAndPolicies/containerRepositoryResultsPage/containerRepositoryResultsTable/ContainerRepositoryResultsTable';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { FIREWALL_CONTAINER_REPOSITORY_RESULTS } from 'MainRoot/constants/states/firewall';

describe('ContainerRepositoryResultsTable', () => {
  let renderTable;

  const mockResults = [
    {
      threatLevel: 1,
      policyName: null,
      violationCount: 0,
      applicationPublicId: 'test-container-public-id',
      scanId: 'scanid1234567890',
      objectName: 'containerImageNamespace1-containerImageName1-containerImageVersion1',
      quarantineTime: 1747153984479,
    },
    {
      threatLevel: 1,
      policyName: null,
      violationCount: 1,
      applicationPublicId: 'test-container-public-id',
      scanId: 'scanid1234567890',
      objectName: 'containerImageNamespace1-containerImageName1-containerImageVersion1',
      quarantineTime: 1747153984479,
    },
    {
      threatLevel: 7,
      policyName: null,
      violationCount: 2,
      applicationPublicId: 'test-container-public-id',
      scanId: 'scanid1234567890',
      objectName: 'containerImageNamespace1-containerImageName1-containerImageVersion1',
      quarantineTime: 1747153984479,
    },
  ];

  const initialState = Object.freeze({
    router: {
      currentState: {
        name: 'firewall.containerRepositoryResults',
        data: {
          title: 'Container Repository Results',
        },
      },
    },
    containerRepositoryResultsPage: {
      loading: false,
      errorMessage: null,

      results: mockResults,
      sortConfiguration: [
        {
          sortableField: 'QUARANTINE_TIME',
          asc: false,
          sortPriority: 1,
        },
        {
          sortableField: 'POLICY_THREAT_LEVEL',
          asc: false,
          sortPriority: 2,
        },
        {
          sortableField: 'OBJECT_NAME',
          asc: true,
          sortPriority: 4,
        },
      ],
      columnFilters: [
        {
          filterableField: 'QUARANTINE_TIME',
          value: '',
        },
      ],
      pagination: {
        page: 1,
        pageSize: 50,
        hasNextPage: false,
      },
    },
  });

  beforeEach(() => {
    renderTable = (minimalProps = {}) =>
      render(<ContainerRepositoryResultsTable />, { preloadedState: R.mergeDeepRight(initialState, minimalProps) });
  });

  describe('content', () => {
    it('renders table headers and rows with the correct content', async () => {
      renderTable();

      const rows = screen.getAllByRole('row');
      expect(rows.length).toEqual(5);

      expect(screen.getByText('THREAT')).toBeVisible();
      expect(screen.getByText('POLICY')).toBeVisible();
      expect(screen.getByText('EVALUATION TIME')).toBeVisible();
      expect(screen.getByText('OBJECT')).toBeVisible();

      expect(rows[2].querySelector('.container-repository-results-table__threat-level-cell').textContent).toContain(
        '1'
      );
      expect(rows[2].querySelector('.container-repository-results-table__quarantine-time').textContent).toContain(
        '2025-05-13 12:33:04'
      );
      expect(rows[2].querySelector('.container-repository-results-table__object-name-cell').textContent).toContain(
        'containerImageNamespace1-containerImageName1-containerImageVersion1'
      );
      expect(rows[3].querySelector('.container-repository-results-table__threat-level-cell').textContent).toContain(
        '1'
      );
      expect(rows[4].querySelector('.container-repository-results-table__threat-level-cell').textContent).toContain(
        '7'
      );
    });

    it('renders table with empty message', async () => {
      renderTable({
        containerRepositoryResultsPage: {
          results: [],
        },
      });

      expect(screen.getByText('No results')).toBeVisible();

      // Header, Fiter Row, and the Empty Message Row
      expect(screen.getAllByRole('row')).toHaveLength(3);
    });

    it('navigates to container report with repository-results origin when a row is clicked', async () => {
      const user = userEvent.setup();
      const stateGoSpy = jest.spyOn(RouterActions, 'stateGo');

      renderTable();

      const firstDataRow = screen.getAllByRole('row')[2];
      await user.click(firstDataRow);

      expect(stateGoSpy).toHaveBeenCalledWith('firewall.containerReport', {
        origin: FIREWALL_CONTAINER_REPOSITORY_RESULTS,
        publicId: 'test-container-public-id',
        scanId: 'scanid1234567890',
      });
    });
  });

  describe('pagination', () => {
    it('renders pagination controls when there are results', () => {
      renderTable({
        containerRepositoryResultsPage: {
          pagination: {
            page: 1,
            pageSize: 50,
            hasNextPage: true,
          },
        },
      });

      expect(screen.queryByTestId('container-repository-results-table__pagination')).toBeInTheDocument();
    });

    it('does not render pagination controls when first page and does not have next page', () => {
      renderTable({
        containerRepositoryResultsPage: {
          pagination: {
            page: 1,
            pageSize: 50,
            hasNextPage: false,
          },
        },
      });

      expect(screen.queryByTestId('container-repository-results-table__pagination')).not.toBeInTheDocument();
    });
  });
});
