/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/*global angular, $, window, CLM */
(function() {
    'use strict';

    function doLoad() {
	    $.extend(true, window, {
	        'Insight' : {
	            'PolicyViolations' : function(node, appId, hash) {
	                var timestamp = (new Date()).getTime(), container = $('<div ng-include src="\'' + CLM.path + 'policy-assets/components/cip/cip-policy-violations.html\'"></div>');
	                node.empty();
	                container.appendTo(node);

	                angular.module('policyViolations' + timestamp, []).service('PolicyViolationData', function() {
	                    return {
	                        hash : hash
	                    };
	                });

	                angular.bootstrap(container[0], [ 'PolicyViolations', 'policyViolations' + timestamp ]);
	            }
	        }
	    });

	    var policyViolationApp = angular.module('PolicyViolations', []);

	    policyViolationApp.controller('PolicyViolationsController', [ '$http', '$scope', '$timeout', 'PolicyViolationData', function($http, $scope, $timeout, policyViolationData) {
	        function errorFn(data, status, headersFn, config) {
	            var header = headersFn();
	            if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
	                $scope.errorResponse = 'Server Error';
	            } else {
	                $scope.errorResponse = data;
	            }
	        }

	        function startIfReady() {
	            if ($scope.policyAlerts !== undefined && $scope.actionTypes !== undefined) {
	                $scope.processedPolicyAlerts = [];
	                angular.forEach($scope.policyAlerts, function(policyAlert, policyAlertIndex) {
	                    var actions = [];
	                    angular.forEach(policyAlert.actions, function(action, actionIndex) {
	                        angular.forEach($scope.actionTypes, function(actionType, actionTypeIndex) {
	                            if (actionType.id === action.actionTypeId && jQuery.inArray(actionType.summary, actions) === -1) {
	                                actions.push(actionType.summary);
	                                return false;
	                            }
	                        });
	                    });
	                    angular.forEach(policyAlert.trigger.componentFacts, function(componentFact, componentFactIndex) {
	                        if (componentFact.hash === policyViolationData.hash) {
	                            var tLvl = policyAlert.trigger.threatLevel;
	                            $scope.processedPolicyAlerts.push({
	                                id : policyAlert.trigger.policyId,
	                                name : policyAlert.trigger.policyName,
	                                threatLevel : tLvl,
	                                groupId : componentFact.groupId,
	                                artifactId : componentFact.artifactId,
	                                version : componentFact.version,
	                                hash : componentFact.hash,
	                                color : tLvl > 7 ? 'red' : tLvl > 3 ? 'orange' : tLvl > 1 ? 'yellow' : tLvl > 0 ? 'darkblue' : 'blue',
	                                constraints : componentFact.constraintFacts,
	                                actions : actions
	                            });
	                        }
	                    });
	                });

	                $scope.processedPolicyAlerts.sort(function(policyA, policyB) {
	                    return policyA.threatLevel > policyB.threatLevel ? -11 : policyA.threatLevel < policyB.threatLevel ? 1 : 0;
	                });
	            }
	        }

	        $http.get('policyalerts.json', {
	            params : {
	                timestamp : new Date().getTime()
	            }
	        }).success(function(data) {
	            $scope.policyAlerts = data.aaData || [];
	            startIfReady();
	        }).error(errorFn);

	        $http.get(CLM.path + 'rest/policy/actionType').success(function(data) {
	            $scope.actionTypes = data;
	            startIfReady();
	        }).error(errorFn);
	    } ]);
    }

	function check() {
		if (window.angular) {
			doLoad();
		} else {
			setTimeout(check, 100);
		}
	}

	setTimeout(check, 0);
}());