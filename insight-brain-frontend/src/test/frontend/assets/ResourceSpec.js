/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import resourceModule from '../../../main/frontend/Resource';

describe('Resource', function () {
  var storeUrl = 'http://localhost:8234/';
  var relatedStoreUrl = function (result) {
    return 'http://localhost:8234/related/' + result.id;
  };

  beforeEach(angular.mock.module(resourceModule.name));

  afterEach(inject(function ($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('Get', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
        id: 'id',
        url: storeUrl,
        template: { id: null },
      }),
      errorSpy = jasmine.createSpy('errorSpy'),
      result = null;

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);

    store.get().then(function () {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    }, errorSpy);
    $httpBackend.flush();

    expect(errorSpy).not.toHaveBeenCalled();
    expect(result[0].id).toEqual('foo');
    expect(result[1].id).toEqual('bar');
  }));

  it('Gets with relational collection', inject(function (
    StoreFactory,
    $httpBackend
  ) {
    var store = StoreFactory.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null },
      relationalConfigs: {
        related: {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null },
        },
      },
    });
    var errorSpy = jasmine.createSpy('errorSpy');
    var result = null;

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
    $httpBackend
      .expectGET(relatedStoreUrl({ id: 'foo' }))
      .respond([{ relatedId: 'relatedFoo' }, { relatedId: 'relatedFooTwo' }]);
    $httpBackend
      .expectGET(relatedStoreUrl({ id: 'bar' }))
      .respond([{ relatedId: 'relatedBar' }]);

    store.get().then(function () {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    }, errorSpy);
    $httpBackend.flush();

    expect(errorSpy).not.toHaveBeenCalled();
    expect(result[0].id).toEqual('foo');
    expect(result[0].related).not.toBeUndefined();
    expect(result[0].related[0].relatedId).toEqual('relatedFoo');
    expect(result[0].related[1].relatedId).toEqual('relatedFooTwo');
    expect(result[1].id).toEqual('bar');
    expect(result[1].related).not.toBeUndefined();
    expect(result[1].related[0].relatedId).toEqual('relatedBar');
  }));

  it('Error -> Get', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
        id: 'id',
        url: storeUrl,
        template: { id: null },
      }),
      spy = jasmine.createSpy('spy'),
      errorSpy = jasmine.createSpy('errorSpy'),
      result = null;

    $httpBackend.expectGET(storeUrl).respond(function () {
      return [0, 'Error', []];
    });
    store.get().then(spy, errorSpy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();
    expect(errorSpy).toHaveBeenCalledWith({
      status: 0,
      data: 'Error',
      headers: jasmine.any(Function),
      config: jasmine.any(Object),
    });

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);

    spy = jasmine.createSpy('errorSpy');
    store.get().then(function () {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    }, spy);
    $httpBackend.flush();

    expect(spy).not.toHaveBeenCalled();
    expect(result[0].id).toEqual('foo');
    expect(result[1].id).toEqual('bar');
  }));

  it('Error -> Get Related', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null },
      relationalConfigs: {
        related: {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null },
        },
      },
    });
    var successSpy = jasmine.createSpy('successSpy');
    var errorSpy = jasmine.createSpy('errorSpy');

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
    $httpBackend
      .expectGET(relatedStoreUrl({ id: 'foo' }))
      .respond([{ relatedId: 'relatedFoo' }, { relatedId: 'relatedFooTwo' }]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'bar' })).respond(function () {
      return [0, 'Error', []];
    });

    store.get().then(successSpy, errorSpy);
    $httpBackend.flush();

    expect(successSpy).not.toHaveBeenCalled();
    expect(errorSpy).toHaveBeenCalledWith({
      status: 0,
      data: 'Error',
      headers: jasmine.any(Function),
      config: jasmine.any(Object),
    });
  }));

  it('Refreshes', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
        id: 'id',
        url: storeUrl,
        template: { id: null },
      }),
      spy = jasmine.createSpy('spy');

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);

    store.get().then(function () {
      expect(arguments[0].length).toEqual(2);
    }, spy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }]);
    store.refresh().then(function () {
      expect(arguments[0].length).toEqual(1);
    }, spy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();
  }));

  it('Refreshes and gets simultaneously', inject(function (
    StoreFactory,
    $httpBackend
  ) {
    var store = StoreFactory.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null },
    });

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
    store.get().then(function () {
      // The original request is fulfilled and 2 items are returned.
      expect(arguments[0].length).toEqual(2);
    });
    $httpBackend.whenGET(storeUrl).respond([{ id: 'foo' }]);
    store.refresh().then(function () {
      // The original request is fulfilled and 2 items are returned.
      expect(arguments[0].length).toEqual(2);
    });
    $httpBackend.flush();

    store.refresh().then(function () {
      // Once the promise is fulfilled, the service is requeried by a refresh
      expect(arguments[0].length).toEqual(1);
    });
    $httpBackend.flush();
  }));

  it('Refreshes related', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null },
      relationalConfigs: {
        related: {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null },
        },
      },
    });
    var spy = jasmine.createSpy('spy');

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
    $httpBackend
      .expectGET(relatedStoreUrl({ id: 'foo' }))
      .respond([{ relatedId: 'relatedFoo' }, { relatedId: 'relatedFooTwo' }]);
    $httpBackend
      .expectGET(relatedStoreUrl({ id: 'bar' }))
      .respond([{ relatedId: 'relatedBar' }]);

    store.get().then(function () {
      expect(arguments[0].length).toEqual(2);
      expect(arguments[0][0].related.length).toEqual(2);
      expect(arguments[0][1].related.length).toEqual(1);
    }, spy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }]);
    $httpBackend
      .expectGET(relatedStoreUrl({ id: 'foo' }))
      .respond([{ relatedId: 'relatedFoo' }]);
    store.refresh().then(function () {
      expect(arguments[0].length).toEqual(1);
      expect(arguments[0][0].related.length).toEqual(1);
    }, spy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();
  }));

  it('Create', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
        id: 'id',
        url: storeUrl,
        template: { data: [], id: null },
      }),
      spy = jasmine.createSpy('spy'),
      errorSpy = jasmine.createSpy('errorSpy'),
      firstObj = store.create();

    firstObj.data.push('foo');
    expect(firstObj.$new).toBeTruthy();
    expect(firstObj.data).toEqual(['foo']);
    expect(store.create().data).toEqual([]);

    $httpBackend.expectPOST(storeUrl).respond({ data: ['foo'], id: 'bar' });
    firstObj.$save().then(spy, errorSpy);
    $httpBackend.flush();

    var resource = spy.calls.first().args[0];
    expect(resource.data).toEqual(['foo']);
    expect(resource.id).toEqual('bar');
    expect(resource.$new).toEqual(false);
    expect(errorSpy).not.toHaveBeenCalled();

    expect(firstObj.data).toEqual(['foo']);
    expect(firstObj.id).toEqual('bar');
  }));

  it('Creates related', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null, data: [] },
      relationalConfigs: {
        related: {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null, relatedData: [] },
        },
      },
    });

    var errorSpy = jasmine.createSpy('errorSpy'),
      firstObj = store.create();

    firstObj.data.push('foo');
    expect(firstObj.related).not.toBeUndefined();
    expect(firstObj.data).toEqual(['foo']);
    var relatedObj = store.create('related');
    relatedObj.relatedData.push('relatedFoo');
    firstObj.related.push(relatedObj);
    expect(firstObj.related[0].relatedData).toEqual(['relatedFoo']);
    expect(store.create().data).toEqual([]);
    expect(store.create().related.length).toEqual(0);

    $httpBackend.expectPOST(storeUrl).respond({ data: ['foo'], id: 'bar' });
    $httpBackend
      .expectPUT(relatedStoreUrl({ id: 'bar' }))
      .respond([{ relatedId: 'relatedBar', relatedData: ['relatedFoo'] }]);
    firstObj.$save().then(function () {
      expect(firstObj.related).not.toBeUndefined();
      expect(firstObj.data).toEqual(['foo']);
      expect(firstObj.related[0].relatedData).toEqual(['relatedFoo']);
    }, errorSpy);
    $httpBackend.flush();

    expect(errorSpy).not.toHaveBeenCalled();

    expect(firstObj.data).toEqual(['foo']);
    expect(firstObj.id).toEqual('bar');
  }));

  it('Reverts related', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null, data: [] },
      relationalConfigs: {
        related: {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null, relatedData: [] },
        },
      },
    });
    var result = null;
    var errorSpy = jasmine.createSpy('errorSpy');

    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }]);
    $httpBackend
      .expectGET(relatedStoreUrl({ id: 'foo' }))
      .respond([{ relatedId: 'relatedFoo' }, { relatedId: 'relatedFooTwo' }]);

    store.get().then(function () {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    }, errorSpy);
    $httpBackend.flush();

    expect(errorSpy).not.toHaveBeenCalled();
    expect(result[0].related.length).toEqual(2);
    result[0].related.push({ relatedId: 'relatedBar' });
    result[0].related.push({ relatedId: 'relatedBaz' });
    expect(result[0].related.length).toEqual(4);

    result[0].$revert();
    expect(result[0].related.length).toEqual(2);
    expect(result[0].related[0].relatedId).toEqual('relatedFoo');
    expect(result[0].related[1].relatedId).toEqual('relatedFooTwo');
  }));

  it('Clone', inject(function (StoreFactory, $httpBackend) {
    var store = StoreFactory.getStore({
        id: 'id',
        url: storeUrl,
        template: { id: null, data: [] },
      }),
      successSpy = jasmine.createSpy('successSpy'),
      result = null,
      clone = null;

    $httpBackend.whenGET(storeUrl).respond([
      { id: 'foo', data: [] },
      { id: 'bar', data: [] },
    ]);

    store.get().then(function () {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    });
    $httpBackend.flush();

    clone = result[0].$clone();
    clone.data.push('foo');

    expect(result[0].data).toEqual([]);
    expect(clone.data).toEqual(['foo']);

    $httpBackend.expectPUT(storeUrl).respond({ id: 'foo', data: ['foo'] });
    clone.$save().then(successSpy);
    $httpBackend.flush();
    expect(successSpy).toHaveBeenCalled();

    expect(result[0].data).toEqual(['foo']);
    expect(clone.data).toEqual(['foo']);
  }));

  describe('isDirty', function () {
    var scope, store, data;

    beforeEach(inject(function ($rootScope, StoreFactory, $httpBackend) {
      store = StoreFactory.getStore({
        id: 'id',
        url: storeUrl,
        template: { id: null, data: [] },
        relationalConfigs: {
          related: {
            id: 'relatedId',
            url: relatedStoreUrl,
            template: { relatedId: null },
          },
        },
      });

      $httpBackend.expectGET(storeUrl).respond([
        {
          id: 'foo',
          name: 'foo',
          arr: ['a', 'b'],
          obj: { id: 'bar', name: 'bar' },
        },
      ]);
      $httpBackend
        .expectGET(relatedStoreUrl({ id: 'foo' }))
        .respond([{ relatedId: 'relatedFoo' }, { relatedId: 'relatedFooTwo' }]);
      store.get().then(function () {
        data = arguments[0];
      });
      $httpBackend.flush();
      scope = $rootScope.$new();
    }));

    afterEach(function () {
      scope.$destroy();
      store = null;
    });

    it('Added Property', function () {
      // Add property
      data[0].blah = true;
      expect(data[0].isDirty()).toEqual(true);
      delete data[0].blah;
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Added Property + $$hashKey', function () {
      scope.data = data;
      data[0].blah = true;
      data[0].$$hashKey = 'asdlfkj';

      expect(data[0].isDirty()).toEqual(true);

      delete data[0].blah;
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Update Property', function () {
      data[0].name = 'foo2';
      expect(data[0].isDirty()).toEqual(true);
      data[0].name = 'foo';
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Update Property + $$hashKey', function () {
      scope.data = data;
      data[0].name = 'foo2';
      data[0].$$hashKey = 'asdlfkj';
      expect(data[0].isDirty()).toEqual(true);

      data[0].name = 'foo';
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Remove Property', function () {
      delete data[0].name;
      expect(data[0].isDirty()).toEqual(true);
      data[0].name = 'foo';
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Remove Property + $$hashKey', function () {
      scope.data = data;
      delete data[0].name;
      data[0].$$hashKey = 'asdlfkj';
      expect(data[0].isDirty()).toEqual(true);

      data[0].name = 'foo';
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Array Property', function () {
      data[0].arr.push('c');
      expect(data[0].isDirty()).toEqual(true);
      data[0].arr.pop();
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Object Property', function () {
      // Add property
      data[0].obj.blah = true;
      expect(data[0].isDirty()).toEqual(true);
      delete data[0].obj.blah;
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Add related', function () {
      data[0].related.push({ relatedId: 'relatedBar' });
      expect(data[0].isDirty()).toBeTruthy();
      data[0].related.pop();
      expect(data[0].isDirty()).not.toBeTruthy();
    });

    it('Removes related', function () {
      var sliced = data[0].related.splice(0, 1);
      expect(data[0].isDirty()).toBeTruthy();
      data[0].related.push(sliced[0]);
      expect(data[0].isDirty()).not.toBeTruthy();
    });

    it('Property with explicitly undefined value', function () {
      data[0].blah = undefined;
      expect(data[0].isDirty()).toEqual(true);
    });

    it('Properties with empty string, null or undefined values', function () {
      data[0].$updateOriginal({
        id: undefined,
        name: 'foo',
        arr: ['a', 'b'],
        obj: { id: 'bar', name: 'bar' },
      });
      expect(data[0].isDirty()).toEqual(false);
      data[0].id = '';
      expect(data[0].isDirty()).toEqual(false);
      data[0].id = null;
      expect(data[0].isDirty()).toEqual(false);
      data[0].id = undefined;
      expect(data[0].isDirty()).toEqual(false);
      data[0].id = '123';
      expect(data[0].isDirty()).toEqual(true);
    });
  });

  describe('Delete', function () {
    it('Existing Object', inject(function (StoreFactory, $httpBackend) {
      var store = StoreFactory.getStore({
          id: 'id',
          url: storeUrl,
          template: { id: null },
        }),
        contents = null,
        spy = jasmine.createSpy('spy'),
        errorSpy = jasmine.createSpy('errorSpy');

      $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
      store.get().then(function () {
        contents = arguments[0];
      });
      $httpBackend.flush();

      $httpBackend.expectDELETE(storeUrl + 'foo').respond({});
      contents[0].$delete().then(spy, errorSpy);
      $httpBackend.flush();
      expect(spy).toHaveBeenCalled();
      expect(errorSpy).not.toHaveBeenCalled();

      expect(contents.length).toEqual(1);
      expect(contents[0].id).toEqual('bar');
    }));

    it('Error', inject(function (StoreFactory, $httpBackend) {
      var store = StoreFactory.getStore({
          id: 'id',
          url: storeUrl,
          template: { id: null },
        }),
        contents = null,
        spy = jasmine.createSpy('spy'),
        errorSpy = jasmine.createSpy('errorSpy');

      $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
      store.get().then(function () {
        contents = arguments[0];
      });
      $httpBackend.flush();

      $httpBackend.expectDELETE(storeUrl + 'foo').respond(500);
      contents[0].$delete().then(spy, errorSpy);
      $httpBackend.flush();

      expect(spy).not.toHaveBeenCalled();
      expect(errorSpy).toHaveBeenCalledWith({
        data: undefined,
        status: 500,
        headers: jasmine.any(Function),
        config: jasmine.any(Object),
      });
      expect(contents.length).toEqual(2);
    }));

    it('Delete New Object', inject(function (
      StoreFactory,
      $httpBackend,
      $rootScope
    ) {
      var store = StoreFactory.getStore({
          id: 'id',
          url: storeUrl,
          template: { id: null },
        }),
        contents = null,
        spy = jasmine.createSpy('spy'),
        errorSpy = jasmine.createSpy('errorSpy');

      $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
      store.get().then(function () {
        contents = arguments[0];
      });
      $httpBackend.flush();

      var o = store.create();
      o.$delete().then(spy, errorSpy);
      $rootScope.$digest();
      expect(spy).toHaveBeenCalled();
      expect(errorSpy).not.toHaveBeenCalled();
      expect(contents.length).toEqual(2);
    }));
  });

  describe('getById', function () {
    var store;

    beforeEach(inject(function (StoreFactory, $httpBackend) {
      store = StoreFactory.getStore({
        id: 'id',
        url: storeUrl,
        type: 'app',
        template: { id: null },
      });

      $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
    }));

    describe('Store already loaded', function () {
      beforeEach(inject(function ($httpBackend) {
        store.get();
        $httpBackend.flush();
      }));

      it('entity exists', inject(function ($httpBackend, $timeout) {
        var result;
        store.getById('foo').then(function (entity) {
          result = entity;
        });
        $timeout.flush();
        expect(result.id).toEqual('foo');
      }));

      describe('missing entity', function () {
        it('reload finds', inject(function ($httpBackend, $timeout) {
          var result;
          store.getById('xxx').then(function (entity) {
            result = entity;
          });
          $httpBackend
            .expectGET(storeUrl)
            .respond([{ id: 'foo' }, { id: 'bar' }, { id: 'xxx' }]);
          $httpBackend.flush();
          $timeout.flush();

          expect(result.id).toEqual('xxx');
        }));

        it('reloads and still missing', inject(function (
          $httpBackend,
          $timeout
        ) {
          var result;
          store.getById('xxx').then(angular.noop, function (error) {
            result = error;
          });
          $httpBackend
            .expectGET(storeUrl)
            .respond([{ id: 'foo' }, { id: 'bar' }]);
          $httpBackend.flush();
          $timeout.flush();

          expect(result).toEqual('Could not find an app with ID xxx.');
        }));
      });
    });

    function nonReloadingTest() {
      it('exists', inject(function ($httpBackend, $timeout) {
        var result;
        store.getById('foo').then(function (entity) {
          result = entity;
        });
        $httpBackend.flush();
        $timeout.flush();
        expect(result.id).toEqual('foo');
      }));

      it('missing', inject(function ($httpBackend, $timeout) {
        var result;
        store.getById('xxx').then(angular.noop, function (error) {
          result = error;
        });
        $httpBackend.flush();
        $timeout.flush();

        expect(result).toEqual('Could not find an app with ID xxx.');
      }));
    }

    describe('Store not loaded', nonReloadingTest);

    describe('Store load in progress', function () {
      beforeEach(function () {
        store.get();
      });

      nonReloadingTest();
    });
  });

  it('peek with and without store loaded', inject(function (
    StoreFactory,
    $httpBackend
  ) {
    var store = StoreFactory.getStore({
      id: 'id',
      url: storeUrl,
      type: 'app',
      template: { id: null },
    });

    expect(store.peek()).toEqual([]);

    store.get();
    $httpBackend.expectGET(storeUrl).respond([{ id: 'foo' }, { id: 'bar' }]);
    $httpBackend.flush();

    var resources = store.peek();
    expect(resources).toBeDefined();
    expect(resources.length).toBe(2);
    expect(resources[0].id).toEqual('foo');
    expect(resources[1].id).toEqual('bar');
  }));

  describe('Observe', function () {
    var $httpBackend, store, callback, unregister, StoreObserveTypeConstant;

    beforeEach(inject([
      'StoreFactory',
      '$httpBackend',
      'store.observe.type.constant',
      function (StoreFactory, _$httpBackend_, _StoreObserveTypeConstant_) {
        $httpBackend = _$httpBackend_;
        StoreObserveTypeConstant = _StoreObserveTypeConstant_;

        store = StoreFactory.getStore({
          id: 'id',
          url: storeUrl,
          type: 'app',
          template: { id: null },
        });

        callback = jasmine.createSpy();
        unregister = store.observe(callback);

        store.get();
        $httpBackend
          .expectGET(storeUrl)
          .respond([{ id: 'foo' }, { id: 'bar' }]);
        $httpBackend.flush();
      },
    ]));

    afterEach(function () {
      unregister();
    });

    it('gets called on Load', function () {
      assertCallbackArguments(StoreObserveTypeConstant.UPDATE, ['foo', 'bar']);
    });

    it('gets called on Refresh', function () {
      callback.calls.reset();

      store.refresh();
      $httpBackend
        .expectGET(storeUrl)
        .respond([{ id: 'foo2' }, { id: 'bar2' }]);
      $httpBackend.flush();

      assertCallbackArguments(StoreObserveTypeConstant.UPDATE, [
        'foo2',
        'bar2',
      ]);
    });

    it('gets called on Resource Delete', function () {
      var newResource = store.create();
      newResource.id = 'abc';
      newResource.$save();
      $httpBackend.expectPOST(storeUrl).respond([newResource]);
      $httpBackend.flush();

      callback.calls.reset();
      newResource.$delete();
      $httpBackend.expectDELETE(storeUrl + 'abc').respond([]);
      $httpBackend.flush();

      expect(callback).toHaveBeenCalledWith(StoreObserveTypeConstant.DELETE, [
        newResource,
      ]);
    });

    it('gets called on Resource Save', function () {
      callback.calls.reset();

      var newResource = store.create();
      newResource.$save();
      $httpBackend.expectPOST(storeUrl).respond([newResource]);
      $httpBackend.flush();

      expect(callback).toHaveBeenCalledWith(StoreObserveTypeConstant.UPDATE, [
        newResource,
      ]);
    });

    it('unregisters properly', function () {
      callback.calls.reset();
      unregister();

      store.refresh();
      $httpBackend
        .expectGET(storeUrl)
        .respond([{ id: 'foo2' }, { id: 'bar2' }]);
      $httpBackend.flush();

      expect(callback).not.toHaveBeenCalled();
    });

    function assertCallbackArguments(type, ids) {
      expect(callback).toHaveBeenCalled();

      expect(callback.calls.mostRecent().args[0]).toEqual(type);

      var secondArgument = callback.calls.mostRecent().args[1];
      expect(secondArgument.length).toBe(2);
      expect(secondArgument[0].id).toEqual(ids[0]);
      expect(secondArgument[1].id).toEqual(ids[1]);
    }
  });
});
