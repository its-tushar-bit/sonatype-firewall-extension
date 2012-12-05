var insightApp = angular.module('insightApp', [], function($locationProvider){
	$locationProvider.html5Mode(true);
});

insightApp.factory('global', function($rootScope) {
    var state = {
    	policyTabCls: 'active',
    	labelTabCls: '',
    	licenseGroupTabCls: ''
    };
        
    return state;
});