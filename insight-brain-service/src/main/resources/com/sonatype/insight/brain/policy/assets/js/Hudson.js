/*global angular, console */
(function () {
	'use strict';

	var hudson = false,
	    tested = null,
	    outstanding = [];

	function startTest($http, clmLocations) {
		function complete() {
			angular.forEach(outstanding, function (fn, key) {
				fn();
			});
		}
		tested = $http.get(clmLocations.getBaseUrl() + '/../../../crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)').success(function () {
			hudson = true;
			tested = true;
			complete();
		}).error(function (xhr, msg) {
			tested = true;
			if (xhr && xhr.status !== 404 && console) {
				console.log('Got (' + xhr.status + ') while checking for Hudson API');
			}
			complete();
		});
	}

	function wrap($http, method, args, clmLocations) {
		var success = [],
			error = [],
			result = {
				success : function (fn) {
					success.push(fn);
					return this;
				},
				error : function (fn) {
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
					config = { headers : {  } };

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
					$http.get(clmLocations.getBaseUrl() + '/../../../crumbIssuer/api/xml', {
                        params : {
						    timestamp : new Date().getTime(),
                            xpath : 'concat(//crumbRequestField,":",//crumb)'
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

	angular.module('Hudson', ['CLMLocation']).service('hudson', ['$http', 'CLMLocations', function ($http, clmLocations) {
		if (tested === null) {
			startTest($http, clmLocations);
		}
		return {
			post : function () {
				if (!hudson && tested === true) {
					return $http.post.apply($http, angular.copy(arguments, []));
				}
				return wrap($http, $http.post, angular.copy(arguments, []), clmLocations);
			}
		};
	}]);
}());