/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduxUiRouterModule from '../../../main/frontend/reduxUiRouter/module';

describe('routerListener', function() {
  var routerListener, mockTransitions, mockTransition, store;

  beforeEach(angular.mock.module(reduxUiRouterModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($injector) {
    routerListener = $injector.get('routerListener');
    store = SpecUtil.mockReduxStore();
    mockTransitions = {
      callback: null,
      onFinish: function(query, callback) {
        this.callback = callback;
      },
      finish: function(transition) {
        this.callback(transition);
      }
    };
    mockTransition = {
      parameters: {
        to: 'to-params',
        from: 'from-params'
      },
      to: function() { return 'to-state';},
      from: function() { return 'from-state';},
      params: function(key) {
        return this.parameters[key];
      }
    };
  }));

  it('listens to onFinish transition event and dispatches UI_ROUTER_ON_FINISH action', function() {
    routerListener(mockTransitions, store);
    expect(store.getActions().length).toBe(0);

    // trigger onFinish transition event
    mockTransitions.finish(mockTransition);

    expect(store.getActions().length).toBe(1);
    expect(store.getActions()[0]).toEqual({
      type: '@@reduxUiRouter/onFinish',
      payload: {
        toState: 'to-state',
        toParams: 'to-params',
        fromState: 'from-state',
        fromParams: 'from-params'
      }
    });
  });
});
