/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, console */
(function () {
	'use strict';

	var hudson = false,
		tested = null,
		outstanding = [];

	function startTest($http, baseUrl) {
		function complete() {
			angular.forEach(outstanding, function (fn, key) {
				fn();
			});
		}

		tested = $http.get(baseUrl.get() + '/../../../crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)').success(function () {
			hudson = true;
			tested = true;
			complete();
		}).error(function (xhr, status) {
				tested = true;
				if (status !== 404 && console) {
					console.log('Got (' + status + ') while checking for Hudson API');
				}
				complete();
			});
	}

	function wrap($http, method, args, baseUrl) {
		var success = [],
			error = [],
			result = {
				success: function (fn) {
					success.push(fn);
					return this;
				},
				error: function (fn) {
					error.push(fn);
					return this;
				}
			},
			iter = function (arr, me, args) {
				angular.forEach(arr, function (fn) {
					fn.apply(me, args);
				});
			},
			successFn = function (data) {
				var header = data !== null ? data.split(':') : null,
					config = { headers: {  } };

				if (header !== null && header.length === 2) {
					config.headers[header[0]] = header[1];
				}
				if (args.length < 3) {
					args.push(config);
				} else {
					args[2] = angular.extend(config, args[2]);
				}

				method(args[0], args[1], args[2]).success(function () {
					iter(success, this, arguments);
				}).error(function () {
						iter(error, this, arguments);
					});
			},
			request = function () {
				if (hudson === true) {
					$http.get(baseUrl.get() + '/../../../crumbIssuer/api/xml', {
						params: {
							timestamp: new Date().getTime(),
							xpath: 'concat(//crumbRequestField,":",//crumb)'
						}
					}).success(successFn).error(function () {
							iter(error, this, arguments);
						});
				} else {
					successFn(null);
				}
			};
		if (tested === true) {
			request();
		} else {
			outstanding.push(request);
		}
		return result;
	}

	angular.module('Hudson', ['CLMLocation']).service('hudson', ['$http', 'BaseUrl', function ($http, baseUrl) {
		if (tested === null) {
			startTest($http, baseUrl);
		}
		return {
			post: function () {
				//note that we are not using angular.copy here, as the special arguments object is not iterable properly in IE8
				var argArray = [], i;
				for (i = 0; i < arguments.length; i++) {
					argArray.push(arguments[i]);
				}
				if (!hudson && tested === true) {
					return $http.post.apply($http, argArray);
				}
				return wrap($http, $http.post, argArray, baseUrl);
			},
			xhrPost: function () {
				var xhr = new XMLHttpRequest();
				return {
					xhr: xhr,
					post: function () {
						var argArray = [], i;
						for (i = 0; i < arguments.length; i++) {
							argArray.push(arguments[i]);
						}
						if (!hudson && tested === true) {
							return xhr.send.apply(xhr, argArray);
						}
						return wrap($http, xhr.send, argArray, baseUrl);
					}
				};
			},
			ajaxPost: function () {
				var argArray = [], i;
				for (i = 0; i < arguments.length; i++) {
					// apply necessary multi form properties
					if (i === 0) {
						var argument = arguments[i];
						argument.cache = false;
						argument.contentType = false;
						argument.processData = false;
						argument.type = 'POST';
					}
					argArray.push(arguments[i]);
				}
				if (!hudson && tested === true) {
					return jQuery.ajax.apply(jQuery, argArray);
				}
				return wrap($http, jQuery.ajax, argArray, baseUrl);
			}
		};
	}]);
}());