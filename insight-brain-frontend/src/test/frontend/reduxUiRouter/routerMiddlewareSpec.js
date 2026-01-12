/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduxUiRouterModule from '../../../main/frontend/reduxUiRouter/module';
import { setStateService } from '../../../main/frontend/reduxUiRouter/routerMiddleware';

describe('routerMiddleware', function () {
  var routerMiddleware, $state, next, successSpy;

  beforeEach(
    angular.mock.module(reduxUiRouterModule.name, function ($provide) {
      successSpy = jasmine.createSpy('successSpy');
      next = jasmine.createSpy('next').and.returnValue('nextReturnValue');
      $state = jasmine.createSpyObj('$state', ['go', 'reload', 'transitionTo', 'includes']);
      $provide.service('$state', function () {
        return $state;
      });
    })
  );

  beforeEach(inject(function ($injector) {
    setStateService($state);
    routerMiddleware = $injector.get('routerMiddleware')();
  }));

  it('calls $state.go on @@reduxUiRouter/stateGo actions, and passes action to next middleware', function () {
    var action = {
      type: '@@reduxUiRouter/stateGo',
      payload: {
        to: 'toState',
        params: 'testParams',
        options: 'testOptions',
      },
    };

    $state.go.and.returnValue({
      then: function (callback) {
        callback();
        return successSpy();
      },
    });

    routerMiddleware(next)(action);
    expect($state.go).toHaveBeenCalledWith('toState', 'testParams', 'testOptions');
    expect(next).toHaveBeenCalledWith(action);
    expect(successSpy).toHaveBeenCalled();
  });

  it('calls $state.reload on @@reduxUiRouter/stateReload actions, and passes action to next middleware', function () {
    var action = {
      type: '@@reduxUiRouter/stateReload',
      payload: 'state to reload',
    };

    $state.reload.and.returnValue({
      then: function (callback) {
        callback();
        return successSpy();
      },
    });

    routerMiddleware(next)(action);
    expect($state.reload).toHaveBeenCalledWith('state to reload');
    expect(next).toHaveBeenCalledWith(action);
    expect(successSpy).toHaveBeenCalled();
  });

  it('calls $state.reload with undefined when no state specified', function () {
    var action = {
      type: '@@reduxUiRouter/stateReload',
      payload: undefined,
    };

    $state.reload.and.returnValue({
      then: function (callback) {
        callback();
        return successSpy();
      },
    });

    routerMiddleware(next)(action);
    expect($state.reload).toHaveBeenCalledWith(undefined);
    expect(next).toHaveBeenCalledWith(action);
    expect(successSpy).toHaveBeenCalled();
  });

  it('calls $state.transitionTo on @@reduxUiRouter/transitionTo actions, and passes action to next middleware', function () {
    var action = {
      type: '@@reduxUiRouter/transitionTo',
      payload: {
        to: 'toState',
        params: 'testParams',
        options: 'testOptions',
      },
    };

    $state.transitionTo.and.returnValue({
      then: function (callback) {
        callback();
        return successSpy();
      },
    });

    routerMiddleware(next)(action);
    expect($state.transitionTo).toHaveBeenCalledWith('toState', 'testParams', 'testOptions');
    expect(next).toHaveBeenCalledWith(action);
    expect(successSpy).toHaveBeenCalled();
  });
});
