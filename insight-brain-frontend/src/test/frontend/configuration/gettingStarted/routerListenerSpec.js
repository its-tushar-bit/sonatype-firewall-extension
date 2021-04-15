/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import gettingStartedModule from '../../../../main/frontend/configuration/gettingStarted/module';

describe('gettingStarted routerListener', function () {
  var telemetryServiceMock, $state, $rootScope;

  beforeEach(
    angular.mock.module(gettingStartedModule.name, function ($provide, $stateProvider) {
      telemetryServiceMock = jasmine.createSpyObj('gettingStartedUsageTelemetryService', ['submitData']);

      $provide.service('gettingStartedUsageTelemetryService', function () {
        return telemetryServiceMock;
      });

      $stateProvider.state('someOtherState', {
        url: '/someOtherState',
      });
    })
  );

  beforeEach(inject(function (_$state_, $transitions, routerListener, _$rootScope_) {
    $state = _$state_;
    $rootScope = _$rootScope_;
    routerListener($transitions, telemetryServiceMock);
  }));

  it('fires "DEPARTED" telemetry event when transitions from gettingStarted page', function () {
    $state.go('someOtherState');
    $rootScope.$digest();

    $state.go('gettingStarted');
    $rootScope.$digest();
    expect(telemetryServiceMock.submitData).not.toHaveBeenCalled();

    $state.go('someOtherState');
    $rootScope.$digest();
    expect(telemetryServiceMock.submitData).toHaveBeenCalledWith('DEPARTED', {
      departedTo: 'someOtherState',
    });
  });
});
