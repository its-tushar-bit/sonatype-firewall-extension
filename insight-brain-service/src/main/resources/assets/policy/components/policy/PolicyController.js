/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
/*jslint plusplus: true */
(function () {
	'use strict';

	var policyModule = angular.module('Policy', ['Hudson', 'PolicyEditor', 'CLMAppLocation', 'AngularCommon']);

	policyModule.controller('PolicyController', ['$scope', '$location', '$http', 'hudson', '$timeout', '$rootScope', '$q', 'PolicyStore', 'ActionStore', 'applicationStore', 'CLMAppLocations', 'policyEvaluator', function ($scope, $location, $http, hudson, $timeout, $rootScope, $q, policyStore, actionStore, applicationStore, clmAppLocations, policyEvaluator) {

		function handleHttpError(headerText, bodyText, status) {
			$scope.httpError = {
				body : status === 0 ? 'Unable to connect to server.' : bodyText,
				header : headerText
			};
			$('#httpErrorModal').modal('show');
		}

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
						$scope.alerts.push({
							type : 'error',
							msg : 'An error occurred while deleting policy. (' + messages.getHttpErrorMessage(error) + ')'
						});
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
					handleHttpError('Policy Initialization Error', error.data, error.status);
				});
			}
		};

		$scope.doLoad = function () {
			$scope.error = null;
			var promises = [policyStore.get().get(), actionStore.get(), $http.get(clmAppLocations.getApplicablePolicies())];
			if (clmAppLocations.isApplication()) {
				promises.push($http.get(clmAppLocations.getEntityUrl(), {
					params: { timestamp: new Date().getTime() }
				}));
			}

			$q.all(promises).then(function (results) {
				$scope.state = {
					policyList : results[0],
					actionStageList : results[1][1]
				};
				$scope.applicablePolicies = results[2].data.policiesByOwner;
				angular.forEach($scope.applicablePolicies, function (applicablePolicy, index) {
					applicablePolicy.editable = index === 0;
				});
				if (results.length === 4) {
					$scope.application = results[3].data;
				}
			}, function (errors) {
				$scope.error = angular.isArray(errors) ? errors[0] : errors;
			});
		};

		$scope.doLoad();

		$scope.encodeURIComponent = window.encodeURIComponent;
	}]);

	policyModule.directive('policyCards', ['ActionStore', function (actionStore) {
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
			templateUrl : '../policy-assets/components/policy/policy-cards.html',
			scope : {
				policies : '=policyCards',
				editable : '=editable'
			},
			transclude : true,
			priority: 99,
			link: function(scope, elem, attr, ctrl) {
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
			}
		};
	}]);
}());
