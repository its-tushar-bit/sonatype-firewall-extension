/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityServicesModule from '../../../../main/frontend/utility/services/utility.services.module';

describe('state.history.service.spec.js', function () {
  var $rootScope, StateHistoryService;

  beforeEach(angular.mock.module(utilityServicesModule.name));

  beforeEach(inject([
    '$rootScope',
    'state.history.service',
    function (_$rootScope_, _StateHistoryService_) {
      $rootScope = _$rootScope_;
      StateHistoryService = _StateHistoryService_;
    },
  ]));

  it('Adds state to history on stateChangeSuccess', function () {
    var newState = { name: 'new.state' };

    $rootScope.$broadcast('$stateChangeSuccess', undefined, undefined, newState);
    expect(StateHistoryService.getPreviousState()).toEqual(newState);

    var anotherState = { name: 'another.state' };
    $rootScope.$broadcast('$stateChangeSuccess', undefined, undefined, anotherState);
    expect(StateHistoryService.getPreviousState()).toEqual(anotherState);
  });
});
