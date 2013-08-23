/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, Option */
(function () {
	'use strict';
	var module = angular.module('PolicyEditor', ['CLMAppLocation', 'CLMLocation', 'Hudson', 'ResourceModule', 'ui.compat', 'ui.bootstrap', 'AngularCommon', 'CommonServices']);
	
	module.service('PolicyStore', ['ConstraintStore', 'CLMLocations', 'CLMAppLocations', 'CLMResource', function (constraintStore, clmLocations, clmAppLocations, clmResource) {
		var conditionTypes = null,
			policyStoreTemplate = {
				id : 'id',
				template : function () {
					var o = {
						threatLevel : 5,
						constraints : [{ conditions: [], operator: 'OR', id : '' + new Date().getTime() }],
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
			getConditionTypes : function () {
			    return conditionTypes;
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

	module.controller('PolicyEditorController', ['$scope', '$state', '$location', '$dialog', '$timeout', 'Messages', 'PolicyStore', '$q', 'ActionStore', function ($scope, $state, $location, $dialog, $timeout, messages, policyStore, $q, actionStore) {
		function isDirty() {
			if ($scope.policy) {
				return $scope.policy.isDirty();
			}
			return false;
		}
		
        function showActionIcon(stageId, action) {
            if ($scope.policy.actions[stageId]) {
                for ( var i = 0 ; i < $scope.policy.actions[stageId].length ; i++ ) {
                    if ($scope.policy.actions[stageId][i].actionTypeId == action) {
                        return true;
                    } 
                }
            }
        }
		
		function toggleAction(stageId, action) {
		    var add = true;
            if ($scope.policy.actions[stageId]) {
                for ( var i = $scope.policy.actions[stageId].length - 1 ; i >= 0 ; i-- ) {
                    switch ($scope.policy.actions[stageId][i].actionTypeId) {
                    case 'warn':
                        $scope.policy.actions[stageId].splice(i,1);
                        if (action === 'warn') {
                            add = false;
                        }
                        break;
                    case 'fail':
                        $scope.policy.actions[stageId].splice(i,1);
                        if (action === 'fail') {
                            add = false;
                        }
                        break;
                    } 
                }
            } 
            
            if (add) {
                $scope.policy.actions = $scope.policy.actions || {};
                if (!$scope.policy.actions[stageId]) {
                    $scope.policy.actions[stageId] = [];
                }
                $scope.policy.actions[stageId].push({
                    actionTypeId: action
                });
            }
		}
		
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
		
		$scope.addConstraint = function() {
		    var constraint = { 
		        id: '' + new Date().getTime(), 
		        conditions: [], 
		        operator: 'OR' 
		    };
		    $scope.policy.constraints.push(constraint);
		    $timeout(function(){
		        $('#collapse' + constraint.id).collapse('show');
		    });
		};

		$scope.editNotification = function (stage) {
			var addresses = [];

			if ($scope.policy.actions[stage.id]) {
				for (var i = 0 ; i < $scope.policy.actions[stage.id].length ; i++) {
					if ($scope.policy.actions[stage.id][i].actionTypeId == 'notify') {
						addresses = $scope.policy.actions[stage.id][i].target.split(',');
						break;
					}
				}
			}

			$dialog.dialog({
				backdrop : true,
				backdropClick : false,
				backdropFade : true,
				dialogFade : true,
				templateUrl : 'notification',
				controller : ['$scope', 'dialog', function (scope, dialog) {
					var EMAIL_REGEXP = /^\S+@\S+\.\S+$/;
					scope.validateEmail = function (value) {
						return !value || EMAIL_REGEXP.test(value);
					};
					scope.setEditorError = function (error) {
						scope.error = error;
					};
					scope.notificationEmailList = addresses;

					scope.save = function () {
						var found = false;
						$scope.policy.actions[stage.id] = $scope.policy.actions[stage.id] || [];
						for (var i = 0 ; i < $scope.policy.actions[stage.id].length ; i++) {
							if ($scope.policy.actions[stage.id][i].actionTypeId == 'notify') {
								if (addresses.length) {
									//if valid addresses, update target
									$scope.policy.actions[stage.id][i].target = addresses.join();
								} else {
									//otherwise dump the action
									$scope.policy.actions[stage.id].splice(i,1);
								}
								found = true;
								break;
							}
						}
						if (!found) {
							$scope.policy.actions[stage.id].push({
								actionTypeId : 'notify',
								target : addresses.join()
							});
						}
						dialog.close(true);
					};
					scope.cancel = function () {
						dialog.close(true);
					};
				}]
			}).open();
		};

		$scope.toggleWarnAction = function(stage) {
		    toggleAction(stage.id, 'warn');
        };
        
        $scope.toggleFailureAction = function(stage) {
            toggleAction(stage.id, 'fail');
        };
        
        $scope.showWarningIcon = function(stage) {
            return showActionIcon(stage.id, 'warn');
        };
        
        $scope.showFailureIcon = function(stage) {
            return showActionIcon(stage.id, 'fail');
        };

        $scope.getEmailList = function(stage) {
            if ($scope.policy.actions[stage.id]) {
                for ( var i = 0 ; i < $scope.policy.actions[stage.id].length ; i++ ) {
                    if ($scope.policy.actions[stage.id][i].actionTypeId == 'notify') {
                        return $scope.policy.actions[stage.id][i].target.split(',').join(', ');
                    } 
                }
            }
        };

        //make sure user is aware they are about to lose changes
		$scope.$on('pageChangeStarted', function (event) {
			if (isDirty()) {
				event.preventDefault();
			}
		});
		

        $scope.cancel = function () {
            if ($scope.policy) {
                if ($scope.policy.isDirty()) {
                    // show dialog
                    $dialog.dialog({
                        backdrop : true,
                        backdropClick : false,
                        backdropFade : true,
                        dialogFade : true,
                        template : '<div class="modal-header"><h3>Unsaved Changes</h3></div>' +
                                   '<div class="modal-body">This policy may contain unsaved changes.  Continuing will discard any unsaved changes.</div>' +
                                    '<div class="modal-footer"><button class="btn" ng-click="cancel()">Cancel</button>' +
                                    '<button class="btn btn-danger" ng-click="discard()">Discard</button></div>',
                        controller : ['$scope', 'dialog', function (scope, dialog) {
                            scope.discard = function () {
                                dialog.close(true);
                                $scope.policy.$revert();
                                $scope.hide();
                            };
                            scope.cancel = function () {
                                dialog.close(true);
                            };
                        }]
                    }).open();
                } else {
                    $scope.hide();
                }
            }
        };
        $scope.savePolicy = function () {
            if ($scope.validate()) {
                $scope.policy.$save().then(function (policy) {
                    $scope.hide();
                }, function (error) {
                    $scope.alerts.push({
                        type : 'error',
                        msg : 'An error occurred while saving the policy. (' + messages.getHttpErrorMessage(error) + ')'
                    });
                });
            }
        };


    $scope.createConditionValidationMessage = function(dataType, constraintName, index) {
      var msg = 'Please enter ';
      switch (dataType){
        case 'Integer':
          msg += 'a whole number';
          break;
        case 'Float':
          msg += 'a decimal number';
          break;
        case 'String':
        default :
          msg += 'a value';
      }
      msg += ' for condition #' + index + ' in constraint "' + constraintName + '"';
      return msg;
    }

    $scope.validate = function() {
            var msg = null;
            $scope.alerts = [];
            if ($scope.policy) {
                var form = $scope[$scope.getFormName()];
                if (form) {
                    var error = form.name.$error;
                    if (error) {
                        if (error.required) {
                            msg = 'Policy name is required.';
                        } else if (error.spaces) {
                            msg = 'Policy name cannot contain leading, trailing or double spaces or tabs.';
                        } else if (error.alphaNumeric) {
                            msg = 'Policy name must be alpha numeric.';
                        } else if (!$scope.policy.constraints || !$scope.policy.constraints.length) {
                            msg = 'You must add at least one constraint to the policy.';
                        } else {
                            $.each($scope.policy.constraints, function(constraintIndex,constraint) {
                                if (!constraint.name) {
                                    msg = 'Enter a valid name for constraint #' + (constraintIndex + 1);
                                } else if(!constraint.operator) {
                                    msg = 'You must select any or all of the conditions for constraint "' + constraint.name + '"';
                                } else if (!constraint.conditions || !constraint.conditions.length) {
                                    msg = 'You must add at least one condition to constraint "' + constraint.name + '"';
                                } else {
                                    $.each(constraint.conditions, function(conditionIndex, condition) {
                                        var conditionType = policyStore.getConditionTypes()[condition.conditionTypeId];
                                        if (!conditionType) {
                                            msg = 'Please select a valid condition type for condition #' +
                                                (conditionIndex + 1) + ' in constraint "' + constraint.name + '"';
                                            return false;
                                        } else if (conditionType.valueTypeId && !condition.value) {
                                            msg = $scope.createConditionValidationMessage(conditionType.valueType.dataType,
                                                constraint.name, conditionIndex + 1);
                                            return false;
                                        }
                                    });
                                }
                                if (msg) {
                                    return false;
                                }
                            });
                        }
                    }
                }
            }

            if (msg) {
                $scope.alerts.push({
                    msg:msg,
                    type:'error'
                });
                return false;
            } else {
                return true;
            }
        };
        $scope.doLoad = function () {
            $scope.error = null;
            $q.all([actionStore.get()]).then(function (results) {
                var actionStages = results[0][1];

                $scope.actionStages = actionStages;
            }, function (errors) {
                $scope.error = angular.isArray(errors) ? errors[0] : errors;
            });
        };
        $scope.alerts = [];
        $scope.doLoad();
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
			'name' : 'All'
		},{
			'value' : 'OR',
			'name' : 'Any'
		}];

		/**
		 * Prevents the event from continuing
		 */
		$scope.stop = function ($event) {
			$event.stopPropagation();
		};

		/**
		 * Returns whether constraint's accordion expanded
		 */
		$scope.isExpanded = function (constraint) {
			return $('#collapse' + constraint.id).hasClass('in');
		};

		/**
		 * Toggle the accordion expansion associated with the constraint
		 */
		$scope.toggleConstraint = function (constraint) {
			if ($scope.isExpanded(constraint)) {
				$('#collapse' + constraint.id).collapse('hide');
			} else {
				$('#collapse' + constraint.id).collapse('show');
			}
		};

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

			//when switching from 1 -> 2 conditions, enable the operator field and force selection
			if ($scope.constraint.conditions.length === 1) {
			    $scope.constraint.operator = null;
			}
			
			$scope.constraint.conditions.push({
				conditionTypeId: conditionType.id,
				operator: conditionType.supportedOperators[0]
			});
		};

		$scope.removeCondition = function (conditionIndex) {
			$scope.constraint.conditions.splice(conditionIndex, 1);
			
			//when switching from 2 -> 1 conditions, disable the operator field and default the selection
			if ($scope.constraint.conditions.length === 1) {
                $scope.constraint.operator = 'OR';
            }
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

	module.directive('inlinePolicyCreator', ['PolicyStore', function (policyStore) {
		return {
			restrict : 'A',
			templateUrl : 'policy-quick-add',
			scope : {},
			link : function (scope) {
				scope.hide = function () {
					scope.policy = null;
				};
				scope.createPolicy = function () {
					return policyStore.get().create();
				};
				scope.getFormName = function() {
				    return 'inlinePolicyForm';
				};
				scope.click = function () {
		            if (!scope.policy) {
		                scope.policy = policyStore.get().create();
		            }
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

	module.directive('inlinePolicyEditor', [function () {
        return {
            restrict : 'A',
            templateUrl : '../assets/components/policy-editor/policy-inline-editor.html?' + clmBuildTimestamp,
			link : function (scope) {
				scope.hide = function () {
					scope.policyEditMap[scope.policy.id] = null;
				};
				scope.getFormName = function() {
				    return 'inlinePolicyForm';
				};
				scope.$on('$destroy', function () {
					if (scope.policy) {
						scope.policy.$revert();
					}
				});
			}
        };
    }]);

	module.directive('ageInDays', function () {
		return {
			restrict : 'A',
			scope : {
				model : '=ngModel'
			},
			template : "<input type='number' style='width:100px;vertical-align:top' ng-model='value' placeholder='{{placeholder}}' required> <select style='width:100px;vertical-align:top' ng-model='modifier' ng-options='timeSpan.value as timeSpan.name for timeSpan in timeSpans' required></select>",
			link : function (scope, element, attrs) {
				function updateModel() {
					if (typeof scope.value === 'number' && scope.modifier) {
						scope.model = '' + (scope.value * scope.modifier);
					} else {
						scope.model = null;
					}
				}
				function updateValue() {
					var numModel = parseInt(scope.model, 10);

					if (isNaN(numModel) || numModel === null || numModel === undefined) {
						scope.value = null;
						scope.modifier = 365;
					} else {
						if (numModel >= 365 && numModel % 365 === 0) {
							scope.modifier = 365;
						} else if (numModel >= 30 && numModel % 30 === 0) {
							scope.modifier = 30;
						} else {
							scope.modifier = 1;
						}
						scope.value = numModel / scope.modifier;
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
