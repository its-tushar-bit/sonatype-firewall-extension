/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import gettingStartedModule from '../../../../main/frontend/configuration/module';
import * as gettingStartedTelemetryServiceHelper from '../../../../main/frontend/configuration/gettingStarted/gettingStartedTelemetryServiceHelper';

describe('gettingStartedRouterListener', function () {
  let $state, $rootScope;

  beforeEach(
    angular.mock.module(gettingStartedModule.name, function ($provide, $stateProvider) {
      spyOn(gettingStartedTelemetryServiceHelper, 'submitData');

      $stateProvider.state('someOtherState', {
        url: '/someOtherState',
      });
    })
  );

  beforeEach(inject(function (_$state_, $transitions, routerListener, _$rootScope_) {
    $state = _$state_;
    $rootScope = _$rootScope_;
    routerListener($transitions);
  }));

  it('fires "DEPARTED" telemetry event when transitions from gettingStarted page', function () {
    $state.go('someOtherState');
    $rootScope.$digest();

    $state.go('gettingStarted');
    $rootScope.$digest();
    expect(gettingStartedTelemetryServiceHelper.submitData).not.toHaveBeenCalled();

    $state.go('someOtherState');
    $rootScope.$digest();
    expect(gettingStartedTelemetryServiceHelper.submitData).toHaveBeenCalledWith('DEPARTED', {
      departedTo: 'someOtherState',
    });
  });
});
