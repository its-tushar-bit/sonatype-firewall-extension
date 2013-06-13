describe('Resource', function () {
	'use strict';
	var storeUrl = 'http://localhost:8234/';

	angular.module('Hudson', []).factory('hudson', ['$http', function($http){
		return $http;
	}]);
	
	beforeEach(module('ResourceModule', 'Hudson'));
	
	it('Get', inject(function (CLMResource, $httpBackend) {
		var store = CLMResource.getStore({
				id : 'id',
				url : storeUrl,
				template : { id : null}
			}),
			errorSpy = jasmine.createSpy('errorSpy'),
			result = null;

		$httpBackend.expectGET(storeUrl).respond([{ id : 'foo' }, { id : 'bar' }]);

		store.get().then(function () {
			expect(arguments.length).toEqual(1);
			result = arguments[0];
		}, errorSpy);
		$httpBackend.flush();

		expect(errorSpy).not.toHaveBeenCalled();
		expect(result[0].id).toEqual('foo');
		expect(result[1].id).toEqual('bar');
	}));

	it('Error -> Get', inject(function (CLMResource, $httpBackend) {
		var store = CLMResource.getStore({
				id : 'id',
				url : storeUrl,
				template : { id : null}
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
			status : 0,
			data : 'Error',
			headers : jasmine.any(Function),
			config : jasmine.any(Object)
		});

		$httpBackend.expectGET(storeUrl).respond([{ id : 'foo' }, { id : 'bar' }]);

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

	it('Refresh', inject(function (CLMResource, $httpBackend) {
		var store = CLMResource.getStore({
				id : 'id',
				url : storeUrl,
				template : { id : null}
			}),
			spy = jasmine.createSpy('spy'),
			result = null;

		$httpBackend.expectGET(storeUrl).respond([{ id : 'foo' }, { id : 'bar' }]);
	
		store.get().then(function () {
			expect(arguments[0].length).toEqual(2);
		}, spy);
		$httpBackend.flush();
		expect(spy).not.toHaveBeenCalled();

		$httpBackend.expectGET(storeUrl).respond([{ id : 'foo' }]);
		store.refresh().then(function () {
			expect(arguments[0].length).toEqual(1);
		}, spy);
		$httpBackend.flush();
		expect(spy).not.toHaveBeenCalled();
	}));

	it('Create', inject(function (CLMResource, $httpBackend) {
		var store = CLMResource.getStore({
				id : 'id',
				url : storeUrl,
				template : { data : [], id : null }
			}),
			spy = jasmine.createSpy('spy'),
			errorSpy = jasmine.createSpy('errorSpy'),
			firstObj = store.create();

		firstObj.data.push('foo');
		expect(firstObj.data).toEqual(['foo']);
		expect(store.create().data).toEqual([]);

		$httpBackend.expectPOST(storeUrl).respond({ data : ['foo'], id : 'bar' });
		firstObj.$save().then(spy, errorSpy);
		$httpBackend.flush();

		expect(spy).toHaveBeenCalledWith({ data : ['foo'], id : 'bar', isDirty : jasmine.any(Function), $updateOriginal : jasmine.any(Function), $getOriginal : jasmine.any(Function), $revert : jasmine.any(Function), $clone : jasmine.any(Function), $save : jasmine.any(Function), $delete : jasmine.any(Function) });
		expect(errorSpy).not.toHaveBeenCalled();

		expect(firstObj.data).toEqual(['foo']);
		expect(firstObj.id).toEqual('bar');
	}));

    describe('isDirty', function () {
        var scope, store, data;

        beforeEach(inject(function ($rootScope, CLMResource, $httpBackend) {
            store = CLMResource.getStore({
                id : 'id',
                url : storeUrl,
                template : { data : [], id : null }
            });

            $httpBackend.expectGET(storeUrl).respond([{ id : 'foo', name : 'foo', arr : ['a','b'], obj : { id : 'bar', name : 'bar' } }]);
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
            delete(data[0].blah);
            expect(data[0].isDirty()).toEqual(false);
        });

        it('Added Property + $$hashKey', function () {
            scope.data = data;
            data[0].blah = true;
            data[0].$$hashKey = 'asdlfkj';

            expect(data[0].isDirty()).toEqual(true);

            delete(data[0].blah);
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
            data[0].arr.pop()
            expect(data[0].isDirty()).toEqual(false);
        });

        it('Object Property', function () {
            // Add property
            data[0].obj.blah = true;
            expect(data[0].isDirty()).toEqual(true);
            delete(data[0].obj.blah);
            expect(data[0].isDirty()).toEqual(false);
        });
    });

	describe('Delete', function () {
		it('Existing Object', inject(function (CLMResource, $httpBackend) {
			var store = CLMResource.getStore({
					id : 'id',
					url : storeUrl,
					template : { id : null }
				}),
				contents = null,
				spy = jasmine.createSpy('spy'),
				errorSpy = jasmine.createSpy('errorSpy');

			$httpBackend.expectGET(storeUrl).respond([{ id : 'foo' }, { id : 'bar' }]);
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

		it('Error', inject(function (CLMResource, $httpBackend) {
			var store = CLMResource.getStore({
					id : 'id',
					url : storeUrl,
					template : { id : null }
				}),
				contents = null,
				spy = jasmine.createSpy('spy'),
				errorSpy = jasmine.createSpy('errorSpy');

			$httpBackend.expectGET(storeUrl).respond([{ id : 'foo' }, { id : 'bar' }]);
			store.get().then(function() {
				contents = arguments[0];
			});
			$httpBackend.flush();

			$httpBackend.expectDELETE(storeUrl + 'foo').respond(500);
			contents[0].$delete().then(spy, errorSpy);
			$httpBackend.flush();

			expect(spy).not.toHaveBeenCalled();
			expect(errorSpy).toHaveBeenCalledWith({
			    data : undefined,
			    status : 500,
			    headers : jasmine.any(Function),
			    config : jasmine.any(Object)
			});
			expect(contents.length).toEqual(2);
		}));

		it('Delete New Object', inject(function (CLMResource, $httpBackend, $rootScope) {
			var store = CLMResource.getStore({
					id : 'id',
					url : storeUrl,
					template : { id : null }
				}),
				contents = null,
				spy = jasmine.createSpy('spy'),
				errorSpy = jasmine.createSpy('errorSpy');

			$httpBackend.expectGET(storeUrl).respond([{ id : 'foo' }, { id : 'bar' }]);
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