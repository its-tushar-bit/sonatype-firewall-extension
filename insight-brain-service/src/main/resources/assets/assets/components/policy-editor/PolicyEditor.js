/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, Option */
(function () {
	'use strict';
	var module = angular.module('PolicyEditor', ['CLMLocation', 'Hudson', 'NotificationManagement', 'ResourceModule', 'ui.compat', 'AngularCommon']);

	module.service('PolicyStore', ['ApplicationId', 'CLMLocations', 'CLMAppLocations', 'CLMResource', function (appId, clmLocations, clmAppLocations, clmResource) {
		var policyStoreTemplate = {
				id : 'id',
				template : {
					threatLevel : 5,
					constraints : []
				},
				params : {
					timestamp : new Date().getTime()
				}
			},
			policyStores = {};

		return {
			get : function () {
				var store = policyStores[appId.encoded()];
				if (!store) {
					// Expire existing stores, prevents user from encountering stale data
					angular.forEach(policyStores, function (value, key) {
						policyStores[key] = null;
					});
					store = clmResource.getStore(angular.extend({ url : clmAppLocations.getPolicyUrl() }, policyStoreTemplate));
					policyStores[appId.encoded()] = store;
				}
				return store;
			},
			serializeActions : function (uiActions) {
				var policyActions = {};
				angular.forEach(uiActions, function (stage, stageName) {
					var serializedActions = [];
					if (stage.action !== null && stage.action !== 'none') {
						serializedActions.push({ actionTypeId : stage.action });
					}
					angular.forEach(stage.notify, function (email) {
						serializedActions.push({ actionTypeId : 'notify', target : email });
					});
					policyActions[stageName] = serializedActions;
				});
				return policyActions;
			},
			deserializeActions :  function (policyActions) {
				//Re-arrange action data for UI
				var uiActions = {};
				angular.forEach(policyActions, function (actions, stageName) {
					uiActions[stageName] = {
						action : null,
						notify : []
					};
					angular.forEach(actions, function (action, index) {
						if (action.actionTypeId === 'notify') {
							uiActions[stageName].notify.push(action.target);
						} else {
							uiActions[stageName].action = action.actionTypeId;
						}
					});
				});
				return uiActions;
			},
			actionEquals : function (policy, actions) {
				var changed = false;
				angular.forEach(policy.actions, function (action, stage) {
					angular.forEach(action, function (a) {
						if (a.actionTypeId === 'notify') {
							changed = changed || (actions[stage].notify.indexOf(a.target));
						} else {
							changed = changed || (actions[stage].action !== a.actionTypeId);
						}
					});
				});
				return changed;
			}
		};
	}]);

	module.service('ActionStore', ['CLMLocations', 'CLMResource', '$q', function (clmLocations, clmResource, $q) {
		var actionTypeStore = clmResource.getStore({
				id : 'id',
				url : clmLocations.getActionTypeUrl()
			}),
			actionStageStore = clmResource.getStore({
				id : 'id',
				url : clmLocations.getActionStageUrl()
			}),
			actionPromise = $q.all([actionTypeStore.get(), actionStageStore.get()]);
		return {
			'get' : function () {
				return actionPromise;
			}
		};
	}]);

	module.service('ConstraintStore', ['CLMLocations', 'CLMAppLocations', 'CLMResource', '$q', function (clmLocations, clmAppLocations, clmResource, $q) {
		var conditionTypeStore = clmResource.getStore({
				id : 'id',
				url : clmLocations.getConditionTypeUrl()
			});

		return {
			'get' : function () {
				var conditionValueTypeStore = clmResource.getStore({
						id : 'id',
						url : clmAppLocations.getConditionValueTypeUrl()
					}),
					conditionDeferred = $q.all([conditionTypeStore.get(), conditionValueTypeStore.get()]);
				return conditionDeferred;
			}
		};
	}]);

	module.controller('PolicyEditorController', ['$scope', '$state', '$q', '$location', 'Messages', 'PolicyStore', 'ActionStore', function ($scope, $state, $q, $location, messages, policyStore, actionStore) {

		function viewConfirmation(header, body, declineText, acceptText, acceptFn, declineFn) {
			$scope.state.confirmationHeader = header;
			$scope.state.confirmationBody = body;
			$scope.state.confirmationDeclineText = declineText;
			$scope.state.confirmationAcceptText = acceptText;
			$scope.state.confirmationAcceptFn = acceptFn;
			$scope.state.confirmationDeclineFn = declineFn;
			$('#confirmationModal').modal('show');
		}

		function returnFn() {
			var path = $location.path();
			$location.path(path.substring(0, path.lastIndexOf('/')));
		}

		function getActions(actionStages) {
			var actions = {};
			angular.forEach(actionStages, function (stage) {
				actions[stage.id] = [];
			});
			return policyStore.deserializeActions(actions);
		}

		function isDirty() {
			var changed = false;
			if (angular.isUndefined($scope.state.currentPolicy.id)) {
				changed = $scope.state.currentPolicy.constraints.length > 0 || $scope.state.currentPolicy.name;
				if (!changed) {
					angular.forEach(policyStore.serializeActions($scope.state.actions), function (actions, stage) {
						changed = changed || actions.length > 0;
					});
				}
			} else {
				angular.forEach($scope.policies, function (policy, index) {
					if (policy.id === $scope.state.currentPolicy.id) {
						changed = !angular.equals(policy, $scope.state.currentPolicy) || policyStore.actionEquals($scope.state.currentPolicy, $scope.state.actions);
					}
				});
			}
			return changed;
		}
		$scope.alerts = [];

		$scope.doLoad = function () {
			var currentPolicyStore = policyStore.get();
			$scope.error = null;
			$q.all([currentPolicyStore.get(), actionStore.get()]).then(function (results) {
				var policies = results[0],
					actionStages = results[1][1],
					state = {
						currentPolicy : null,
						actions : {}
					};

				$scope.state = state;
				$scope.policies = policies;
				$scope.actionStages = actionStages;

				if ($state.params.policyId === 'new' || angular.isUndefined($state.params.policyId)) {
					state.currentPolicy = currentPolicyStore.create();
				} else {
					angular.forEach(policies, function (policy, index) {
						if (policy.id === $state.params.policyId) {
							state.currentPolicy = policy.$clone();
							return false;
						}
					});
					// TODO If currentPolicy === null, show error
				}
				$scope.state.actions = angular.extend(getActions(actionStages), policyStore.deserializeActions($scope.state.currentPolicy.actions));
			}, function (error) {
				$scope.error = error;
			});
		};

		$scope.savePolicy = function () {
			$scope.state.currentPolicy.actions = policyStore.serializeActions($scope.state.actions);
			$scope.state.currentPolicy.$save().then(function (policy) {
                $scope.state.currentPolicy = policy.$clone();
                returnFn();
            }, function (error) {
				$scope.alerts.push({
					type : 'error',
					msg : 'An error occurred while saving the policy. (' + messages.getHttpErrorMessage(error) + ')'
				});
			});
		};

		$scope.confirmationAccept = function () {
			$('#confirmationModal').modal('hide');
			if ($scope.state.confirmationAcceptFn) {
				$scope.state.confirmationAcceptFn();
			}
		};

		$scope.confirmationDecline = function () {
			$('#confirmationModal').modal('hide');
			if ($scope.state.confirmationDeclineFn) {
				$scope.state.confirmationDeclineFn();
			}
		};

		$scope.viewRemoveConstraint = function (constraintIndex) {
			viewConfirmation("Delete Constraint?",
				"Are you sure you want to delete the Constraint named '" + $scope.state.currentPolicy.constraints[constraintIndex].name + "'?",
				'Cancel',
				'Delete',
				function () {
					$scope.state.currentPolicy.constraints.splice(constraintIndex, 1);
				});
		};

		$scope.editConstraint = function (constraint) {
			$scope.$broadcast('policy.editConstraint', constraint);
		};

		$scope.editNotification = function (addresses) {
			$scope.$broadcast('editNotification', addresses);
		};

		// Respond to constraint change
		$scope.$on('policy.constraintSaved', function (event, constraint) {
			event.stopPropagation();
			var present = false;
			angular.forEach($scope.state.currentPolicy.constraints, function (candidate) {
				present = present || candidate === constraint;
			});
			if (!present) {
				// New constraint
				$scope.state.currentPolicy.constraints.push(constraint);
			}
		});

		$scope.$on('pageChangeStarted', function (event) {
			if (isDirty()) {
				event.preventDefault();
			}
		});

		$scope.doLoad();
	}]);

	module.controller('ConstraintEditorController', ['$scope', '$timeout',  'ConstraintStore', function ($scope, $timeout, constraints) {
		$scope.cancelConstraint = function () {
			$('#editConstraintModal').modal('hide');
			$scope.originalConstraint = $scope.currentConstraint = null;
		};

		$scope.saveConstraint = function () {
			angular.forEach($scope.currentConstraint.conditions, function (condition) {
				// Remove temporary values used by the UI
				delete condition.v;
				delete condition.valueModifier;
			});
			if ($scope.originalConstraint) {
				angular.forEach($scope.currentConstraint, function (value, key) {
					$scope.originalConstraint[key] = value;
				});
			}
			$scope.$emit('policy.constraintSaved', $scope.originalConstraint || $scope.currentConstraint);
			$('#editConstraintModal').modal('hide');
			$scope.originalConstraint = $scope.currentConstraint = null;
		};

		$scope.updateAge = function (condition) {
			// Kludge to allow the UI to show two fields but combine them behind the scenes.  The value should only be set when both fields are valid
			condition.value = (condition.v !== '' && condition.v != null && condition.valueModifier) ? condition.v * condition.valueModifier : null;
		};

		$scope.validateConstraint = function () {
			var i,
				conditions = $scope.currentConstraint.conditions,
				conditionType;

			delete $scope.constraintValidationMsg;

			if (!$scope.currentConstraint.name) {
				$scope.constraintValidationMsg = 'Please enter a name for this constraint';
				return;
			}

			for (i = 0; i < conditions.length; i++) {
				conditionType = $scope.conditionTypes[conditions[i].conditionTypeId];
				if (conditionType === null || angular.isUndefined(conditionType)) {
					$scope.constraintValidationMsg = 'Please select a valid condition type for condition #' + (i + 1);
					return;
				} else if (conditionType.valueTypeId && (conditions[i].value === null || angular.isUndefined(conditions[i].value))) {
					$scope.constraintValidationMsg = 'Please enter a value for condition #' + (i + 1);
					return;
				}
			}
		};

		$scope.conditionTypeChanged = function (condition) {		
			// Remove values that were entered with the previous condition type
			delete condition.value;
			delete condition.v;
			delete condition.valueModifier;

			// This could be replaced with ng-init but the html is fairly verbose as it is
			condition.operator = $scope.conditionTypes[condition.conditionTypeId].supportedOperators[0];
			switch ($scope.conditionTypes[condition.conditionTypeId].valueTypeId) {
				case 'LicenseCategoryValueType':
				case 'LicenseValueType':
				case 'LicenseThreatGroupValueType':
				case 'LicenseStatusValueType':
				case 'MatchStateValueType':
				case 'SecurityVulnerabilityStatusValueType':
				case 'LabelValueType':
					if ($scope.conditionTypes[condition.conditionTypeId].valueType.availableValues && $scope.conditionTypes[condition.conditionTypeId].valueType.availableValues.length > 0) {
						condition.value = $scope.conditionTypes[condition.conditionTypeId].valueType.availableValues[0].id;
					}
					break;
				case 'AgeInDaysValueType':
					condition.valueModifier = 1;
					break;
			}

			$scope.validateConstraint();
		};

		$scope.addCondition = function () {
			var conditionType = $scope.conditionTypes['AgeInDays'];

			$scope.currentConstraint.conditions.push({
				conditionTypeId: conditionType.id,
				operator: conditionType.supportedOperators[0],
				valueModifier: 365
			});

			$scope.validateConstraint();
		};

		$scope.removeCondition = function (conditionIndex) {
			$scope.currentConstraint.conditions.splice(conditionIndex, 1);
			$scope.validateConstraint();
		};

		$scope.$on('policy.editConstraint', function (event, constraint) {
			event.preventDefault();
			// Possibility that the conditions bits haven't been loaded yet.
			var fn = function () {
				if ($scope.conditionTypes) {
					$scope.originalConstraint = constraint || null;
					$scope.currentConstraint = constraint ? angular.copy(constraint) : { conditions: [], operator: 'OR' };

					if ($scope.currentConstraint.conditions.length === 0) {
						$scope.addCondition();
					} else {
						angular.forEach($scope.currentConstraint.conditions, function (condition) {
							if ($scope.conditionTypes[condition.conditionTypeId]) {
								switch ($scope.conditionTypes[condition.conditionTypeId].valueTypeId) {
									case "PercentageValueType":
									case "IntegerValueType":
										var value = parseInt(condition.value, 10);
										if (!isNaN(value)) {
											condition.value = value;
										}
										break;
								}
							}

							if (condition.conditionTypeId === "AgeInDays") {
								if (condition.value >= 365 && condition.value % 365 === 0) {
									condition.valueModifier = 365;
								} else if (condition.value >= 30 && condition.value % 30 === 0) {
									condition.valueModifier = 30;
								} else {
									condition.valueModifier = 1;
								}
								condition.v = condition.value / condition.valueModifier;
							}
						});
					}
					$scope.validateConstraint();
					$('#constraintName').focus();
				} else {
					$timeout(fn, 100);
				}
			};

			fn();
			$('#editConstraintModal').modal('show');
		});

		$scope.$on('pageChangeStarted', function (event) {
			if ($scope.originalConstraint != null || $scope.currentConstraint != null) {
				event.preventDefault();
			}
		});

		constraints.get().then(function (results) {
			var typeValues = {};
			$scope.conditionTypes = {};
			angular.forEach(results[1], function (typeValue) {
				typeValues[typeValue.id] = typeValue;
			});
			angular.forEach(results[0], function (type) {
				var typeValue = type.valueTypeId == null ? null : typeValues[type.valueTypeId];
				type.valueType = typeValue;
				$scope.conditionTypes[type.id] = type;
			});
		}, function (error) {

//			handleHttpError('Policy Initialization Error', error.data, error.status);
		});
	}]);

	module.directive('ieOptions', ['$parse', function($parse) {
		return {
			restrict: 'A',
			require: 'ngModel',
			link: function(scope, elem, attr, ctrl) {
				var options = attr.ieOptions;
				scope.$watch(options, function() {
					var collection = $parse(options)(scope);
					elem.find('option').remove();
					$.each(collection, function(index) {
						var option = new Option(collection[index], collection[index]);
						elem[0].options[elem[0].options.length] = option;
					});
//					ctrl.$setViewValue(elem.val());
				});
			}
		};
	}]);
}());