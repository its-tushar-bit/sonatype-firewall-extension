/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mergeDeepRight } from 'ramda';

import { axiosMockAdapter, render, screen, within } from 'TestRoot/SpecUtil';
import ListSimilarWaiversTable from 'MainRoot/waivers/ListSimilarWaiversTable';
import { getSimilarWaiversUrl } from 'MainRoot/util/CLMLocation';

describe('ListSimilarWaiversTable', () => {
  const initState = {
    router: {
      currentParams: {
        violationId: 'violationId',
      },
    },
  };
  const renderComponent = (preloadedState = initState) => render(<ListSimilarWaiversTable />, { preloadedState });
  let axiosMock;
  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  describe('renders a table', () => {
    it('with headers and loading', async () => {
      axiosMock.onGet(getSimilarWaiversUrl('violationId')).reply(200, []);
      renderComponent();
      screen.getByText('Loading…');
      await screen.findByText('help documentation');
      const table = screen.getByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      expect(groups.length).toBe(2);

      const headers = within(groups[0]).getAllByRole('columnheader');
      expect(headers.length).toBe(2);
      expect(within(headers[0]).getByText('DURATION')).toBeVisible();
      expect(within(headers[1]).getByText('WAIVER DETAILS')).toBeVisible();
    });

    it('with message if there are no waivers to display', async () => {
      axiosMock.onGet(getSimilarWaiversUrl('violationId')).reply(200, []);
      renderComponent();
      await screen.findByText('help documentation');

      const table = screen.getByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      expect(groups.length).toBe(2);

      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(1);
      expect(
        within(rows[0]).getByText('No similar waivers for this violation, to learn more about waivers see our')
      ).toBeVisible();

      const helpDocsLink = within(rows[0]).getByRole('link', { name: 'help documentation' });
      expect(helpDocsLink).toBeVisible();
      expect(helpDocsLink).toHaveAttribute('href', 'https://links.sonatype.com/products/nxiq/doc/similar-waivers');
    });

    it('with a row per similar waiver', async () => {
      const similarWaivers = [
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            policyWaiverId: '5778bf934d651d0a5694533314c9a9dc',
            expiryTime: null,
            createTime: '2023-01-11T15:32:35.849+0000',
            scopeOwnerType: 'application',
            creatorName: 'Terraria',
            scopeOwnerName: 'App',
            comment: 'waiver at app level',
            reasonText: 'mitigated externally',
          }),
        },
        getSimilarWaiver(),
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            policyWaiverId: '5778bf934d651d0a5694533314c9a9dc',
            createTime: '2050-12-31T15:32:35.849+0000',
            expiryTime: '2050-12-31T15:32:35.849+0000',
            constraintFacts: [
              {
                constraintName: 'Some constraint name',
                conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }],
              },
            ],
          }),
        },
      ];
      axiosMock.onGet(getSimilarWaiversUrl('violationId')).reply(200, similarWaivers);
      renderComponent({
        ...initState,
        violation: { similarWaivers: [], similarWaiversFilterSelectedIds: new Set([]) },
      });
      await screen.findAllByText('Organization - Org');
      const table = screen.getByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(3);

      const row1Level = rows[0];
      const row1Cells = within(row1Level).getAllByRole('cell');
      expect(row1Cells.length).toBe(2);
      expect(row1Cells[0]).toHaveTextContent('Created2050-12-31Expiration2050-12-31');
      expect(row1Cells[1]).toHaveTextContent(
        'ScopeOrganization - OrgComponentorg.apache.logging.log4j : log4j-core : 2.15.0Conditionsreason 1reason 2Reason—Commentwaiver at org levelAuthorAdmin BuiltIn'
      );

      const row2Level = rows[1];
      const row2Cells = within(row2Level).getAllByRole('cell');
      expect(row2Cells.length).toBe(2);
      expect(row2Cells[0]).toHaveTextContent('Created2024-01-11Expiration2024-01-12');
      expect(row2Cells[1]).toHaveTextContent(
        'ScopeOrganization - OrgComponentorg.apache.logging.log4j : log4j-core : 2.15.0Reason—Commentwaiver at org levelAuthorAdmin BuiltIn'
      );

      const row3Level = rows[2];
      const row3Cells = within(row3Level).getAllByRole('cell');
      expect(row3Cells.length).toBe(2);
      expect(row3Cells[0]).toHaveTextContent('Created2023-01-11ExpirationDoes not expire');
      expect(row3Cells[1]).toHaveTextContent(
        'ScopeApplication - AppComponentorg.apache.logging.log4j : log4j-core : 2.15.0Reasonmitigated externallyCommentwaiver at app levelAuthorTerraria'
      );
    });

    it('with similar waivers filtered by active', async () => {
      const similarWaivers = [
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            policyWaiverId: '1295825fa3585f894b0ab236a489826b',
            createTime: '2023-05-10T15:32:35.849+0000',
            expiryTime: '2023-05-10T15:32:35.849+0000',
            constraintFacts: [
              {
                constraintName: 'Some constraint name',
                conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }],
              },
            ],
          }),
        },
        getSimilarWaiver(),
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            policyWaiverId: '5778bf934d651d0a5694533314c9a9dc',
            createTime: '2050-12-31T15:32:35.849+0000',
            expiryTime: '2050-12-31T15:32:35.849+0000',
          }),
        },
      ];
      axiosMock.onGet(getSimilarWaiversUrl('violationId')).reply(200, similarWaivers);
      renderComponent({
        ...initState,
        violation: { similarWaivers: [], similarWaiversFilterSelectedIds: new Set(['active']) },
      });
      await screen.findAllByText('Organization - Org');
      const table = screen.getByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(1);

      const row1Level = rows[0];
      const row1Cells = within(row1Level).getAllByRole('cell');
      expect(row1Cells.length).toBe(2);
      expect(row1Cells[0]).toHaveTextContent('Created2050-12-31Expiration2050-12-31');
      expect(row1Cells[1]).toHaveTextContent(
        'ScopeOrganization - OrgComponentorg.apache.logging.log4j : log4j-core : 2.15.0Reason—Commentwaiver at org levelAuthorAdmin BuiltIn'
      );
    });

    it('with similar waivers filtered by commented', async () => {
      const similarWaivers = [
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            policyWaiverId: '1295825fa3585f894b0ab236a489826b',
            createTime: '2023-05-10T15:32:35.849+0000',
            comment: '',
          }),
        },
        getSimilarWaiver(),
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            policyWaiverId: '5778bf934d651d0a5694533314c9a9dc',
            createTime: '2050-12-31T15:32:35.849+0000',
            constraintFacts: [
              {
                constraintName: 'Some constraint name',
                conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }],
              },
            ],
          }),
        },
      ];
      axiosMock.onGet(getSimilarWaiversUrl('violationId')).reply(200, similarWaivers);
      renderComponent({
        ...initState,
        violation: { similarWaivers: [], similarWaiversFilterSelectedIds: new Set(['comment']) },
      });
      await screen.findAllByText('Organization - Org');
      const table = screen.getByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(2);

      const row1Level = rows[0];
      const row1Cells = within(row1Level).getAllByRole('cell');
      expect(row1Cells.length).toBe(2);
      expect(row1Cells[0]).toHaveTextContent('Created2050-12-31Expiration2024-01-12');
      expect(row1Cells[1]).toHaveTextContent(
        'ScopeOrganization - OrgComponentorg.apache.logging.log4j : log4j-core : 2.15.0Conditionsreason 1reason 2Reason—Commentwaiver at org levelAuthorAdmin BuiltIn'
      );

      const row2Level = rows[1];
      const row2Cells = within(row2Level).getAllByRole('cell');
      expect(row2Cells.length).toBe(2);
      expect(row2Cells[0]).toHaveTextContent('Created2024-01-11Expiration2024-01-12');
      expect(row2Cells[1]).toHaveTextContent(
        'ScopeOrganization - OrgComponentorg.apache.logging.log4j : log4j-core : 2.15.0Reason—Commentwaiver at org levelAuthorAdmin BuiltIn'
      );
    });

    it('with similar waivers filtered by exact', async () => {
      const similarWaivers = [
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            policyWaiverId: '1295825fa3585f894b0ab236a489826b',
            createTime: '2023-05-10T15:32:35.849+0000',
          }),
        },
        getSimilarWaiver(),
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            policyWaiverId: '5778bf934d651d0a5694533314c9a9dc',
            createTime: '2050-12-31T15:32:35.849+0000',
            matcherStrategy: 'OTHER STRATEGY',
            constraintFacts: [
              {
                constraintName: 'Some constraint name',
                conditionFacts: [{ reason: 'reason 1' }, { reason: 'reason 2' }],
              },
            ],
          }),
        },
      ];
      axiosMock.onGet(getSimilarWaiversUrl('violationId')).reply(200, similarWaivers);
      renderComponent({
        ...initState,
        violation: { similarWaivers: [], similarWaiversFilterSelectedIds: new Set(['exact']) },
      });
      await screen.findAllByText('Organization - Org');
      const table = screen.getByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(2);

      const row1Level = rows[0];
      const row1Cells = within(row1Level).getAllByRole('cell');
      expect(row1Cells.length).toBe(2);
      expect(row1Cells[0]).toHaveTextContent('Created2024-01-11Expiration2024-01-12');
      expect(row1Cells[1]).toHaveTextContent(
        'ScopeOrganization - OrgComponentorg.apache.logging.log4j : log4j-core : 2.15.0Reason—Commentwaiver at org levelAuthorAdmin BuiltIn'
      );

      const row2Level = rows[1];
      const row2Cells = within(row2Level).getAllByRole('cell');
      expect(row2Cells.length).toBe(2);
      expect(row2Cells[0]).toHaveTextContent('Created2023-05-10Expiration2024-01-12');
      expect(row2Cells[1]).toHaveTextContent(
        'ScopeOrganization - OrgComponentorg.apache.logging.log4j : log4j-core : 2.15.0Reason—Commentwaiver at org levelAuthorAdmin BuiltIn'
      );
    });

    it('as firewall with unknown component', async () => {
      const similarWaivers = [
        {
          ...mergeDeepRight(getSimilarWaiver(), {
            displayName: null,
          }),
        },
      ];
      axiosMock.onGet(getSimilarWaiversUrl('violationId')).reply(200, similarWaivers);
      renderComponent({
        router: {
          currentState: {
            name: 'firewall',
          },
          currentParams: {
            componentDisplayName: 'firewall component name',
          },
        },
        componentDetailsPolicyViolations: { selectedPolicyViolation: { policyViolationId: 'violationId' } },
      });
      await screen.findAllByText('Organization - Org');
      const table = screen.getByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(1);

      const row1Level = rows[0];
      const row1Cells = within(row1Level).getAllByRole('cell');
      expect(row1Cells.length).toBe(2);
      expect(row1Cells[0]).toHaveTextContent('Created2024-01-11Expiration2024-01-12');
      expect(row1Cells[1]).toHaveTextContent(
        'ScopeOrganization - OrgComponentfirewall component nameReason—Commentwaiver at org levelAuthorAdmin BuiltIn'
      );
    });
  });
});

function getSimilarWaiver() {
  return {
    policyWaiverId: 'cc5ee63a42e54f77ae15119c22608df5',
    policyViolationId: 'd8a5383d7fc54451a7c6d94d5d47b3fb',
    comment: 'waiver at org level',
    createTime: '2024-01-11T15:32:35.849+0000',
    expiryTime: '2024-01-12T05:00:00.000+0000',
    scopeOwnerType: 'organization',
    scopeOwnerId: 'c2636d3dc94f4c0ea219c5f054d6e0a9',
    scopeOwnerName: 'Org',
    hash: 'ba55c13d7ac2fd44df9c',
    policyId: 'a619d895ab4840b9b2d207e6c92ce797',
    vulnerabilityId: 'CVE-2021-44228',
    creatorId: 'admin',
    creatorName: 'Admin BuiltIn',
    matcherStrategy: 'EXACT_COMPONENT',
    associatedPackageUrl: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.15.0?type=jar',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'log4j-core',
        extension: 'jar',
        groupId: 'org.apache.logging.log4j',
        version: '2.15.0',
      },
    },
    displayName: {
      parts: [
        {
          field: 'Group',
          value: 'org.apache.logging.log4j',
        },
        {
          value: ' : ',
        },
        {
          field: 'Artifact',
          value: 'log4j-core',
        },
        {
          value: ' : ',
        },
        {
          field: 'Version',
          value: '2.15.0',
        },
      ],
      name: 'log4j-core',
    },
    constraintFacts: [{ constraintName: 'Some constraint name', conditionFacts: [] }],
  };
}
