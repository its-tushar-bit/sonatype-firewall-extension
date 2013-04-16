/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window */
(function () {
	'use strict';
	var module = angular.module('PolicyEditor', ['CLMLocation', 'Hudson', 'NotificationManagement', 'ResourceModule']);

	module.service('PolicyStore', ['CLMLocations', 'CLMResource', function (clmLocations, clmResource) {
		var policyStore = clmResource.getStore({
			id : 'id',
			url : clmLocations.getPolicyUrl(),
			template : {
				threatLevel : 5,
				constraints : []
			},
			params : {
				timestamp : new Date().getTime()
			}
		});
		policyStore.serializeActions = function (uiActions) {
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
		};
		policyStore.deserializeActions =  function (policyActions) {
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
		};
		return policyStore;
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

	module.service('ConstraintStore', ['CLMLocations', 'CLMResource', '$q', function (clmLocations, clmResource, $q) {
		var conditionTypeStore = clmResource.getStore({
				id : 'id',
				url : clmLocations.getConditionTypeUrl()
			}),
			conditionValueTypeStore = clmResource.getStore({
				id : 'id',
				url : clmLocations.getConditionValueTypeUrl()
			}),
			conditionDeferred = $q.all([conditionTypeStore.get(), conditionValueTypeStore.get()]);

		return {
			'get' : function () {
				return conditionDeferred;
			}
		};
	}]);

	module.controller('PolicyEditorController', ['$scope', '$routeParams', '$q', 'PolicyStore', 'ActionStore', function ($scope, $routeParams, $q, policyStore, actionStore) {

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
			$scope.$emit('editPolicyComplete');
		}

		function handleHttpError(headerText, bodyText, status) {
			$scope.httpError = {
				body : status === 0 ? 'Unable to connect to server.' : bodyText,
				header : headerText
			};
			$('#httpErrorModal').modal('show');
		}

		function getActions(actionStages) {
			var actions = {};
			angular.forEach(actionStages, function (stage) {
				actions[stage.id] = [];
			});
			return policyStore.deserializeActions(actions);
		}

		$scope.savePolicy = function () {
			var errorFn = function (data, status, headers, config) {
					handleHttpError('Saving Policy', data, status);
				};

			$scope.state.currentPolicy.actions = policyStore.serializeActions($scope.state.actions);
			$scope.state.currentPolicy.$save().then(returnFn, errorFn);
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

		$scope.viewCancelPolicy = function () {
			var changed = false,
				executeFn = function () {
					if (changed) {
						viewConfirmation("Cancel Policy Changes?", "Are you sure you want to cancel?  Any changes made to the Policy will be lost.", 'No', 'Yes', returnFn);
					} else {
						returnFn();
					}
				};
			if (angular.isUndefined($scope.state.currentPolicy.id)) {
				changed = $scope.state.currentPolicy.constraints.length > 0 || $scope.state.currentPolicy.name;
				if (!changed) {
					angular.forEach(policyStore.serializeActions($scope.state.actions), function (actions, stage) {
						changed = changed || actions.length > 0;
					});
				}
				executeFn();
			} else {
				angular.forEach($scope.policies, function (policy, index) {
					if (policy.id === $scope.state.currentPolicy.id) {
						changed = !angular.equals(policy, $scope.state.currentPolicy);
						executeFn();
					}
				});
			}
		};

		$scope.isPolicyValid = function () {
			if (!$scope.state || !$scope.state.currentPolicy) {
				return false;
			}

			var uniqueName = true;
			angular.forEach($scope.policies, function (policy) {
				if (policy.id !== $scope.state.currentPolicy.id && policy.name === $scope.state.currentPolicy.name) {
					uniqueName = false;
				}
			});
			return (uniqueName && $scope.state.currentPolicy.name
					&& $scope.state.currentPolicy.threatLevel >= 0
					&& $scope.state.currentPolicy.constraints.length > 0) == true;
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
			if (angular.isUndefined(constraint.id)) {
				// New constraint
				$scope.state.currentPolicy.constraints.push(constraint);
			} else {
				// Update existing constraint
				angular.forEach($scope.state.currentPolicy.constraints, function (candidate) {
					if (candidate.id === constraint.id) {
						candidate.conditions = constraint.conditions;
						candidate.name = constraint.name;
					}
				});
			}
		});

		$q.all([policyStore.get(), actionStore.get()]).then(function (results) {
			var policies = results[0],
				actionStages = results[1][1],
				state = {
					currentPolicy : null,
					actions : {}
				};

			$scope.state = state;
			$scope.policies = policies;
			$scope.actionStages = actionStages;

			if ($routeParams.policyId === 'new' || angular.isUndefined($routeParams.policyId)) {
				state.currentPolicy = policyStore.create();
			} else {
				angular.forEach(policies, function (policy, index) {
					if (policy.id === $routeParams.policyId) {
						state.currentPolicy = angular.copy(policy);
						return false;
					}
				});
				// TODO If currentPolicy === null, show error
			}
			$scope.state.actions = angular.extend(getActions(actionStages), policyStore.deserializeActions($scope.state.currentPolicy.actions));
		}, function (error) {
			handleHttpError('Policy Initialization Error', error.data, error.status);
		});
	}]);

	module.controller('ConstraintEditorController', ['$scope', '$timeout',  'ConstraintStore', function ($scope, $timeout, constraints) {
		$scope.cancelConstraint = function () {
			$('#editConstraintModal').modal('hide');
		};

		$scope.saveConstraint = function () {
			angular.forEach($scope.currentConstraint.conditions, function (condition) {
				// Remove temporary values used by the UI
				delete condition.v;
				delete condition.valueModifier;
			});
			$scope.$emit('policy.constraintSaved', $scope.currentConstraint);
			$('#editConstraintModal').modal('hide');
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
			condition.operator = $scope.conditionTypes[condition.conditionTypeId].supportedOperators[0];

			// Remove values that were entered with the previous condition type
			delete condition.value;
			delete condition.v;
			delete condition.valueModifier;

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
					$scope.currentConstraint = constraint ? angular.copy(constraint) : { conditions: [], operator: 'OR' };

					if ($scope.currentConstraint.conditions.length === 0) {
						$scope.addCondition();
					} else {
						angular.forEach($scope.currentConstraint.conditions, function (condition) {
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
}());