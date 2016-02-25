var StoreUtils = function() {
  var me = {
    createMockStore: createMockStore,
    createMockHierarchyStoreData: createMockHierarchyStoreData
  };

  function createMockStore(storeName) {
    return new MockStore(storeName);
  }

  function MockStore(storeName) {
    var promises,
        me = this;

    me.resolveGet = resolvePromise('get');
    me.rejectGet = rejectPromise('get');

    me.resolveRefresh = resolvePromise('refresh');
    me.rejectRefresh = rejectPromise('refresh');

    me.resolveGetApplicable = resolvePromise('getApplicable');
    me.resolveGetApplied = resolvePromise('getApplied');
    me.resolveSave = resolvePromise('save');
    me.resolveRemove = resolvePromise('remove');

    beforeEach(inject([
      storeName, '$q', function(store, $q) {
        promises = {
          get: $q.defer(),
          getApplicable: $q.defer(),
          getApplied: $q.defer(),
          save: $q.defer(),
          remove: $q.defer(),
          refresh: $q.defer()
        };

        for (var key in promises) {
          if (promises.hasOwnProperty(key)) {
            spyOn(promises[key].promise, 'then').andCallThrough();
            if (store.hasOwnProperty(key)) {
              spyOn(store, key).andReturn(promises[key].promise);
            }
          }
        }
      }
    ]));

    function resolvePromise(promiseName) {
      return function(value) {
        if (!promises) {
          throw "Promises not defined. Make sure to call resolve in an 'it'";
        }

        expect(promises[promiseName].promise.then).toHaveBeenCalled();
        promises[promiseName].resolve(value);
      };
    }

    function rejectPromise(promiseName) {
      return function(value) {
        if (!promises) {
          throw "Promises not defined. Make sure to call resolve in an 'it'";
        }

        promises[promiseName].reject(value);
      };
    }
  }

  function createMockHierarchyStoreData(data, field) {
    data[field].forEach(function(owner) {
      owner.store = {create: jasmine.createSpy()};
    });

    return data[field];
  }

  return me;
};
