/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 * third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
/*global angular, $, CLM */
(function() {
  'use strict';
  function PolicyViolationTab(node, options) {
    this.node = node;
    this.options = options;
  }

  function createPlugin() {
    PolicyViolationTab.prototype = new Insight.InformationPanelPlugin({ priority: 32 });
    PolicyViolationTab.prototype.isVisible = function() {
      return !((freemium && !this.options.sampleData) || this.gav.matchState === 'unknown') &&
          Brain.hasFeature('policy-violations');
    };
    PolicyViolationTab.prototype.create = function() {
      var timestamp = (new Date()).getTime(),
          container = $('<div clm-include="\'' + CLM.path + 'cip/cip-policy-violations.html\'"></div>'),
          me = this;
      me.node.empty();
      container.appendTo(me.node);
      angular.module('policyViolations' + timestamp, []).service('PolicyViolationData', function() {
        return {
          hash: me.gav.hash,
          groupId : me.gav.groupId,
          artifactId : me.gav.artifactId,
          version : me.gav.version,
          appId: applicationId
        };
      });
      angular.bootstrap(container[0], ['PolicyViolations', 'policyViolations' + timestamp, 'AngularCommon']);
    };
    PolicyViolationTab.prototype.destroy = function() {
      this.node.empty();
    };
    PolicyViolationTab.prototype.getTitle = function() {
      return 'Policy';
    };
    return PolicyViolationTab;
  }

  (function() {
    var policyViolationApp = angular.module('PolicyViolations',
            ['CommonServices', 'HttpInterceptors', 'UnauthenticatedResponseHttpInterceptor']);

    policyViolationApp.controller('PolicyViolationsController', [
      '$http', '$scope', '$q', '$modal', 'PolicyViolationData', 'Messages',
      function($http, $scope, $q, $modal, policyViolationData, messages) {
        function errorFn(data, status, headersFn, config) {
          $scope.alerts.push({
            type: 'error',
            msg: messages.getHttpErrorMessage(arguments)
          });
        }
        

        // because we need to support reports generated from older servers, we must tweak the data so that it
        // fits what the html expects
        function processConstraint(constraint) {
          var processedConstraint = {
            constraintId: constraint.constraintId,
            constraintName: constraint.constraintName,
            constraintOperator: constraint.operatorName,
            conditions: []
          };

          angular.forEach(constraint.conditionFacts, function(conditionFact) {
            processedConstraint.conditions.push({
              conditionType: conditionFact.conditionTypeId,
              conditionSummary: conditionFact.summary,
              conditionReason: conditionFact.reason
            });
          });

          return processedConstraint;
        }
        
        function addIfNotFound(actions, action) {
          if (!action) {
            return;
          }
          for ( var i = 0 ; i < actions.length ; i++ ) {
            if (actions[i].actionSummary === action.actionSummary) {
              //found a match, bail out
              return;
            }
          }
          actions.push(action);
        }
        
        function processAction(action, actionTypes) {
          var processedAction = null;
          $.each(actionTypes, function(){
            if (this.id === action.actionTypeId) {
              processedAction = {
                actionSummary: this.summary
              };
              return false;
            }
          });
          return processedAction;
        }
        
        function buildPolicyAlert(data) {
          return angular.extend(data, {
            color: data.threatLevel > 7 ? 'red' : data.threatLevel > 3 ? 'orange' : data.threatLevel > 1 ? 'yellow'
                    : data.threatLevel > 0 ? 'darkblue' : 'blue'
          });
        }
        
        function sortPolicyAlerts() {
          $scope.processedPolicyAlerts.sort(function(a, b) {
            return b.threatLevel - a.threatLevel;
          });
        }
        
        function handleError(error) {
          $scope.alerts.push({
            type: 'error',
            msg: messages.getHttpErrorMessage(error)
          });
        }
        
        function doLegacyLoad() {
          $q.all([$http.get(CLM.path + 'rest/policy/actionType'), $http.get('policyalerts.json')]).then(function (result) {
            var actionTypes = result[0].data;
            var policyAlerts = result[1].data.aaData || [];

            $scope.processedPolicyAlerts = [];
            angular.forEach(policyAlerts, function(policyAlert, policyAlertIndex) {
              var processedActions = [];
              angular.forEach(policyAlert.actions, function(action, actionIndex) {
                addIfNotFound(processedActions, processAction(action, actionTypes));
              });
              
              angular.forEach(policyAlert.trigger.componentFacts, function(componentFact, componentFactIndex) {
                if (componentFact.hash === policyViolationData.hash) {
                  var processedConstraints = [];
                  angular.forEach(componentFact.constraintFacts, function(constraintFact){
                    processedConstraints.push(processConstraint(constraintFact));
                  });
                  $scope.processedPolicyAlerts.push(
                    buildPolicyAlert({
                      id: policyAlert.trigger.policyId,
                      name: policyAlert.trigger.policyName,
                      threatLevel: policyAlert.trigger.threatLevel,
                      groupId: componentFact.groupId,
                      artifactId: componentFact.artifactId,
                      version: componentFact.version,
                      hash: componentFact.hash,
                      constraints: processedConstraints,
                      actions: processedActions
                    }));
                }
              });
            });
            sortPolicyAlerts();
          }, handleError);
        }

        function doLoad() {
          $http.get('policythreats.json').then(function(result) {
            // if version isn't set we are dealing with old data, so revert to old request and massage data as
            // necessary
            if (!result.data.version) {
              doLegacyLoad();
            } else {
              var policyThreats = result.data.aaData || [];
              $scope.processedPolicyAlerts = [];

              angular.forEach(policyThreats, function(policyThreat, policyThreatIndex) {
                if (policyThreat.hash === policyViolationData.hash) {
                  angular.forEach(policyThreat.activeViolations, function(activeViolation, activeViolationIndex) {
                    var actions = [];
                    angular.forEach(activeViolation.actions, function(action){
                      addIfNotFound(actions, action);
                    });
                    $scope.processedPolicyAlerts.push(buildPolicyAlert({
                      id: activeViolation.policyId,
                      name: activeViolation.policyName,
                      threatLevel: activeViolation.policyThreatLevel,
                      groupId: policyThreat.groupId,
                      artifactId: policyThreat.artifactId,
                      version: policyThreat.version,
                      hash: policyThreat.hash,
                      constraints: activeViolation.constraints,
                      actions: actions
                    }));
                  });
                }
              });

              sortPolicyAlerts();
            }
          }, handleError);
        }

        $scope.waiveComponent = function(policyAlert) {
          $modal.open({
            templateUrl : 'add-waiver-modal-tmpl',
            controller : 'AddWaiverController',
            backdrop : 'static',
            keyboard : false,
            resolve : {
              policy : function () { return policyAlert; }
            }
          });
        };
        $scope.viewWaivers = function() {
          $modal.open({
            templateUrl : 'view-waivers-modal-tmpl',
            controller : 'ViewWaiverController',
            backdrop : 'static',
            keyboard : false
          });
        };
        $scope.alerts = [];

        doLoad();
      }
    ]);

    policyViolationApp.controller('AddWaiverController', [
      '$http', '$scope', 'PolicyViolationData', 'Messages', 'policy',
      function($http, $scope, policyViolationData, messages, policy) {
        function doLoad() {
          $scope.policy = policy;
          $scope.component = policyViolationData;
          $scope.waiverLoading = true;

          //get the tree of contexts, and flatten down into a list we can display properly
          $http.get(CLM.path + 'rest/policyWaiver/application/' + policyViolationData.appId + '/applicable/context/' +
                  $scope.policy.id).success(function(data) {
            function processContext(context) {
              if (context.children) {
                angular.forEach(context.children, function (child) {
                  processContext(child);
                });
              }

              $scope.waiverTargets.push({
                id : context.id,
                name : context.name,
                type : context.type
              });
            }

            //if only application present, no need to show the app/org radio buttons
            $scope.waiverSelectOwner = (data.children && data.children.length);
            $scope.waiverTargets = [];
            $scope.waiverLoading = false;
            processContext(data);

            $scope.waiver = {
              hash : policyViolationData.hash,
              policyId : $scope.policy.id,
              ownerId : $scope.waiverTargets[0].id,
              comment : ''
            };
            $scope.owner = {
              type : $scope.waiverTargets[0].type
            };
          }).error(function(data, status) {
            $scope.waiverLoading = false;
            $scope.waiveAssignError = messages.getHttpErrorMessage(arguments);
          });
        }
        doLoad();

        //user really wants to waive the component, so send the request on down
        $scope.acceptWaiveComponent = function() {
          $scope.waiverSaving = true;
          $scope.waiveAssignError = null;

          $http.post(CLM.path + 'rest/policyWaiver/' + $scope.owner.type + '/' + $scope.waiver.ownerId,
                  $scope.waiver).success(function(responseData) {
            $scope.waiverSaving = false;
            $scope.$close();
          }).error(function(data, status, headersFn, config) {
            $scope.waiverSaving = false;
            $scope.waiveAssignError = messages.getHttpErrorMessage(arguments);
          });
        };
      }
    ]);

    policyViolationApp.controller('ViewWaiverController', [
      '$scope', '$http', 'PolicyViolationData', 'Messages',
      function($scope, $http, policyViolationData, messages) {
        function handleHttpError(data, status, headerFn, config) {
          $scope.appError = messages.getHttpErrorMessage(arguments);
        }

        function doLoad() {
          $scope.waiversLoading = true;
          // get the waivers from the server
          $http.get(CLM.path + 'rest/policyWaiver/application/' + policyViolationData.appId + '/component/' +
              policyViolationData.hash).success(function(data) {
            $scope.waiversLoading = false;
            $scope.waivers = [];
            angular.forEach(data.waiversByOwner, function(waiversByOwner, ownerIndex) {
              angular.forEach(waiversByOwner.waivers, function(waiver, waiverIndex) {
                waiver.type = waiversByOwner.ownerType;
                waiver.ownerName = waiversByOwner.ownerName;
                $scope.waivers.push(waiver);
              });
            });
          }).error(handleHttpError);
        }

        doLoad();

        $scope.remove = function(waiver) {
          $scope.confirmDelete = waiver;
          $scope.appError = null;
        };

        $scope.removeWaiver = function() {
          var waiver = $scope.confirmDelete;
          $scope.confirmDelete = null;
          $scope.appError = null;
          $http['delete'](CLM.path + 'rest/policyWaiver/' + waiver.type + '/' + waiver.ownerId + '/' +
                  waiver.id).success(function() {
            $scope.waivers.splice($scope.waivers.indexOf(waiver), 1);
          }).error(handleHttpError);
        };
      }
    ]);
  }());

  CLM.loadPlugin(createPlugin, 'Policy');
}());