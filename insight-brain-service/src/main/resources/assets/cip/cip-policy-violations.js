/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 * third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
/*global angular, $, window, CLM */
(function() {
	'use strict';

	$.extend(true, window, {
		'Insight' : {
			'PolicyViolations' : function(node, appId, hash) {
				var timestamp = (new Date()).getTime(), container = $('<div ng-include src="\'' + CLM.path + 'cip/cip-policy-violations.html\'"></div>');
				node.empty();
				container.appendTo(node);

				angular.module('policyViolations' + timestamp, []).service('PolicyViolationData', function() {
					return {
						hash : hash,
						appId : appId
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
				
				//move the modal out into the body, so it appears properly ABOVE the backdrop
				$("#componentWaiverModal").appendTo("body");
			}
		}
		
		//Waive component policy trigger, so that it will no longer be triggered in future
		$scope.waiveComponent = function(policyAlert) {
		    //get the tree of contexts, and flatten down into a list we can display properly
		    $http.get(CLM.path + 'rest/waiver/application/' + policyViolationData.appId + '/applicable/context/' + policyAlert.id).success(function(data){
		        function processContext(context) {
		            if (context) {
		                //only bother checking children if an org, apps dont have children
    		            if (context.type === 'organization') {
                            $scope.waiverTargets.push({id:context.id,name:context.name,type:context.type});
                            angular.forEach(context.children, function(childContext, childContextIndex){
                                processContext(childContext); 
                            });
                        } else {
                            //insert the app in position 1, app should always be shown first, and will be defaulted
                            $scope.waiverTargets.splice(0, 0, {id:context.id,name:context.name,type:context.type});
                            //set the app as the default selected value
                            $scope.waiverSelectedOwner = context.id + '$$' + context.type;
                        }
		            }
		        }
		        
		        //if only application present, no need to show the app/org radio buttons
		        $scope.waiverSelectOwner = (data.children && data.children.length);
		        $scope.waiverTargets = [];
	            processContext(data);
	            $scope.waiverComment = undefined;
                
		        $('#componentWaiverModal').modal('show');
		    }).error(errorFn);
		}
		
		//pretty simple, they decline just dump the modal
		$scope.declineWaiveComponent = function() {
		    $('#componentWaiverModal').modal('hide');
		}
		
		//user really wants to waive the component, so send the request on down
		$scope.acceptWaiveComponent = function() {
		    //TODO: send request to server
		    $('#componentWaiverModal').modal('hide');
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
}());