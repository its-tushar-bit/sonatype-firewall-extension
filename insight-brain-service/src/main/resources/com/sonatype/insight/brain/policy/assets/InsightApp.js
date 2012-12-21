var insightApp = angular.module('insightApp', [], function($locationProvider){
	$locationProvider.html5Mode(true);
});

insightApp.factory('global', function($rootScope) {
    return {};
});

insightApp.getBaseUrl = function(){
	var idx = location.href.indexOf('/policy-assets/');
	
	if (idx > -1) {
		return location.href.substring(0,idx);
	}
	
	return '';
}

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
	return insightApp.getBaseUrl() + '/rest/policy/' + insightApp.appId;
}

insightApp.directive('slickgrid', SlickGridComponent);