/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { axiosMockAdapter, render, screen, waitFor, within } from 'TestRoot/SpecUtil';
import ListWaiversTable from 'MainRoot/waivers/ListWaiversTable';
import * as waiverActions from 'MainRoot/waivers/waiverActions';
import { getApplicableWaiversUrl } from 'MainRoot/util/CLMLocation';

describe('ListWaiversTable', () => {
  const minimalProps = {
    violationDetails: getViolationDetailsProp(),
  };
  const renderComponent = (preloadedState) => render(<ListWaiversTable {...minimalProps} />, { preloadedState });

  let axiosMock;
  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  describe('renders a table', () => {
    it('with headers', async () => {
      renderComponent();
      const table = await screen.findByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      expect(groups.length).toBe(2);

      const headers = within(groups[0]).getAllByRole('columnheader');
      expect(headers.length).toBe(3);
      expect(within(headers[0]).getByText('DURATION')).toBeVisible();
      expect(within(headers[1]).getByText('WAIVER DETAILS')).toBeVisible();
      // final header (headers[6]) is empty to accommodate the delete icon
    });

    it('with message if there are no waivers to display', async () => {
      renderComponent();
      const table = await screen.findByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      expect(groups.length).toBe(2);

      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(1);
      expect(
        within(rows[0]).getByText("You don't have any waivers: to learn more about waivers you can check our")
      ).toBeVisible();

      const helpDocsLink = within(rows[0]).getByRole('link', { name: 'help documentation.' });
      expect(helpDocsLink).toBeVisible();
      expect(helpDocsLink).toHaveAttribute('href', 'https://links.sonatype.com/products/nxiq/doc/waivers');
    });

    it('with a loading spinner if applicable waivers are loading', () => {
      renderComponent({ violation: { loadingApplicableWaivers: true } });

      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });

    describe('with error handling', () => {
      it('with an error and retry button if something failed loading applicable waivers', async () => {
        axiosMock.onGet(getApplicableWaiversUrl(minimalProps.violationDetails.policyViolationId)).reply(200, {
          activeWaivers: [{ ...getBasicWaiverData() }],
          expiredWaivers: [],
        });
        renderComponent({ violation: { loadApplicableWaiversError: 'Error' } });

        await waitFor(() => screen.getByText(/An error occurred loading data/));
        const retryButton = screen.getByRole('button', { name: 'Retry' });
        expect(retryButton).toBeVisible();
        retryButton.click();

        const table = await screen.findByRole('table');
        const groups = within(table).getAllByRole('rowgroup');
        const rows = within(groups[1]).getAllByRole('row');
        expect(rows.length).toBe(1);
        expect(within(rows[0]).getByText('updated waiver')).toBeVisible();

        expect(screen.queryByText(/An error occurred loading data/)).not.toBeInTheDocument();
      });
    });

    it('with a row per active or expired waiver', async () => {
      const expectedFirstWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id1',
        createTime: '2023-12-28T18:29:30.649+0000',
        comment: 'waiver at app level',
        reasonText: 'Mitigated externally',
      };
      const expectedSecondWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id2',
        createTime: '2023-12-14T18:29:30.649+0000',
        comment: 'waiver at org level',
        scopeOwnerType: 'organization',
        scopeOwnerName: 'main org',
        expiryTime: '2023-12-21T18:29:30.649+0000',
        creatorName: 'Vesper Noir',
      };
      renderComponent({ violation: { activeWaivers: [expectedFirstWaiver], expiredWaivers: [expectedSecondWaiver] } });

      const table = await screen.findByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(2);

      const rowAtAppLevel = rows[0];
      expect(within(rowAtAppLevel).getByText('2023-12-28')).toBeVisible();
      expect(within(rowAtAppLevel).getByText('Admin BuiltIn')).toBeVisible();
      expect(within(rowAtAppLevel).getByText('Application - app2')).toBeVisible();
      expect(
        within(rowAtAppLevel).getByText('org.springframework.security : spring-security-config : 5.2.0.RELEASE')
      ).toBeVisible();
      expect(within(rowAtAppLevel).getByText('Does not expire')).toBeVisible();
      expect(within(rowAtAppLevel).getByText('waiver at app level')).toBeVisible();
      expect(within(rowAtAppLevel).getByText('Mitigated externally')).toBeVisible();

      const rowAtOrgLevel = rows[1];
      expect(within(rowAtOrgLevel).getByText('2023-12-14')).toBeVisible();
      expect(within(rowAtOrgLevel).getByText('Vesper Noir')).toBeVisible();
      expect(within(rowAtOrgLevel).getByText('Organization - main org')).toBeVisible();
      expect(
        within(rowAtOrgLevel).getByText('org.springframework.security : spring-security-config : 5.2.0.RELEASE')
      ).toBeVisible();
      expect(within(rowAtOrgLevel).getByText('2023-12-21')).toBeVisible();
      expect(within(rowAtOrgLevel).getByText('waiver at org level')).toBeVisible();
      expect(within(rowAtOrgLevel).getByText('Reason')).toBeVisible();
      expect(within(rowAtOrgLevel).getByText('—')).toBeVisible();
    });

    it('with active waivers in descending order by expiry time', async () => {
      const expectedFirstWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id1',
        createTime: '2023-12-28T18:29:30.649+0000',
      };
      const expectedSecondWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id2',
        createTime: '2023-12-14T18:29:30.649+0000',
      };
      const expectedThirdWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id3',
        createTime: '2023-12-01T18:29:30.649+0000',
      };

      renderComponent({
        violation: { activeWaivers: [expectedSecondWaiver, expectedThirdWaiver, expectedFirstWaiver] },
      });

      const table = await screen.findByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(3);

      expect(within(rows[0]).getByText('2023-12-28')).toBeVisible();
      expect(within(rows[1]).getByText('2023-12-14')).toBeVisible();
      expect(within(rows[2]).getByText('2023-12-01')).toBeVisible();
    });

    it('with expired waivers in descending order by expiry time after the active waivers', async () => {
      const expectedActiveWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'idActive',
        createTime: '2023-12-31T18:29:30.649+0000',
      };
      const expectedFirstWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id1',
        createTime: '2023-12-28T18:29:30.649+0000',
      };
      const expectedSecondWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id2',
        createTime: '2023-12-14T18:29:30.649+0000',
      };
      const expectedThirdWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id3',
        createTime: '2023-12-01T18:29:30.649+0000',
      };

      renderComponent({
        violation: {
          activeWaivers: [expectedActiveWaiver],
          expiredWaivers: [expectedSecondWaiver, expectedThirdWaiver, expectedFirstWaiver],
        },
      });

      const table = await screen.findByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(4);

      expect(within(rows[0]).getByText('2023-12-31')).toBeVisible();
      expect(within(rows[1]).getByText('2023-12-28')).toBeVisible();
      expect(within(rows[2]).getByText('2023-12-14')).toBeVisible();
      expect(within(rows[3]).getByText('2023-12-01')).toBeVisible();
    });

    it('with different component name per row depending on the matching strategy', async () => {
      const expectedFirstWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id1',
        createTime: '2023-12-28T18:29:30.649+0000',
        matcherStrategy: 'EXACT_COMPONENT',
      };
      const expectedSecondWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id2',
        createTime: '2023-12-14T18:29:30.649+0000',
        matcherStrategy: 'ALL_COMPONENTS',
      };
      const expectedThirdWaiver = {
        ...getBasicWaiverData(),
        policyWaiverId: 'id3',
        createTime: '2023-12-01T18:29:30.649+0000',
        matcherStrategy: 'ALL_VERSIONS',
      };

      renderComponent({
        violation: { activeWaivers: [expectedFirstWaiver, expectedSecondWaiver, expectedThirdWaiver] },
      });

      const table = await screen.findByRole('table');
      const groups = within(table).getAllByRole('rowgroup');
      const rows = within(groups[1]).getAllByRole('row');
      expect(rows.length).toBe(3);

      expect(
        within(rows[0]).getByText('org.springframework.security : spring-security-config : 5.2.0.RELEASE')
      ).toBeVisible();

      expect(
        within(rows[0]).getByText('org.springframework.security : spring-security-config : 5.2.0.RELEASE')
      ).toBeVisible();

      expect(within(rows[1]).getByText('All')).toBeVisible();

      expect(
        within(rows[2]).getByText('org.springframework.security : spring-security-config (all versions)')
      ).toBeVisible();
    });

    describe('with a delete option per row', () => {
      let setWaiverToDeleteSpy;
      beforeEach(() => {
        setWaiverToDeleteSpy = jest.spyOn(waiverActions, 'setWaiverToDelete');
      });
      afterEach(() => {
        // this is required to prevent failure on other tests that rely on the original action
        // this is most likely due to the way actions are imported for the spying in this test
        setWaiverToDeleteSpy.mockRestore();
      });

      it('with a delete button per row that will trigger a delete confirmation', async () => {
        const expectedActiveWaiver = {
          ...getBasicWaiverData(),
        };
        renderComponent({ violation: { activeWaivers: [expectedActiveWaiver] } });

        expect(await screen.queryByRole('dialog')).not.toBeInTheDocument();

        const table = await screen.findByRole('table');
        const groups = within(table).getAllByRole('rowgroup');
        const rows = within(groups[1]).getAllByRole('row');
        expect(rows.length).toBe(1);

        const rowDeleteButton = within(rows[0]).getByRole('button');
        expect(rowDeleteButton).toBeVisible();
        expect(setWaiverToDeleteSpy).not.toHaveBeenCalled();
        rowDeleteButton.click();
        expect(setWaiverToDeleteSpy).toHaveBeenCalledWith(expectedActiveWaiver);
        const deleteConfirmationModal = await screen.findByRole('dialog');
        expect(deleteConfirmationModal).toBeVisible();
        expect(
          await within(deleteConfirmationModal).getByText('Are you sure you want to delete this waiver?')
        ).toBeVisible();
      });
    });

    describe('with auto waiver', () => {
      it('displays auto waiver information when present', async () => {
        const autoWaiver = getAutoWaiverData();
        renderComponent({
          violation: {
            autoWaiver,
            activeWaivers: [],
            expiredWaivers: [],
          },
        });

        const table = await screen.findByRole('table');
        const groups = within(table).getAllByRole('rowgroup');
        const rows = within(groups[1]).getAllByRole('row');

        expect(rows.length).toBe(1);
        const autoWaiverRow = rows[0];

        expect(within(autoWaiverRow).getByText('2023-12-28')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Application - app2')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Auto')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Any Component')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Current or latest non-violating')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Admin BuiltIn')).toBeVisible();
      });

      it('shows auto waiver before regular waivers', async () => {
        const autoWaiver = getAutoWaiverData();
        const regularWaiver = {
          ...getBasicWaiverData(),
          createTime: '2023-12-29T18:29:30.649+0000', // newer than auto waiver
        };

        renderComponent({
          violation: {
            autoWaiver,
            activeWaivers: [regularWaiver],
            expiredWaivers: [],
          },
        });

        const table = await screen.findByRole('table');
        const groups = within(table).getAllByRole('rowgroup');
        const rows = within(groups[1]).getAllByRole('row');
        expect(rows.length).toBe(2);

        let autoWaiverRow = rows[0];
        let regularWaiverRow = rows[1];
        expect(within(autoWaiverRow).getByText('2023-12-28')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Application - app2')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Auto')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Any Component')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Current or latest non-violating')).toBeVisible();
        expect(within(autoWaiverRow).getByText('Admin BuiltIn')).toBeVisible();

        expect(within(regularWaiverRow).getByText(regularWaiver.comment)).toBeVisible();
        expect(
          within(regularWaiverRow).getByText('org.springframework.security : spring-security-config : 5.2.0.RELEASE')
        ).toBeVisible();
      });

      it('handles loading state for auto waiver', async () => {
        renderComponent({
          violation: {
            loadingAutoWaiver: true,
            loadingApplicableWaivers: false,
          },
        });

        expect(screen.getByText('Loading…')).toBeInTheDocument();
      });

      it('handles error state for auto waiver', async () => {
        renderComponent({
          violation: {
            loadAutoWaiverError: 'Failed to load auto waiver',
            autoWaiver: null,
          },
        });

        await waitFor(() => screen.getByText(/An error occurred loading data/));
        const retryButton = screen.getByRole('button', { name: 'Retry' });
        expect(retryButton).toBeVisible();
      });

      it('shows clog icon for auto waiver', async () => {
        const autoWaiver = getAutoWaiverData();
        renderComponent({
          violation: {
            autoWaiver,
            activeWaivers: [],
            expiredWaivers: [],
          },
        });

        const table = await screen.findByRole('table');
        const groups = within(table).getAllByRole('rowgroup');
        const rows = within(groups[1]).getAllByRole('row');
        const autoWaiverRow = rows[0];

        expect(within(autoWaiverRow).queryByRole('icon')).not.toBeInTheDocument();
      });
    });
  });
});

function getViolationDetailsProp() {
  return {
    policyViolationId: 'dummyPolicyViolationId',
    policyName: 'dummy Policy Name',
    policyThreatCategory: 'security',
    policyOwner: {
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerName: 'Root Organization',
      ownerType: 'organization',
    },
    threatLevel: 9,
    openTime: '2023-11-28T17:44:50.910-07:00',
    stageData: {
      build: {
        mostRecentEvaluationTime: '2023-12-11T14:35:57.931-07:00',
        mostRecentScanId: '8594a2bea4b843538fb872cf3de8fe3a',
        actionTypeId: null,
      },
    },
    applicationPublicId: 'app2',
    applicationName: 'app2',
    organizationName: 'Main org',
    constraintViolations: [
      {
        constraintId: '2cdc959899dd47c8bd82fd10edc3dd0c',
        constraintName: 'Critical risk CVSS score',
        reasons: [
          {
            reason: 'Found security vulnerability CVE-2023-34034 with severity >= 9 (severity = 9.8)',
            reference: {
              type: 'SECURITY_VULNERABILITY_REFID',
              value: 'CVE-2023-34034',
            },
          },
        ],
      },
    ],
    displayName: {
      parts: [
        {
          field: 'Group',
          value: 'org.springframework.security',
        },
        {
          value: ' : ',
        },
        {
          field: 'Artifact',
          value: 'spring-security-config',
        },
        {
          value: ' : ',
        },
        {
          field: 'Version',
          value: '5.2.0.RELEASE',
        },
      ],
      name: 'spring-security-config',
    },
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'spring-security-config',
        classifier: '',
        extension: 'jar',
        groupId: 'org.springframework.security',
        version: '5.2.0.RELEASE',
      },
    },
  };
}

function getBasicWaiverData() {
  return {
    policyWaiverId: '3606146603a543588aa76bba284997c3',
    policyViolationId: '6590f282437347da93f590ed3f65c4f4',
    comment: 'updated waiver',
    createTime: '2023-12-18T18:29:30.649+0000',
    scopeOwnerType: 'application',
    scopeOwnerId: '366f3a50e366482a8b81b54f3152b056',
    scopeOwnerName: 'app2',
    hash: '01f6c413187a55017deb',
    policyId: 'ae268e0c50144e39ab8c0a44da7ca495',
    vulnerabilityId: 'CVE-2023-34034',
    creatorId: 'admin',
    creatorName: 'Admin BuiltIn',
    matcherStrategy: 'EXACT_COMPONENT',
    associatedPackageUrl: 'pkg:maven/org.springframework.security/spring-security-config@5.2.0.RELEASE?type=jar',
    componentIdentifier: {
      format: 'maven',
      coordinates: {
        artifactId: 'spring-security-config',
        extension: 'jar',
        groupId: 'org.springframework.security',
        version: '5.2.0.RELEASE',
      },
    },
    displayName: {
      parts: [
        {
          field: 'Group',
          value: 'org.springframework.security',
        },
        {
          value: ' : ',
        },
        {
          field: 'Artifact',
          value: 'spring-security-config',
        },
        {
          value: ' : ',
        },
        {
          field: 'Version',
          value: '5.2.0.RELEASE',
        },
      ],
      name: 'spring-security-config',
    },
  };
}

function getAutoWaiverData() {
  return {
    autoPolicyWaiverId: 'auto-waiver-123',
    ownerId: '366f3a50e366482a8b81b54f3152b056',
    ownerType: 'application',
    ownerName: 'app2',
    publicId: 'app2-id',
    threatLevel: 9,
    reachable: false,
    pathForward: false,
    creatorId: 'admin',
    creatorName: 'Admin BuiltIn',
    createTime: '2023-12-28T18:29:30.649+0000',
  };
}
