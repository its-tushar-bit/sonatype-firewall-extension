/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
/*jslint plusplus: true */
(function () {
	'use strict';

	var policyModule = angular.module('Policy', ['Hudson']);

	policyModule.controller('InsightPolicyController', ['$scope', 'global', '$http', 'hudson', '$timeout', 'CLMLocations', '$rootScope', function ($scope, global, $http, hudson, $timeout, clmLocations, $rootScope) {

		function updatePolicySummary(data) {
			data.summary = {
				constraints: data.constraints.length + ' Constraint(s) to be evaluated'
			};

			var actionCount = 0,
				actionNames = '';
			angular.forEach(data.actions, function (value, key) {
				var j;
				if (value.length > 0) {
					actionCount++;
					if (actionNames.length > 0) {
						actionNames += ', ';
					}

					for (j = 0; j < $scope.state.actionStageList.length; j++) {
						if ($scope.state.actionStageList[j].id == key) {
							actionNames += $scope.state.actionStageList[j].name + ': ';
							break;
						}
					}

					for (j = 0; j < value.length; j++) {
						if (j > 0) {
							actionNames += '/';
						}
						if (value[j].actionTypeId === 'warn') {
							actionNames += 'Warn';
						} else if (value[j].actionTypeId === 'fail') {
							actionNames += 'Fail';
						} else if (value[j].actionTypeId === 'notify') {
							actionNames += 'Notify';
						}
					}
				}
			});

			data.summary.actionCount = actionCount;
			data.summary.actions = actionNames;
		}
		
		function removePolicySummary(data) {
			delete data.summary;
		}

		function getConditionType(id) {
			var i;
			for (i = 0; i < $scope.state.conditionTypeList.length; i++) {
				if ($scope.state.conditionTypeList[i].id == id) {
					return $scope.state.conditionTypeList[i];
				}
			}
			return null;
		}

		function getConditionValueType(id) {
			var i;
			for (i = 0; i < $scope.state.conditionValueTypeList.length; i++) {
				if ($scope.state.conditionValueTypeList[i].id == id) {
					return $scope.state.conditionValueTypeList[i];
				}
			}
			return null;
		}

		function addUIConditionData(data) {
			angular.forEach(data.constraints, function (constraint, constraintIndex) {
				angular.forEach(constraint.conditions, function (condition, conditionIndex) {
					condition.conditionType = getConditionType(condition.conditionTypeId);
					condition.valueType = getConditionValueType(condition.conditionType.valueTypeId);
					if (condition.value) {
						var parts = condition.value.split(',');
						if (parts.length > 1) {
							condition.value = parts;
						} else if (condition.valueType && condition.valueType.id === 'AgeInDaysValueType') {
							if (condition.value >= 365 && condition.value % 365 === 0) {
								condition.value = condition.value / 365;
								condition.valueModifier = 'y';
							} else if (condition.value >= 30 && condition.value % 30 === 0) {
								condition.value = condition.value / 30;
								condition.valueModifier = 'm';
							} else {
								condition.valueModifier = 'd';
							}
						}
					}
				});
			});
		}

		function removeUIConditionData(data) {
			angular.forEach(data.constraints, function (constraint, constraintIndex) {
				angular.forEach(constraint.conditions, function (condition, conditionIndex) {
					if (angular.isArray(condition.value)) {
						condition.value = condition.value.join();
					} else if (condition.valueType && condition.valueType.id === 'AgeInDaysValueType') {
						if (condition.valueModifier === 'y') {
							condition.value = condition.value * 365;
						} else if (condition.valueModifier === 'm') {
							condition.value = condition.value * 30;
						}
					}

					delete condition.valueModifier;
					delete condition.conditionType;
					delete condition.valueType;
				});
			});
		}

		function showHttpMask(bodyText) {
			$scope.state.httpMaskBody = bodyText;
			$('#httpMaskModal').modal('show');
		}

		function hideHttpMask() {
			$('#httpMaskModal').modal('hide');
		}

		function handleHttpError(headerText, bodyText, status) {
			hideHttpMask();
			$scope.state.httpErrorBody = status === 0 ? 'Unable to connect to server.' : bodyText;
			$scope.state.httpErrorHeader = headerText;
			$('#httpErrorModal').modal('show');
		}

		function httpRequest(url, method, maskText, errorText, item, successFn) {
			if (maskText) {
				showHttpMask(maskText);
			}

			var _http = $http;
			if (method === 'post') {
				_http = hudson;
			}

			_http[method](url, item).success(function (data, status, headers, config) {
				successFn(data, status, headers, config);
				if (maskText) {
					hideHttpMask();
				}
			}).error(function (data, status, headers, config) {
				handleHttpError(errorText, data, status);
			});
		}

		function httpGet(url, errorText, successFn) {
			httpRequest(url, 'get', null, errorText, null, successFn);
		}

		function httpPost(url, maskText, errorText, item, successFn) {
			httpRequest(url, 'post', maskText, errorText, item, successFn);
		}

		function httpPut(url, maskText, errorText, item, successFn) {
			httpRequest(url, 'put', maskText, errorText, item, successFn);
		}

		function httpDelete(url, maskText, errorText, successFn) {
			httpRequest(url, 'delete', maskText, errorText, null, successFn);
		}

		function isDoneLoading() {
			return $scope.state.conditionTypeList !== undefined
				&& $scope.state.actionTypeList !== undefined
				&& $scope.state.actionStageList !== undefined
				&& $scope.state.conditionValueTypeList !== undefined
				&& $scope.state.policyList !== undefined;
		}

		function resetConstraint() {
			$scope.state.currentConstraint = {
				conditions: [],
				operator: 'OR'
			};
			$scope.addCondition();
			delete $scope.state.actionEditMode;
		}

		function resetActions() {
			var i, j, item;
			$scope.state.actionTableData = [];

			if ($scope.state.currentPolicy) {
				for (i = 0; i < $scope.state.actionStageList.length; i++) {
					item = {
						id: $scope.state.actionStageList[i].id,
						name: $scope.state.actionStageList[i].name,
						action: 'none',
						targetCount: 0
					};
					
					if ($scope.state.currentPolicy.actions[$scope.state.actionStageList[i].id] && $scope.state.currentPolicy.actions[$scope.state.actionStageList[i].id].length > 0) {
						angular.forEach($scope.state.currentPolicy.actions[$scope.state.actionStageList[i].id],function(value,key){
							if (value.actionTypeId === 'notify'){
								item.target = value.target;
								
								if (item.target) {
									item.targetCount = item.target.split(/,/g).length;
								}
							} else {
								item.action = value.actionTypeId;
							}
						});
					}
					
					$scope.state.actionTableData.push(item);
				}
			}
		}

		function reset() {
			delete $scope.state.policyChanged;
			delete $scope.state.policyWatchStopFn;
			resetConstraint();
			delete $scope.state.currentPolicy;
			delete $scope.state.showAddPolicyScreen;
			resetActions();
		}

		function postLoad() {
			angular.forEach($scope.state.policyList, function (policy, key) {
				updatePolicySummary(policy);
				addUIConditionData(policy);
			});

			reset();
			hideHttpMask();
		}

		function loadList(url, stateVar, errorText) {
			delete $scope.state[stateVar];
			httpGet(url + '?timestamp=' + new Date().getTime(), errorText, function (data, status, headers, config) {
				$scope.state[stateVar] = data;
				if (isDoneLoading()) {
					postLoad();
				}
			});
		}

		function pushActionDataToModel() {
			if ($scope.state.actionEditMode) {
				if ($scope.state.currentPolicy) {
					var handleAction = function (id) {
						var result = [],
							i;

						for (i = 0; i < $scope.state.actionTableData.length; i++) {
							if ($scope.state.actionTableData[i].id === id) {
								if ($scope.state.actionTableData[i].action != 'none'){
									result.push({
										actionTypeId: $scope.state.actionTableData[i].action
									});
								}
								
								if ($scope.state.actionTableData[i].target){
									result.push({
										actionTypeId: 'notify',
										target: $scope.state.actionTableData[i].target
									});
								}
							}
						}

						return result;
					};

					angular.forEach($scope.state.actionStageList, function (value, key) {
						$scope.state.currentPolicy.actions[value.id] = handleAction(value.id);
					});
				}
			}
		}

		function viewConfirmation(header, body, declineText, acceptText, acceptFn, declineFn) {
			$scope.state.confirmationHeader = header;
			$scope.state.confirmationBody = body;
			$scope.state.confirmationDeclineText = declineText;
			$scope.state.confirmationAcceptText = acceptText;
			$scope.state.confirmationAcceptFn = acceptFn;
			$scope.state.confirmationDeclineFn = declineFn;
			$('#confirmationModal').modal('show');
		}

		function hidePolicy() {
			if ($scope.state.policyWatchStopFn) {
				$scope.state.policyWatchStopFn();
			}
			reset();
		}
		
		function watchPolicyChange(){
			//this is simply to wait on doing this until after the current digest
			$timeout(function () {
				$scope.state.policyWatchStopFn = $scope.$watch('state.currentPolicy', function () {
					$scope.state.policyChanged = true;
				}, true);
				
				$('#policyName').focus();
			}, 100);
		}
		
		function setConstraintFormFocus(){
			//this is simply to wait on doing this until after the current digest
			//note im passing in false as there is no need to run angulars $apply, we aren't touching anything
			$timeout(function () {
				$('#constraintName').focus();
			}, 100, false);
		}
		
		function getIndex(obj) {
			if (obj && obj.$index >= 0) {
				return obj.$index;
			}
			
			return $scope.$index;
		}
		
		function deletePolicy() {
			httpDelete(clmLocations.getPolicyUrl() + '/' + $scope.state.policyList[$scope.state.deletePolicyIndex].id, 'Deleting policy...', 'Policy Delete Error', function () {
				$scope.state.policyList.splice($scope.state.deletePolicyIndex, 1);
				delete $scope.state.deletePolicyIndex;
			});
		};
		
		function cancelDeletePolicy() {
			delete $scope.state.deletePolicyIndex;
		}
		
		function deleteConstraint() {
			$scope.state.currentPolicy.constraints.splice($scope.state.deleteConstraintIndex, 1);
			$scope.validatePolicy();
		};

		$scope.state = global;

		$scope.viewEditPolicy = function () {
			$scope.state.currentPolicy = angular.copy($scope.state.policyList[getIndex(this)]);
			$scope.state.showAddPolicyScreen = true;
			$scope.state.addPolicyTitle = 'Edit Policy';
			resetActions();
			$scope.validatePolicy();

			watchPolicyChange();
		};

		$scope.viewRemovePolicy = function () {
			$scope.state.deletePolicyIndex = getIndex(this);
			viewConfirmation("Delete Policy?", "Are you sure you want to delete the Policy named '" + $scope.state.policyList[$scope.state.deletePolicyIndex].name + "'?  This action is not reversible.", 'Cancel', 'Delete', deletePolicy, cancelDeletePolicy);
		};

		$scope.viewCreatePolicy = function ($event) {
			$event.preventDefault();
			$scope.state.currentPolicy = {
				constraints: [],
				actions: {},
				threatLevel: 5
			};
			$scope.state.showAddPolicyScreen = true;
			$scope.state.addPolicyTitle = 'Create a New Policy';
			resetActions();
			
			watchPolicyChange();
		};

		$scope.savePolicy = function () {
			//I copy the item here as I don't want to dirty the UI data with changes needed for the server
			var item = angular.copy($scope.state.currentPolicy),
				i;
			removeUIConditionData(item);
			removePolicySummary(item);
			//edit
			if ($scope.state.currentPolicy.id) {
				httpPut(clmLocations.getPolicyUrl(), 'Saving policy...', 'Policy Save Error', item, function (data, status, headers, config) {
					updatePolicySummary(data);
					addUIConditionData(data);
					for (i = 0; i < $scope.state.policyList.length; i++) {
						if ($scope.state.policyList[i].id === data.id) {
							angular.copy(data, $scope.state.policyList[i]);
							break;
						}
					}
					hidePolicy();
				});
			} else {
				httpPost(clmLocations.getPolicyUrl(), 'Saving policy...', 'Policy Save Error', item, function (data, status, headers, config) {
					updatePolicySummary(data);
					addUIConditionData(data);
					$scope.state.policyList.push(data);
					hidePolicy();
				});
			}
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
			if ($scope.state.policyChanged) {
				viewConfirmation("Cancel Policy Changes?", "Are you sure you want to cancel?  Any changes made to the Policy will be lost.", 'No', 'Yes', hidePolicy);
			} else {
				hidePolicy();
			}
		};

		$scope.validatePolicy = function () {
			delete $scope.state.policyValid;
			if ($scope.state.currentPolicy.name
					&& $scope.state.currentPolicy.threatLevel >= 0
					&& $scope.state.currentPolicy.constraints.length > 0) {
				$scope.state.policyValid = true;
			}
		};

		$scope.viewRemoveConstraint = function () {
			$scope.state.deleteConstraintIndex = getIndex(this);
			viewConfirmation("Delete Constraint?", "Are you sure you want to delete the Constraint named '" + $scope.state.currentPolicy.constraints[$scope.state.deleteConstraintIndex].name + "'?", 'Cancel', 'Delete', deleteConstraint);
		};

		$scope.viewAddConstraint = function ($event) {
			if ($event) {
				$event.preventDefault();
			}
			resetConstraint();
			$('#editConstraintModal').modal('show');
			setConstraintFormFocus();
		};

		$scope.viewEditConstraint = function () {
			//copy so we dont update data in the current list
			$scope.state.currentConstraint = angular.copy($scope.state.currentPolicy.constraints[getIndex(this)]);
			$scope.validateConstraint();
			$('#editConstraintModal').modal('show');
			setConstraintFormFocus();
		};

		$scope.cancelConstraint = function () {
			$('#editConstraintModal').modal('hide');
			resetConstraint();
		};

		$scope.addConstraint = function () {
			var constraintObj = {
					name: $scope.state.currentConstraint.name,
					conditions: $scope.state.currentConstraint.conditions,
					operator: $scope.state.currentConstraint.operator,
					enabled: true,
					//TODO: this will ultimately come from the server
					id: $scope.state.currentConstraint.id ? $scope.state.currentConstraint.id : $scope.state.currentConstraint.name
				},
				found = false,
				i;

			for (i = 0; i < $scope.state.currentPolicy.constraints.length; i++) {
				if ($scope.state.currentPolicy.constraints[i].id == constraintObj.id) {
					$scope.state.currentPolicy.constraints[i] = constraintObj;
					found = true;
					break;
				}
			}

			if (!found) {
				$scope.state.currentPolicy.constraints.push(constraintObj);
			}

			resetConstraint();

			$('#editConstraintModal').modal('hide');

			$scope.validatePolicy();
		};

		$scope.validateConstraint = function () {
			var i;
			delete $scope.state.constraintValidationMsg;

			if (!$scope.state.currentConstraint.name) {
				$scope.state.constraintValidationMsg = 'Please enter a name for this constraint';
				return;
			}

			for (i = 0; i < $scope.state.currentConstraint.conditions.length; i++) {
				if ($scope.state.currentConstraint.conditions[i].valueType && !$scope.state.currentConstraint.conditions[i].value) {
					$scope.state.constraintValidationMsg = 'Please enter a value for condition #' + (i + 1);
					return;
				}
			}
		};

		$scope.conditionTypeChanged = function () {
			var condition = $scope.state.currentConstraint.conditions[getIndex(this)];
			condition.conditionType = getConditionType(condition.conditionTypeId);
			condition.valueType = getConditionValueType(condition.conditionType.valueTypeId);

			condition.operator = condition.conditionType.supportedOperators[0];

			delete condition.value;

			condition.valueModifier = 'y';

			$scope.validateConstraint();
		};

		$scope.addCondition = function () {
			var conditionType = getConditionType($scope.state.conditionTypeList[0].id),
				valueType = getConditionValueType(conditionType.valueTypeId);

			$scope.state.currentConstraint.conditions.push({
				conditionTypeId: conditionType.id,
				conditionType: conditionType,
				operator: conditionType.supportedOperators[0],
				valueType: valueType,
				valueModifier: 'y'
			});

			$scope.validateConstraint();
		};

		$scope.removeCondition = function () {
			$scope.state.currentConstraint.conditions.splice(getIndex(this), 1);
			$scope.validateConstraint();
		};

		$scope.editActions = function () {
			if ($scope.state.actionEditMode) {
				pushActionDataToModel();
			}
			$scope.state.actionEditMode = !$scope.state.actionEditMode;
		};
		
		$scope.viewEditNotifications = function () {
			if ($scope.state.actionEditMode) {
				$scope.state.notificationEmailList = [];
				$scope.state.currentActionStep = $scope.state.actionTableData[getIndex(this)];
				if ($scope.state.currentActionStep.target){
					$scope.state.notificationEmailList = $scope.state.currentActionStep.target.split(',');
				}
				$('#editNotificationsModal').modal('show');
			}
		}
		
		$scope.addNotificationEmail = function () {
			$scope.state.notificationEmailList.push($scope.state.currentNotificationEmail);
			item.targetCount = item.target.split(/,/g).length;
		}
		
		$scope.cancelNotificationEmail = function () {
			$('#editNotificationsModal').modal('hide');
		}
		
		$scope.doneNotificationEmail = function () {
			$('#editNotificationsModal').modal('hide');
			$scope.state.currentActionStep.target = $scope.state.notificationEmailList.join();
			$scope.state.currentActionStep.targetCount = $scope.state.notificationEmailList.length;
		}
		
		$scope.removeNotificationEmail = function () {
			$scope.state.notificationEmailList.splice(getIndex(this), 1);
		}

		$scope.$watch('state.actionTableData', function () {
			pushActionDataToModel();
		}, true);

		$rootScope.$on('tabChange', function (event, args) {
			if (args[0].indexOf('policy') >= 0 && $scope.state.policyChanged) {
				event.preventDefault();
				event.stopPropagation();
				viewConfirmation("Unsaved Changes", "Navigating away will lose changes to the current policy.  Do you want to do this?", 'Yes', 'No', null, function () {
					args[1]();
				});
			}
		});

		showHttpMask('Loading data from server...');

		loadList(clmLocations.getConditionTypeUrl(), 'conditionTypeList', 'Condition Type Initialization Error');
		loadList(clmLocations.getActionTypeUrl(), 'actionTypeList', 'Action Type Initialization Error');
		loadList(clmLocations.getActionStageUrl(), 'actionStageList', 'Action Stage Initialization Error');
		loadList(clmLocations.getConditionValueTypeUrl(), 'conditionValueTypeList', 'Condition Value Type Initialization Error');
		loadList(clmLocations.getPolicyUrl(), 'policyList', 'Policy Initialization Error');
	}]);
	
	var EMAIL_REGEXP = /^\S+@\S+\.\S+$/;
	policyModule.directive('emailInput', function() {
	  return {
	    require: 'ngModel',
	    link: function(scope, elm, attrs, ctrl) {
	      ctrl.$parsers.unshift(function(viewValue) {
	      	if (!viewValue) {
	      		scope.state.notificationValidationMsg = "Enter an email address";
	      		return undefined;
	      	} else if (!EMAIL_REGEXP.test(viewValue)) {
	        	scope.state.notificationValidationMsg = "Enter a valid email address";
	    		return undefined;
	        } else {
	        	delete scope.state.notificationValidationMsg;
	        	return viewValue;
	        }
	      });
	    }
	  };
	});
}());
