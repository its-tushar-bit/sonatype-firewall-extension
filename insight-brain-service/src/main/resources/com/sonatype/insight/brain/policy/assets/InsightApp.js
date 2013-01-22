/*global angular, $, window, console, clmBuildTimestamp */
(function () {
    "use strict";
    angular.module('CLMLocation', []).factory('CLMLocations', function () {
        return {
            getQueryString : function (key) {
                var results = new RegExp('[\\?&]' + key + '=([^&#]*)').exec(window.location.href);
                if (results) {
                    return results[1];
                }
            },

            getAppId : function () {
                if (this.appId) {
                    return this.appId;
                }

                this.appId = this.getQueryString('appId');

                return this.appId;
            },

            getBaseUrl : function () {
                if (this.baseUrl) {
                    return this.baseUrl;
                }

                this.baseUrl = '';

                var idx = window.location.href.indexOf('/policy-assets/');

                if (idx > -1) {
                    this.baseUrl = window.location.href.substring(0, idx);
                }

                return this.baseUrl;
            },

            getLabelsUrl : function () {
                return this.getBaseUrl() + '/rest/label/application/' + this.getAppId();
            },

            getDeleteLabelsUrl : function (label) {
                return this.getBaseUrl() + '/rest/label/application/' + this.getAppId() + '/' + label.id;
            },

            getConditionTypeUrl : function () {
                return this.getBaseUrl() + '/rest/policy/conditionType';
            },

            getActionTypeUrl : function () {
                return this.getBaseUrl() + '/rest/policy/actionType';
            },

            getActionStageUrl : function () {
                return this.getBaseUrl() + '/rest/policy/stageType';
            },

            getConditionValueTypeUrl : function () {
                return this.getBaseUrl() + '/rest/conditionValueType/' + this.getAppId();
            },

            getPolicyUrl : function () {
                return this.getBaseUrl() + '/rest/policy/' + this.getAppId();
            }
        };
    });
}());

var insightApp = angular.module('insightApp', ['Labels', 'Policy', 'ngSanitize'], ['$routeProvider', function ($routeProvider) {
	$routeProvider.when('/policy', {
		templateUrl : 'components/policy.html?' + clmBuildTimestamp,
		controller : 'InsightPolicyController'
	});
	$routeProvider.when('/labels', {
		templateUrl : 'components/labels.html?' + clmBuildTimestamp,
		controller : 'LabelController'
	});
	$routeProvider.when('/license-group', {
		templateUrl : 'components/license-group.html?' + clmBuildTimestamp
	});
	$routeProvider.otherwise({redirectTo : '/policy'});
}]);

insightApp.controller('TabController', ['$scope', '$location', function ($scope, $location) {
	$scope.$watch(function () {return $location.path(); }, function () {
		$scope.tabUrl = $location.path();
		angular.element('.modal-backdrop').remove(); // Bootstrap modal creates elements at the document root
	});
}]);

insightApp.directive('tip', function () {
	return function (scope, element, attrs) {
		$(element).tooltip();
	};
});

insightApp.run(['$http', '$rootScope', function ($http, $rootScope) {
	$rootScope.features = {};
	$http.get('../rest/features').success(function (data) {
		angular.forEach(data, function (value, key) {
			$rootScope.features[value] = true;
		});
	}).error(function () {
		if (console) {
			console.log('Failed to load features, some features may not be available');
		}
	});
}]);

insightApp.filter('escape', function () {
	return function (input) {
		if (!input) {
			return input;
		}

		if (input.indexOf('<html>') >= 0) {
			return input;
		} else {
			return input.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(
					/>/g, '&gt;').replace(/\n/g, '<br/>');
		}
	};
});

insightApp.factory('global', function ($rootScope) {
    return {};
});
