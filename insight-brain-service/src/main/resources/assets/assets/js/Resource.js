/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
	'use strict';
	var module = angular.module('ResourceModule', []);

	function getErrorFn(deferred) {
		return function (data, status, headers, config) {
			deferred.reject({
				data: data,
				status : status,
				headers : headers,
				config : config
			});
		};
	}

	module.service('CLMResource', ['$q', '$http', 'hudson', function ($q, $http, hudson) {
		return {
			'getStore' : function (config) {
				var store = [],
					error = false,
					storeDeferred = null;
				config.id = config.id || 'id';

				function Resource(originalObject) {
					var original,
						me = this;

					this.isDirty = function () {
						return angular.equals(me, original);
					};
					this.$updateOriginal = function (updated) {
						original = updated;
						angular.extend(me, original);
					};

					this.$updateOriginal(originalObject);
				}

				function doLoad() {
					var localDeferred = null;

					storeDeferred = localDeferred = $q.defer();

					$http.get(config.url, { params : config.params }).success(function (data) {
						if (localDeferred === storeDeferred) {
							var result = [];
							angular.forEach(data, function (obj, i) {
								result.push(new Resource(obj));
							});
							store.splice(0, store.length)
							store.push.apply(store, result);
							storeDeferred.resolve(store);
						}
					}).error(function () {
						error = true;
					}).error(getErrorFn(storeDeferred));
				}

				Resource.prototype['$save'] = function () {
					var deferred = $q.defer(),
						id = this[config.id],
						me = this;
					if (id === null || angular.isUndefined(id)) {
						// Newly created object
						hudson.post(config.url, this, { params : config.params }).success(function (data) {
							me.$updateOriginal(data);
							store.push(me);
							deferred.resolve(me);
						}).error(getErrorFn(deferred));
					} else {
						// Update to existing objcet
						$http.put(config.url, this, { params : config.params }).success(function (data) {
							me.$updateOriginal(data);
							deferred.resolve(me);
						}).error(getErrorFn(deferred));
					}
					return deferred.promise;
				};

				Resource.prototype['$delete'] = function () {
					var deferred = $q.defer(),
						id = this[config.id],
						url = config.url.charAt(config.url.length - 1) === '/' ? config.url + id : config.url + '/' + id,
						index = -1,
						me = this;
					if (id !== null && angular.isDefined(id)) {
						$http['delete'](url, this, { params : config.params }).success(function () {
							// remove from store
							angular.forEach(store, function (candidate, candidateIndex) {
								if (candidate[config.id] === id) {
									index = candidateIndex;
								}
							});
							if (index !== -1) {
								store.splice(index, 1);
							}
						}).error(getErrorFn(deferred));
					}
				};

				return {
					'get' : function () {
						if (error || !storeDeferred) {
							// An error occured previously, or the store hasn't been loaded
							error = false;
							doLoad();
						}
						return storeDeferred.promise;
					},
					'create' : function () {
						return new Resource(config.template || {});
					},
					'refresh' : function () {
						error = false;
						doLoad();
						return storeDeferred.promise;
					}
				};
			}
		};
	}]);
}());