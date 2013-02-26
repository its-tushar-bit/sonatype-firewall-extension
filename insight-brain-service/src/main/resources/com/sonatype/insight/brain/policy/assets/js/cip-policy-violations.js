/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, CLM */
(function () {
    'use strict';

	$.extend(true, window, {
	    'Insight' : {
			'PolicyViolations' : function (node, applicationId, hash) {
				var timestamp = (new Date()).getTime(),
					container = $('<div ng-include src="\'' + CLM.path + 'policy-assets/components/cip-policy-violations.html\'"></div>');
				node.empty();
				container.appendTo(node);

				angular.module('policyViolations' + timestamp, []);
				angular.bootstrap(container[0], ['PolicyViolations', 'policyViolations' + timestamp]);
	        }
	    }
	});

	var policyViolationApp = angular.module('PolicyViolations', []);

	policyViolationApp.controller('PolicyViolationsController', ['$http', '$scope', function ($http, $scope) {
		function errorFn(data, status, headersFn, config) {
		    var header = headersFn();
            if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
                $scope.errorResponse = 'Server Error';
            } else {
                $scope.errorResponse = data;
            }
		}
		
		function startIfReady() {
		    if ($scope.policyAlerts !== undefined
		        && $scope.actionTypes !== undefined) {
	            angular.forEach($scope.policyAlerts, function(policyAlert,policyAlertIndex){
	                angular.forEach(policyAlert.trigger.componentFacts, function(componentFact, componentFactIndex){
	                    policyAlert.componentFact = componentFact;
	                    if (policyAlert.trigger.threatLevel > 7) {
	                        policyAlert.color = 'red';
	                    } else if (policyAlert.trigger.threatLevel > 3) {
	                        policyAlert.color = 'orange';
	                    } else if (policyAlert.trigger.threatLevel > 0) {
	                        policyAlert.color = 'yellow';
	                    } else {
	                        policyAlert.color = 'blue';
	                    }
	                });
	                angular.forEach(policyAlert.actions, function(action, actionIndex){
	                    angular.forEach($scope.actionTypes, function(actionType, actionTypeIndex){
	                        if (actionType.id === action.actionTypeId) {
	                            action.summary = actionType.summary;
	                        }
	                    });
	                });
	            });    
		    }
		}
		
	    $http.get('policyalerts.json', { params : { timestamp : new Date().getTime() } }).success(function (data) {
			$scope.policyAlerts = data.aaData;
			startIfReady();
		}).error(errorFn);
		
		$http.get(CLM.path + 'rest/policy/actionType').success(function (data) {
		    $scope.actionTypes = data;
		    startIfReady();
		}).error(errorFn);
	}]);
}());