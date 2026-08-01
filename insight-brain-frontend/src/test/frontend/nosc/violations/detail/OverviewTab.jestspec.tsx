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
  getPermissionContextTestUrl,
  getViolationDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import { componentDetailHref } from 'MainRoot/nosc/components/detail/componentDetailHref';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';
import type {
  ApplicableWaiversDTO,
  ViolationDetailsDTO,
} from 'MainRoot/nosc/violations/detail/violationDetailTypes';

const VIOLATION_ID = 'violation-123';

const VIOLATION_FIXTURE: ViolationDetailsDTO = {
  policyViolationId: VIOLATION_ID,
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
}: {
  readonly hasWaiverPermission?: boolean;
  readonly violation?: ViolationDetailsDTO;
} = {}) {
  const axiosMock = axiosMockAdapter();
  axiosMock.onGet(getViolationDetailsUrl(VIOLATION_ID)).reply(200, violation);
  axiosMock.onGet(getApplicableWaiversUrl(VIOLATION_ID)).reply(200, EMPTY_WAIVERS);
  axiosMock.onGet(getApplicationSummaryUrl('demo-app')).reply(200, { id: 'app-internal-1' });
  axiosMock
    .onPut(getPermissionContextTestUrl('application', 'app-internal-1'))
    .reply(200, hasWaiverPermission ? ['WAIVE_POLICY_VIOLATIONS'] : []);
  return axiosMock;
}

function renderOverview({
  hasWaiverPermission,
  productFeatures = {},
  productLicense,
}: {
  readonly hasWaiverPermission?: boolean;
  readonly productFeatures?: Record<string, boolean>;
  readonly productLicense?: { license: { products: string[] } };
} = {}) {
  mockViolationRequests({ hasWaiverPermission });
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
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  afterEach(() => {
    jest.restoreAllMocks();
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

  it('routes to Add Waiver when the user has waiver permission', async () => {
    const { router } = renderOverview();
    const goSpy = jest.spyOn(router.stateService, 'go').mockImplementation(jest.fn());

    const addWaiverButton = await screen.findByTestId('nosc-violation-detail-add-waiver');
    await waitFor(() => expect(addWaiverButton).toBeEnabled());
    await userEvent.click(addWaiverButton);

    expect(goSpy).toHaveBeenCalledWith('addWaiver', { violationId: VIOLATION_ID });
  });

  it('hides Add Waiver when the user does not have waiver permission', async () => {
    renderOverview({ hasWaiverPermission: false });

    await screen.findByTestId('nosc-violation-detail-overview-tab');

    expect(screen.queryByTestId('nosc-violation-detail-add-waiver')).not.toBeInTheDocument();
  });

  it('routes to Request Waiver when the workflow is enabled without waiver permission', async () => {
    const { router } = renderOverview({
      hasWaiverPermission: false,
      productFeatures: {
        'waiver-request-workflow-enabled': true,
        'waiver-request-workflow': true,
      },
    });
    const goSpy = jest.spyOn(router.stateService, 'go').mockImplementation(jest.fn());

    const requestWaiverButton = await screen.findByTestId('nosc-violation-detail-request-waiver');
    await userEvent.click(requestWaiverButton);

    expect(screen.queryByTestId('nosc-violation-detail-add-waiver')).not.toBeInTheDocument();
    expect(goSpy).toHaveBeenCalledWith('requestWaiver', { violationId: VIOLATION_ID });
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
    expect(requestWaiverButton).toHaveAccessibleName('Request Waiver (Enterprise Feature)');

    await userEvent.click(requestWaiverButton);

    expect(goSpy).not.toHaveBeenCalledWith('requestWaiver', { violationId: VIOLATION_ID });
  });
});
