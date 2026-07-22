/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within } from '@testing-library/react';
import { axiosMockAdapter, userEvent } from 'TestRoot/SpecUtil';
import { renderNexusOneViolationDetail } from 'TestRoot/nosc/violations/detail/renderNexusOneViolationDetail';
import {
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
  getViolationDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import { _setBaseUrlForTesting, setBaseUrl } from 'MainRoot/util/urlUtil';
import type {
  ApplicableWaiversDTO,
  ViolationDetailsDTO,
} from 'MainRoot/nosc/violations/detail/violationDetailTypes';

const VIOLATION_ID = 'violation-123';

const VIOLATION_FIXTURE: ViolationDetailsDTO = {
  policyViolationId: VIOLATION_ID,
  policyName: 'Critical Open Source Policy',
  policyThreatCategory: 'license',
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
};

const ACTIVE_APP_WAIVER = {
  policyWaiverId: 'waiver-active-app',
  scopeOwnerType: 'application',
  scopeOwnerId: 'demo-app',
  scopeOwnerName: 'Demo App',
  policyId: 'policy-1',
  expiryTime: '2026-08-01T00:00:00Z',
  comment: 'Accepted through the next release.',
};

const ACTIVE_ORG_WAIVER = {
  policyWaiverId: 'waiver-active-org',
  scopeOwnerType: 'organization',
  scopeOwnerId: 'org-1',
  scopeOwnerName: 'Demo Org',
  policyId: 'policy-1',
  expiryTime: null,
  comment: 'Parent org exception.',
};

const EXPIRED_WAIVER = {
  policyWaiverId: 'waiver-expired',
  scopeOwnerType: 'application',
  scopeOwnerId: 'demo-app',
  scopeOwnerName: 'Demo App',
  policyId: 'policy-1',
  expiryTime: '2026-01-01T00:00:00Z',
  comment: 'Old release exception.',
};

const POPULATED_WAIVERS: ApplicableWaiversDTO = {
  activeWaivers: [ACTIVE_APP_WAIVER, ACTIVE_ORG_WAIVER],
  expiredWaivers: [EXPIRED_WAIVER],
};

const EMPTY_WAIVERS: ApplicableWaiversDTO = {
  activeWaivers: [],
  expiredWaivers: [],
};

function mockViolationRequests(waivers: ApplicableWaiversDTO) {
  const axiosMock = axiosMockAdapter();
  axiosMock.onGet(getViolationDetailsUrl(VIOLATION_ID)).reply(200, VIOLATION_FIXTURE);
  axiosMock.onGet(getApplicableWaiversUrl(VIOLATION_ID)).reply(200, waivers);
  axiosMock.onGet(getApplicationSummaryUrl('demo-app')).reply(200, { id: 'app-internal-1' });
  axiosMock
    .onPut(getPermissionContextTestUrl('application', 'app-internal-1'))
    .reply(200, ['WAIVE_POLICY_VIOLATIONS']);
}

async function renderWaiversTab(waivers: ApplicableWaiversDTO) {
  mockViolationRequests(waivers);
  renderNexusOneViolationDetail(VIOLATION_ID);

  await screen.findByTestId('nosc-violation-detail-header');
  await userEvent.click(screen.getByTestId('nosc-violation-detail-tab-waivers'));

  return screen.findByTestId('nosc-violation-detail-waivers-tab');
}

describe('WaiversTab', () => {
  beforeAll(() => {
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('shows an empty applicable-waivers state', async () => {
    const waiversTab = await renderWaiversTab(EMPTY_WAIVERS);

    expect(screen.getByTestId('nosc-violation-detail-tab-waivers')).toHaveTextContent('0');
    expect(waiversTab).toHaveTextContent('No applicable waivers');
    expect(waiversTab).toHaveTextContent('Active and expired waivers that apply to this violation will appear here.');
  });

  it('shows active and expired applicable waivers with an active-waiver badge count', async () => {
    const waiversTab = await renderWaiversTab(POPULATED_WAIVERS);

    expect(screen.getByTestId('nosc-violation-detail-tab-waivers')).toHaveTextContent('2');
    expect(waiversTab).toHaveTextContent('2 active');
    expect(waiversTab).toHaveTextContent('1 expired');

    const activeTable = within(waiversTab).getByTestId('nosc-violation-detail-active-waivers-table');
    expect(within(activeTable).getByRole('columnheader', { name: 'Owner' })).toBeInTheDocument();
    expect(within(activeTable).getByRole('columnheader', { name: 'Scope' })).toBeInTheDocument();
    expect(within(activeTable).getByRole('columnheader', { name: 'Expiry' })).toBeInTheDocument();
    expect(within(activeTable).getByRole('columnheader', { name: 'Comment' })).toBeInTheDocument();
    expect(within(activeTable).getByRole('columnheader', { name: 'Actions' })).toBeInTheDocument();
    expect(activeTable).toHaveTextContent('Demo App');
    expect(activeTable).toHaveTextContent('Application');
    expect(activeTable).toHaveTextContent('2026-08-01');
    expect(activeTable).toHaveTextContent('Accepted through the next release.');
    expect(activeTable).toHaveTextContent('Never');
    const detailLinks = within(activeTable).getAllByTestId(
      'nosc-violation-detail-active-waivers-table-row-detail-link',
    );
    expect(detailLinks).toHaveLength(2);
    expect(detailLinks[0]).toHaveAttribute(
      'href',
      expect.stringContaining('/waivers/application/demo-app/waiver-active-app'),
    );

    const expiredTable = within(waiversTab).getByTestId('nosc-violation-detail-expired-waivers-table');
    expect(expiredTable).toHaveTextContent('Old release exception.');
  });
});
