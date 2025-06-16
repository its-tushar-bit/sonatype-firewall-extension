/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as R from 'ramda';
import React from 'react';

import {
  getContainerRepositoryReportSummaryUrl,
  getContainerRepositoryResultsUrl,
  getRepositoryInfoUrl,
} from 'MainRoot/util/CLMLocation';

import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';

import ContainerRepositoryResultsPage from 'MainRoot/OrgsAndPolicies/containerRepositoryResultsPage/ContainerRepositoryResultsPage';

describe('ContainerRepositoryResultsPage', () => {
  let renderPage;
  const axiosMock = axiosMockAdapter();
  const repositoryId = 'repository-id';

  const mockResults = [
    {
      threatLevel: 1,
      policyName: null,
      violationCount: 0,
      scanId: 'scanid1234567890',
      objectName: 'containerImageNamespace1-containerImageName1-containerImageVersion1',
      quarantineTime: 1747153984479,
    },
    {
      threatLevel: 1,
      policyName: null,
      violationCount: 1,
      scanId: 'scanid1234567890',
      objectName: 'containerImageNamespace1-containerImageName1-containerImageVersion1',
      quarantineTime: 1747153984479,
    },
    {
      threatLevel: 7,
      policyName: null,
      violationCount: 2,
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
      currentParams: {
        '#': null,
        repositoryId,
      },
      prevState: {
        name: '',
        url: '^',
      },
    },
    containerRepositoryResultsPage: {
      loading: false,
      errorMessage: null,
      evaluationSummary: {
        totalContainerImageCount: 0,
        totalContainerImageViolationCount: 0,
        criticalViolationCount: 0,
        severeViolationCount: 0,
        moderateViolationCount: 0,
        affectedContainerImageCount: 0,
        quarantinedContainerImageCount: 0,
      },
      results: null,
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
      showFilterDrawer: false,

      violationStateFilters: [],
      threatLevelRange: [0, 10],
      showReevaluationModal: false,
      submitMask: {
        show: false,
        success: false,
      },
    },
  });

  beforeEach(() => {
    renderPage = (minimalProps = {}) =>
      render(<ContainerRepositoryResultsPage />, { preloadedState: R.mergeDeepRight(initialState, minimalProps) });
  });

  describe('Evaluation Summary', () => {
    it('renders the correct summary content', async () => {
      axiosMock.onGet(getRepositoryInfoUrl(repositoryId)).reply(200, { publicId: 'repository-public-id' });
      axiosMock.onPost(getContainerRepositoryResultsUrl(repositoryId)).reply(200, {
        repositoryResultsDetails: mockResults,
        hasNextPage: false,
      });
      axiosMock.onGet(getContainerRepositoryReportSummaryUrl(repositoryId)).reply(200, {
        totalContainerImageCount: 33,
        totalContainerImageViolationCount: 11,
        criticalViolationCount: 1,
        severeViolationCount: 2,
        moderateViolationCount: 3,
        affectedContainerImageCount: 22,
        quarantinedContainerImageCount: 44,
      });

      const { container } = renderPage();

      await waitFor(() => expect(screen.queryByText('Loading…')).toBeNull());

      const criticalCounter = container.querySelector('.nx-small-threat-counter--critical');
      expect(criticalCounter).toBeInTheDocument();
      expect(criticalCounter).toHaveTextContent('1');

      const severeCounter = container.querySelector('.nx-small-threat-counter--severe');
      expect(severeCounter).toBeInTheDocument();
      expect(severeCounter).toHaveTextContent('2');

      const moderateCounter = container.querySelector('.nx-small-threat-counter--moderate');
      expect(moderateCounter).toBeInTheDocument();
      expect(moderateCounter).toHaveTextContent('3');

      expect(screen.getByText('11 VIOLATIONS')).toBeInTheDocument();
      expect(screen.getByText('Affecting 22 containers')).toBeInTheDocument();
      expect(screen.getByText('33 CONTAINERS')).toBeInTheDocument();
      expect(screen.getByTestId('evaluation-summary-quarantined-count')).toHaveTextContent('44');
    });
  });
});
