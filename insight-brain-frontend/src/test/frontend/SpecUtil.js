/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.CLM = {
  path: '../brain/'
};
window.clmBuildTimestamp = '';
window.angularDebug = true;
window.SpecUtil = {
  setupProviders: function(applicationId, organizationId) {
    angular.module('ApplicationIdProvider', []).service('ApplicationId', function() {
      // TODO Are ui-router parameters encoded or decoded?
      return {
        encoded: function() {
          return applicationId;
        }
      };
    }).service('OrganizationId', function() {
      return {
        encoded: function() {
          return organizationId;
        }
      };
    });
  },

  toRegExp: function toRegExp(url) {
    var addedTimestamp = false, parts = url.split('?');
    //Note that i go through all of this funkiness as the params are added to the request
    //alphabetically from the angular code, so when testing query param matching, need
    //to make sure the timestamp param is in the proper position
    if (parts.length > 1) {
      parts = parts[1].split('&');

      for (var i = 0; i < parts.length; i++) {
        if ('timestamp' < parts[i]) {
          url = url.replace(parts[i], 'timestamp=[0-9]+&' + parts[i]);
          addedTimestamp = true;
          break;
        }
      }
    }

    return new RegExp(url.replace('?', '\\?').replace('+', '\\+') +
        (!addedTimestamp ? ((url.indexOf('?') < 0 ? '\\?' : '&') + 'timestamp=[0-9]+') : ''));
  },

  setInput: function(inputElement, val) {
    var evt = document.createEvent('HTMLEvents');
    inputElement.val(val);

    inject(function($sniffer) {
      var type = inputElement[0].localName;
      evt.initEvent($sniffer.hasEvent(type) ? type : 'change', false, false);
    });
    inputElement[0].dispatchEvent(evt);
  },

  mockPermissionService: function($provide) {
    $provide.factory('PermissionService', ['$q', function ($q) {
      var deferred = $q.defer();
      deferred.resolve();
      function fn() {
        return deferred.promise;
      }
      return {
        isAuthorized: fn
      };
    }]);
  },

  promiseWrapper: function($q) {
    return function(promise) {
      var deferred = $q.defer();

      promise.then(function() {
        deferred.resolve.apply(deferred, arguments);
      }, function() {
        deferred.reject.apply(deferred, arguments);
      });

      return deferred.promise;
    };
  },

  expectStateChangePrevented: function($scope) {
    var event = $scope.$broadcast('pageChangeStarted');

    expect(event.defaultPrevented).toBeTruthy();
  },

  expectStateChangeNotPrevented: function($scope) {
    var event = $scope.$broadcast('pageChangeStarted');

    expect(event.defaultPrevented).toBeFalsy();
  },

  /**
   * This is used to test components connected to redux store.
   * Just add this in the beginning of your test:
   *
   *    beforeEach(module(function($provide) {
   *      SpecUtil.mockNgRedux($provide);
   *    }));
   *
   *  It will create spies for all action creators passed to $ngRedux.connect().
   *  Also connect() returns a spy to enable testing of unsubscribe.
   */
  mockNgRedux: function($provide) {
    var unsubscribeSpy = jasmine.createSpy('unsubscribe');

    $provide.service('$ngRedux', function() {
      this.connect = jasmine.createSpy('connect').and.callFake(function(mapStateToThis, actions) {
        if (actions) {
          // stub each action creator with spy
          Object.keys(actions).forEach(function(actionCreator) {
            // check if spy already created
            if (actions[actionCreator].and) {
              return;
            }
            spyOn(actions, actionCreator);
          });
        }
        return function(vm) {
          angular.extend(vm, actions);
          return unsubscribeSpy;
        };
      });
    });

    return unsubscribeSpy;
  },

  /**
   * This is used to test redux actions creators (mostly used for testing async actions).
   * This is factory function that takes state and creates redux store mock.
   * Use store.dispatch() to dispatch action under test
   * Use store.getActions() to test actions dispatched
   *
   *    var initialState = [
   *      {foo: 1, bar: 2},
   *      {foo: 1, bar: 1},
   *      {foo: 3, bar: 3}
   *    ];
   *    var store = SpecUtil.mockReduxStore(initialState);
   *    var sortFields: ['-foo', 'bar'];
   *    store.dispatch(actions.sortResults(sortFields));
   *
   *    expect(store.getActions().length).toBe(2);
   *
   *    // this action will update sortFields in the state
   *    expect(store.getActions()[0]).toEqual({
   *      type: 'SORT_RESULTS_REQUESTED',
   *      payload: {
   *        sortFields: sortFields
   *      }
   *    });
   *
   *    expect(store.getActions()[1]).toEqual({
   *      type: 'SORT_RESULTS_FULFILLED',
   *      payload: [
   *        {foo: 3, bar: 3},
   *        {foo: 1, bar: 1},
   *        {foo: 1, bar: 2}
   *      ]
   *    });
   *
   * @param state
   * @returns {dispatch: Function, getActions: Function}
   */
  mockReduxStore: function(state) {
    state = state || {};
    var actions = [];

    function getState() {
      return state;
    }

    function getActions() {
      return actions;
    }

    function dispatch(action) {
      if (angular.isFunction(action)) {
        return action(dispatch, getState);
      }
      else {
        actions.push(action);
        return action;
      }
    }

    return {
      dispatch: dispatch,
      getActions: getActions
    };
  },

  /**
   * Returns a function that can be used to mock axios calls to the different http verbs.
   *
   * The returned function takes in an object of the form { httpVerb: callDefinitions },
   * where _httpVerb_ is either `post`, `put`, `get`, or `del`
   * —note that `delete` is a javascript reserved keyword, so we have to use `del` instead)—
   * and _callDefinitions_ is an object of the form {urlString: response},
   * where _urlString_ is a string representing the expected url,
   * and _response_ is any value that should be returned when the given url is requested with the given verb.
   *
   * Example usage:
   *    const mockAxiosCalls = axiosMockerGenerator(axios);
   *    mockAxiosCalls({
   *      get: {
   *        '/url/1': { data: 1 },
   *        '/url/2': Promise.resolve({ data: 2 })
   *      },
   *      post: {
   *        '/another/url': Promise.reject({ status: 404 })
   *      },
   *      put: {}, // passing an empty object to one of the verbs would result in a simple spy on it.
   *      del: {
   *        '/url/4': Promise.resolve({ data: 'success' }),
   *        '/url/5': Promise.reject({ status: 500 })
   *      }
   *    });
   * @param  axios
   * @returns Function
   */
  axiosMockerGenerator: function(axios) {
    return function(responses) {
      responses = responses || {};

      var get = responses.get;
      var post = responses.post;
      var put = responses.put;
      var del = responses.del;

      if (get) {
        spyOn(axios, 'get').and.callFake(function(url) {
          return get[url];
        });
      }

      if (post) {
        spyOn(axios, 'post').and.callFake(function(url) {
          return post[url];
        });
      }

      if (put) {
        spyOn(axios, 'put').and.callFake(function(url) {
          return put[url];
        });
      }

      if (del) {
        spyOn(axios, 'delete').and.callFake(function(url) {
          return del[url];
        });
      }
    };
  }
};

// custom equality tester for Sets
// Sets are supported starting jasmine 2.6.0
// https://github.com/jasmine/jasmine/blob/master/release_notes/2.6.0.md
var customEqualityTesterForSets = function(as, bs) {
  if (as instanceof Set && bs instanceof Set) {
    return as.size === bs.size && all(isIn(bs), as);
  }
};

function all(pred, as) {
  var notAll = false;
  // using forEach so it works in with ES5
  as.forEach(function(a) {
    notAll = notAll || !pred(a);
  });

  return !notAll;
}

function isIn(as) {
  return function (a) {
    return as.has(a);
  };
}

// customize jasmine globally for all Specs
beforeEach(function() {
  jasmine.addCustomEqualityTester(customEqualityTesterForSets);
});
