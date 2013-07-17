/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, Option */
(function () {
	'use strict';
	var module = angular.module('PolicyEditor', ['CLMAppLocation', 'Hudson', 'NotificationManagement', 'ResourceModule', 'ui.compat', 'ui.bootstrap', 'AngularCommon']);

	module.service('PolicyStore', ['CLMLocations', 'CLMAppLocations', 'CLMResource', function (clmLocations, clmAppLocations, clmResource) {
		var policyStoreTemplate = {
				id : 'id',
				template : {
					threatLevel : 5,
					constraints : [{ conditions: [], operator: null }],
					actions : {}
				},
				params : {
					timestamp : new Date().getTime()
				}
			},
			policyStores = {};

		return {
			get : function () {
				var ownerId = clmAppLocations.getEntityId(),
					store = policyStores[ownerId];
				if (!store) {
					// Expire existing stores, prevents user from encountering stale data
					angular.forEach(policyStores, function (value, key) {
						policyStores[key] = null;
					});
					store = clmResource.getStore(angular.extend({ url : clmAppLocations.getPolicyUrl() }, policyStoreTemplate));
					policyStores[ownerId] = store;
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
			isActionDirty : function (policy, actions) {
				var changed = false;
				angular.forEach(actions, function (dAction, dStage) {
					var emailCount = 0;
					angular.forEach(policy.actions[dStage], function (policyAction) {
						if (policyAction.actionTypeId === 'notify') {
							changed = changed || (dAction.notify.indexOf(policyAction.target) === -1);
							emailCount++;
						} else {
							changed = changed || (policyAction.actionTypeId !== dAction.action);
						}
					});
					changed = changed || emailCount !== dAction.notify.length || (dAction.action !== null && emailCount === policy.actions[dStage].length);
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
				url : clmLocations.getConditionTypeUrl(),
                                params : {
                                  timestamp : new Date().getTime()
                                }
			});

		return {
			'get' : function () {
				var conditionValueTypeStore = clmResource.getStore({
						id : 'id',
						url : clmAppLocations.getConditionValueTypeUrl(),
                                                params : {
                                                  timestamp : new Date().getTime()
                                                }
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
			if ($scope.policy) {
				if (angular.isUndefined($scope.policy.id)) {
					changed = $scope.policy.constraints.length > 0 || $scope.policy.name;
					if (!changed) {
						angular.forEach(policyStore.serializeActions($scope.actions), function (actions, stage) {
							changed = changed || actions.length > 0;
						});
					}
				} else {
					angular.forEach($scope.policies, function (policy, index) {
						if (policy.id === $scope.policy.id) {
							changed = $scope.policy.isDirty() || policyStore.isActionDirty($scope.policy, $scope.actions);
						}
					});
				}
			}
			return changed;
		}

		$scope.doLoad = function () {
			$scope.error = null;
			$q.all([actionStore.get()]).then(function (results) {
				var actionStages = results[0][1];

				$scope.actionStages = actionStages;
			}, function (errors) {
				$scope.error = angular.isArray(errors) ? errors[0] : errors;
			});
		};

		$scope.$watch('policy', function () {
			if ($scope.policy) {
				$scope.actions = angular.extend(getActions($scope.actionStages), policyStore.deserializeActions($scope.policy.actions));
			} else {
				$scope.actions = null;
			}
		});


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
				"Are you sure you want to delete the Constraint named '" + $scope.policy.constraints[constraintIndex].name + "'?",
				'Cancel',
				'Delete',
				function () {
					$scope.policy.constraints.splice(constraintIndex, 1);
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
			angular.forEach($scope.policy.constraints, function (candidate) {
				present = present || candidate === constraint;
			});
			if (!present) {
				// New constraint
				$scope.policy.constraints.push(constraint);
			}
		});

		//make sure user is aware they are about to lose changes
		$scope.$on('pageChangeStarted', function (event) {
			if (isDirty()) {
				event.preventDefault();
			}
		});

		$scope.doLoad();
	}]);

	module.directive('ModalConstraintController', ['$scope', '$timeout',  'ConstraintStore', function ($scope, $timeout, constraints) {
		$scope.cancelConstraint = function () {
			$('#editConstraintModal').modal('hide');
			$scope.originalConstraint = $scope.currentConstraint = null;
		};

		//ditch edits in this case
		$scope.$on('pageChangeAccepted', function (event) {
			$scope.cancelConstraint();
		});

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

		$scope.$on('policy.editConstraint', function (event, constraint) {
			$('#editConstraintModal').modal('show');
			$scope.originalConstraint = constraint || null;
			$scope.constraint = constraint ? angular.copy(constraint) : { conditions: [], operator: null };
		});
	}]);

	module.controller('ConstraintEditorController', ['$scope', '$timeout',  'ConstraintStore', function ($scope, $timeout, constraints) {
		function isDirty() {
			if ($scope.originalConstraint) {
				if ($scope.originalConstraint.name != $scope.constraint.name || $scope.originalConstraint.operator != $scope.constraint.operator ||
						$scope.originalConstraint.conditions.length != $scope.constraint.conditions.length)
					return true;
				for (var i = 0; i < $scope.originalConstraint.conditions.length; i++) {
					if ($scope.originalConstraint.conditions[i].value != $scope.constraint.conditions[i].value ||
							$scope.originalConstraint.conditions[i].operator != $scope.constraint.conditions[i].operator ||
							$scope.originalConstraint.conditions[i].conditionTypeId != $scope.constraint.conditions[i].conditionTypeId)
						return true;
				}
			}
			return false;
		}
		$scope.constraintConditionChoices = [{
			'value' : 'AND',
			'name' : 'all'
		},{
			'value' : 'OR',
			'name' : 'any'
		}];

		//make sure user is aware they are about to lose changes
		$scope.$on('pageChangeStarted', function (event) {
			if (isDirty()) {
				event.preventDefault();
			}
		});

		$scope.validateConstraint = function () {
			var i,
				conditions = $scope.constraint.conditions,
				conditionType;

			delete $scope.constraintValidationMsg;

			if (!$scope.constraint.name) {
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

			if(!$scope.constraint.operator)
			{
				$scope.constraintValidationMsg = 'You must select any or all of the conditions';
				return;
			}
		};

		$scope.conditionTypeChanged = function (condition) {		
			// Remove values that were entered with the previous condition type
			delete condition.value;

			// This could be replaced with ng-init but the html is fairly verbose as it is
			condition.operator = $scope.conditionTypes[condition.conditionTypeId].supportedOperators[0];
			switch ($scope.conditionTypes[condition.conditionTypeId].valueTypeId) {
				case 'LicenseCategoryValueType':
				case 'LicenseValueType':
				case 'LicenseThreatGroupValueType':
				case 'LicenseStatusValueType':
				case 'IdentificationSourceValueType':
				case 'MatchStateValueType':
				case 'SecurityVulnerabilityStatusValueType':
				case 'LabelValueType':
					if ($scope.conditionTypes[condition.conditionTypeId].valueType.availableValues && $scope.conditionTypes[condition.conditionTypeId].valueType.availableValues.length > 0) {
						condition.value = $scope.conditionTypes[condition.conditionTypeId].valueType.availableValues[0].id;
					}
					break;
			}

			$scope.validateConstraint();
		};
		$scope.$watch('constraint', function (constraint) {
			if (constraint) {
				var fn = function () {
					if ($scope.conditionTypes) {
						if ($scope.constraint.conditions.length === 0) {
							$scope.addCondition();
						} else {
							angular.forEach($scope.constraint.conditions, function (condition) {
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
							});
						}
						$scope.validateConstraint();
						$('#constraintName').focus();
						$scope.originalConstraint = angular.copy(constraint);
					} else {
						$timeout(fn, 100);
					}
				};
				fn();
			} else {
				$scope.originalConstraint = null;
			}
		});

		$scope.addCondition = function () {
			var conditionType = $scope.conditionTypes['AgeInDays'];

			$scope.constraint.conditions.push({
				conditionTypeId: conditionType.id,
				operator: conditionType.supportedOperators[0]
			});

			$scope.validateConstraint();
		};

		$scope.removeCondition = function (conditionIndex) {
			$scope.constraint.conditions.splice(conditionIndex, 1);
			$scope.validateConstraint();
		};

		constraints.get().then(function (results) {
			var typeValues = {};
			$scope.conditionTypes = {};
			angular.forEach(results[1], function (typeValue) {
				typeValues[typeValue.id] = typeValue;
			});
			angular.forEach(results[0], function (type) {
				var typeValue = type.valueTypeId ? typeValues[type.valueTypeId] : null;
				type.valueType = typeValue;
				$scope.conditionTypes[type.id] = type;
			});
		}, function (error) {
			// TODO handle this error
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
				});
			}
		};
	}]);

	module.directive('inlinePolicyCreator', ['$dialog', 'Messages', function ($dialog, messages) {
		return {
			restrict : 'A',
			templateUrl : "../assets/components/policy-editor/policy-quick-add.html",
			scope : {
				createPolicy : '&inlinePolicyCreator'
			},
			link : function (scope, element, attrs) {
				scope.click = function () {
					if (!scope.policy) {
						scope.policy = scope.createPolicy();
					}
				};
				scope.cancel = function () {
					if (scope.policy) {
						if (scope.policy.isDirty()) {
							// show dialog
						$dialog.dialog({
								backdrop : true,
								backdropClick : false,
								backdropFade : true,
								dialogFade : true,
								template : '<div class="modal-body">May contain unsaved changes.</div>' +
											'<div class="modal-footer"><button class="btn" ng-click="cancel()">Cancel</button>' +
											'<button class="btn btn-danger" ng-click="discard()">Discard</button></div>',
								controller : ['$scope', 'dialog', function ($scope, dialog) {
									$scope.discard = function () {
										dialog.close(true);
										scope.policy = null;
									};
									$scope.cancel = function () {
										dialog.close(true);
									};
								}]
							}).open();
						} else {
							scope.policy = null;
						}
					}
				};
				scope.savePolicy = function () {
					scope.policy.$save().then(function (policy) {
						scope.policy = null;
					}, function (error) {
						scope.alerts.push({
							type : 'error',
							msg : 'An error occurred while saving the policy. (' + messages.getHttpErrorMessage(error) + ')'
						});
					});
				};
				scope.alerts = [];
			}
		};
	}]);

	module.directive('inlineConstraintEditor', function () {
		return {
			restrict : 'A',
			scope : {
				constraint : '=inlineConstraintEditor'
			},
			controller : 'ConstraintEditorController'
		};
	});

	module.directive('ageInDays', function () {
		return {
			restrict : 'A',
			scope : {
				model : '=ngModel'
			},
			template : "<input type='number' style='width:100px;vertical-align:top' ng-model='value' placeholder='{{placeholder}}' required> <select style='width:100px;vertical-align:top' ng-model='modifier' ng-options='timeSpan.value as timeSpan.name for timeSpan in timeSpans' required></select>",
			link : function (scope, element, attrs) {
				function updateModel() {
					scope.model = (scope.value !== '' && scope.value !== null && scope.modifier) ? scope.value * scope.modifier : null;
				}
				function updateValue() {
					if (!scope.model) {
						scope.value = null;
						scope.modifier = 365;
					} else {
						if (scope.model >= 365 && scope.model % 365 === 0) {
							scope.modifier = 365;
						} else if (scope.model >= 30 && scope.model % 30 === 0) {
							scope.modifier = 30;
						} else {
							scope.modifier = 1;
						}
						scope.value = scope.model / scope.modifier;
					}
				}
				scope.timeSpans = [{'value':1, 'name':'Days'},{'value':30, 'name':'Months'},{'value':365, 'name':'Years'}];
				// TODO Some work here when editing an existing condition to ensure we don't touch the initial state
				scope.$watch('model', updateValue);
				scope.$watch('value', updateModel);
				scope.$watch('modifier', updateModel);
			}
		};
	});
}());
