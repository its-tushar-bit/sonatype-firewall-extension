/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createInheritOrNoMonitorOption } from 'MainRoot/OrgsAndPolicies/utility/monitoredStageUtil';

describe('monitoredStageUtil', () => {
  const stages = [
    { stageTypeId: 'Develop', stageName: 'Develop' },
    { stageTypeId: 'Deploy', stageName: 'Deploy' },
  ];

  it('Inherit option takes value from parent...', () => {
    const policyMonitoringByOwner = [{}, { ownerName: 'Sonatype', policyMonitoring: { stageTypeId: 'Deploy' } }];
    const result = createInheritOrNoMonitorOption(policyMonitoringByOwner, stages);
    expect(result.stageName).toBe('Inherit from Sonatype (Deploy)');
  });

  it('... even if that option is "not monitored"', () => {
    const policyMonitoringByOwner = [{}, { ownerName: 'The Parent' }, { ownerName: 'root' }];
    const result = createInheritOrNoMonitorOption(policyMonitoringByOwner, stages);
    expect(result.stageName).toBe('Inherit from The Parent (Do not monitor)');
  });

  it('No inheritance for root org, just the plain "not monitored"', () => {
    const policyMonitoringByOwner = [{}];
    const result = createInheritOrNoMonitorOption(policyMonitoringByOwner, stages);
    expect(result.stageName).toBe('Do not monitor');
  });
});
