/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../../main/frontend/owner.manager/owner.manager.module';

describe('monitored.stage.service.spec.js', function() {

  var monitoredStageService,
      stages = [{stageTypeId: 'Develop', stageName: 'Develop'}, {stageTypeId: 'Deploy', stageName: 'Deploy'}];

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(inject([
    'monitored.stage.service', function(_MonitoredStageService_) {
      monitoredStageService = _MonitoredStageService_;
    }
  ]));

  it('Inherit option takes value from parent...', function() {
    var policyMonitoringByOwner = [{}, {ownerName: 'Sonatype', policyMonitoring: {stageTypeId: 'Deploy'}}];
    var result = monitoredStageService.createInheritOrNoMonitorOption(policyMonitoringByOwner, stages);
    expect(result.stageName).toBe('Inherit from Sonatype (Deploy)');
  });

  it('... even if that option is "not monitored"', function() {
    var policyMonitoringByOwner = [{}, {ownerName: 'The Parent'}, {ownerName: 'root'}];
    var result = monitoredStageService.createInheritOrNoMonitorOption(policyMonitoringByOwner, stages);
    expect(result.stageName).toBe('Inherit from The Parent (Do not monitor)');
  });

  it('No inheritance for root org, just the plain "not monitored"', function() {
    var policyMonitoringByOwner = [{}];
    var result = monitoredStageService.createInheritOrNoMonitorOption(policyMonitoringByOwner, stages);
    expect(result.stageName).toBe('Do not monitor');
  });
});
