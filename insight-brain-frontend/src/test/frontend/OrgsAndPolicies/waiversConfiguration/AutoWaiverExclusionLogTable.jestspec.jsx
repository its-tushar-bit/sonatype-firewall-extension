/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import { getAutoWaiverExclusionsByAutoWaiverIdUrl } from 'MainRoot/util/CLMLocation';
import AutoWaiverExclusionLogTable from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverExclusionLogTable';

describe('Auto Waiver Exclusion Log Table', () => {
  let axiosMock,
    renderTable,
    minimalProps,
    initialState,
    expectedAutoWaiverExclusionsByAutoWaiverIdUrl,
    autoWaiverExclusions,
    ownerType,
    autoWaiverId,
    autoWaiverOwnerId,
    organizationId;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    minimalProps = {
      disableDelete: true,
    };

    ownerType = 'application';
    autoWaiverOwnerId = '1b936810f8e446e3871ca032f292c9f7';
    autoWaiverId = 'e41dc09605a1494d9c0ed536b44213cf';
    organizationId = 'ROOT_ORGANIZATION_ID';

    autoWaiverExclusions = [
      {
        autoPolicyWaiverExclusionId: 'a1266cfc941c4a9d943ad01c9f4e8f45',
        ownerId: '1b936810f8e446e3871ca032f292c9f7',
        createTime: '2025-04-09T17:52:18.600+0000',
        autoPolicyWaiverId: 'e41dc09605a1494d9c0ed536b44213cf',
        ownerName: 'TestApp2',
        threatLevel: 9,
        policyName: 'Security-High',
        componentDisplayName: 'io.undertow : undertow-core : 2.3.10.Final',
        vulnerabilityIdentifiers: 'CVE-2023-1973',
      },
      {
        autoPolicyWaiverExclusionId: '6a57c6f7a6de484b8fb4121170f3d7dc',
        ownerId: '1b936810f8e446e3871ca032f292c9f7',
        createTime: '2025-04-08T17:52:25.537+0000',
        autoPolicyWaiverId: 'e41dc09605a1494d9c0ed536b44213cf',
        ownerName: 'TestApp3',
        threatLevel: 9,
        policyName: 'Security-High',
        componentDisplayName: 'io.undertow : undertow-core : 2.3.10.Final',
        vulnerabilityIdentifiers: 'CVE-2024-1635',
      },
      {
        autoPolicyWaiverExclusionId: '7381e2d2f39b4fd294b01b4948c04c03',
        ownerId: '1b936810f8e446e3871ca032f292c9f7',
        createTime: '2025-04-07T17:52:32.496+0000',
        autoPolicyWaiverId: 'e41dc09605a1494d9c0ed536b44213cf',
        ownerName: 'TestApp4',
        threatLevel: 9,
        policyName: 'Security-High',
        componentDisplayName: 'io.undertow : undertow-core : 2.3.10.Final',
        vulnerabilityIdentifiers: 'CVE-2024-5971',
      },
      {
        autoPolicyWaiverExclusionId: '24ccef0b09b24a41bd52fa94b1c7c59c',
        ownerId: '1b936810f8e446e3871ca032f292c9f7',
        creatorName: 'Admin BuiltIn',
        autoPolicyWaiverId: 'e41dc09605a1494d9c0ed536b44213cf',
        ownerName: 'TestApp5',
        threatLevel: 9,
        policyName: 'Security-High',
        componentDisplayName: 'io.undertow : undertow-core : 2.3.10.Final',
        vulnerabilityIdentifiers: 'CVE-2024-6162',
      },
      {
        autoPolicyWaiverExclusionId: '1410dc5787fa490e9959fba447cf1a6b',
        ownerId: '1b936810f8e446e3871ca032f292c9f7',
        creatorName: 'Admin BuiltIn',
        autoPolicyWaiverId: 'e41dc09605a1494d9c0ed536b44213cf',
        ownerName: 'TestApp6',
        threatLevel: 9,
        policyName: 'Security-High',
        componentDisplayName: 'io.undertow : undertow-core : 2.3.10.Final',
        vulnerabilityIdentifiers: 'CVE-2024-7885',
      },
    ];

    initialState = {
      router: {
        currentParams: {
          autoWaiverId,
          autoWaiverOwnerId,
          organizationId,
          ownerType,
        },
        currentState: {
          name: 'management.edit.organization.auto-waiver-details',
          data: {
            title: 'Organization Auto Waiver Details',
          },
        },
      },
      orgsAndPolicies: {
        root: {
          loading: false,
          loadError: null,
          selectedOwner: {
            name: 'Root Organization',
            nameLowercaseNoWhitespace: 'rootorganization',
            id: 'ROOT_ORGANIZATION_ID',
            parentOrganizationId: null,
            legacyViolationEnabled: null,
            allowLegacyViolationOverride: true,
            repositoryConnectionEnabled: null,
            allowRepositoryConnectionOverride: true,
            artifactoryConnectionEnabled: null,
            allowArtifactoryConnectionOverride: true,
          },
          policiesByOwner: null,
        },
      },
    };

    expectedAutoWaiverExclusionsByAutoWaiverIdUrl = getAutoWaiverExclusionsByAutoWaiverIdUrl(
      ownerType,
      autoWaiverOwnerId,
      autoWaiverId
    );

    renderTable = (preloadedState = initialState) =>
      render(<AutoWaiverExclusionLogTable {...minimalProps} />, { preloadedState });
  });

  describe('Display rendering', () => {
    it('renders table headers and rows with data for each exclusion', async () => {
      axiosMock.onGet(expectedAutoWaiverExclusionsByAutoWaiverIdUrl).reply(200, autoWaiverExclusions);

      await waitFor(() => {
        renderTable();
      });

      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].url).toBe(expectedAutoWaiverExclusionsByAutoWaiverIdUrl);

      const rows = screen.getAllByRole('row');
      expect(rows.length).toEqual(6);

      expect(screen.getByText('Date')).toBeVisible();
      expect(screen.getByText('Owner')).toBeVisible();
      expect(screen.getByText('Threat')).toBeVisible();
      expect(screen.getByText('Policy')).toBeVisible();
      expect(screen.getByText('Component')).toBeVisible();
      expect(screen.getByText('Vulnerability')).toBeVisible();

      expect(screen.getByText('2025-04-09')).toBeVisible();
      expect(screen.getByText('TestApp2')).toBeVisible();
      expect(screen.getByText('CVE-2023-1973')).toBeVisible();

      // test for similarity between rows
      expect(screen.getAllByText('9')).toHaveLength(5);
      expect(screen.getAllByText('Security-High')).toHaveLength(5);
      expect(screen.getAllByText('io.undertow : undertow-core : 2.3.10.Final')).toHaveLength(5);
    });

    it('renders table with empty message when no exclusions', async () => {
      await waitFor(() => {
        renderTable();
      });

      expect(screen.getByText('No exclusions found')).toBeVisible();
      //header and the empty message row
      expect(screen.getAllByRole('row')).toHaveLength(2);
    });
  });

  describe('Row actions', () => {
    it('opens delete modal on delete button click', async () => {
      axiosMock.onGet(expectedAutoWaiverExclusionsByAutoWaiverIdUrl).reply(200, autoWaiverExclusions);

      await waitFor(() => {
        minimalProps.disableDelete = false;
        renderTable();
      });

      const deleteButtons = await screen.findAllByRole('button', { id: /delete/i });
      expect(deleteButtons).toHaveLength(5);
      deleteButtons.forEach((value) => expect(value).not.toHaveClass('disabled'));

      fireEvent.click(deleteButtons[0]);

      expect(
        await screen.findByText('Click Continue to resume auto-waiver eligibility for this violation')
      ).toBeVisible();
    });

    it('disables a delete button and prevents button click', async () => {
      axiosMock.onGet(expectedAutoWaiverExclusionsByAutoWaiverIdUrl).reply(200, autoWaiverExclusions);

      await waitFor(() => {
        renderTable();
      });

      const deleteButtons = await screen.findAllByRole('button', { id: /Cannot delete an inherited auto-waiver/i });
      expect(deleteButtons).toHaveLength(5);
      deleteButtons.forEach((value) => expect(value).toHaveClass('disabled'));
    });
  });
});
