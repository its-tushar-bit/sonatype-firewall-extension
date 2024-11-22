/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  createInheritOrNoMonitorOption,
  getMonitoredStageFromAncestors,
  getSbomManagerMonitoredStageDetails,
} from 'MainRoot/OrgsAndPolicies/utility/monitoredStageUtil';

describe('monitoredStageUtil', () => {
  const lifecycleStages = [
    { stageTypeId: 'Develop', stageName: 'Develop' },
    { stageTypeId: 'Deploy', stageName: 'Deploy' },
  ];

  const sbomStages = [{ stageTypeId: 'compliance', stageName: 'Compliance' }];

  it('Inherit option takes value from parent...', () => {
    const policyMonitoringByOwner = [{}, { ownerName: 'Sonatype', policyMonitorings: [{ stageTypeId: 'Deploy' }] }];
    const result = createInheritOrNoMonitorOption(policyMonitoringByOwner, lifecycleStages);
    expect(result.stageName).toBe('Inherit from Sonatype (Deploy)');
  });

  it('... even if that option is "not monitored"', () => {
    const policyMonitoringByOwner = [{}, { ownerName: 'The Parent' }, { ownerName: 'root' }];
    const result = createInheritOrNoMonitorOption(policyMonitoringByOwner, lifecycleStages);
    expect(result.stageName).toBe('Inherit from The Parent (Do not monitor)');
  });

  it('No inheritance for root org, just the plain "not monitored"', () => {
    const policyMonitoringByOwner = [{}];
    const result = createInheritOrNoMonitorOption(policyMonitoringByOwner, lifecycleStages);
    expect(result.stageName).toBe('Do not monitor');
  });

  describe('getMonitoredStageFromAncestors', () => {
    it('should return the correct stage when the stage is found in the child', () => {
      const policyMonitoringByOwner = [
        { policyMonitorings: [{ stageTypeId: 'compliance' }] },
        { policyMonitorings: [{ stageTypeId: 'compliance' }] },
      ];
      const result = getMonitoredStageFromAncestors(policyMonitoringByOwner, sbomStages);
      expect(result.stageTypeId).toBe('compliance');
    });

    it('should return the correct stage when the stage is found in the parent', () => {
      const policyMonitoringByOwner = [
        { policyMonitorings: [] },
        { policyMonitorings: [{ stageTypeId: 'compliance' }] },
      ];
      const result = getMonitoredStageFromAncestors(policyMonitoringByOwner, sbomStages);
      expect(result.stageTypeId).toBe('compliance');
    });

    it('should return undefined when no stage is found', () => {
      const policyMonitoringByOwner = [{ policyMonitorings: [] }, { policyMonitorings: [] }];
      const result = getMonitoredStageFromAncestors(policyMonitoringByOwner, sbomStages);
      expect(result).toBeUndefined();
    });

    it("should return undefined when the owner's stage is not SBOM stage", () => {
      const policyMonitoringByOwner = [{ policyMonitorings: [{ stageTypeId: 'Develop' }] }];
      const result = getMonitoredStageFromAncestors(policyMonitoringByOwner, sbomStages);
      expect(result).toBeUndefined();
    });

    it("should return compliance when the child owner's stage is not SBOM stage and the parent's stage is a SBOM stage", () => {
      const policyMonitoringByOwner = [
        { policyMonitorings: [{ stageTypeId: 'Develop' }] },
        { policyMonitorings: [{ stageTypeId: 'compliance' }] },
      ];
      const result = getMonitoredStageFromAncestors(policyMonitoringByOwner, sbomStages);
      expect(result.stageTypeId).toBe('compliance');
    });
  });

  describe('getSbomManagerMonitoredStageDetails', () => {
    it('should return null when policyMonitoringByOwner is empty', () => {
      const result = getSbomManagerMonitoredStageDetails([], sbomStages, false);
      expect(result).toBeNull();
    });

    it('should return null when sbomStages is empty', () => {
      const result = getSbomManagerMonitoredStageDetails(
        [{ policyMonitorings: [{ stageTypeId: 'Develop' }] }],
        [],
        false
      );
      expect(result).toBeNull();
    });

    it('should return the correct details when there is only one owner', () => {
      const result = getSbomManagerMonitoredStageDetails(
        [{ policyMonitorings: [{ stageTypeId: 'compliance' }] }],
        sbomStages,
        false
      );
      expect(result).toEqual({
        label: 'Disable continuous monitoring for SBOM Manager',
        toggleEnabled: true,
      });
    });

    it('should return the correct details when there are multiple owners', () => {
      const result = getSbomManagerMonitoredStageDetails(
        [{ policyMonitorings: [] }, { ownerName: 'The Parent', policyMonitorings: [{ stageTypeId: 'compliance' }] }],
        sbomStages,
        false
      );
      expect(result).toEqual({
        label:
          "Continuous Monitoring is up and running at The Parent, so this means it's active for this organization and all its dependents.",
        toggleEnabled: false,
      });
    });

    it('should return the correct details when there are multiple owners and it is an application', () => {
      const result = getSbomManagerMonitoredStageDetails(
        [
          { ownerName: 'The Application', policyMonitorings: [] },
          { ownerName: 'The Parent', policyMonitorings: [{ stageTypeId: 'compliance' }] },
        ],
        sbomStages,
        true
      );
      expect(result).toEqual({
        label: "Continuous Monitoring is up and running at The Parent, so this means it's active for this application.",
        toggleEnabled: false,
      });
    });

    it('should return the correct details when there are multiple owners and only the child has compliance', () => {
      const result = getSbomManagerMonitoredStageDetails(
        [
          { ownerName: 'The Sub-Org', policyMonitorings: [{ stageTypeId: 'compliance' }] },
          { ownerName: 'The Parent', policyMonitorings: [{ stageTypeId: 'develop' }] },
        ],
        sbomStages,
        false
      );
      expect(result).toEqual({
        label: 'Disable continuous monitoring for SBOM Manager',
        toggleEnabled: true,
      });
    });

    it('should return the correct details when there are multiple owners and none of them have compliance', () => {
      const result = getSbomManagerMonitoredStageDetails(
        [
          { ownerName: 'The Sub-Org', policyMonitorings: [{ stageTypeId: 'develop' }] },
          { ownerName: 'The Parent', policyMonitorings: [{ stageTypeId: 'develop' }] },
        ],
        sbomStages,
        false
      );
      expect(result).toEqual({
        label:
          'Continuous Monitoring is currently disabled at the root organization. Would you like to enable it for this organization and all its dependents?',
        toggleEnabled: true,
      });
    });
  });
});
