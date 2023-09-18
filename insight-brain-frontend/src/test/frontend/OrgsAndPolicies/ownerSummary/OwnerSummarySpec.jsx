/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import OwnerSummary from 'MainRoot/OrgsAndPolicies/ownerSummary/OwnerSummary';
import {
  getOrganizationsUrl,
  getApplicationsUrl,
  getDashboardStageUrl,
  getApplicablePolicies,
  getApplicationSummaryUrl,
} from 'MainRoot/util/CLMLocation';
import { render, axiosMockAdapter, within, screen, fireEvent } from 'TestRoot/SpecUtil';

describe('OwnerSummary', () => {
  let axiosMock, preloadedState;

  const renderComponent = (preloadedState) => render(<OwnerSummary />, { preloadedState });

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  describe('if owner is an organization', () => {
    const ownerType = 'organization';
    const ownerId = 'be17ea5538de4679ba3a9220734ddbf7';
    beforeAll(() => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          sourceControl: {
            data: {
              repositoryUrl: null,
              provider: {
                value: null,
                parentValue: 'github',
              },
              token: {
                value: null,
              },
            },
          },
        },
      };
    });

    beforeEach(() => {
      axiosMock.onGet(getOrganizationsUrl()).reply(200, [
        {
          allowArtifactoryConnectionOverride: true,
          allowPolicyViolationGrandfatheringOverride: true,
          allowRepositoryConnectionOverride: true,
          artifactoryConnectionEnabled: null,
          id: 'be17ea5538de4679ba3a9220734ddbf7',
          name: 'broadcast',
          nameLowercaseNoWhitespace: 'broadcast',
          parentOrganizationId: 'ROOT_ORGANIZATION_ID',
          policyViolationGrandfatheringEnabled: null,
          repositoryConnectionEnabled: null,
          contact: {
            displayName: 'Provided Contact Display Name',
          },
        },
        {
          allowArtifactoryConnectionOverride: true,
          allowPolicyViolationGrandfatheringOverride: true,
          allowRepositoryConnectionOverride: true,
          artifactoryConnectionEnabled: null,
          id: 'b4483a7072384f59a1387fed9e501dce',
          name: 'new org',
          nameLowercaseNoWhitespace: 'neworg',
          parentOrganizationId: 'ROOT_ORGANIZATION_ID',
          policyViolationGrandfatheringEnabled: null,
          repositoryConnectionEnabled: null,
        },
      ]);
      axiosMock.onGet(getApplicablePolicies(ownerType, ownerId)).reply(200, { data: { policiesByOwner: {} } });
    });

    it('renders a loading indicator', () => {
      renderComponent(preloadedState);
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('renders an alert with retry if something goes wrong', async () => {
      axiosMock.onGet(getOrganizationsUrl()).reply(() => Promise.reject('An error occurred loading data.'));

      renderComponent(preloadedState);

      let failureAlert = await screen.findByRole('alert');

      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');

      let retryButton = await within(failureAlert).getByRole('button');

      expect(retryButton).toBeVisible();
      fireEvent.click(retryButton);

      expect(await screen.findByText('Loading…')).toBeVisible();
      failureAlert = await screen.findByRole('alert');
      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent('An error occurred loading data.');
    });

    it('renders an alert if there is no matching owner', async () => {
      axiosMock.onGet(getOrganizationsUrl()).reply(200, []);

      renderComponent(preloadedState);

      let failureAlert = await screen.findByRole('alert');

      expect(failureAlert).toBeVisible();
      expect(failureAlert).toHaveTextContent(
        'Could not find an organization with ID be17ea5538de4679ba3a9220734ddbf7.'
      );
    });

    it('renders proper header with selected contact if contact is provided', async () => {
      renderComponent(preloadedState);

      expect(await screen.findByText('broadcast')).toBeVisible();
      expect(await screen.findByText('Provided Contact Display Name')).toBeVisible();
    });

    it('renders insufficient permissions tree', async () => {
      preloadedState.orgsAndPolicies.ownerSideNav = { displayedOrganization: { synthetic: true } };
      renderComponent(preloadedState);
      const treeMessage = await screen.findByText(
        'View all organizations and applications on which you have permissions. Click on the link for the org or app below to access details.'
      );
      expect(treeMessage).toBeVisible();
      expect(screen.queryByText('broadcast')).not.toBeInTheDocument();
      expect(screen.queryByText('Provided Contact Display Name')).not.toBeInTheDocument();
    });
  });

  describe('if owner is an application', () => {
    const ownerType = 'application';
    const ownerId = 'be17ea5538de4679ba3a9220734ddbf7';

    beforeEach(() => {
      preloadedState = {
        router: {
          currentState: {
            name: 'management.view.application',
            url: '/application/{applicationPublicId}',
            data: {
              title: 'Application Management',
              viewportSized: true,
            },
          },
          currentParams: {
            applicationPublicId: 'a-aws-4-lll_app_filter',
          },
        },
        orgsAndPolicies: {
          sourceControl: {
            data: {
              repositoryUrl: null,
              provider: {
                value: null,
                parentValue: 'github',
              },
              token: {
                value: null,
              },
            },
          },
        },
      };

      axiosMock.onGet(getApplicationsUrl()).reply(200, [
        {
          id: ownerId,
          name: 'a-aws 4-lll app filter',
          organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
          organizationName: '4-level-org',
          publicId: 'a-aws-4-lll_app_filter',
          contact: {
            displayName: 'Provided Contact Display Name',
          },
        },
        {
          id: 'fd6c2140f61c460d9d900d8f72620475',
          name: 'www',
          organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
          organizationName: '4-level-org',
          publicId: 'www',
          contact: null,
        },
      ]);
      axiosMock.onGet(getApplicablePolicies(ownerType, 'a-aws-4-lll_app_filter')).reply(200, { policiesByOwner: {} });
      axiosMock.onGet(getDashboardStageUrl()).reply(200, []);
      axiosMock.onGet(getApplicationSummaryUrl('a-aws-4-lll_app_filter')).reply(200, {
        contact: {
          displayName: 'Provided Contact Display Name',
        },
        id: ownerId,
        name: 'a-aws 4-lll app filter',
        organizationId: '1b3066fd0c6f4f4785a5bcb27a9652e4',
        organizationName: '4-level-org',
        publicId: 'a-aws-4-lll_app_filter',
        policyEvaluations: {},
        policyEvaluationsResults: {},
      });
    });

    it('renders proper header with selected contact and publicId', async () => {
      renderComponent(preloadedState);

      expect(await screen.findByText('a-aws 4-lll app filter')).toBeVisible();
      expect(await screen.findByText('(a-aws-4-lll_app_filter)')).toBeVisible();
      expect(await screen.findByText('Provided Contact Display Name')).toBeVisible();
    });
  });
});
