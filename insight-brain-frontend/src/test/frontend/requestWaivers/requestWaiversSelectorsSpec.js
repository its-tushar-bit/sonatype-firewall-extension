/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectPolicyViolationError } from '../../../main/frontend/requestWaivers/requestWaiversSelectors';

describe('requestWaiversSelectors', () => {
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
        violationId: 'some-policy-violation-id',
      },
    },
    applicationReport: {
      selectedReport: {
        aggregatedEntries: [
          {
            derivedComponentName: 'My Component',
            hash: 'some-component-hash',
            componentIdentifier: { format: 'maven' },
            derivedDependencyType: 'transitive',
            policyViolationId: 'some-policy-violation-id',
          },
          {
            hash: 'a-component-hash',
            policyViolationId: 'some=other-policy-violation-id',
          },
        ],
      },
      loadError: false,
      pendingLoads: new Set(['test']),
    },
  };

  describe('selectPolicyViolationError', () => {
    it('returns "Error getting policy violation." when there are no pendingLoads, no policy violation associated to the URL violationId and no load errors', () => {
      const actual = selectPolicyViolationError({
        ...mockState,
        router: {
          ...mockState.router.currentParams,
          violationId: 'some-bad-policy-violation-id',
        },
        applicationReport: {
          ...mockState.applicationReport,
          pendingLoads: new Set(),
        },
      });
      expect(actual).toEqual('Error getting policy violation.');
    });

    it('returns "" when there are pendingLoads, no policy violation associated to the URL violationId and no load errors', () => {
      const actual = selectPolicyViolationError({
        ...mockState,
        router: {
          ...mockState.router.currentParams,
          violationId: 'some-bad-policy-violation-id',
        },
      });
      expect(actual).toEqual('');
    });

    it('returns "" when there are no pendingLoads, there is a policy violation associated to the URL violationId and no load errors', () => {
      const actual = selectPolicyViolationError({
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          pendingLoads: new Set(),
        },
      });
      expect(actual).toEqual('');
    });

    it('returns loadError message when there is a loadError', () => {
      const actual = selectPolicyViolationError({
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          loadError: 'Test load error',
        },
      });
      expect(actual).toEqual('Test load error');
    });

    it('returns "" message when there is no loadError and no policy violation error', () => {
      const actual = selectPolicyViolationError({
        ...mockState,
        applicationReport: {
          ...mockState.applicationReport,
          pendingLoads: new Set(),
        },
      });
      expect(actual).toEqual('');
    });
  });
});
