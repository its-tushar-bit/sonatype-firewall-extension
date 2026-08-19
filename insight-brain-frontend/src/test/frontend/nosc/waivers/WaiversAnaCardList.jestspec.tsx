/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within } from '@testing-library/react';
import type { AnaWaiverRow } from 'MainRoot/nosc/waivers/waiversListTypes';
import WaiversAnaCardList from 'MainRoot/nosc/waivers/WaiversAnaCardList';
import { renderNexusOneRoute } from 'TestRoot/nosc/renderNexusOneRoute';

const MANUAL: AnaWaiverRow = {
  id: 'w-1',
  policyId: 'p-1',
  policyName: 'Critical CVSS 9+',
  threatLevel: 9,
  reason: 'Risk accepted by AppSec for the current release train',
  comment: null,
  createdAt: '2026-05-01T10:00:00Z',
  expiresAt: '2030-01-01T00:00:00Z',
  scopeOwnerType: 'application',
  scopeOwnerId: 'app-1',
  waivedBy: 'alice',
  organizationName: 'Java Team',
  organizationId: 'org-1',
  applicationName: 'Apple - Java',
  applicationId: 'app-1',
  isAuto: false,
  isRequested: false,
  status: null,
};

const AUTO: AnaWaiverRow = {
  ...MANUAL,
  id: 'w-auto',
  policyId: null,
  policyName: null,
  threatLevel: 4,
  reason: null,
  expiresAt: null,
  applicationName: null,
  applicationId: null,
  organizationName: 'Platform',
  scopeOwnerType: 'organization',
  scopeOwnerId: 'org-root',
  isAuto: true,
};

describe('WaiversAnaCardList', () => {
  it('renders vision-style cards without a Component line', () => {
    renderNexusOneRoute(
      <WaiversAnaCardList waivers={[MANUAL, AUTO]} linkFrom="waivers-list" />,
      'nexusOneWaivers',
    );

    const list = screen.getByTestId('waivers-ana-list');
    const cards = within(list).getAllByTestId('waivers-ana-list-card');
    expect(cards).toHaveLength(2);

    expect(within(list).getByText('Critical CVSS 9+')).toBeInTheDocument();
    expect(within(list).getByText(/application · Apple - Java/)).toBeInTheDocument();
    expect(within(list).getByText(/organization · Platform/)).toBeInTheDocument();
    expect(within(list).getByText('Auto-Waived')).toBeInTheDocument();
    expect(within(list).getByText('Active')).toBeInTheDocument();
    expect(within(list).queryByText(/Component/i)).not.toBeInTheDocument();

    const links = screen.getAllByTestId('waivers-ana-list-row-detail-link');
    expect(links[0].getAttribute('href')).toContain('/waivers/application/app-1/w-1');
    expect(links[1].getAttribute('href')).toContain('type=autoWaiver');
  });
});
