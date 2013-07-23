/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
/*jslint plusplus: true */
(function () {
	'use strict';

	var policyModule = angular.module('Policy', ['Hudson', 'PolicyEditor', 'CLMAppLocation', 'AngularCommon', 'CommonServices']);

	policyModule.controller('PolicyController', ['$scope', '$location', '$http', 'hudson', '$timeout', '$rootScope', '$q', 'PolicyStore', 'ActionStore', 'CLMAppLocations', 'CLMLocations', 'Messages', 'policyEvaluator', function ($scope, $location, $http, hudson, $timeout, $rootScope, $q, policyStore, actionStore, clmAppLocations, clmLocations, messages, policyEvaluator) {

		function viewConfirmation(header, body, declineText, acceptText, acceptFn, declineFn) {
			$scope.state.confirm = {
				header : header,
				body : body,
				declineText : declineText,
				acceptText : acceptText,
				acceptFn : function () {
					delete $scope.state.confirm;
					$('#confirmationModal').modal('hide');
					acceptFn();
				},
				declineFn : function () {
					delete $scope.state.confirm;
					$('#confirmationModal').modal('hide');
					declineFn();
				}
			};
			$('#confirmationModal').modal('show');
		}

		$scope.alerts = [];
		$scope.location = $location;

		$scope.viewRemovePolicy = function (policy) {
			viewConfirmation("Delete Policy?",
				"Are you sure you want to delete the Policy named '" + policy.name + "'?  This action is not reversible.",
				'Cancel',
				'Delete',
				function () {
					policy.$delete().then(angular.noop, function (error) {
						$scope.$broadcast('showServerError', arguments);
					});
				}, angular.noop);
		};

		$scope.reEvaluatePolicy = function(application, policyEvaluation) {
			if (!$scope.reEvaluatingPolicy) {
				$scope.reEvaluatingPolicy = true;
				policyEvaluator.evaluate(application, policyEvaluation).then(function(data) {
					$scope.reEvaluatingPolicy = false;
				}, function(error) {
					$scope.reEvaluatingPolicy = false;
                    $scope.alerts.push({
                        type : 'error',
                        msg : 'An error occurred attempting to re-evaluate the policy. (' + messages.getHttpErrorMessage(error) + ')'
                    });
				});
			}
		};

		$scope.doLoad = function () {
			$scope.error = null;
			var promises = [policyStore.get().get(), actionStore.get(), $http.get(clmAppLocations.getApplicablePolicies(), {
                params: { timestamp: new Date().getTime() }
            })];
			if (clmAppLocations.isApplication()) {
				promises.push($http.get(clmAppLocations.getEntityUrl(), {
					params: { timestamp: new Date().getTime() }
				}));
			}

			$q.all(promises).then(function (results) {
				$scope.state = {
					actionStageList : results[1][1]
				};
				$scope.applicablePolicies = results[2].data.policiesByOwner;
				angular.forEach($scope.applicablePolicies, function (applicablePolicy, index) {
					applicablePolicy.editable = index === 0;
					if (index === 0) {
						applicablePolicy.policies = results[0];
					}
				});
				if (results.length === 4) {
					$scope.application = results[3].data;
					$scope.application.stageCount = 0;
				    angular.forEach($scope.application.policyEvaluations,function(policyEvaluation,stage){
                        policyEvaluation.reportUrl = clmLocations.getReportUrl($scope.application.publicId, policyEvaluation.scanId);
                        $scope.application.stageCount++;
                    });
				}
			}, function (errors) {
				$scope.error = angular.isArray(errors) ? errors[0] : errors;
			});
		};

		$scope.createPolicy = function () {
			return policyStore.get().create();
		};
		
		$scope.toggleAll = function (applicablePolicy) {
            var action = $scope.allExpanded[applicablePolicy.ownerId] ? 'hide' : 'show';
            $('#' + applicablePolicy.ownerId).find('.accordion-body').collapse(action);
            $scope.allExpanded[applicablePolicy.ownerId] = !($scope.allExpanded[applicablePolicy.ownerId] || false);
        };

    $scope.isExpanded = function(applicablePolicy) {
      return $scope.allExpanded[applicablePolicy.ownerId] || false;
    };

		$scope.doLoad();

		$scope.encodeURIComponent = window.encodeURIComponent;

    $scope.allExpanded = {};
	}]);

	policyModule.directive('policyItems', ['ActionStore', function (actionStore) {
		function capitalize(text) {
			if (text && text.length > 1) {
				return text.substring(0, 1).toUpperCase() + text.substring(1);
			}
			return text;
		}
		var actionStageList = null;
		actionStore.get().then(function (data) {
			actionStageList = data[1];
		});
		return {
			restrict : 'A',
			templateUrl : '../policy-assets/components/policy/policy-items.html',
			scope : {
				policies : '=policyItems',
				editable : '=editable',
				remove : '='
			},
			priority: 99,
			link: function(scope, elem, attr, ctrl) {
			    scope.policyEditMap = {};
				scope.getActionCount = function (policy) {
					var actionCount = 0;
					angular.forEach(policy.actions, function (value, key) {
						if (value.length > 0) {
							actionCount++;
						}
					});
					return actionCount;
				};
				scope.getActions = function (policy) {
					var actions = '';
					angular.forEach(policy.actions, function (value, key) {
						var j, currentStageText = '', formattedName;
						if (value.length > 0) {
							if (actions.length > 0) {
								actions += ', ';
							}

							for (j = 0; j < actionStageList.length; j++) {
								if (actionStageList[j].id == key) {
									currentStageText += actionStageList[j].name + ': ';
									break;
								}
							}

							for (j = 0; j < value.length; j++) {
								formattedName = capitalize(value[j].actionTypeId);
								if (currentStageText.indexOf(formattedName) < 0) {
									if (j > 0) {
										currentStageText += '/';
									}
									currentStageText += formattedName;
								}
							}

							if (currentStageText) {
								actions += currentStageText;
							}
						}
					});
					return actions;
				};
				scope.getActionStages = function() {
				    return actionStageList;
				};
				scope.getStageIconPath = function(stage, policy) {
				    if (policy.actions[stage.id]) {
				        for ( var i = 0 ; i < policy.actions[stage.id].length ; i++ ) {
			                if (policy.actions[stage.id][i].actionTypeId == 'warn') {
			                    return "../assets/img/policyalert.png";
	                        } else if (policy.actions[stage.id][i].actionTypeId == 'fail') {
	                            return "../assets/img/policyerror.png";
	                        }
				        }
				    }
				};
				
				scope.edit = function(policy) {
				    scope.policyEditMap[policy.id] = true;
				    $('#collapse' + policy.id).collapse('show');
				};
			}
		};
	}]);

	policyModule.service('policyEvaluator', function ($q, hudson, CLMLocations) {
		return {
			evaluate: function(application, policyEvaluation) {
				var deferred = $q.defer();
				var stage = policyEvaluation.stage;
				hudson.post(CLMLocations.evaluatePolicyUrl(application.publicId, policyEvaluation.scanId), stage).success(function (data) {
					policyEvaluation.time = new Date();
					for (var stageTypeId in application.policyEvaluationsResults) {
						if (stageTypeId === stage.stageTypeId) {
							application.policyEvaluationsResults[stageTypeId] = data;
							break;
						}
					}
					deferred.resolve(data);
				}).error(function (data, status, headers, config) {
					deferred.reject({ data: data, status : status, headers : headers, config : config });
				});
				return deferred.promise;
			}
		};
	});
}());
