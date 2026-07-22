/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor, within } from '@testing-library/react';
import { act, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { renderNexusOneViolationDetail } from 'TestRoot/nosc/violations/detail/renderNexusOneViolationDetail';
import {
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
  getViolationDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import { classicViolationHref } from 'MainRoot/nosc/violations/detail/violationDetailUtils';
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
    },
  },
  applicationPublicId: 'demo-app',
  applicationName: 'Demo App',
  organizationName: 'Demo Org',
  displayName: { parts: [{ field: 'name', value: 'demo-component' }] },
  hash: 'component-hash',
};

const SECURITY_VIOLATION_FIXTURE: ViolationDetailsDTO = {
  ...VIOLATION_FIXTURE,
  policyThreatCategory: 'security',
  constraintViolations: [
    {
      constraintName: 'Security Risk',
      reasons: [
        {
          reason: 'Known vulnerability',
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

describe('ViolationDetail', () => {
  let axiosMock: any;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    _setBaseUrlForTesting('http://localhost');
  });

  afterAll(() => {
    setBaseUrl();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  it('renders the native overview tab for a violation id', async () => {
    axiosMock.onGet(getViolationDetailsUrl(VIOLATION_ID)).reply(200, VIOLATION_FIXTURE);
    axiosMock.onGet(getApplicableWaiversUrl(VIOLATION_ID)).reply(200, EMPTY_WAIVERS);
    axiosMock.onGet(getApplicationSummaryUrl('demo-app')).reply(200, { id: 'app-internal-1' });
    axiosMock
      .onPut(getPermissionContextTestUrl('application', 'app-internal-1'))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);

    const { router } = renderNexusOneViolationDetail(VIOLATION_ID);

    await waitFor(() => expect(screen.getByTestId('nosc-violation-detail-page')).toBeInTheDocument());

    expect(await screen.findByTestId('nosc-violation-detail-header')).toHaveTextContent(
      'Critical Open Source Policy',
    );
    expect(screen.getByTestId('nosc-violation-detail-overview-tab')).toBeInTheDocument();
    expect(screen.getByTestId('nosc-violation-detail-breadcrumb')).toHaveAttribute(
      'data-violation-id',
      VIOLATION_ID,
    );
    expect(
      within(screen.getByTestId('nosc-violation-detail-breadcrumb')).getByRole('link', { name: 'Violations' }),
    ).toHaveAttribute('href', '#/violations');
    expect(
      within(screen.getByTestId('nosc-violation-detail-overview-tab')).getByRole('link', {
        name: 'View in Classic',
      }),
    ).toHaveAttribute('href', classicViolationHref(VIOLATION_ID));
    expect(router.globals.current.name).toBe('nexusOneViolationDetail.overview');
  });

  it('related-risk context rail marks the violation as current after identity loads', async () => {
    axiosMock.onGet(getViolationDetailsUrl(VIOLATION_ID)).reply(200, VIOLATION_FIXTURE);
    axiosMock.onGet(getApplicableWaiversUrl(VIOLATION_ID)).reply(200, EMPTY_WAIVERS);
    axiosMock.onGet(getApplicationSummaryUrl('demo-app')).reply(200, { id: 'app-internal-1' });
    axiosMock
      .onPut(getPermissionContextTestUrl('application', 'app-internal-1'))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);

    renderNexusOneViolationDetail(VIOLATION_ID);

    await screen.findByTestId('nosc-violation-detail-header');
    const rail = await screen.findByTestId('nosc-violation-detail-context-rail');
    expect(within(rail).getByText('Critical Open Source Policy')).toHaveAttribute('aria-current', 'page');
    expect(
      within(rail).queryByRole('link', { name: 'Critical Open Source Policy' }),
    ).not.toBeInTheDocument();
    expect(within(rail).getByRole('link', { name: 'Demo App' })).toHaveAttribute(
      'href',
      '#/applications/demo-app?stageId=build',
    );
    // Component and Vulnerability detail routes are not registered yet — available
    // placeholders stay non-links.
    expect(within(rail).getByText('demo-component')).toBeInTheDocument();
    expect(within(rail).queryByRole('link', { name: 'demo-component' })).not.toBeInTheDocument();
    expect(within(rail).getByText('Vulnerability')).toBeInTheDocument();
    expect(within(rail).queryByRole('link', { name: 'Vulnerability' })).not.toBeInTheDocument();
  });

  it('hides the vulnerability tab and redirects direct vulnerability navigation for non-security violations', async () => {
    axiosMock.onGet(getViolationDetailsUrl(VIOLATION_ID)).reply(200, VIOLATION_FIXTURE);
    axiosMock.onGet(getApplicableWaiversUrl(VIOLATION_ID)).reply(200, EMPTY_WAIVERS);
    axiosMock.onGet(getApplicationSummaryUrl('demo-app')).reply(200, { id: 'app-internal-1' });
    axiosMock
      .onPut(getPermissionContextTestUrl('application', 'app-internal-1'))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);

    const { router } = renderNexusOneViolationDetail(VIOLATION_ID);
    await screen.findByTestId('nosc-violation-detail-overview-tab');
    await act(() => router.stateService.go('nexusOneViolationDetail.vulnerability', { id: VIOLATION_ID }));

    await screen.findByTestId('nosc-violation-detail-overview-tab');

    expect(screen.queryByTestId('nosc-violation-detail-tab-vulnerability')).not.toBeInTheDocument();
    await waitFor(() => expect(router.globals.current.name).toBe('nexusOneViolationDetail.overview'));
  });

  it('shows the vulnerability tab trigger for security violations', async () => {
    axiosMock.onGet(getViolationDetailsUrl(VIOLATION_ID)).reply(200, SECURITY_VIOLATION_FIXTURE);
    axiosMock.onGet(getApplicableWaiversUrl(VIOLATION_ID)).reply(200, EMPTY_WAIVERS);
    axiosMock.onGet(getApplicationSummaryUrl('demo-app')).reply(200, { id: 'app-internal-1' });
    axiosMock
      .onPut(getPermissionContextTestUrl('application', 'app-internal-1'))
      .reply(200, ['WAIVE_POLICY_VIOLATIONS']);

    renderNexusOneViolationDetail(VIOLATION_ID);

    expect(await screen.findByTestId('nosc-violation-detail-tab-vulnerability')).toBeInTheDocument();
  });
});
