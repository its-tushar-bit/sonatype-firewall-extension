/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, Option, clmBuildTimestamp, AngularStateUtils, jQuery */
(function() {
  'use strict';
  var module = angular.module('PolicyEditor', [
    'CLMAppLocation', 'CLMLocation', 'ResourceModule', 'ui.router', 'ui.bootstrap', 'AngularCommon',
    'CommonServices', 'Stores', 'ProductFeaturesModule', 'Tags', 'Validators'
  ]);

  module.controller('PolicyEditorController', [
    '$scope', '$rootScope', '$state', '$location', '$modal', '$timeout', 'Dialog', 'Messages', 'PolicyStore', '$q', 'StageTypeStore',
    'ProductFeatures', 'PolicyTagStore', 'CLMAppLocations', '$http',
    function($scope, $rootScope, $state, $location, $modal, $timeout, Dialog, messages, policyStore, $q, StageTypeStore, ProductFeatures, PolicyTagStore, CLMAppLocations, $http) {
      var originalTags;

      function isDirty() {
        if ($scope.policy) {
          return $scope.policy.isDirty() || isAppliedTagsChanged();
        }
        return false;
      }

      function showActionIcon(stageId, action) {
        if ($scope.policy.actions[stageId]) {
          for (var i = 0; i < $scope.policy.actions[stageId].length; i++) {
            if ($scope.policy.actions[stageId][i].actionTypeId === action) {
              return true;
            }
          }
        }
      }

      function toggleAction(stageId, action) {
        var add = true;
        if ($scope.policy.actions[stageId]) {
          for (var i = $scope.policy.actions[stageId].length - 1; i >= 0; i--) {
            switch ($scope.policy.actions[stageId][i].actionTypeId) {
              case 'warn':
                $scope.policy.actions[stageId].splice(i, 1);
                if (action === 'warn') {
                  add = false;
                }
                break;
              case 'fail':
                $scope.policy.actions[stageId].splice(i, 1);
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

      function errorFunction(error) {
        $scope.alerts.push({
          type: 'error',
          msg: 'An error occurred while saving the policy. (' + messages.getHttpErrorMessage(error) + ')'
        });
      }

      function isAppliedTagsChanged() {
        var originalPolicyTagIds = jQuery.map(originalTags, function(tag) { return tag.id; });
        if ($scope.tags) {
          for (var i = 0; i < $scope.tags.length; i++) {
            var tag = $scope.tags[i];
            var tagId = tag.id;
            if ($scope.appliedTagIds.indexOf(tagId) > -1 && originalPolicyTagIds.indexOf(tagId) === -1 ||
              $scope.appliedTagIds.indexOf(tagId) === -1 && originalPolicyTagIds.indexOf(tagId) > -1) {
              return true;
            }
          }
        }
        return false;
      }

      $scope.isApplication = CLMAppLocations.isApplication();
      
      $scope.isPolicyMonitoringLicensed = function() {
        return ProductFeatures.isAvailable('policy-monitoring');
      };

      $scope.removeConstraint = function(constraint) {
        Dialog.open({
          title : 'Remove Constraint',
          body : 'Are you sure you want to delete this constraint?',
          buttons : [{
            name : 'Cancel',
            type: 'cancel'
          }, {
            name : 'Delete',
            type : 'danger',
            click : function() {
              angular.forEach($scope.policy.constraints, function(value, index) {
                if (constraint === value) {
                  $scope.policy.constraints.splice(index, 1);
                  return false;
                }
              });
            }
          }]
        });
      };

      $scope.addConstraint = function() {
        var constraint = {
          id: '' + new Date().getTime(),
          conditions: [],
          operator: 'OR'
        };
        $scope.policy.constraints.push(constraint);
        $timeout(function() {
          $('#collapse' + constraint.id).collapse('show');
        });
      };

      /**
       * Open a modal to allow for editing notification email addresses.
       * @param addresses the new addresses to notify
       * @param actions the existing set of notifications to update
       * @param callback  function that will be called with the new list of actions as a param
       */
      function openNotificationModal(actions, callback) {
        $modal.open({
          backdrop: 'static',
          templateUrl: 'notification',
          controller: 'NotificationModalController',
          scope: angular.extend($scope.$new(), {
            actions: actions,
            roles: $scope.roles
          })
        }).result.then(function() {
          callback(actions);
        });
      }

      $scope.extractAddresses = function(actions) {
        // extract the emails for notification action types
        var addresses = [];
        for (var i = 0; i < actions.length; i++) {
          if (actions[i].actionTypeId === 'notify' && !actions[i].targetType) {
            addresses.push(actions[i].target);
          }
        }
        return addresses;
      };

      $scope.extractRoles = function(actions) {
        if (!$scope.roles) {
          return [];
        }
        var roles = [];
        for (var i = 0; i < actions.length; i++) {
          if (actions[i].actionTypeId === 'notify' && actions[i].targetType === 'role') {
            for (var j = 0; j < $scope.roles.length; j++) {
              if (actions[i].target === $scope.roles[j].roleId) {
                roles.push($scope.roles[j]);
                break;
              }
            }
          }
        }
        return roles;
      };

      /**
       * Extracts the notification targets with type email
       */
      $scope.getEmailList = function(stage) {
        return $scope.extractAddresses($scope.policy.actions[stage.id] || []);
      };

      $scope.getRolesList = function(stage) {
        return $scope.extractRoles($scope.policy.actions[stage.id] || []);
      };

      $scope.getMonitoringEmailList = function() {
        return $scope.extractAddresses($scope.policy.monitorNotifyActions || []);
      };

      $scope.getMonitoringRoleList = function() {
        return $scope.extractRoles($scope.policy.monitorNotifyActions || []);
      };

      $scope.editNotification = function(stage) {
        // local reference for updating/adding notification actions
        var actions = $scope.policy.actions[stage.id] || [];
        openNotificationModal(actions, function(actions){
          // replace policy action state with updated copy
          $scope.policy.actions[stage.id] = actions;
        });
      };

      $scope.editMonitoringNotificationActions = function() {
        var actions = $scope.policy.monitorNotifyActions || [];
        openNotificationModal(actions, function(actions) {
          $scope.policy.monitorNotifyActions = actions;
        });
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

      $scope.clearTags = function() {
        $scope.appliedTagIds = [];
      };

      $scope.isRoot = function () {
        return $state.params.organizationId === 'ROOT_ORGANIZATION_ID';
      };

      //make sure user is aware they are about to lose changes
      $scope.$on('pageChangeStarted', function(event) {
        if (isDirty()) {
          event.preventDefault();
        }
      });

      $scope.cancel = function() {
        if ($scope.policy) {
          if ($scope.policy.isDirty()) {
            // show dialog
            Dialog.open({
              title : 'Unsaved Changes',
              body : 'This policy may contain unsaved changes. Continuing will discard any unsaved changes.',
              buttons : [{
                name : 'Cancel',
                type: 'cancel'
              }, {
                name : 'Discard',
                type : 'danger',
                click : function() {
                  $scope.policy.$revert();
                  $scope.hide();
                }
              }]
            });
          }
          else {
            $scope.hide();
          }
        }
      };

      $scope.savePolicy = function() {
        if ($scope.validate()) {
          $scope.policy.$save().then(function() {
            var promises = [];
            if (!$scope.isApplication) {
              var originalPolicyTagIds = jQuery.map(originalTags, function(tag) { return tag.id; });
              var tagGrepper = function(tag) { return tag.id === tagId; };
              for (var i = 0; i < $scope.tags.length; i++) {
                var tag = $scope.tags[i];
                var tagId = tag.id;
                if ($scope.appliedTagIds.indexOf(tagId) > -1 && originalPolicyTagIds.indexOf(tagId) === -1) {
                  var newPolicyTag = PolicyTagStore.getByPolicyId($scope.policy.id).create();
                  angular.extend(newPolicyTag, { id: tagId });
                  promises.push(newPolicyTag.$save());
                } else if ($scope.appliedTagIds.indexOf(tagId) === -1 && originalPolicyTagIds.indexOf(tagId) > -1) {
                  var oldPolicyTag = jQuery.grep(originalTags, tagGrepper)[0];
                  promises.push(oldPolicyTag.$delete());
                }
              }
              $q.all(promises).then(function() {
                PolicyTagStore.getByPolicyId($scope.policy.id).get().then(function(policyTags) {
                  $rootScope.$broadcast('policySaveComplete', !$scope.isApplication, $scope.policy.id, policyTags);
                  $scope.hide();
                }, errorFunction);
              }, errorFunction);
            }
            else {
              $rootScope.$broadcast('policySaveComplete', !$scope.isApplication, $scope.policy.id, []);
              $scope.hide();
            }
          }, errorFunction);
        }
      };

      $scope.createConditionValidationMessage = function(dataType, constraintName, index) {
        var msg = 'Please enter ';
        switch (dataType) {
          case 'Integer':
            msg += 'a whole number';
            break;
          case 'Float':
            msg += 'a decimal number';
            break;
          case 'String':
          /* falls through */
          default :
            msg += 'a value';
            break;
        }
        msg += ' for condition #' + index + ' in constraint "' + constraintName + '"';
        return msg;
      };

      $scope.validate = function() {
        var msg = null;
        $scope.alerts = [];
        if ($scope.policy) {
          if ($scope.hasPolicyTags && $scope.appliedTagIds.length === 0) {
            msg = 'Must select tags to associate with the policy.';
          }
          var form = $scope[$scope.getFormName()];
          if (form) {
            var error = form.name.$error;
            if (error) {
              if (error.required) {
                msg = 'Policy name is required.';
              }
              else if (error.spaces) {
                msg = 'Policy name cannot contain leading, trailing or double spaces or tabs.';
              }
              else if (error.validNameCharacters) {
                msg = 'Policy name must use valid characters: alphanumeric, "_", ".", "-", or spaces.';
              }
              else if (!$scope.policy.constraints || !$scope.policy.constraints.length) {
                msg = 'You must add at least one constraint to the policy.';
              }
              else {
                $.each($scope.policy.constraints, function(constraintIndex, constraint) {
                  if (!constraint.name) {
                    msg = 'Enter a valid name for constraint #' + (constraintIndex + 1);
                  }
                  else if (!constraint.operator) {
                    msg = 'You must select any or all of the conditions for constraint "' + constraint.name + '"';
                  }
                  else if (!constraint.conditions || !constraint.conditions.length) {
                    msg = 'You must add at least one condition to constraint "' + constraint.name + '"';
                  }
                  else {
                    $.each(constraint.conditions, function(conditionIndex, condition) {
                      var conditionType = policyStore.getConditionTypes()[condition.conditionTypeId];
                      if (!conditionType) {
                        msg = 'Please select a valid condition type for condition #' +
                            (conditionIndex + 1) + ' in constraint "' + constraint.name + '"';
                        return false;
                      }
                      else if (conditionType.valueTypeId && !condition.value) {
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
            msg: msg,
            type: 'error'
          });
          return false;
        }
        else {
          return true;
        }
      };

      $scope.doLoad = function() {
        $scope.error = null;
        originalTags = [];
        $scope.appliedTagIds = [];
        var promises = [
          StageTypeStore.get(),
          $http.get(CLMAppLocations.getRoleMappingUrl())
        ];
        if (!$scope.policy.$new && !$scope.isApplication) {
          promises.push(PolicyTagStore.getByPolicyId($scope.policy.id).get());
        }
        $q.all(promises).then(function(results) {
          var actionStages = results[0];
          $scope.actionStages = actionStages;
          $scope.roles = results[1].data.membersByRole;

          if (results.length === 3) {
            $scope.appliedTagIds = [];
            angular.forEach(results[2], function (appliedTag) {
              originalTags.push(appliedTag);
              $scope.appliedTagIds.push(appliedTag.id);
            });
          }
        }, function(errors) {
          $scope.error = angular.isArray(errors) ? errors[0] : errors;
        });
      };
      $scope.$watchCollection('appliedTagIds', function() {
        $scope.hasPolicyTags = $scope.appliedTagIds.length > 0;
      });
      $scope.alerts = [];
      $scope.doLoad();
    }
  ]);

  module.controller('NotificationModalController', [
    '$scope', '$timeout', 'validationHelper', function($scope, $timeout, validationHelper) {
      var EMAIL_REGEXP = /^\S+@\S+\.\S+$/;
      $scope.validateEmail = function(value) {
        return {
          email: !value || EMAIL_REGEXP.test(value)
        };
      };

      var emails = $scope.extractAddresses($scope.actions);
      var roles = $scope.extractRoles($scope.actions);
      $scope.notifications = {
        emails: emails,
        roles: roles
      };

      function setAvailableRoles() {
        var availableRoles = angular.copy($scope.roles);
        for (var i = availableRoles.length - 1; i >= 0; i--) {
          for (var j = 0; j < $scope.notifications.roles.length; j++) {
            if (availableRoles[i].roleId === $scope.notifications.roles[j].roleId) {
              availableRoles.splice(i, 1);
              break;
            }
          }
        }
        $scope.availableRoles = availableRoles;
      }

      function rerunValidators() {
        validationHelper.revalidateChildren(angular.element('#notification-editor'));
      }

      function setEmailFormPristine() {
        var emailForm = angular.element('form[name="emailForm"]');
        if (emailForm && emailForm.length > 0) {
          emailForm.controller('form').$setPristine();
        }
      }

      $scope.addEmail = function() {
        $scope.notifications.emails.push($scope.entries.email);
        $scope.entries.email = '';
        setEmailFormPristine();
      };

      $scope.addRole = function() {
        $scope.notifications.roles.push($scope.entries.role);
        $scope.entries.role = null;
        setAvailableRoles();
        setEmailFormPristine();
      };

      $scope.remove = function(item, group) {
        for (var i = 0; i < group.length; i++) {
          if (item === group[i]) {
            group.splice(i, 1);
          }
        }
        setAvailableRoles();
        rerunValidators();
        setEmailFormPristine();
      };

      $scope.save = function() {
        // remove existing notify actions since all entries will be added when saved
        // loop in reverse to avoid missing items when splice reindexes the array,
        // causing the counter to be off if done in a normal for loop
        var i = $scope.actions.length;
        while (i--) {
          if ($scope.actions[i].actionTypeId === 'notify') {
            $scope.actions.splice(i,1);
          }
        }
        // add notify action for each address
        for (var j = 0; j < $scope.notifications.emails.length; j++) {
          $scope.actions.push({ actionTypeId: 'notify', target: $scope.notifications.emails[j] });
        }

        for (var k = 0; k < $scope.notifications.roles.length; k++) {
          $scope.actions.push({ actionTypeId: 'notify', target: $scope.notifications.roles[k].roleId, targetType: 'role' });
        }

        $scope.$close($scope.actions);
      };

      $scope.entries = {
        email: '',
        role: null
      };

      $scope.$watch('availableRoles', function () {
        // fixes only one-character visible on IE9
        $timeout(function () {
          var element = angular.element('#role');
          element.css('width', element.css('width'));
        }, 1);
      });

      setAvailableRoles();
    }
  ]);

  module.controller('ConstraintEditorController', [
    '$scope', '$timeout', 'ConstraintStore', function($scope, $timeout, constraints) {
      function isDirty() {
        if ($scope.originalConstraint) {
          if ($scope.originalConstraint.name !== $scope.constraint.name ||
              $scope.originalConstraint.operator !== $scope.constraint.operator ||
              $scope.originalConstraint.conditions.length !== $scope.constraint.conditions.length) {
            return true;
          }
          for (var i = 0; i < $scope.originalConstraint.conditions.length; i++) {
            if ($scope.originalConstraint.conditions[i].value !== $scope.constraint.conditions[i].value ||
                $scope.originalConstraint.conditions[i].operator !== $scope.constraint.conditions[i].operator ||
                $scope.originalConstraint.conditions[i].conditionTypeId !==
                  $scope.constraint.conditions[i].conditionTypeId) {
              return true;
            }
          }
        }
        return false;
      }

      $scope.constraintConditionChoices = [
        {
          'value': 'AND',
          'name': 'All'
        },
        {
          'value': 'OR',
          'name': 'Any'
        }
      ];

      /**
       * Prevents the event from continuing
       */
      $scope.stop = function($event) {
        $event.stopPropagation();
      };

      /**
       * Returns whether constraint's accordion expanded
       */
      $scope.isExpanded = function(constraint) {
        return $('#collapse' + constraint.id).hasClass('in');
      };

      /**
       * Toggle the accordion expansion associated with the constraint
       */
      $scope.toggleConstraint = function(constraint) {
        if ($scope.isExpanded(constraint)) {
          $('#collapse' + constraint.id).collapse('hide');
        }
        else {
          $('#collapse' + constraint.id).collapse('show');
        }
      };

      //make sure user is aware they are about to lose changes
      $scope.$on('pageChangeStarted', function(event) {
        if (isDirty()) {
          event.preventDefault();
        }
      });

      // Remove original constraint of successful save to prevent dirty check
      $scope.$on('policySaveComplete', function() {
        delete $scope.originalConstraint;
      });

      $scope.conditionTypeChanged = function(condition) {
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
            if ($scope.conditionTypes[condition.conditionTypeId].valueType.availableValues &&
                $scope.conditionTypes[condition.conditionTypeId].valueType.availableValues.length > 0) {
              condition.value = $scope.conditionTypes[condition.conditionTypeId].valueType.availableValues[0].id;
            }
            break;
        }
      };
      $scope.$watch('constraint', function(constraint) {
        if (constraint) {
          var fn = function() {
            if ($scope.conditionTypes) {
              if ($scope.constraint.conditions.length === 0) {
                $scope.addCondition();
              }
              $('#constraintName').focus();
              $scope.originalConstraint = angular.copy(constraint);
            }
            else {
              $timeout(fn, 100);
            }
          };
          fn();
        }
        else {
          $scope.originalConstraint = null;
        }
      });

      $scope.addCondition = function() {
        var conditionType = $scope.conditionTypes.AgeInDays;

        //when switching from 1 -> 2 conditions, enable the operator field and force selection
        if ($scope.constraint.conditions.length === 1) {
          $scope.constraint.operator = null;
        }

        $scope.constraint.conditions.push({
          conditionTypeId: conditionType.id,
          operator: conditionType.supportedOperators[0],
          value: null
        });
      };

      $scope.removeCondition = function(conditionIndex) {
        $scope.constraint.conditions.splice(conditionIndex, 1);

        //when switching from 2 -> 1 conditions, disable the operator field and default the selection
        if ($scope.constraint.conditions.length === 1) {
          $scope.constraint.operator = 'OR';
        }
      };

      constraints.get().then(function(results) {
        var typeValues = {};
        $scope.conditionTypes = {};
        angular.forEach(results[1], function(typeValue) {
          typeValues[typeValue.id] = typeValue;
        });
        angular.forEach(results[0], function(type) {
          var typeValue = type.valueTypeId ? typeValues[type.valueTypeId] : null;
          type.valueType = typeValue;
          $scope.conditionTypes[type.id] = type;
        });
      }, function() {
        // TODO handle this error
      });
    }
  ]);

  module.directive('ieOptions', [
    '$parse', function($parse) {
      return {
        restrict: 'A',
        require: 'ngModel',
        link: function(scope, elem, attr) {
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
    }
  ]);

  module.directive('inlinePolicyCreator', [
    'PolicyStore', '$state', function(policyStore, $state) {
      return {
        restrict: 'A',
        templateUrl: 'policy-quick-add',
        scope: {
          tags: '=',
          ownerName: '='
        },
        link: function(scope) {
          scope.hide = function() {
            scope.policy = null;
            AngularStateUtils.toParentStateIfNewItem(scope);
          };
          scope.getFormName = function() {
            return 'inlinePolicyForm';
          };
          scope.click = function() {
            if (!scope.policy) {
              policyStore.get().then(function(store) {
                scope.policy = store.create();
                AngularStateUtils.toNewItemState(scope);
              });
            }
          };
          
          scope.$state = $state;
          AngularStateUtils.fnOnNewItemState(scope, scope.click);
        }
      };
    }
  ]);

  module.directive('inlineConstraintEditor', function() {
    return {
      restrict: 'A',
      scope: {
        constraint: '=inlineConstraintEditor'
      },
      controller: 'ConstraintEditorController'
    };
  });

  module.directive('inlinePolicyEditor', [
    function() {
      return {
        restrict: 'A',
        templateUrl: '../assets/components/policy-editor/policy-inline-editor.html?' + clmBuildTimestamp,
        link: function(scope) {
          scope.hide = function() {
            scope.policyEditMap[scope.policy.id] = null;
          };
          scope.getFormName = function() {
            return 'inlinePolicyForm';
          };
          scope.$on('$destroy', function() {
            if (scope.policy) {
              scope.policy.$revert();
            }
          });
        }
      };
    }
  ]);

  module.directive('ageInDays', function() {
    return {
      restrict: 'A',
      scope: {
        model: '=ngModel'
      },
      template: '<input type="number" style="width:100px;vertical-align:top" ng-model="value" placeholder="{{placeholder}}" required> <select style="width:100px;vertical-align:top" ng-model="modifier" ng-options="timeSpan.value as timeSpan.name for timeSpan in timeSpans" required></select>',
      link: function(scope) {
        function updateModel() {
          if (typeof scope.value === 'number' && scope.modifier) {
            scope.model = '' + (scope.value * scope.modifier);
          }
          else {
            // NOTE: Keep this in sync with all the places where the AgeInDays condition is initialized for a policy constraint or dirty checks will fail
            scope.model = null;
          }
        }

        function updateValue() {
          var numModel = parseInt(scope.model, 10);

          if (isNaN(numModel) || numModel === null || numModel === undefined) {
            scope.value = null;
            scope.modifier = 365;
          }
          else {
            if (numModel >= 365 && numModel % 365 === 0) {
              scope.modifier = 365;
            }
            else if (numModel >= 30 && numModel % 30 === 0) {
              scope.modifier = 30;
            }
            else {
              scope.modifier = 1;
            }
            scope.value = numModel / scope.modifier;
          }
        }

        scope.timeSpans = [
          {'value': 1, 'name': 'Days'},
          {'value': 30, 'name': 'Months'},
          {'value': 365, 'name': 'Years'}
        ];
        // TODO Some work here when editing an existing condition to ensure we don't touch the initial state
        scope.$watch('model', updateValue);
        scope.$watch('value', updateModel);
        scope.$watch('modifier', updateModel);
      }
    };
  });
}());
