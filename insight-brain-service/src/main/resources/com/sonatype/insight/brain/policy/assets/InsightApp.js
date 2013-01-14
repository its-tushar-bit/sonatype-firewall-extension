var insightApp = angular.module('insightApp', ['ngSanitize'], function(){
});

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
    var vars = [],
        hashes = window.location.search.slice(1).split('&'),
        hash;
    for(var i = 0; i < hashes.length; i++){
        hash = hashes[i].split('=');
        vars.push(hash[0]);
        vars[hash[0]] = hash[1];
    }
    return vars[key];
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
	return insightApp.getBaseUrl() + '/rest/policy/conditionValueType';
}

insightApp.getPolicyUrl = function(){
	return insightApp.getBaseUrl() + '/rest/policy/' + insightApp.getAppId();
}

insightApp.directive('slickgrid', SlickGridComponent);
