/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduxUiRouterModule from '../../../main/frontend/reduxUiRouter/module';
import store from '../../../main/frontend/reduxConfig/store';

describe('routerListener', function () {
  var routerListener, mockTransitions, mockTransition, dispatchSpy;

  beforeAll(function () {
    dispatchSpy = spyOn(store, 'dispatch').and.callThrough();
  });

  beforeEach(angular.mock.module(reduxUiRouterModule.name));

  beforeEach(inject(function ($injector) {
    routerListener = $injector.get('routerListener');
    dispatchSpy.calls.reset();
    mockTransitions = {
      callback: null,
      onFinish: function (query, callback) {
        this.callback = callback;
      },
      finish: function (transition) {
        this.callback(transition);
      },
    };
    mockTransition = {
      parameters: {
        to: 'to-params',
        from: 'from-params',
      },
      to: function () {
        return { name: 'to-state' };
      },
      from: function () {
        return { name: 'from-state' };
      },
      params: function (key) {
        return this.parameters[key];
      },
    };
  }));

  it('listens to onFinish transition event and dispatches UI_ROUTER_ON_FINISH action', function () {
    routerListener(mockTransitions);
    expect(dispatchSpy.calls.count()).toBe(0);

    // trigger onFinish transition event
    mockTransitions.finish(mockTransition);

    expect(dispatchSpy.calls.count()).toBe(1);
    expect(dispatchSpy.calls.mostRecent().args[0]).toEqual({
      type: '@@reduxUiRouter/onFinish',
      payload: {
        toState: { name: 'to-state' },
        toParams: 'to-params',
        fromState: { name: 'from-state' },
        fromParams: 'from-params',
      },
    });
  });
});
