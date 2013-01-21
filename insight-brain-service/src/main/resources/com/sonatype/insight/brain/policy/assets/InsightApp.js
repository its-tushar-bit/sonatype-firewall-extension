$(document).ready(function(){
	$('[data-title]').tooltip();
});
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
	$scope.$watch(function(){return $location.path();}, function(){
		$scope.tabUrl = $location.path();
		angular.element('.modal-backdrop').remove(); // Bootstrap modal creates elements at the document root
	});
}]);

insightApp.run(['$http', '$rootScope', function($http, $rootScope) {
	$rootScope.features = {};
	$http.get('../rest/features').success(function(data) {
		angular.forEach(data, function (value, key) {
			$rootScope.features[value] = true;
		});
	}).error(function () {
		console.log('Failed to load features, some features may not be available');
	});
}]);

insightApp.filter('escape', function() {
	return function(input) {
		if (!input) {
			return input;
		}

		if (input.indexOf('<html>') >= 0) {
			return input;
		} else {
			return input.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(
					/>/g, '&gt;').replace(/\n/g, '<br/>');
		}
	}
});

insightApp.factory('global', function($rootScope) {
    return {};
});

insightApp.getQueryString = function(key) {
	var results = new RegExp('[\\?&]' + key + '=([^&#]*)').exec(window.location.href);
    if (results)
    {
    	return results[1];
    }
}

insightApp.getAppId = function(){
	if (insightApp.appId){
		return insightApp.appId;
	}
	
	insightApp.appId = insightApp.getQueryString('appId');
	
	return insightApp.appId;
}

insightApp.getBaseUrl = function(){
	if (insightApp.baseUrl){
		return insightApp.baseUrl;
	}
	
	insightApp.baseUrl = '';
	
	var idx = location.href.indexOf('/policy-assets/');
	
	if (idx > -1) {
		insightApp.baseUrl = location.href.substring(0,idx);
	}
	
	return insightApp.baseUrl;
}

insightApp.getLabelsUrl = function (){
	return insightApp.getBaseUrl() + '/rest/label/application/' + insightApp.getAppId();
};

insightApp.getDeleteLabelsUrl = function (label){
	return insightApp.getBaseUrl() + '/rest/label/application/' + insightApp.getAppId() + '/' + label.id;
};

insightApp.getConditionTypeUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/conditionType';
}

insightApp.getActionTypeUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/actionType';
}

insightApp.getActionStageUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/stageType';
}

insightApp.getConditionValueTypeUrl = function(){
	return insightApp.getBaseUrl() + '/rest/conditionValueType/' + insightApp.getAppId();
}

insightApp.getPolicyUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/' + insightApp.getAppId();
}