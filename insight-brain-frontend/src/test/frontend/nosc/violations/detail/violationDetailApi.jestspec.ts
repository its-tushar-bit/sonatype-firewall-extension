/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getApplicableWaiversUrl, getViolationDetailsUrl } from 'MainRoot/util/CLMLocation';
import {
  fetchApplicableWaivers,
  fetchCrossStageViolationDetails,
} from 'MainRoot/nosc/violations/detail/violationDetailApi';
import { ApplicableWaiversDTO, ViolationDetailsDTO } from 'MainRoot/nosc/violations/detail/violationDetailTypes';

const violationDetailsFixture: ViolationDetailsDTO = {
  policyViolationId: 'violation-123',
  policyName: 'Block Critical Security',
  policyThreatCategory: 'security',
  policyOwner: {
    ownerName: 'App Team',
    ownerType: 'application',
    ownerId: 'app-internal-123',
    ownerPublicId: 'app-public-123',
  },
  threatLevel: 10,
  openTime: '2026-01-10T12:00:00.000+0000',
  applicationPublicId: 'app-public-123',
  organizationName: 'Example Org',
  applicationName: 'Example App',
  componentIdentifier: {
    format: 'maven',
    coordinates: {
      artifactId: 'example-lib',
      groupId: 'com.example',
      version: '1.2.3',
    },
  },
  displayName: {
    parts: [
      { field: 'Group', value: 'com.example' },
      { field: 'Artifact', value: 'example-lib' },
      { field: 'Version', value: '1.2.3' },
    ],
  },
  filenames: ['example-lib-1.2.3.jar'],
  hash: 'abcdef123456',
  constraintViolations: [
    {
      constraintName: 'Critical CVSS',
      reasons: [
        {
          reason: 'Found security vulnerability CVE-2026-1234 with severity critical',
          reference: {
            type: 'SECURITY_VULNERABILITY_REFID',
            value: 'CVE-2026-1234',
          },
        },
      ],
    },
  ],
  stageData: {
    build: {
      mostRecentEvaluationTime: '2026-01-11T12:00:00.000+0000',
      mostRecentScanId: 'scan-build-1',
      actionTypeId: 'fail',
    },
    release: {
      mostRecentEvaluationTime: '2026-01-12T12:00:00.000+0000',
      mostRecentScanId: 'scan-release-1',
      actionTypeId: 'warn',
    },
  },
  reachabilityStatus: 'REACHABLE',
  waived: false,
};

const applicableWaiversFixture: ApplicableWaiversDTO = {
  activeWaivers: [
    {
      policyWaiverId: 'waiver-active-1',
      comment: 'Accepted for this release',
      scopeOwnerType: 'application',
      scopeOwnerId: 'app-public-123',
      scopeOwnerName: 'Example App',
      hash: 'abcdef123456',
      policyId: 'policy-123',
    },
  ],
  expiredWaivers: [
    {
      policyWaiverId: 'waiver-expired-1',
      scopeOwnerType: 'organization',
      scopeOwnerId: 'org-123',
      scopeOwnerName: 'Example Org',
      policyId: 'policy-123',
    },
  ],
};

describe('violationDetailApi', () => {
  let mock: ReturnType<typeof axiosMockAdapter>;

  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  it('fetches cross-stage violation details from the Classic lifecycle endpoint', async () => {
    mock.onGet(getViolationDetailsUrl('violation-123')).reply(200, violationDetailsFixture);

    await expect(fetchCrossStageViolationDetails('violation-123')).resolves.toEqual(violationDetailsFixture);
    expect(mock.history.get[0].url).toBe(getViolationDetailsUrl('violation-123'));
  });

  it('fetches applicable waivers from the Classic lifecycle endpoint', async () => {
    mock.onGet(getApplicableWaiversUrl('violation-123')).reply(200, applicableWaiversFixture);

    await expect(fetchApplicableWaivers('violation-123')).resolves.toEqual(applicableWaiversFixture);
    expect(mock.history.get[0].url).toBe(getApplicableWaiversUrl('violation-123'));
  });
});
