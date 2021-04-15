/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.ResourceUtils = function () {
  var me = {
    createMockResource: createMockResource,
  };

  function createMockResource() {
    return new MockResource();
  }

  function MockResource() {
    var promises,
      functions = ['$delete', '$save'],
      me = this;

    me.$revert = jasmine.createSpy();
    me.$clone = jasmine.createSpy().and.returnValue(me);
    me.resolveDelete = resolvePromise('$delete');
    me.rejectDelete = rejectPromise('$delete');
    me.resolveSave = resolvePromise('$save');
    me.rejectSave = rejectPromise('$save');

    beforeEach(inject(function ($q) {
      promises = {};

      // Add mock functions and return a unique promise
      functions.forEach(function (fn) {
        promises[fn] = $q.defer();
        me[fn] = function () {
          return promises[fn].promise;
        };
        spyOn(promises[fn].promise, 'then').and.callThrough();
        spyOn(me, fn).and.callThrough();
      });
    }));

    function resolvePromise(promiseName) {
      return function (value) {
        if (!promises) {
          throw 'Promises not defined. Make sure to call resolve in an "it".';
        }

        expect(promises[promiseName].promise.then).toHaveBeenCalled();
        promises[promiseName].resolve(value);
      };
    }

    function rejectPromise(promiseName) {
      return function (value) {
        if (!promises) {
          throw 'Promises not defined. Make sure to call resolve in an "it".';
        }

        expect(promises[promiseName].promise.then).toHaveBeenCalled();
        promises[promiseName].reject(value);
      };
    }

    return me;
  }

  return me;
};
