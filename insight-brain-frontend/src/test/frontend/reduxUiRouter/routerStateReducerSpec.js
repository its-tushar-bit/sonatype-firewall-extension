/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduxUiRouterModule from '../../../main/frontend/reduxUiRouter/module';

describe('routerStateReducer', function() {
  var reduce, initState;

  beforeEach(angular.mock.module(reduxUiRouterModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($injector) {
    reduce = $injector.get('routerStateReducer');
    initState = {
      currentState: {},
      currentParams: {},
      prevState: {},
      prevParams: {}
    };
  }));

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      var action = {
        type: 'UNKNOWN'
      };
      expect(reduce(undefined, action)).toEqual(initState);
    });
  });

  describe('unknown action', function() {
    it('returns original state', function() {
      var state = Object.freeze({foo: 'bar'});
      var action = {
        type: 'UNKNOWN'
      };
      expect(reduce(state, action)).toBe(state);
    });
  });

  describe('@@reduxUiRouter/onFinish action', function() {
    it('updates previous and current router state including custom data', function() {
      var action = {
        type: '@@reduxUiRouter/onFinish',
        payload: {
          fromState: {
            $$state: function() {},
            component: 'components',
            data: {title: 'Components'},
            name: 'dashboard.overview.components',
            url: '/components'
          },
          fromParams: {timeFilterFeature: undefined},
          toState: {
            $$state: function() {},
            controller: 'componentController',
            data: {crumb: 'Component Details'},
            name: 'dashboard.component',
            templateUrl: 'dashboard/component.html?1511889593717',
            url: '/component/{hash}'
          },
          toParams: {hash: '964cd74171f427720480'}
        }
      };
      expect(reduce(initState, action)).toEqual({
        currentState: {
          name: 'dashboard.component',
          url: '/component/{hash}',
          data: {crumb: 'Component Details'}
        },
        currentParams: {hash: '964cd74171f427720480'},
        prevState: {
          name: 'dashboard.overview.components',
          url: '/components',
          data: {title: 'Components'}
        },
        prevParams: {timeFilterFeature: undefined}
      });
    });
  });
});
