/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import RepositoryResultsComponentsTable from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsComponentsTable/RepositoryResultsComponentsTable';
import { actions as repositoryComponentsActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import * as repositoryComponentsSelectors from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { fireEvent } from '@testing-library/react';
import { getRepositoryComponentsUrl } from 'MainRoot/util/CLMLocation';
import * as firewallActions from 'MainRoot/firewall/firewallActions';

describe('RepositoryResultsComponentsTable', () => {
  let renderComponent,
    mock,
    selectLoadingRepositoryComponentsSpy,
    selectErrorComponentsTableSpy,
    selectRepositoryComponentsSpy,
    selectHasMoreResultsSpy,
    selectCurrentPageSpy,
    searchComponentsSpy,
    getRepositoryComponentsSpy,
    sortComponentsSpy,
    goToRepositoryComponentDetailsPageSpy;

  const repoId = 'testRepoId';
  let repositoryComponentsDetails = {
    loadingRepositoryComponents: false,
    errorComponentsTable: null,
    repositoryComponents: [
      {
        threatLevel: 1,
        policyName: 'No violations',
        lastEvaluationTime: new Date('2023-10-19T14:34:13Z'),
        quarantineTime: null,
        componentDisplayText: 'Component name 1',
        waived: false,
        componentHash: 'hash1',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'artifactId1',
            classifier: '',
            extension: 'jar',
            groupId: 'groupId1',
            version: '1',
          },
        },
        matchState: 'matchState',
        proprietary: 'proprietary',
        identificationSource: 'identificationSource',
        pathname: 'pathname',
      },
      {
        threatLevel: 8,
        policyName: 'Security-High',
        lastEvaluationTime: new Date('2023-10-19T15:00:00Z'),
        quarantineTime: null,
        componentDisplayText: 'Component name 2',
        waived: false,
        componentHash: 'hash2',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            artifactId: 'artifactId2',
            classifier: '',
            extension: 'jar',
            groupId: 'groupId2',
            version: '1',
          },
        },
        repositoryId: 'repositoryId',
        matchState: 'matchState',
        proprietary: 'proprietary',
        identificationSource: 'identificationSource',
        pathname: 'pathname',
      },
    ],
    componentsRequestBody: {
      page: 1,
      pageSize: 2,
      searchFilters: [],
      aggregate: false,
    },
    hasMoreResults: true,
    sortConfiguration: { dir: 'asc', column: 'publicId' },
    unsortedComponents: [],
  };

  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  beforeEach(() => {
    selectLoadingRepositoryComponentsSpy = jest.spyOn(
      repositoryComponentsSelectors,
      'selectLoadingRepositoryComponents'
    );
    selectErrorComponentsTableSpy = jest.spyOn(repositoryComponentsSelectors, 'selectErrorComponentsTable');
    selectRepositoryComponentsSpy = jest.spyOn(repositoryComponentsSelectors, 'selectRepositoryComponents');
    selectHasMoreResultsSpy = jest.spyOn(repositoryComponentsSelectors, 'selectHasMoreResults');
    selectCurrentPageSpy = jest.spyOn(repositoryComponentsSelectors, 'selectCurrentPage');

    getRepositoryComponentsSpy = jest.spyOn(repositoryComponentsActions, 'getRepositoryComponents');
    searchComponentsSpy = jest.spyOn(repositoryComponentsActions, 'searchComponents');
    sortComponentsSpy = jest.spyOn(repositoryComponentsActions, 'sortComponents');

    goToRepositoryComponentDetailsPageSpy = jest.spyOn(firewallActions, 'goToRepositoryComponentDetailsPage');

    selectRepositoryComponentsSpy.mockReturnValue([]);
    mock.onPost(getRepositoryComponentsUrl('repository', repoId)).reply(200, {});

    renderComponent = () => render(<RepositoryResultsComponentsTable repositoryId={repoId} />);
  });

  describe('when data are being loaded', () => {
    beforeEach(() => {
      selectLoadingRepositoryComponentsSpy.mockReturnValue(true);
    });

    it('renders loading spinner', () => {
      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
    });
  });

  describe('when the page has a loading error', () => {
    beforeEach(() => {
      selectLoadingRepositoryComponentsSpy.mockReturnValue(false);
      selectErrorComponentsTableSpy.mockReturnValue('Test loading error');
    });

    it('renders error section', () => {
      renderComponent();

      const retryButton = screen.queryByText('Retry');
      expect(screen.getByText('An error occurred loading data. Test loading error')).toBeVisible();
      fireEvent.click(retryButton);
      expect(getRepositoryComponentsSpy).toHaveBeenCalled();
    });
  });

  describe('when there are no components ', () => {
    beforeEach(() => {
      selectLoadingRepositoryComponentsSpy.mockReturnValue(false);
      selectRepositoryComponentsSpy.mockReturnValue([]);
    });

    it('renders default empty message', () => {
      renderComponent();

      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
      expect(screen.getByText('No results')).toBeVisible();
    });
  });

  describe('when components exist', () => {
    beforeEach(() => {
      selectRepositoryComponentsSpy.mockReturnValue(repositoryComponentsDetails.repositoryComponents);
      selectLoadingRepositoryComponentsSpy.mockReturnValue(false);
    });

    it('renders table with all components', () => {
      renderComponent();

      const components = [
        {
          threatLevel: 1,
          policyName: 'No violations',
          quarantineTime: null,
          componentDisplayText: 'Component name 1',
          waived: false,
        },
        {
          threatLevel: 8,
          policyName: 'Security-High',
          quarantineTime: null,
          componentDisplayText: 'Component name 2',
          waived: false,
        },
      ];

      expect(screen.getByText(components[0].threatLevel)).toBeVisible();
      expect(screen.getByText(components[0].policyName)).toBeVisible();
      expect(screen.getByText(components[0].componentDisplayText)).toBeVisible();
      expect(screen.getByText(components[1].threatLevel)).toBeVisible();
      expect(screen.getByText(components[1].policyName)).toBeVisible();
      expect(screen.getByText(components[1].componentDisplayText)).toBeVisible();
    });

    it('filters components by the name or policy or evaluation time or quarantine time', () => {
      renderComponent();

      const searchByNameInput = screen.getByPlaceholderText('component name');
      const searchByPolicyInput = screen.getByPlaceholderText('policy name');
      const [searchByEvaluationTime, searchByQuarantineTime] = screen.getAllByPlaceholderText('date');

      fireEvent.change(searchByNameInput, { target: { value: 'component 1' } });
      fireEvent.change(searchByPolicyInput, { target: { value: 'High' } });
      fireEvent.change(searchByEvaluationTime, { target: { value: '2023-10-19 14:34:13' } });
      fireEvent.change(searchByQuarantineTime, { target: { value: '2023-10-19 14:34:13' } });

      expect(searchComponentsSpy).toHaveBeenCalledWith({
        filterValue: 'component 1',
        filterName: 'COMPONENT_COORDINATES',
      });
      expect(searchComponentsSpy).toHaveBeenCalledWith({
        filterValue: 'High',
        filterName: 'POLICY_NAME',
      });
      expect(searchComponentsSpy).toHaveBeenCalledWith({
        filterValue: '2023-10-19 14:34:13',
        filterName: 'EVALUATION_TIME',
      });
      expect(searchComponentsSpy).toHaveBeenCalledWith({
        filterValue: '2023-10-19 14:34:13',
        filterName: 'QUARANTINE_TIME',
      });
    });

    it('sorts components by the selected field', () => {
      renderComponent();

      const sortByThreatButton = screen.getByRole('button', { name: /threat unsorted/i });
      const sortByPolicyButton = screen.getByRole('button', { name: /policy unsorted/i });
      const sortByEvaluationTimeButton = screen.getByRole('button', { name: /evaluation time unsorted/i });
      const sortByQuarantinedButton = screen.getByRole('button', { name: /quarantine time descending/i });
      const sortByComponentButton = screen.getByRole('button', { name: /component unsorted/i });

      fireEvent.click(sortByThreatButton);
      expect(sortComponentsSpy).toHaveBeenCalledWith('POLICY_THREAT_LEVEL');
      expect(screen.getByRole('button', { name: /threat ascending/i })).toBeVisible();

      fireEvent.click(sortByPolicyButton);
      expect(sortComponentsSpy).toHaveBeenCalledWith('POLICY_NAME');
      expect(screen.getByRole('button', { name: /policy descending/i })).toBeVisible();

      fireEvent.click(sortByEvaluationTimeButton);
      expect(sortComponentsSpy).toHaveBeenCalledWith('EVALUATION_TIME');
      expect(screen.getByRole('button', { name: /evaluation time ascending/i })).toBeVisible();

      fireEvent.click(sortByQuarantinedButton);
      expect(sortComponentsSpy).toHaveBeenCalledWith('QUARANTINE_TIME');
      expect(screen.getByRole('button', { name: /quarantine time ascending/i })).toBeVisible();

      fireEvent.click(sortByComponentButton);
      expect(sortComponentsSpy).toHaveBeenCalledWith('COMPONENT_COORDINATES');
      expect(screen.getByRole('button', { name: /component descending/i })).toBeVisible();
      fireEvent.click(sortByComponentButton);
      expect(screen.getByRole('button', { name: /component ascending/i })).toBeVisible();
    });

    it('redirects to firewall component details page', () => {
      renderComponent();

      const row = repositoryComponentsDetails.repositoryComponents[0];
      const index = row.policyName + row.componentDisplayText + 0;

      fireEvent.click(screen.getByTestId(index));
      expect(goToRepositoryComponentDetailsPageSpy).toHaveBeenCalled();
    });

    it('renders indeterminate pagination if there is more than one page of results', () => {
      selectCurrentPageSpy.mockReturnValue(1);
      selectHasMoreResultsSpy.mockReturnValue(true);
      renderComponent();

      const pagination = screen.getByTestId('components-table-pagination');

      expect(pagination).toBeVisible();
    });

    it('does not render indeterminate pagination if there is only one page of results', () => {
      selectCurrentPageSpy.mockReturnValue(1);
      selectHasMoreResultsSpy.mockReturnValue(false);
      renderComponent();

      const pagination = screen.queryByTestId('components-table-pagination');

      expect(pagination).not.toBeInTheDocument();
    });

    it('renders threat level 0 and policy "No Violations" when components have these fields null', () => {
      repositoryComponentsDetails.repositoryComponents = [
        {
          ...repositoryComponentsDetails.repositoryComponents[0],
          policyName: null,
          threatLevel: null,
        },
      ];
      selectRepositoryComponentsSpy.mockReturnValue(repositoryComponentsDetails.repositoryComponents);

      renderComponent();

      const components = [
        {
          threatLevel: null,
          policyName: null,
          quarantineTime: null,
          componentDisplayText: 'Component name 1',
          waived: false,
        },
      ];

      expect(screen.getByText(0)).toBeVisible();
      expect(screen.getByText('No Violations')).toBeVisible();
      expect(screen.getByText(components[0].componentDisplayText)).toBeVisible();
    });
  });
});
