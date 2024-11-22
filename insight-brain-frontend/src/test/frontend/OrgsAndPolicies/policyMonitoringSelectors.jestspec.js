/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectPolicyMonitoringByOwner,
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringLoading,
  selectPolicyMonitoringSubmitError,
  selectLegacyViolationStatusMessage,
  selectIsLegacyViolationSupported,
  selectIsMonitoringSupported,
  selectPolicyMonitoringMonitoredStage,
} from 'MainRoot/OrgsAndPolicies/policyMonitoringSelectors';

describe('policyMonitoringSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        policyMonitoring: {
          loadError: 'loadError',
          submitError: 'submitError',
          loading: false,
          isMonitoringSupported: false,
          isLegacyViolationSupported: false,
          legacyViolationStatusMessage: 'legacy violation status message',
          monitoredStage: 'monitored stage',
          localProprietaryCount: 10,
          inheritedProprietaryCount: 10,
          stages: [{ stageTypeId: 'stageOne' }],
          actionStages: [{ stageTypeId: 'actionStageOne' }],
          policiesByOwner: [
            {
              ownerName: 'Root Organization 2',
              policyMonitoring: [
                {
                  id: '8c54015dddc5465dbfb973b9979081e7',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  stageTypeId: 'develop',
                },
              ],
            },
          ],
          policyMonitoringByOwner: [
            {
              ownerName: 'Root Organization',
              policyMonitorings: [
                {
                  id: '8c54015dddc5465dbfb973b9979081e7',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  stageTypeId: 'develop',
                },
                {
                  id: '8c54015dddc5465dbfb973b9979081e8',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  stageTypeId: 'compliance',
                },
              ],
            },
          ],
        },
      },
    };
  });

  describe('selectPolicyMonitoringLoading', () => {
    it('returns loading', () => {
      expect(selectPolicyMonitoringLoading(mockState)).toBe(false);
    });
  });

  describe('selectPolicyMonitoringLoadError', () => {
    it('returns loadError', () => {
      expect(selectPolicyMonitoringLoadError(mockState)).toBe('loadError');
    });
  });

  describe('selectPolicyMonitoringSubmitError', () => {
    it('returns submitError', () => {
      expect(selectPolicyMonitoringSubmitError(mockState)).toBe('submitError');
    });
  });

  describe('selectPolicyMonitoringByOwner', () => {
    it('returns policyMonitoringByOwner array', () => {
      const expected = [
        {
          ownerName: 'Root Organization',
          policyMonitorings: [
            {
              id: '8c54015dddc5465dbfb973b9979081e7',
              ownerId: 'ROOT_ORGANIZATION_ID',
              stageTypeId: 'develop',
            },
            {
              id: '8c54015dddc5465dbfb973b9979081e8',
              ownerId: 'ROOT_ORGANIZATION_ID',
              stageTypeId: 'compliance',
            },
          ],
        },
      ];

      expect(selectPolicyMonitoringByOwner(mockState)).toEqual(expected);
    });
  });

  describe('selectIsMonitoringSupported', () => {
    it('returns isMonitoringSupported', () => {
      expect(selectIsMonitoringSupported(mockState)).toBe(false);
    });
  });

  describe('selectIsLegacyViolationSupported', () => {
    it('returns isLegacyViolationSupported', () => {
      expect(selectIsLegacyViolationSupported(mockState)).toBe(false);
    });
  });

  describe('selectPolicyMonitoringMonitoredStage', () => {
    it('returns monitoredStage', () => {
      expect(selectPolicyMonitoringMonitoredStage(mockState)).toBe('monitored stage');
    });
  });

  describe('selectLegacyViolationStatusMessage', () => {
    it('returns legacyViolationStatusMessage', () => {
      expect(selectLegacyViolationStatusMessage(mockState)).toBe('legacy violation status message');
    });
  });
});
