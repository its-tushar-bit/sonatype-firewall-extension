/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, Option */
(function () {
	'use strict';
	var module = angular.module('PolicyEditor', ['CLMAppLocation', 'Hudson', 'NotificationManagement', 'ResourceModule', 'ui.compat', 'ui.bootstrap', 'AngularCommon', 'CommonServices']);
	
	module.service('ConditionTypes', function(){
	    var types;
	    
	    return {
	        get : function() {
	            return types;
	        },
	        set : function(conditionTypes) {
	            types = conditionTypes;
	        }
	    }
	});

	module.service('PolicyStore', ['ConstraintStore', 'CLMLocations', 'CLMAppLocations', 'CLMResource', function (constraintStore, clmLocations, clmAppLocations, clmResource) {
		var conditionTypes = null,
			policyStoreTemplate = {
				id : 'id',
				template : function () {
					var o = {
						threatLevel : 5,
						constraints : [{ conditions: [], operator: null }],
						actions : {}
					};
					if (conditionTypes) {
						var conditionType = conditionTypes.AgeInDays;
						o.constraints[0].conditions.push({
							conditionTypeId: conditionType.id,
							operator: conditionType.supportedOperators[0],
							value : null
						});
					}
					return o;
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
					constraintStore.get().then(function (results) {
						conditionTypes = {};
						angular.forEach(results[0], function (type) {
							conditionTypes[type.id] = type;
						});
					});
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

	module.controller('PolicyEditorController', ['$scope', '$state', '$q', '$location', '$dialog', '$timeout', 'Messages', 'PolicyStore', 'ActionStore', function ($scope, $state, $q, $location, $dialog, $timeout, messages, policyStore, actionStore) {
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
		
		$scope.removeConstraint = function (constraint) {
		    $dialog.dialog({
                backdrop : true,
                backdropClick : false,
                backdropFade : true,
                dialogFade : true,
                template : '<div class="modal-body">Are you sure you want to delete this Constraint?</div>' +
                            '<div class="modal-footer"><button class="btn" ng-click="cancel()">Cancel</button>' +
                            '<button class="btn btn-danger" ng-click="discard()">Delete</button></div>',
                controller : ['$scope','dialog', function ($localScope, dialog) {
                    $localScope.discard = function () {
                        dialog.close(true);
                        angular.forEach($scope.policy.constraints, function(value,index) {
                            if (constraint === value) {
                                $scope.policy.constraints.splice(index,1);
                                return false;
                            }
                        });
                    };
                    $localScope.cancel = function () {
                        dialog.close(true);
                    };
                }]
            }).open();
		};

		$scope.editConstraint = function (constraint) {
            $('#collapse' + constraint.id).collapse('show');
		};
		
		$scope.addConstraint = function() {
		    var constraint = { 
		        id: '' + new Date().getTime(), 
		        conditions: [], 
		        operator: null 
		    };
		    $scope.policy.constraints.push(constraint);
		    $timeout(function(){
		        $('#collapse' + constraint.id).collapse('show');
		    });
		}

		$scope.editNotification = function (stage,policy) {
		    $scope.currentNotificationStage = stage.id;
		    $scope.currentNotificationPolicy = policy;
		    var addresses = [];

			if (policy.actions[stage.id]) {
				for (var i = 0 ; i < policy.actions[stage.id].length ; i++) {
					if (policy.actions[stage.id][i].actionTypeId == 'notify') {
						addresses = policy.actions[stage.id][i].target.split(',');
						break;
					}
				}
			}
			$scope.$broadcast('editNotification', addresses);
		};
		
        $scope.$on('editNotificationDone', function (event, addresses) {
            var found = false;
            for (var i = 0 ; i < $scope.currentNotificationPolicy.actions[$scope.currentNotificationStage].length ; i++) {
                if ($scope.currentNotificationPolicy.actions[$scope.currentNotificationStage][i].actionTypeId == 'notify') {
                    $scope.currentNotificationPolicy.actions[$scope.currentNotificationStage][i].target = addresses.join();
                    found = true;
                    break;
                }
            }
            if (!found) {
                $scope.currentNotificationPolicy.actions[$scope.currentNotificationStage].push({
                    actionTypeId : 'notify',
                    target : addresses.join()
                })
            }
            
            $scope.currentNotificationPolicy = null;
            $scope.currentNotificationStage = null;
        });
		
		$scope.toggleWarnAction = function(stage, policy) {
            var add = true;
            if (policy.actions[stage.id]) {
                for ( var i = policy.actions[stage.id].length - 1 ; i >= 0 ; i-- ) {
                    switch (policy.actions[stage.id][i].actionTypeId) {
                    case 'warn':
                        policy.actions[stage.id].splice(i,1);
                        add = false;
                        break;
                    case 'fail':
                        policy.actions[stage.id].splice(i,1);
                        break;
                    } 
                }
            } 
            
            if (add) {
                policy.actions[stage.id] = [{
                    actionTypeId: 'warn'
                }];
            }
        };
        $scope.toggleFailureAction = function(stage, policy) {
            var add = true;
            if (policy.actions[stage.id]) {
                for ( var i = policy.actions[stage.id].length - 1 ; i >= 0 ; i-- ) {
                    switch (policy.actions[stage.id][i].actionTypeId) {
                    case 'fail':
                        policy.actions[stage.id].splice(i,1);
                        add = false;
                        break;
                    case 'warn':
                        policy.actions[stage.id].splice(i,1);
                        break;
                    } 
                }
            } 
            
            if (add) {
                policy.actions[stage.id] = [{
                    actionTypeId: 'fail'
                }];
            }
        };
        $scope.showWarningIcon = function(stage, policy) {
            if (policy.actions[stage.id]) {
                for ( var i = 0 ; i < policy.actions[stage.id].length ; i++ ) {
                    if (policy.actions[stage.id][i].actionTypeId == 'warn') {
                        return true;
                    } 
                }
            }
        };
        $scope.showFailureIcon = function(stage, policy) {
            if (policy.actions[stage.id]) {
                for ( var i = 0 ; i < policy.actions[stage.id].length ; i++ ) {
                    if (policy.actions[stage.id][i].actionTypeId == 'fail') {
                        return true;
                    } 
                }
            }
        };
        $scope.getEmailList = function(stage, policy) {
            if (policy.actions[stage.id]) {
                for ( var i = 0 ; i < policy.actions[stage.id].length ; i++ ) {
                    if (policy.actions[stage.id][i].actionTypeId == 'notify') {
                        return policy.actions[stage.id][i].target;
                    } 
                }
            }
        }

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

	module.controller('ConstraintEditorController', ['$scope', '$timeout',  'ConstraintStore', 'ConditionTypes', function ($scope, $timeout, constraints, $conditionTypes) {
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
		};

		$scope.removeCondition = function (conditionIndex) {
			$scope.constraint.conditions.splice(conditionIndex, 1);
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
			$conditionTypes.set($scope.conditionTypes);
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

	module.controller('InlinePolicyEditorController', ['$scope', '$dialog', 'Messages', 'ConditionTypes', function (scope, $dialog, messages, conditionTypes) {
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
								scope.policy.$revert();
								scope.hide();
							};
							$scope.cancel = function () {
								dialog.close(true);
							};
						}]
					}).open();
				} else {
					scope.hide();
				}
			}
		};
		scope.savePolicy = function () {
		    scope.alerts = [];
		    if (scope.validate()) {
    			scope.policy.$save().then(function (policy) {
    				scope.hide();
    			}, function (error) {
    				scope.alerts.push({
    					type : 'error',
    					msg : 'An error occurred while saving the policy. (' + messages.getHttpErrorMessage(error) + ')'
    				});
    			});
		    }
		};
		scope.validate = function() {
            var msg = null;
            if (scope.policy) {
                if (!scope.policy.name) {
                    msg = 'Enter a valid name for the policy';
                } else {
                    $.each(scope.policy.constraints, function(constraintIndex,constraint){
                        if (!constraint.name) {
                            msg = 'Enter a valid name for constraint #' + (constraintIndex + 1);
                            return false;
                        } else if(!constraint.operator) {
                            msg = 'You must select any or all of the conditions for constraint #' + (constraintIndex + 1);
                            return false;
                        }
                        
                        $.each(constraint.conditions, function(conditionIndex, condition){
                            var conditionType = conditionTypes.get()[condition.conditionTypeId];
                            if (!conditionType) {
                                msg = 'Please select a valid condition type for condition #' + (conditionIndex + 1) + ' in constraint #' + (constraintIndex + 1);
                                return false;
                            } else if (conditionType.valueTypeId && !(condition.value === 0 || condition.value)) {
                                msg = 'Please enter a value for condition #' + (conditionIndex + 1) + ' in constraint #' + (constraintIndex + 1);
                                return false;
                            }
                        });
                        
                        if (msg) {
                            return false;
                        }
                    });
                }
            }
            
            if (msg) {
                scope.alerts.push({
                    msg:msg,
                    type:'error'
                });
                return false;
            } else {
                return true;
            }
        };
		scope.alerts = [];
	}]);

	module.directive('inlinePolicyCreator', ['$dialog', 'Messages', function ($dialog, messages) {
		return {
			restrict : 'A',
			templateUrl : "../assets/components/policy-editor/policy-quick-add.html",
			scope : {
				createPolicy : '&inlinePolicyCreator'
			},
			controller : 'InlinePolicyEditorController',
			link : function (scope) {
				scope.hide = function () {
					scope.policy = null;
				};
			}
		};
	}]);
	
	module.directive('inlinePolicyEditor', ['$dialog', 'Messages', function ($dialog, messages) {
        return {
            restrict : 'A',
            templateUrl : "../assets/components/policy-editor/policy-inline-editor.html",
			controller : 'InlinePolicyEditorController',
			link : function (scope) {
				scope.hide = function () {
					scope.policyEditMap[scope.policy.id] = null;
				};
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
