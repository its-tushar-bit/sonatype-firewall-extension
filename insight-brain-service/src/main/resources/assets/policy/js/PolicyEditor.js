/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
	'use strict';
	var module = angular.module('PolicyEditor', ['CLMLocation', 'Hudson', 'NotificationManagement', 'ResourceModule']),
		port = window.location.origin.match(/:/g).length > 1 ? window.location.origin.substring(window.location.origin.lastIndexOf(':')) : null;

	function escapeUrl(url) {
		return port === null ? url : url.replace(port, '\\' + port)
	}

	module.service('PolicyStore', ['CLMLocations', 'CLMResource', function (clmLocations, clmResource) {
		var policyStore = clmResource.getStore({
			id : 'id',
			url : clmLocations.getPolicyUrl(),
			template : {
				threatLevel : 0,
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
/*
	module.service('PoliciesService', ['CLMLocations', '$resource', '$q', 'CLMResource', function (clmLocations, $resource, $q, clmResource) {
		var deferred = $q.defer(),
			actionTypeStore = clmResource.getStore({
				id : 'id',
				url : clmLocations.getActionTypeUrl()
			}),
			actionStageStore = clmResource.getStore({
				id : 'id',
				url : clmLocations.getActionStageUrl()
			}),
			actionPromise = $q.all([actionTypeStore.get(), actionStageStore.get()]),
			policyStore = clmResource.getStore({
				id : 'id',
				url : clmLocations.getPolicyUrl() + '?timestamp=' + new Date().getTime()
			});

		return {
			'getPolicyStore' : function () {
				return policyStore;
			},
			'getActions' : function () {
				return actionPromise;
			},
			'save' : function (policy) {

			},
			'serializeActions' : function (uiActions) {
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
			'deserializeActions' : function (policyActions) {
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
			}
		};
	}]);
*/
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
			if ($scope.shouldForward !== false) {
				window.location.hash = '#/policy';
			} else {
				$scope.$broadcast('editPolicyDone');
			}
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
			return uniqueName && $scope.state.currentPolicy.name
					&& $scope.state.currentPolicy.threatLevel >= 0
					&& $scope.state.currentPolicy.constraints.length > 0;
		}

		$scope.viewRemoveConstraint = function (constraintIndex) {
			viewConfirmation("Delete Constraint?",
				"Are you sure you want to delete the Constraint named '" + $scope.state.currentPolicy.constraints[constraintIndex].name + "'?",
				'Cancel',
				'Delete',
				function () {
					$scope.state.currentPolicy.constraints.splice(constraintIndex, 1);
				});
		};

		$scope.viewAddConstraint = function ($event) {
			if ($event) {
				$event.preventDefault();
			}
			$scope.state.currentConstraint = {
				conditions: [],
				operator: 'OR'
			};
		};

		$scope.viewEditConstraint = function (constraint) {
			//copy so we dont update data in the current list
			$scope.state.currentConstraint = angular.copy(constraint);
		};

		function handleHttpError(headerText, bodyText, status) {
			hideHttpMask();
			$scope.state.httpErrorBody = status === 0 ? 'Unable to connect to server.' : bodyText;
			$scope.state.httpErrorHeader = headerText;
			$('#httpErrorModal').modal('show');
		}

		$scope.editNotification = function (addresses) {
			$scope.$broadcast('editNotification', addresses);
		};

		$scope.$on('constraintChanged', function (event, constraint) {
			event.stopPropagation();
			if (angular.isUndefined(constraint.id)) {
				$scope.state.currentPolicy.constraints.push(constraint);
			} else if ($scope.state.currentConstraint !== null) {
				$scope.state.currentConstraint.conditions = null; // Don't want to merge condition array
				angular.forEach($scope.state.currentPolicy.constraints, function (candidate) {
					if (candidate.id === constraint.id) {
						angular.extend(candidate, constraint);
					}
				});
			}
			$scope.state.currentConstraint = null;
		}); 

		function getActions(actionStages) {
			var actions = {};
			angular.forEach(actionStages, function (stage) {
				actions[stage.id] = [];
			});
			return policyStore.deserializeActions(actions);
		}

		if ($routeParams.policyId === 'new') {
			actionStore.get().then(function (results) {
				var actionStages = results[1],
					state = {
						currentPolicy : policyStore.create(),
						actions : {}
					 };
				state.actions = getActions(actionStages);

				$scope.actionStages = actionStages,
				$scope.state = state;
			}, function (data, status, headers, config) {
				handleHttpError('Policy Initialization Error', data, status);
			});
		} else {
			$q.all([policyStore.get(), actionStore.get()]).then(function (results) {
				var policies = results[0],
					actionStages = results[1][1];
				$scope.policies = policies;
				angular.forEach(policies, function (policy, index) {
					 if (policy.id === $routeParams.policyId) {
						angular.extend($scope,  {
							state : {
								currentPolicy : angular.copy(policy)
							},
							actionStages : actionStages
						});
						return false;
					}
				});
				// TODO If currentPolicy === null, show error
				$scope.state.actions = angular.extend(getActions(actionStages), policyStore.deserializeActions($scope.state.currentPolicy.actions));
			}, function (data, status, headers, config) {
				handleHttpError('Policy Initialization Error', data, status);
			});
		}
	}]);

	module.controller('ConstraintEditorController', ['$scope', 'ConstraintStore', function ($scope, constraints) {
		$scope.cancelConstraint = function () {
			$scope.state.currentConstraint = null;
		};

		$scope.addConstraint = function () {
			angular.forEach($scope.currentConstraint.conditions, function (condition) {
				delete condition.v;
				delete condition.valueModifier;
			});
			$scope.$emit('constraintChanged', $scope.currentConstraint);
			$scope.condition = null;
		};

		$scope.updateAge = function (condition) {
			condition.value = (condition.v !== '' && condition.v != null && condition.valueModifier) ? condition.v * condition.valueModifier : null;
		}

		$scope.validateConstraint = function () {
			var i;
			delete $scope.state.constraintValidationMsg;

			if (!$scope.currentConstraint.name) {
				$scope.state.constraintValidationMsg = 'Please enter a name for this constraint';
				return;
			}

			for (i = 0; i < $scope.currentConstraint.conditions.length; i++) {
				if ($scope.currentConstraint.conditions[i].value === null || angular.isUndefined($scope.currentConstraint.conditions[i].value)) {
					$scope.state.constraintValidationMsg = 'Please enter a value for condition #' + (i + 1);
					return;
				} else if (!$scope.currentConstraint.conditions[i].conditionTypeId) {
					$scope.state.constraintValidationMsg = 'Please select a valid condition type for condition #' + (i + 1);
					return;
				}
			}
		};

		$scope.conditionTypeChanged = function (condition) {
			condition.operator = $scope.conditionTypes[condition.conditionTypeId].supportedOperators[0];
			delete condition.value;
			delete condition.v;
			delete condition.valueModifier;

			$scope.validateConstraint();
		};

		$scope.addCondition = function () {
			var conditionType = $scope.conditionTypes['AgeInDays'],
				valueType = conditionType.valueType;

			$scope.currentConstraint.conditions.push({
				conditionTypeId: conditionType.id,
				conditionType: conditionType,
				operator: conditionType.supportedOperators[0],
				valueType: valueType,
				valueModifier: 365
			});

			$scope.validateConstraint();
		};

		$scope.removeCondition = function (conditionIndex) {
			$scope.currentConstraint.conditions.splice(conditionIndex, 1);
			$scope.validateConstraint();
		};

		$scope.$watch('state.currentConstraint', function (newValue, oldValue) {
			if (newValue !== null && angular.isDefined(newValue)) {
				$scope.currentConstraint = angular.copy(newValue);
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
				$('#editConstraintModal').modal('show');
				$('#constraintName').focus()
			} else if (newValue === null || angular.isUndefined(newValue)) {
				$('#editConstraintModal').modal('hide');
				$scope.currentConstraint = null;
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
		}, function () {
			// error handling
		});

	}]);
}());