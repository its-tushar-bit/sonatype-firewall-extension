/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { configureStore } from '@reduxjs/toolkit';
import axios from 'axios';
import {
  getApplicableWaiversUrl,
  getApplicationSummaryUrl,
  getPermissionContextTestUrl,
  getViolationDetailsUrl,
  getVulnerabilityJsonDetailUrl,
} from 'MainRoot/util/CLMLocation';
import reducer, { loadViolationDetail } from 'MainRoot/nosc/violations/detail/violationDetailSlice';
import {
  selectViolationDetailIdentityState,
  selectViolationDetailVulnerabilitySummaryState,
  selectViolationDetailWaiversState,
  selectViolationHasPermissionForAppWaivers,
} from 'MainRoot/nosc/violations/detail/violationDetailSelectors';
import type {
  ApplicableWaiversDTO,
  ComponentIdentifierDTO,
  ViolationDetailsDTO,
} from 'MainRoot/nosc/violations/detail/violationDetailTypes';

const componentIdentifier: ComponentIdentifierDTO = {
  format: 'maven',
  coordinates: {
    artifactId: 'demo',
    groupId: 'com.example',
    version: '1.0.0',
  },
};

function violationDetails(overrides: Partial<ViolationDetailsDTO> = {}): ViolationDetailsDTO {
  return {
    policyViolationId: 'violation-1',
    policyName: 'Security Policy',
    policyThreatCategory: 'security',
    policyOwner: {
      ownerName: 'Demo Org',
      ownerType: 'organization',
      ownerId: 'org-1',
      ownerPublicId: 'demo-org',
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
    organizationName: 'Demo Org',
    applicationName: 'Demo App',
    componentIdentifier,
    constraintViolations: [
      {
        constraintName: 'cvss',
        reasons: [
          {
            reason: 'CVE-2026-0001',
            reference: {
              type: 'SECURITY_VULNERABILITY_REFID',
              value: 'CVE-2026-0001',
            },
          },
        ],
      },
    ],
    ...overrides,
  };
}

const waivers: ApplicableWaiversDTO = {
  activeWaivers: [
    {
      policyWaiverId: 'waiver-active',
      scopeOwnerType: 'application',
      scopeOwnerId: 'app-internal-1',
      scopeOwnerName: 'Demo App',
      policyId: 'policy-1',
    },
  ],
  expiredWaivers: [
    {
      policyWaiverId: 'waiver-expired',
      scopeOwnerType: 'application',
      scopeOwnerId: 'app-internal-1',
      scopeOwnerName: 'Demo App',
      policyId: 'policy-1',
    },
  ],
};

function store(extraReducers: Record<string, (state: unknown) => unknown> = {}) {
  return configureStore({
    reducer: {
      violationDetail: reducer,
      ...extraReducers,
    },
  });
}

function mockSuccessfulRequests(
  identity: ViolationDetailsDTO,
  expectedIdentificationSource = identity.identificationSource
) {
  const vulnerabilityQueryParameters: Record<string, string> = {
    ownerType: 'application',
    ownerId: 'demo-app',
    scanId: 'scan-1',
  };
  if (expectedIdentificationSource) {
    vulnerabilityQueryParameters.identificationSource = expectedIdentificationSource;
  }

  jest.spyOn(axios, 'get').mockImplementation((url: string) => {
    if (url === getViolationDetailsUrl('violation-1')) {
      return Promise.resolve({ data: identity });
    }
    if (url === getApplicableWaiversUrl('violation-1')) {
      return Promise.resolve({ data: waivers });
    }
    if (url === getApplicationSummaryUrl('demo-app')) {
      return Promise.resolve({ data: { id: 'app-internal-1' } });
    }
    if (
      url ===
      getVulnerabilityJsonDetailUrl('CVE-2026-0001', componentIdentifier, {
        ...vulnerabilityQueryParameters,
      })
    ) {
      return Promise.resolve({ data: { identifier: 'CVE-2026-0001', mainSeverity: { score: 9.8 } } });
    }
    return Promise.reject(new Error(`Unexpected GET ${url}`));
  });
  jest.spyOn(axios, 'put').mockImplementation((url: string) => {
    if (url === getPermissionContextTestUrl('application', 'app-internal-1')) {
      return Promise.resolve({ data: ['WAIVE_POLICY_VIOLATIONS'] });
    }
    return Promise.reject(new Error(`Unexpected PUT ${url}`));
  });
}

function deferred<T>() {
  let resolve: (value: T) => void;
  let reject: (reason?: unknown) => void;
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve;
    reject = promiseReject;
  });
  return { promise, resolve: resolve!, reject: reject! };
}

describe('violationDetailSlice', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('loads identity, waivers, and app waiver permission', async () => {
    const identity = violationDetails({ policyThreatCategory: 'license' });
    const testStore = store();
    mockSuccessfulRequests(identity);

    await testStore.dispatch(loadViolationDetail('violation-1'));

    const state = testStore.getState();
    expect(selectViolationDetailIdentityState(state)).toMatchObject({
      status: 'ready',
      data: identity,
      error: null,
    });
    expect(selectViolationDetailWaiversState(state)).toMatchObject({
      status: 'ready',
      active: waivers.activeWaivers,
      expired: waivers.expiredWaivers,
      error: null,
    });
    expect(selectViolationHasPermissionForAppWaivers(state)).toBe(true);
    expect(selectViolationDetailVulnerabilitySummaryState(state).status).toBe('idle');
  });

  it('records identity load failure without inventing permission state', async () => {
    const testStore = store();
    jest.spyOn(axios, 'get').mockImplementation((url: string) => {
      if (url === getViolationDetailsUrl('violation-1')) {
        return Promise.reject(new Error('Request failed with status code 404'));
      }
      if (url === getApplicableWaiversUrl('violation-1')) {
        return Promise.resolve({ data: waivers });
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });

    await testStore.dispatch(loadViolationDetail('violation-1'));

    const state = testStore.getState();
    expect(selectViolationDetailIdentityState(state)).toMatchObject({
      status: 'error',
      data: null,
      error: 'Request failed with status code 404',
    });
    expect(selectViolationHasPermissionForAppWaivers(state)).toBeNull();
    expect(selectViolationDetailVulnerabilitySummaryState(state).status).toBe('idle');
  });

  it('loads vulnerability summary with Classic selected component identification source', async () => {
    const identity = violationDetails();
    const testStore = store({
      router: () => ({ currentParams: { hash: 'component-hash-1' } }),
      applicationReport: () => ({
        selectedReport: {
          allEntries: [{ hash: 'component-hash-1', identificationSource: 'MAVEN' }],
        },
      }),
    });
    mockSuccessfulRequests(identity, 'MAVEN');

    await testStore.dispatch(loadViolationDetail('violation-1'));

    expect(axios.get).toHaveBeenCalledWith(
      getVulnerabilityJsonDetailUrl('CVE-2026-0001', componentIdentifier, {
        ownerType: 'application',
        ownerId: 'demo-app',
        scanId: 'scan-1',
        identificationSource: 'MAVEN',
      }),
      expect.any(Object)
    );
    expect(selectViolationDetailVulnerabilitySummaryState(testStore.getState())).toMatchObject({
      status: 'ready',
      data: { identifier: 'CVE-2026-0001', mainSeverity: { score: 9.8 } },
      error: null,
    });
  });

  it('leaves vulnerability summary idle for non-security violations', async () => {
    const identity = violationDetails({ policyThreatCategory: 'license' });
    const testStore = store();
    mockSuccessfulRequests(identity);

    await testStore.dispatch(loadViolationDetail('violation-1'));

    expect(axios.get).not.toHaveBeenCalledWith(expect.stringContaining('/api/v2/vulnerabilities/'), expect.anything());
    expect(selectViolationDetailVulnerabilitySummaryState(testStore.getState())).toMatchObject({
      status: 'idle',
      data: null,
      error: null,
    });
  });

  it('ignores stale responses when a newer violation id load has started', async () => {
    const testStore = store();
    const firstIdentity = deferred<{ data: ViolationDetailsDTO }>();
    const firstWaivers = deferred<{ data: ApplicableWaiversDTO }>();
    const secondIdentity = deferred<{ data: ViolationDetailsDTO }>();
    const secondWaivers = deferred<{ data: ApplicableWaiversDTO }>();
    const firstPermission = deferred<{ data: ReadonlyArray<string> }>();
    const secondPermission = deferred<{ data: ReadonlyArray<string> }>();

    jest.spyOn(axios, 'get').mockImplementation((url: string) => {
      if (url === getViolationDetailsUrl('violation-1')) return firstIdentity.promise;
      if (url === getViolationDetailsUrl('violation-2')) return secondIdentity.promise;
      if (url === getApplicableWaiversUrl('violation-1')) return firstWaivers.promise;
      if (url === getApplicableWaiversUrl('violation-2')) return secondWaivers.promise;
      if (url === getApplicationSummaryUrl('demo-app')) return Promise.resolve({ data: { id: 'app-internal-1' } });
      if (url === getApplicationSummaryUrl('other-app')) return Promise.resolve({ data: { id: 'app-internal-2' } });
      return Promise.reject(new Error(`Unexpected GET ${url}`));
    });
    jest.spyOn(axios, 'put').mockImplementation((url: string) => {
      if (url === getPermissionContextTestUrl('application', 'app-internal-1')) return firstPermission.promise;
      if (url === getPermissionContextTestUrl('application', 'app-internal-2')) return secondPermission.promise;
      return Promise.reject(new Error(`Unexpected PUT ${url}`));
    });

    const firstLoad = testStore.dispatch(loadViolationDetail('violation-1'));
    const secondLoad = testStore.dispatch(loadViolationDetail('violation-2'));

    secondIdentity.resolve({
      data: violationDetails({
        policyViolationId: 'violation-2',
        policyThreatCategory: 'license',
        applicationPublicId: 'other-app',
      }),
    });
    secondWaivers.resolve({ data: { activeWaivers: [], expiredWaivers: [] } });
    secondPermission.resolve({ data: [] });
    await secondLoad;

    firstIdentity.resolve({
      data: violationDetails({ policyViolationId: 'violation-1', policyThreatCategory: 'license' }),
    });
    firstWaivers.resolve({ data: waivers });
    firstPermission.resolve({ data: ['WAIVE_POLICY_VIOLATIONS'] });
    await firstLoad;

    const state = testStore.getState();
    expect(state.violationDetail.violationId).toBe('violation-2');
    expect(selectViolationDetailIdentityState(state).data?.policyViolationId).toBe('violation-2');
    expect(selectViolationDetailWaiversState(state).active).toEqual([]);
    expect(selectViolationHasPermissionForAppWaivers(state)).toBe(false);
  });
});
