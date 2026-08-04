/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import { axiosMockAdapter, userEvent } from 'TestRoot/SpecUtil';
import { renderNexusOneViolationDetail } from 'TestRoot/nosc/violations/detail/renderNexusOneViolationDetail';
import {
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
  getOwnerContextHierarchyUrl,
  getPermissionContextTestUrl,
  getPolicyWaiverReasonsUrl,
  getViolationDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import { componentDetailHref } from 'MainRoot/nosc/components/detail/componentDetailHref';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import type {
  ApplicableWaiversDTO,
  ViolationDetailsDTO,
} from 'MainRoot/nosc/violations/detail/violationDetailTypes';
import {
  pendingWaiverRequestSessionKey,
  writePendingWaiverRequestSessionFlag,
} from 'MainRoot/nosc/waivers/waiverActionEligibility';

const VIOLATION_ID = 'violation-123';

const VIOLATION_FIXTURE: ViolationDetailsDTO = {
  policyViolationId: VIOLATION_ID,
  policyId: 'policy-1',
  policyName: 'Critical Open Source Policy',
  policyThreatCategory: 'security',
  policyOwner: {
    ownerName: 'Demo Org',
    ownerType: 'organization',
    ownerId: 'org-1',
  },
  threatLevel: 8,
  openTime: '2026-07-18T10:00:00Z',
  stageData: {
    build: {
      mostRecentEvaluationTime: '2026-07-18T10:00:00Z',
      mostRecentScanId: 'scan-1',
    },
  },
  applicationPublicId: 'demo-app',
  applicationName: 'Demo App',
  organizationName: 'Demo Org',
  displayName: { parts: [{ field: 'name', value: 'demo-component' }] },
  hash: 'component-hash',
  reachabilityStatus: 'reachable',
  waived: false,
  constraintViolations: [
    {
      constraintName: 'Critical Security Risk',
      reasons: [
        {
          reason: 'CVSS score is greater than 9',
          reference: { type: 'SECURITY_VULNERABILITY_REFID', value: 'CVE-2026-0001' },
        },
      ],
    },
  ],
};

const EMPTY_WAIVERS: ApplicableWaiversDTO = {
  activeWaivers: [],
  expiredWaivers: [],
};

function mockViolationRequests({
  hasWaiverPermission = true,
  violation = VIOLATION_FIXTURE,
  waivers = EMPTY_WAIVERS,
}: {
  readonly hasWaiverPermission?: boolean;
  readonly violation?: ViolationDetailsDTO;
  readonly waivers?: ApplicableWaiversDTO;
} = {}) {
  const axiosMock = axiosMockAdapter();
  axiosMock.onGet(getViolationDetailsUrl(VIOLATION_ID)).reply(200, violation);
  axiosMock.onGet(getApplicableWaiversUrl(VIOLATION_ID)).reply(200, waivers);
  axiosMock.onGet(getApplicationSummaryUrl('demo-app')).reply(200, { id: 'app-internal-1' });
  axiosMock
    .onPut(getPermissionContextTestUrl('application', 'app-internal-1'))
    .reply(200, hasWaiverPermission ? ['WAIVE_POLICY_VIOLATIONS'] : []);
  if (violation.policyId) {
    axiosMock
      .onGet(getOwnerContextHierarchyUrl('application', violation.applicationPublicId, violation.policyId))
      .reply(200, {
        type: 'organization',
        id: 'org-1',
        name: 'Demo Org',
        children: [{ type: 'application', id: 'app-1', name: 'Demo App', children: [] }],
      });
    axiosMock.onGet(getPolicyWaiverReasonsUrl()).reply(200, []);
  }
  return axiosMock;
}

function renderOverview({
  hasWaiverPermission,
  productFeatures = {},
  productLicense,
  violation,
  waivers,
}: {
  readonly hasWaiverPermission?: boolean;
  readonly productFeatures?: Record<string, boolean>;
  readonly productLicense?: { license: { products: string[] } };
  readonly violation?: ViolationDetailsDTO;
  readonly waivers?: ApplicableWaiversDTO;
} = {}) {
  mockViolationRequests({ hasWaiverPermission, violation, waivers });
  return renderNexusOneViolationDetail(VIOLATION_ID, {
    preloadedState: {
      productFeatures: {
        productFeatures,
      },
      ...(productLicense ? { productLicense } : {}),
    },
  });
}

describe('OverviewTab', () => {
  beforeAll(() => {
    installRadixJsdomShims();
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  it('shows the policy decision core with constraints and NOUX entity links', async () => {
    renderOverview();

    const overview = await screen.findByTestId('nosc-violation-detail-overview-tab');

    expect(overview).toHaveTextContent('Critical Open Source Policy');
    expect(screen.getByTestId('violation-threat-badge')).toHaveAccessibleName('Threat level 8');
    expect(overview).toHaveTextContent('Open');
    expect(within(overview).getByRole('link', { name: 'Demo App' })).toHaveAttribute(
      'href',
      '#/applications/demo-app',
    );
    expect(within(overview).getByRole('link', { name: 'demo-component' })).toHaveAttribute(
      'href',
      componentDetailHref('demo-app', 'component-hash', 'scan-1'),
    );
    expect(within(overview).queryByRole('link', { name: /classic/i })).not.toBeInTheDocument();
    expect(overview).toHaveTextContent('Critical Security Risk');
    expect(overview).toHaveTextContent('CVSS score is greater than 9');
    expect(within(overview).getByText('reachable')).toBeInTheDocument();
    expect(overview).not.toHaveTextContent('Reachability: reachable');
  });

  it('opens Create Waiver modal when the user has waiver permission', async () => {
    renderOverview();

    await screen.findByTestId('nosc-violation-detail-overview-tab');
    await waitFor(() => expect(screen.getByTestId('nosc-violation-detail-add-waiver')).toBeEnabled());
    await userEvent.click(screen.getByTestId('nosc-violation-detail-add-waiver'));

    expect(await screen.findByTestId('create-waiver-modal')).toBeInTheDocument();
  });

  it('falls back to Classic addWaiver when policyId is absent', async () => {
    mockViolationRequests({
      violation: { ...VIOLATION_FIXTURE, policyId: undefined as unknown as string },
    });
    const { router } = renderNexusOneViolationDetail(VIOLATION_ID, {
      preloadedState: { productFeatures: { productFeatures: {} } },
    });
    const goSpy = jest.spyOn(router.stateService, 'go').mockImplementation(jest.fn());

    await screen.findByTestId('nosc-violation-detail-overview-tab');
    await waitFor(() => expect(screen.getByTestId('nosc-violation-detail-add-waiver')).toBeEnabled());
    await userEvent.click(screen.getByTestId('nosc-violation-detail-add-waiver'));

    expect(screen.queryByTestId('create-waiver-modal')).not.toBeInTheDocument();
    expect(goSpy).toHaveBeenCalledWith('addWaiver', { violationId: VIOLATION_ID });
    goSpy.mockRestore();
  });

  it('shows disabled Create Waiver with permission reason when the user cannot waive', async () => {
    renderOverview({ hasWaiverPermission: false });

    await screen.findByTestId('nosc-violation-detail-overview-tab');
    await waitFor(() =>
      expect(screen.getByTestId('nosc-violation-detail-add-waiver')).toBeDisabled(),
    );
    const addWaiverButton = screen.getByTestId('nosc-violation-detail-add-waiver');
    expect(addWaiverButton).toHaveAttribute(
      'data-disabled-reason',
      "You don't have permission to create waivers",
    );
    expect(addWaiverButton).toHaveAccessibleName(/don't have permission/i);
  });

  it('opens Request Waiver modal when the workflow is enabled without waiver permission', async () => {
    renderOverview({
      hasWaiverPermission: false,
      productFeatures: {
        'waiver-request-workflow-enabled': true,
        'waiver-request-workflow': true,
      },
    });

    await screen.findByTestId('nosc-violation-detail-overview-tab');
    await waitFor(() =>
      expect(screen.getByTestId('nosc-violation-detail-add-waiver')).toBeDisabled(),
    );
    await waitFor(() =>
      expect(screen.getByTestId('nosc-violation-detail-request-waiver')).toBeEnabled(),
    );
    await userEvent.click(screen.getByTestId('nosc-violation-detail-request-waiver'));

    expect(await screen.findByTestId('request-waiver-modal')).toBeInTheDocument();
  });

  it('disables Create and Request when the violation is already waived', async () => {
    renderOverview({
      hasWaiverPermission: true,
      productFeatures: {
        'waiver-request-workflow-enabled': true,
        'waiver-request-workflow': true,
      },
      violation: { ...VIOLATION_FIXTURE, waived: true },
      waivers: {
        activeWaivers: [
          {
            policyWaiverId: 'waiver-1',
            policyId: 'policy-1',
            scopeOwnerType: 'application',
            scopeOwnerId: 'app-1',
            scopeOwnerName: 'Demo App',
          },
        ],
        expiredWaivers: [],
      },
    });

    await screen.findByTestId('nosc-violation-detail-overview-tab');
    await waitFor(() =>
      expect(screen.getByTestId('nosc-violation-detail-add-waiver')).toBeDisabled(),
    );
    await waitFor(() =>
      expect(screen.getByTestId('nosc-violation-detail-request-waiver')).toBeDisabled(),
    );
    expect(screen.getByTestId('nosc-violation-detail-add-waiver')).toHaveAttribute(
      'data-disabled-reason',
      'This violation is already waived',
    );
    expect(screen.getByTestId('nosc-violation-detail-request-waiver')).toHaveAttribute(
      'data-disabled-reason',
      'This violation is already waived',
    );
  });

  it('disables Request Waiver when a pending request is remembered in sessionStorage', async () => {
    sessionStorage.removeItem(pendingWaiverRequestSessionKey(VIOLATION_ID));
    writePendingWaiverRequestSessionFlag(VIOLATION_ID);

    renderOverview({
      hasWaiverPermission: true,
      productFeatures: {
        'waiver-request-workflow-enabled': true,
        'waiver-request-workflow': true,
      },
    });

    await screen.findByTestId('nosc-violation-detail-overview-tab');
    await waitFor(() =>
      expect(screen.getByTestId('nosc-violation-detail-request-waiver')).toBeDisabled(),
    );
    expect(screen.getByTestId('nosc-violation-detail-request-waiver')).toHaveAttribute(
      'data-disabled-reason',
      'A waiver request already exists for this violation',
    );
  });

  it('shows gated Request Waiver when workflow is enabled without entitlement', async () => {
    const { router } = renderOverview({
      hasWaiverPermission: false,
      productFeatures: {
        'waiver-request-workflow-enabled': true,
      },
      productLicense: {
        license: {
          products: ['Sonatype Lifecycle Pro'],
        },
      },
    });
    const goSpy = jest.spyOn(router.stateService, 'go').mockImplementation(jest.fn());

    const requestWaiverButton = await screen.findByTestId('nosc-violation-detail-request-waiver');
    expect(requestWaiverButton).toBeDisabled();
    expect(requestWaiverButton).toHaveAccessibleName(/Enterprise feature/i);

    await userEvent.click(requestWaiverButton);

    expect(goSpy).not.toHaveBeenCalledWith('requestWaiver', { violationId: VIOLATION_ID });
    goSpy.mockRestore();
  });
});
