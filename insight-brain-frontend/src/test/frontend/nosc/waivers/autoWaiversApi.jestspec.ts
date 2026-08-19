/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  createAutoPolicyWaiver,
  createAutoWaiverExclusion,
  deleteAutoPolicyWaiver,
  deleteAutoWaiverExclusion,
  fetchApplicableAutoWaiverForViolation,
  fetchApplicableAutoWaivers,
  fetchAutoWaiverExclusions,
  formatAutoWaiverConditions,
  normalizeAutoWaiverOwnerType,
  updateAutoPolicyWaiver,
} from 'MainRoot/nosc/waivers/autoWaiversApi';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('autoWaiversApi', () => {
  beforeEach(() => {
    mockedAxios.get.mockReset();
    mockedAxios.post.mockReset();
    mockedAxios.put.mockReset();
    mockedAxios.delete.mockReset();
  });

  it('normalizes owner types and formats conditions', () => {
    expect(normalizeAutoWaiverOwnerType('APPLICATION')).toBe('application');
    expect(normalizeAutoWaiverOwnerType('root_organization')).toBe('organization');
    expect(
      formatAutoWaiverConditions({ hasNotReachable: true, hasNoPathForward: true }),
    ).toBe('Not reachable · No path forward');
  });

  it('loads applicable configs with one owner-scoped GET', async () => {
    mockedAxios.get.mockResolvedValue({
      data: [{ autoPolicyWaiverId: 'aw-1', autoPolicyWaiverOwnerId: 'ROOT_ORGANIZATION_ID' }],
    });
    const rows = await fetchApplicableAutoWaivers({
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
    });
    expect(rows).toHaveLength(1);
    expect(mockedAxios.get).toHaveBeenCalledWith(
      expect.stringContaining(
        '/api/v2/autoPolicyWaivers/v2/organization/ROOT_ORGANIZATION_ID/applicableAutoWaivers',
      ),
    );
  });

  it('POSTs create and PUTs update payloads', async () => {
    mockedAxios.post.mockResolvedValue({ data: { autoPolicyWaiverId: 'aw-1' } });
    mockedAxios.put.mockResolvedValue({ data: { autoPolicyWaiverId: 'aw-1' } });
    const body = {
      threatLevel: 5,
      reachability: true,
      pathForward: false,
      scopesOperatorAny: true,
    };
    await createAutoPolicyWaiver({
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
      body,
    });
    expect(mockedAxios.post).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/autoPolicyWaivers/organization/ROOT_ORGANIZATION_ID'),
      body,
    );
    await updateAutoPolicyWaiver({
      ownerType: 'organization',
      ownerId: 'ROOT_ORGANIZATION_ID',
      autoPolicyWaiverId: 'aw-1',
      body,
    });
    expect(mockedAxios.put).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/autoPolicyWaivers/organization/ROOT_ORGANIZATION_ID/aw-1'),
      expect.objectContaining({ autoPolicyWaiverId: 'aw-1', threatLevel: 5 }),
    );
  });

  it('deletes configs and exclusions', async () => {
    mockedAxios.delete.mockResolvedValue({ data: null });
    await deleteAutoPolicyWaiver({
      ownerType: 'application',
      ownerId: 'app-1',
      autoPolicyWaiverId: 'aw-1',
    });
    await deleteAutoWaiverExclusion({
      ownerType: 'application',
      ownerId: 'app-1',
      autoPolicyWaiverId: 'aw-1',
      autoPolicyWaiverExclusionId: 'ex-1',
    });
    expect(mockedAxios.delete).toHaveBeenCalledTimes(2);
  });

  it('lists exclusions with page params and creates exclusion', async () => {
    mockedAxios.get.mockResolvedValue({ data: [] });
    mockedAxios.post.mockResolvedValue({ data: { autoPolicyWaiverExclusionId: 'ex-1' } });
    await fetchAutoWaiverExclusions({
      ownerType: 'organization',
      ownerId: 'org-1',
      autoPolicyWaiverId: 'aw-1',
      page: 2,
      pageSize: 10,
    });
    expect(mockedAxios.get).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/autoPolicyWaiverExclusions/organization/org-1/aw-1'),
      expect.objectContaining({ params: { page: 2, pageSize: 10 } }),
    );
    await createAutoWaiverExclusion({
      ownerType: 'organization',
      ownerId: 'org-1',
      body: {
        applicationPublicId: 'demo-app',
        ownerId: 'org-1',
        scanId: 'scan-1',
        policyViolationId: 'viol-1',
        autoPolicyWaiverId: 'aw-1',
        matchStrategy: 'POLICY_VIOLATION',
      },
    });
    expect(mockedAxios.post).toHaveBeenCalledWith(
      expect.stringContaining('/api/v2/autoPolicyWaiverExclusions/organization/org-1'),
      expect.objectContaining({ matchStrategy: 'POLICY_VIOLATION' }),
    );
  });

  it('returns null when no applicable auto-waiver exists for a violation', async () => {
    mockedAxios.get.mockRejectedValue({ response: { status: 404 } });
    await expect(fetchApplicableAutoWaiverForViolation('viol-missing')).resolves.toBeNull();
  });
});
