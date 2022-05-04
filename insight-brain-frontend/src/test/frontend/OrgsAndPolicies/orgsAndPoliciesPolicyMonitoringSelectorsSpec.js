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
  selectPoliciesByOwner,
  selectPoliciesByOwnerWithEnforcementActions,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSelectors';

describe('orgsAndPoliciesPolicyMonitoringSelectors', () => {
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

  describe('selectPoliciesByOwner', () => {
    it('returns policiesByOwner array', () => {
      const expected = [
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
      ];

      expect(selectPoliciesByOwner(mockState)).toEqual(expected);
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

  describe('selectPoliciesByOwnerWithEnforcementActions', () => {
    it('returns policies with enformacement actions', () => {
      const policiesByOwner = StoreUtils().createMockHierarchyStoreData(
        PolicyTileMockData.getApplicablePolicies(),
        'policiesByOwner'
      );
      const state = Object.freeze({
        orgsAndPolicies: {
          stages: {
            cli: { stageTypes: MockData.getStageData() },
            action: { stageTypes: MockData.getStageData() },
          },
          policyMonitoring: {
            policiesByOwner,
            policyMonitoringByOwner: [{ policyMonitoring: { stageTypeId: 'develop', stageName: 'Develop' } }],
          },
        },
      });

      const policiesByOwnerWithEnforcementActions = selectPoliciesByOwnerWithEnforcementActions(state);

      policiesByOwnerWithEnforcementActions.forEach(function (owner, ownerIndex) {
        owner.policies.forEach(function (policy, policyIndex) {
          expect(policy.name).toEqual(policiesByOwner[ownerIndex].policies[policyIndex].name);
          expect(policy.threatLevel).toEqual(policiesByOwner[ownerIndex].policies[policyIndex].threatLevel);
          expect(policy.actions).toEqual(policiesByOwner[ownerIndex].policies[policyIndex].actions);
          expect(policy.enforcementAction).toBeDefined();
          expect(policy.enforcementAction['build'][0].actionTypeId).toEqual(
            policiesByOwner[ownerIndex].policies[policyIndex].actions['build'][0].actionTypeId
          );
          expect(policy.enforcementAction['stage-release'][0].actionTypeId).toEqual(
            policiesByOwner[ownerIndex].policies[policyIndex].actions['stage-release'][0].actionTypeId
          );
        });
      });
    });
  });
});
