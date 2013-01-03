var insightApp = angular.module('insightApp', [], function(){
});

insightApp.factory('global', function($rootScope) {
    return {};
});

insightApp.getQueryString = function(key) {
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
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