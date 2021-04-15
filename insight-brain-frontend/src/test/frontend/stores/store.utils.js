/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.StoreUtils = function () {
  var me = {
    createMockStore: createMockStore,
    createMockHierarchyStoreData: createMockHierarchyStoreData,
  };

  function createMockStore(storeName) {
    return new MockStore(storeName);
  }

  function MockStore(storeName) {
    var promises,
      me = this,
      store,
      $q;

    me.resolveGet = resolvePromise('get');
    me.rejectGet = rejectPromise('get');

    me.resolveGetById = resolvePromise('getById');
    me.rejectGetById = rejectPromise('getById');

    me.resolveRefresh = resolvePromise('refresh');
    me.rejectRefresh = rejectPromise('refresh');

    me.resolveGetApplicable = resolvePromise('getApplicable');
    me.resolveGetApplied = resolvePromise('getApplied');
    me.resolveSave = resolvePromise('save');
    me.resolveRemove = resolvePromise('remove');

    beforeEach(inject([
      storeName,
      '$q',
      function (_store_, _$q_) {
        store = _store_;
        $q = _$q_;
        promises = {
          get: $q.defer(),
          getById: $q.defer(),
          getApplicable: $q.defer(),
          getApplied: $q.defer(),
          save: $q.defer(),
          remove: $q.defer(),
          refresh: $q.defer(),
        };

        for (var key in promises) {
          if (promises.hasOwnProperty(key)) {
            spyOn(promises[key].promise, 'then').and.callThrough();
            if (store.hasOwnProperty(key)) {
              spyOn(store, key).and.returnValue(promises[key].promise);
            }
          }
        }
      },
    ]));

    function resolvePromise(promiseName) {
      return function (value) {
        if (!promises) {
          throw "Promises not defined. Make sure to call resolve in an 'it'";
        }

        expect(promises[promiseName].promise.then).toHaveBeenCalled();
        promises[promiseName].resolve(value);
        resetPromise(promiseName);
      };
    }

    function rejectPromise(promiseName) {
      return function (value) {
        if (!promises) {
          throw "Promises not defined. Make sure to call resolve in an 'it'";
        }

        promises[promiseName].reject(value);
        resetPromise(promiseName);
      };
    }

    function resetPromise(promiseName) {
      promises[promiseName] = $q.defer();
      spyOn(promises[promiseName].promise, 'then').and.callThrough();
      store[promiseName].and.returnValue(promises[promiseName].promise);
    }
  }

  function createMockHierarchyStoreData(data, field) {
    data[field].forEach(function (owner) {
      owner.store = { create: jasmine.createSpy().and.returnValue({}) };
    });

    return data[field];
  }

  return me;
};
