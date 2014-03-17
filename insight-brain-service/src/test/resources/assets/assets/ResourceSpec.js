describe('Resource', function() {
  'use strict';
  var storeUrl = 'http://localhost:8234/';
  var relatedStoreUrl = function(result) {
    return 'http://localhost:8234/related/' + result.id;
  };

  beforeEach(module('ResourceModule'));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('Get', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
          id: 'id',
          url: storeUrl,
          template: { id: null}
        }),
        errorSpy = jasmine.createSpy('errorSpy'),
        result = null;

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' },
      { id: 'bar' }
    ]);

    store.get().then(function() {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    }, errorSpy);
    $httpBackend.flush();

    expect(errorSpy).not.toHaveBeenCalled();
    expect(result[0].id).toEqual('foo');
    expect(result[1].id).toEqual('bar');
  }));

  it('Gets with relational collection', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null },
      relationalConfigs: {
        'related': {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null }
        }
      }
    });
    var errorSpy = jasmine.createSpy('errorSpy');
    var result = null;

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' },
      { id: 'bar' }
    ]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'foo' })).respond([
      { relatedId: 'relatedFoo' },
      { relatedId: 'relatedFooTwo' }
    ]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'bar' })).respond([
      { relatedId: 'relatedBar' }
    ]);

    store.get().then(function() {
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

  it('Error -> Get', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
          id: 'id',
          url: storeUrl,
          template: { id: null}
        }),
        spy = jasmine.createSpy('spy'),
        errorSpy = jasmine.createSpy('errorSpy'),
        result = null;

    $httpBackend.expectGET(storeUrl).respond(function() {
      return [0, 'Error', []];
    });
    store.get().then(spy, errorSpy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();
    expect(errorSpy).toHaveBeenCalledWith({
      status: 0,
      data: 'Error',
      headers: jasmine.any(Function),
      config: jasmine.any(Object)
    });

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' },
      { id: 'bar' }
    ]);

    spy = jasmine.createSpy('errorSpy');
    store.get().then(function() {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    }, spy);
    $httpBackend.flush();

    expect(spy).not.toHaveBeenCalled();
    expect(result[0].id).toEqual('foo');
    expect(result[1].id).toEqual('bar');
  }));

  it('Error -> Get Related', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null },
      relationalConfigs: {
        'related': {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null }
        }
      }
    });
    var successSpy = jasmine.createSpy('successSpy');
    var errorSpy = jasmine.createSpy('errorSpy');

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' },
      { id: 'bar' }
    ]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'foo' })).respond([
      { relatedId: 'relatedFoo' },
      { relatedId: 'relatedFooTwo' }
    ]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'bar' })).respond(function() {
      return [0, 'Error', []];
    });

    store.get().then(successSpy, errorSpy);
    $httpBackend.flush();

    expect(successSpy).not.toHaveBeenCalled();
    expect(errorSpy).toHaveBeenCalledWith({
      status: 0,
      data: 'Error',
      headers: jasmine.any(Function),
      config: jasmine.any(Object)
    });
  }));

  it('Refreshes', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
          id: 'id',
          url: storeUrl,
          template: { id: null}
        }),
        spy = jasmine.createSpy('spy'),
        result = null;

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' },
      { id: 'bar' }
    ]);

    store.get().then(function() {
      expect(arguments[0].length).toEqual(2);
    }, spy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' }
    ]);
    store.refresh().then(function() {
      expect(arguments[0].length).toEqual(1);
    }, spy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();
  }));

  it('Refreshes related', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null },
      relationalConfigs: {
        'related': {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null }
        }
      }
    });
    var spy = jasmine.createSpy('spy');

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' },
      { id: 'bar' }
    ]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'foo' })).respond([
      { relatedId: 'relatedFoo' },
      { relatedId: 'relatedFooTwo' }
    ]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'bar' })).respond([
      { relatedId: 'relatedBar' }
    ]);

    store.get().then(function() {
      expect(arguments[0].length).toEqual(2);
      expect(arguments[0][0].related.length).toEqual(2);
      expect(arguments[0][1].related.length).toEqual(1);
    }, spy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' }
    ]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'foo' })).respond([
      { relatedId: 'relatedFoo' }
    ]);
    store.refresh().then(function() {
      expect(arguments[0].length).toEqual(1);
      expect(arguments[0][0].related.length).toEqual(1);

    }, spy);
    $httpBackend.flush();
    expect(spy).not.toHaveBeenCalled();
  }));

  it('Create', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
          id: 'id',
          url: storeUrl,
          template: { data: [], id: null }
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

    expect(spy).toHaveBeenCalledWith({
      data: [ 'foo' ],
      id: 'bar',
      $new : false,
      isDirty: jasmine.any(Function),
      $updateOriginal: jasmine.any(Function),
      $getOriginal: jasmine.any(Function),
      $revert: jasmine.any(Function),
      $clone: jasmine.any(Function),
      $save: jasmine.any(Function),
      $delete: jasmine.any(Function)
    });
    expect(errorSpy).not.toHaveBeenCalled();

    expect(firstObj.data).toEqual(['foo']);
    expect(firstObj.id).toEqual('bar');
  }));

  it('Creates related', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null, data: [] },
      relationalConfigs: {
        'related': {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null, relatedData: [] }
        }
      }
    });

    var spy = jasmine.createSpy('spy'),
        errorSpy = jasmine.createSpy('errorSpy'),
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
    $httpBackend.expectPUT(relatedStoreUrl({ id: 'bar' })).respond([
      { relatedId: 'relatedBar', relatedData: ['relatedFoo'] }
    ]);
    firstObj.$save().then(function(result) {
      expect(firstObj.related).not.toBeUndefined();
      expect(firstObj.data).toEqual(['foo']);
      expect(firstObj.related[0].relatedData).toEqual(['relatedFoo']);
    }, errorSpy);
    $httpBackend.flush();

    expect(errorSpy).not.toHaveBeenCalled();

    expect(firstObj.data).toEqual(['foo']);
    expect(firstObj.id).toEqual('bar');
  }));

  it('Reverts related', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
      id: 'id',
      url: storeUrl,
      template: { id: null, data: [] },
      relationalConfigs: {
        'related': {
          id: 'relatedId',
          url: relatedStoreUrl,
          template: { relatedId: null, relatedData: [] }
        }
      }
    });
    var result = null;
    var errorSpy = jasmine.createSpy('errorSpy');

    $httpBackend.expectGET(storeUrl).respond([
      { id: 'foo' }
    ]);
    $httpBackend.expectGET(relatedStoreUrl({ id: 'foo' })).respond([
      { relatedId: 'relatedFoo' },
      { relatedId: 'relatedFooTwo' }
    ]);

    store.get().then(function() {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    }, errorSpy);
    $httpBackend.flush();

    expect(errorSpy).not.toHaveBeenCalled();
    expect(result[0].related.length).toEqual(2);
    result[0].related.push({relatedId: 'relatedBar'});
    result[0].related.push({relatedId: 'relatedBaz'});
    expect(result[0].related.length).toEqual(4);

    result[0].$revert();
    expect(result[0].related.length).toEqual(2);
    expect(result[0].related[0].relatedId).toEqual('relatedFoo');
    expect(result[0].related[1].relatedId).toEqual('relatedFooTwo');
  }));

  it('Clone', inject(function(CLMResource, $httpBackend) {
    var store = CLMResource.getStore({
          id: 'id',
          url: storeUrl,
          template: { id: null, data: [] }
        }),
        successSpy = jasmine.createSpy('successSpy'),
        result = null,
        clone = null;

    $httpBackend.whenGET(storeUrl).respond([
      { id: 'foo', data: [] },
      { id: 'bar', data: [] }
    ]);

    store.get().then(function() {
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

  describe('isDirty', function() {
    var scope, store, data;

    beforeEach(inject(function($rootScope, CLMResource, $httpBackend) {
      store = CLMResource.getStore({
        id: 'id',
        url: storeUrl,
        template: { id: null, data: [] },
        relationalConfigs: {
          'related': {
            id: 'relatedId',
            url: relatedStoreUrl,
            template: { relatedId: null }
          }
        }
      });

      $httpBackend.expectGET(storeUrl).respond([
        { id: 'foo', name: 'foo', arr: ['a', 'b'], obj: { id: 'bar', name: 'bar' } }
      ]);
      $httpBackend.expectGET(relatedStoreUrl({ id: 'foo' })).respond([
        { relatedId: 'relatedFoo' },
        { relatedId: 'relatedFooTwo' }
      ]);
      store.get().then(function() {
        data = arguments[0];
      });
      $httpBackend.flush();
      scope = $rootScope.$new();
    }));

    afterEach(function() {
      scope.$destroy();
      store = null;
    });

    it('Added Property', function() {
      // Add property
      data[0].blah = true;
      expect(data[0].isDirty()).toEqual(true);
      delete(data[0].blah);
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Added Property + $$hashKey', function() {
      scope.data = data;
      data[0].blah = true;
      data[0].$$hashKey = 'asdlfkj';

      expect(data[0].isDirty()).toEqual(true);

      delete(data[0].blah);
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Update Property', function() {
      data[0].name = 'foo2';
      expect(data[0].isDirty()).toEqual(true);
      data[0].name = 'foo';
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Update Property + $$hashKey', function() {
      scope.data = data;
      data[0].name = 'foo2';
      data[0].$$hashKey = 'asdlfkj';
      expect(data[0].isDirty()).toEqual(true);

      data[0].name = 'foo';
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Remove Property', function() {
      delete data[0].name;
      expect(data[0].isDirty()).toEqual(true);
      data[0].name = 'foo';
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Remove Property + $$hashKey', function() {
      scope.data = data;
      delete data[0].name;
      data[0].$$hashKey = 'asdlfkj';
      expect(data[0].isDirty()).toEqual(true);

      data[0].name = 'foo';
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Array Property', function() {
      data[0].arr.push('c');
      expect(data[0].isDirty()).toEqual(true);
      data[0].arr.pop();
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Object Property', function() {
      // Add property
      data[0].obj.blah = true;
      expect(data[0].isDirty()).toEqual(true);
      delete(data[0].obj.blah);
      expect(data[0].isDirty()).toEqual(false);
    });

    it('Add related', function() {
      data[0].related.push({ relatedId: 'relatedBar'});
      expect(data[0].isDirty()).toBeTruthy();
      data[0].related.pop();
      expect(data[0].isDirty()).not.toBeTruthy();
    });

    it('Removes related', function() {
      var sliced = data[0].related.splice(0, 1);
      expect(data[0].isDirty()).toBeTruthy();
      data[0].related.push(sliced[0]);
      expect(data[0].isDirty()).not.toBeTruthy();
    });
  });

  describe('Delete', function() {
    it('Existing Object', inject(function(CLMResource, $httpBackend) {
      var store = CLMResource.getStore({
            id: 'id',
            url: storeUrl,
            template: { id: null }
          }),
          contents = null,
          spy = jasmine.createSpy('spy'),
          errorSpy = jasmine.createSpy('errorSpy');

      $httpBackend.expectGET(storeUrl).respond([
        { id: 'foo' },
        { id: 'bar' }
      ]);
      store.get().then(function() {
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

    it('Error', inject(function(CLMResource, $httpBackend) {
      var store = CLMResource.getStore({
            id: 'id',
            url: storeUrl,
            template: { id: null }
          }),
          contents = null,
          spy = jasmine.createSpy('spy'),
          errorSpy = jasmine.createSpy('errorSpy');

      $httpBackend.expectGET(storeUrl).respond([
        { id: 'foo' },
        { id: 'bar' }
      ]);
      store.get().then(function() {
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
        config: jasmine.any(Object)
      });
      expect(contents.length).toEqual(2);
    }));

    it('Delete New Object', inject(function(CLMResource, $httpBackend, $rootScope) {
      var store = CLMResource.getStore({
            id: 'id',
            url: storeUrl,
            template: { id: null }
          }),
          contents = null,
          spy = jasmine.createSpy('spy'),
          errorSpy = jasmine.createSpy('errorSpy');

      $httpBackend.expectGET(storeUrl).respond([
        { id: 'foo' },
        { id: 'bar' }
      ]);
      store.get().then(function() {
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
});