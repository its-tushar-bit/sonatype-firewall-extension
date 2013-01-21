(function(){
	'use strict';

	var hudson = false;

	// This is a bit of a hack, but AngularJS won't inject the $http service early
	$.get(insightApp.getBaseUrl() + '/../../../crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)').success(function(){
		hudson = true;
	}).error(function(xhr, msg){
		if (xhr && xhr.status !== 404) {
			console.log('Got (' + xhr.status + ') while checking for Hudson API');
		}
	});

	function wrap($http, method, args) {
		var success = [],
			error = [],
			result = {
				success : function(fn) {
					success.push(fn);
					return this;
				},
				error : function(fn) {
					error.push(fn);
					return this;
				}
			},
			iter = function (arr, me, args) {
				angular.forEach(arr, function (fn) {
					fn.apply(me, args);
				});
			};

		$http.get(insightApp.getBaseUrl() + '/../../../crumbIssuer/api/xml',
					{
						params : {
							timestamp : new Date().getTime(),
							xpath : 'concat(//crumbRequestField,":",//crumb)'
						},
						headers : {
						   'Cache-Control' : 'no-store',
						   'Pragma' : 'no-cache'
						}
					}).success(function (data) {
			var header = data.split(':'),
				config = { headers : {  } };
				config.headers[header[0]] = header[1];
			if (args.length < 3) {
				args.push(config);
			} else {
				args[2] = angular.extend(config, args[2]);
			}
			method(args[0], args[1], args[2]).success(function(){
				iter(success, this, arguments);
			}).error(function(){
				iter(error, this, arguments);
			});
		}).error(function(){
			iter(error, this, arguments);
		});

		return result;
	};

	angular.module('Hudson', []).service('hudson', ['$http', function($http){
		return {
			post : function() {
				if (!hudson) {
					return $http.post.apply($http, angular.copy(arguments, []));
				}
				return wrap($http, $http.post, angular.copy(arguments, []));
			}/*,  It seems put isn't monitored for XSRF
			put : function() {
				return foo($http, $http.put, angular.copy(arguments, []));
			}*/
		};
	}]);
}());