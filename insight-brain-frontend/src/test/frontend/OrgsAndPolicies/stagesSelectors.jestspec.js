/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectCliStageTypes,
  selectActionStageTypes,
  selectActionStagesIsLoading,
  selectActionStagesLoadError,
  selectDashboardStagesIsLoading,
  selectDashboardStagesLoadError,
  selectDashboardStageTypes,
  selectCliStagesIsLoading,
  selectCliStagesLoadError,
  selectCliStagesWithInheritOrNoMonitorOption,
  selectSbomStageTypes,
  selectSbomStagesLoadError,
  selectSbomStagesIsLoading,
} from 'MainRoot/OrgsAndPolicies/stagesSelectors';

describe('policyMonitoringSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        stages: {
          cli: {
            loading: false,
            error: 'cli stage types load error',
            stageTypes: [
              { stageName: 'develop', stageTypeId: 'develop' },
              { stageName: 'build', stageTypeId: 'build' },
            ],
          },
          action: {
            loading: false,
            error: 'action stage types load error',
            stageTypes: [{ stageTypeId: 'actionStageOne' }],
          },
          dashboard: {
            loading: false,
            error: 'dashboard stage types load error',
            stageTypes: [{ stageTypeId: 'dashboardStageOne' }],
          },
          sbom: {
            loading: false,
            error: 'sbom stage types load error',
            stageTypes: [{ stageTypeId: 'sbomStageOne' }],
          },
        },
        policyMonitoring: {
          policyMonitoringByOwner: [
            {
              ownerName: 'ApplicationId',
              policyMonitorings: [
                {
                  id: '8c54015dddc5465dbfb973b9979081e7',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  stageTypeId: 'develop',
                },
              ],
            },
            {
              ownerName: 'Root Organization',
              policyMonitorings: [
                {
                  id: '8c54015dddc5465dbfb973b9979081e8',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  stageTypeId: 'build',
                },
              ],
            },
          ],
        },
      },
    };
  });

  describe('selectCliStageTypes', () => {
    it('returns cli stage types', () => {
      expect(selectCliStageTypes(mockState)).toEqual([
        { stageName: 'develop', stageTypeId: 'develop' },
        { stageName: 'build', stageTypeId: 'build' },
      ]);
    });
  });

  describe('selectCliStagesLoadError', () => {
    it('returns cli stage types load error', () => {
      expect(selectCliStagesLoadError(mockState)).toEqual('cli stage types load error');
    });
  });

  describe('selectCliStagesIsLoading', () => {
    it('returns cli stage types isLoading flag', () => {
      expect(selectCliStagesIsLoading(mockState)).toBe(false);
    });
  });

  describe('selectActionStageTypes', () => {
    it('returns action stage types', () => {
      expect(selectActionStageTypes(mockState)).toEqual([{ stageTypeId: 'actionStageOne' }]);
    });
  });

  describe('selectActionStagesLoadError', () => {
    it('returns action stage types load error', () => {
      expect(selectActionStagesLoadError(mockState)).toEqual('action stage types load error');
    });
  });

  describe('selectActionStagesIsLoading', () => {
    it('returns action stage types isLoading flag', () => {
      expect(selectActionStagesIsLoading(mockState)).toBe(false);
    });
  });

  describe('selectDashboardStageTypes', () => {
    it('returns dashboard stage types', () => {
      expect(selectDashboardStageTypes(mockState)).toEqual([{ stageTypeId: 'dashboardStageOne' }]);
    });
  });

  describe('selectDashboardStagesLoadError', () => {
    it('returns dashboard stage types load error', () => {
      expect(selectDashboardStagesLoadError(mockState)).toEqual('dashboard stage types load error');
    });
  });

  describe('selectDashboardStagesIsLoading', () => {
    it('returns dashboard stage types isLoading flag', () => {
      expect(selectDashboardStagesIsLoading(mockState)).toBe(false);
    });
  });

  describe('selectSbomStageTypes', () => {
    it('returns sbom stage types', () => {
      expect(selectSbomStageTypes(mockState)).toEqual([{ stageTypeId: 'sbomStageOne' }]);
    });
  });

  describe('selectSbomStagesLoadError', () => {
    it('returns sbom stage types load error', () => {
      expect(selectSbomStagesLoadError(mockState)).toEqual('sbom stage types load error');
    });
  });

  describe('selectSbomStagesIsLoading', () => {
    it('returns sbom stage types isLoading flag', () => {
      expect(selectSbomStagesIsLoading(mockState)).toBe(false);
    });
  });

  describe('selectCliStagesWithInheritOrNoMonitorOption', () => {
    it('returns cli stage types with inherit or no monitor option', () => {
      expect(selectCliStagesWithInheritOrNoMonitorOption(mockState)).toEqual([
        { stageName: 'Inherit from Root Organization (build)' },
        { stageName: 'develop', stageTypeId: 'develop' },
        { stageName: 'build', stageTypeId: 'build' },
      ]);
    });
  });
});
