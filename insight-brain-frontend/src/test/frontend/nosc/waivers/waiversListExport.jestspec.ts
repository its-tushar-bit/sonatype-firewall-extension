/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import type { AnaWaiverRow } from 'MainRoot/nosc/waivers/waiversListTypes';
import { buildWaiversCsv } from 'MainRoot/nosc/waivers/waiversListExport';

const ROW: AnaWaiverRow = {
  id: 'w-1',
  policyId: 'p-1',
  policyName: 'Critical CVSS 9+',
  threatLevel: 9,
  reason: 'ok',
  comment: 'note',
  createdAt: '2026-05-01T10:00:00Z',
  expiresAt: '2026-12-31T00:00:00Z',
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

describe('buildWaiversCsv', () => {
  it('exports page rows with headers for fields available on AnaWaiverRow', () => {
    const csv = buildWaiversCsv([ROW]);
    expect(csv).toContain('Waiver ID,Threat Level,Policy');
    expect(csv).toContain('w-1,9,Critical CVSS 9+');
    expect(csv).toContain('Apple - Java');
    // No component column — Ana list rows do not project component identity.
    expect(csv).not.toMatch(/Component/i);
  });
});
