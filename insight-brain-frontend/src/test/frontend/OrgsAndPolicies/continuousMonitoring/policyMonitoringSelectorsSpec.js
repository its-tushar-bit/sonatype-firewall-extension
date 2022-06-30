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
  selectGrandfatheringStatusMessage,
  selectIsGrandfatheringSupported,
  selectIsMonitoringSupported,
  selectPolicyMonitoringMonitoredStage,
} from 'MainRoot/OrgsAndPolicies/continuousMonitoring/policyMonitoringSelectors';

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
          isGrandfatheringSupported: false,
          grandfatheringStatusMessage: 'grandfathering status message',
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
              policyMonitoring: [
                {
                  id: '8c54015dddc5465dbfb973b9979081e7',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  stageTypeId: 'develop',
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
          policyMonitoring: [
            {
              id: '8c54015dddc5465dbfb973b9979081e7',
              ownerId: 'ROOT_ORGANIZATION_ID',
              stageTypeId: 'develop',
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

  describe('selectIsGrandfatheringSupported', () => {
    it('returns isGrandfatheringSupported', () => {
      expect(selectIsGrandfatheringSupported(mockState)).toBe(false);
    });
  });

  describe('selectPolicyMonitoringMonitoredStage', () => {
    it('returns monitoredStage', () => {
      expect(selectPolicyMonitoringMonitoredStage(mockState)).toBe('monitored stage');
    });
  });

  describe('selectGrandfatheringStatusMessage', () => {
    it('returns grandfatheringStatusMessage', () => {
      expect(selectGrandfatheringStatusMessage(mockState)).toBe('grandfathering status message');
    });
  });
});
