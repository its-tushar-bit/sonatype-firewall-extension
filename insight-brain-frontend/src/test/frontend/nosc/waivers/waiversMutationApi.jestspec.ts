/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  createPolicyWaiver,
  createPolicyWaiverRequest,
  deletePolicyWaiver,
  expiryDateToIsoEndOfDay,
  fetchWaiverScopeTargets,
  reviewPolicyWaiverRequest,
  updatePolicyWaiver,
  withdrawPolicyWaiverRequest,
} from 'MainRoot/nosc/waivers/waiversMutationApi';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('waiversMutationApi', () => {
  beforeEach(() => {
    mockedAxios.get.mockReset();
    mockedAxios.post.mockReset();
    mockedAxios.put.mockReset();
    mockedAxios.delete.mockReset();
  });

  it('formats expiry dates as local end-of-day ISO', () => {
    const iso = expiryDateToIsoEndOfDay('2026-12-31');
    expect(iso).toMatch(/^2026-12-31T23:59:59\.999[+-]\d{4}$/);
  });

  it('flattens owner hierarchy into selectable scopes (child first)', async () => {
    mockedAxios.get.mockResolvedValue({
      data: {
        type: 'organization',
        id: 'org-1',
        name: 'Root Org',
        children: [{ type: 'application', id: 'app-1', name: 'App One', children: [] }],
      },
    });
    const scopes = await fetchWaiverScopeTargets({
      ownerType: 'application',
      ownerId: 'app-public',
      policyId: 'policy-1',
    });
    expect(scopes.map((scope) => scope.ownerId)).toEqual(['app-1', 'org-1']);
  });

  it('POSTs create waiver options to the v2 policyWaivers path', async () => {
    mockedAxios.post.mockResolvedValue({ data: null });
    await createPolicyWaiver({
      ownerType: 'application',
      ownerId: 'app-1',
      policyViolationId: 'viol-1',
      options: {
        comment: 'Approved',
        matcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
        expireWhenRemediationAvailable: false,
      },
    });
    expect(mockedAxios.post).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/policyWaivers/application/app-1/viol-1'),
      expect.objectContaining({
        comment: 'Approved',
        matcherStrategy: 'EXACT_COMPONENT',
      }),
    );
  });

  it('POSTs create request and returns the response body', async () => {
    mockedAxios.post.mockResolvedValue({
      data: { policyWaiverRequestId: 'req-1', status: 'REQUESTED' },
    });
    const created = await createPolicyWaiverRequest({
      ownerType: 'organization',
      ownerId: 'org-1',
      policyViolationId: 'viol-1',
      options: {
        comment: 'Please approve',
        matcherStrategy: 'ALL_VERSIONS',
        noteToReviewer: 'Urgent',
      },
    });
    expect(created.policyWaiverRequestId).toBe('req-1');
    expect(mockedAxios.post).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/policyWaiverRequests/organization/org-1/policyViolation/viol-1'),
      expect.objectContaining({ noteToReviewer: 'Urgent' }),
    );
  });

  it('PUTs update / DELETEs waiver / reviews / withdraws request', async () => {
    mockedAxios.put.mockResolvedValue({ data: null });
    mockedAxios.delete.mockResolvedValue({ data: null });
    mockedAxios.post.mockResolvedValue({ data: { status: 'APPROVED' } });

    await updatePolicyWaiver({
      ownerType: 'application',
      ownerId: 'app-1',
      policyWaiverId: 'w-1',
      options: { matcherStrategy: 'EXACT_COMPONENT', expiryTime: '2027-01-01T00:00:00.000+0000' },
    });
    await deletePolicyWaiver({
      ownerType: 'application',
      ownerId: 'app-1',
      policyWaiverId: 'w-1',
    });
    await reviewPolicyWaiverRequest({
      ownerType: 'application',
      ownerId: 'app-1',
      policyWaiverRequestId: 'req-1',
      review: { status: 'APPROVED', matcherStrategy: 'EXACT_COMPONENT' },
    });
    await withdrawPolicyWaiverRequest({
      ownerType: 'application',
      ownerId: 'app-1',
      policyWaiverRequestId: 'req-1',
    });

    expect(mockedAxios.put).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/policyWaivers/application/app-1/w-1'),
      expect.any(Object),
    );
    expect(mockedAxios.delete).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/policyWaivers/application/app-1/w-1'),
    );
    expect(mockedAxios.post).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/policyWaiverRequests/application/app-1/review/req-1'),
      expect.objectContaining({ status: 'APPROVED' }),
    );
    expect(mockedAxios.delete).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/policyWaiverRequests/application/app-1/req-1'),
    );
  });
});
