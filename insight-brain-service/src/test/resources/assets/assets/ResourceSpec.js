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
});