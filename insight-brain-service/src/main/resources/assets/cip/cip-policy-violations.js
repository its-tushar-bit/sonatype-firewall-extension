/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 * third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
/*global angular, $, window, CLM */
(function() {
	'use strict';
	
	function relocateModal(selector) {
    $("body > " + selector).remove();
    $(selector).appendTo("body");
  }

	$.extend(true, window, {
		'Insight' : {
			'PolicyViolations' : function(node, appId, hash) {
			  var timestamp = (new Date()).getTime(), container = $('<div clm-include="\'' + CLM.path + 'cip/cip-policy-violations.html\'"></div>');
				node.empty();
				container.appendTo(node);
				
				angular.module('policyViolations' + timestamp, []).service('PolicyViolationData', function() {
					return {
						hash : hash,
						appId : appId
					};
				});

				angular.bootstrap(container[0], [ 'PolicyViolations', 'policyViolations' + timestamp, 'AngularCommon' ]);
			}
		}
	});
	
	var policyViolationApp = angular.module('PolicyViolations', ['CommonServices', 'Hudson']).service('CurrentPolicyData', function() {
    var currentPolicy = null;
    return {
      get: function(){
        return currentPolicy;
      },
      set: function(policy){
        currentPolicy = policy;
      }
    }
  });
	
	policyViolationApp.controller('PolicyViolationsController', [ 'hudson', '$http', '$scope', '$timeout', 'PolicyViolationData', 'Messages', 'CurrentPolicyData', function(hudson, $http, $scope, $timeout, policyViolationData, messages, currentPolicyData) {
		function errorFn(data, status, headersFn, config) {
		    $scope.alerts.push({
                type : 'error',
                msg : messages.getHttpErrorMessage({ status: status,  data: data })
            });
		}

		function startIfReady() {
			if ($scope.policyAlerts !== undefined && $scope.actionTypes !== undefined) {
				$scope.processedPolicyAlerts = [];
				angular.forEach($scope.policyAlerts, function(policyAlert, policyAlertIndex) {
					var actions = [];
					angular.forEach(policyAlert.actions, function(action, actionIndex) {
						angular.forEach($scope.actionTypes, function(actionType, actionTypeIndex) {
							if (actionType.id === action.actionTypeId && jQuery.inArray(actionType.summary, actions) === -1) {
								actions.push(actionType.summary);
								return false;
							}
						});
					});
					angular.forEach(policyAlert.trigger.componentFacts, function(componentFact, componentFactIndex) {
						if (componentFact.hash === policyViolationData.hash) {
							var tLvl = policyAlert.trigger.threatLevel;
							$scope.processedPolicyAlerts.push({
								id : policyAlert.trigger.policyId,
								name : policyAlert.trigger.policyName,
								threatLevel : tLvl,
								groupId : componentFact.groupId,
								artifactId : componentFact.artifactId,
								version : componentFact.version,
								hash : componentFact.hash,
								color : tLvl > 7 ? 'red' : tLvl > 3 ? 'orange' : tLvl > 1 ? 'yellow' : tLvl > 0 ? 'darkblue' : 'blue',
								constraints : componentFact.constraintFacts,
								actions : actions
							});
						}
					});
				});

				$scope.processedPolicyAlerts.sort(function(policyA, policyB) {
					return policyA.threatLevel > policyB.threatLevel ? -11 : policyA.threatLevel < policyB.threatLevel ? 1 : 0;
				});
			}
    }
		
		$scope.waiveComponent = function(policyAlert) {
		  currentPolicyData.set(policyAlert);
		  $('#componentAddWaiverModal').modal('show');
		};
		
		$scope.viewWaivers = function() {
		  $('#componentExistingWaiverModal').modal('show');
		};
		
		$scope.alerts = [];

		$http.get('policyalerts.json', {
			params : {
				timestamp : new Date().getTime()
			}
		}).success(function(data) {
			$scope.policyAlerts = data.aaData || [];
			startIfReady();
		}).error(errorFn);

		$http.get(CLM.path + 'rest/policy/actionType').success(function(data) {
			$scope.actionTypes = data;
			startIfReady();
		}).error(errorFn);
	} ]);
	
	policyViolationApp.controller('AddWaiverController', [ 'hudson', '$http', '$scope', 'PolicyViolationData', 'Messages', 'CurrentPolicyData', function(hudson, $http, $scope, policyViolationData, messages, currentPolicyData) {
    //after dialog is shown, make sure to apply the angular stuff
    $('#componentAddWaiverModal').on('shown',function(){
      AngularUtils.safeApply($scope, $scope.setupModal);
    });
    
    $scope.setupModal = function() {
      $scope.waiverLoading = true;
      var policy = currentPolicyData.get();
      $scope.waiver = {};
      
      //get the tree of contexts, and flatten down into a list we can display properly
      $http.get(CLM.path + 'rest/policyWaiver/application/' + policyViolationData.appId + '/applicable/context/' + policy.id).success(function(data){
          $scope.waiverLoading = false;
          function processContext(context) {
              if (context) {
                  //only bother checking children if an org, apps dont have children
                  if (context.type === 'organization') {
                          $scope.waiverTargets.push({id:context.id,name:context.name,type:context.type});
                          angular.forEach(context.children, function(childContext, childContextIndex){
                              processContext(childContext); 
                          });
                      } else {
                          //insert the app in position 1, app should always be shown first, and will be defaulted
                          var waiverTarget = {id:policyViolationData.appId,name:context.name,type:context.type};
                          $scope.waiverTargets.splice(0, 0, waiverTarget);
                          //set the app as the default selected value
                          $scope.waiver.selectedTarget = waiverTarget.id + '$$' + waiverTarget.type;
                      }
              }
          }
          
          //if only application present, no need to show the app/org radio buttons
          $scope.waiverSelectOwner = (data.children && data.children.length);
          $scope.waiverTargets = [];
          processContext(data);
          $scope.waiverComment = undefined;
          $scope.waiveAssignError = undefined;
          $scope.waiverPolicyAlert = policy;
      }).error(function(data, status){
          $scope.waiverLoading = false;
          $scope.waiveAssignError = messages.getHttpErrorMessage({ status: status,  data: data });
      });
    };
    
    //move the dialog onto the body in the dom, so the backdrop shows properly
    relocateModal('#componentAddWaiverModal');
    
    //pretty simple, they decline just dump the modal
    $scope.declineWaiveComponent = function() {
        $('#componentAddWaiverModal').modal('hide');
    };
    
    //user really wants to waive the component, so send the request on down
    $scope.acceptWaiveComponent = function() {
            var data = {
                hash : policyViolationData.hash,
                policyId : $scope.waiverPolicyAlert.id,
                comment : $scope.waiverComment
            };
            $scope.waiverSaving = true;
            var parts = $scope.waiver.selectedTarget.split('$$');
            hudson.post(CLM.path + 'rest/policyWaiver/' + parts[1] + '/' + parts[0], data).success(function(responseData){
                $scope.waiverSaving = false;
                $('#componentAddWaiverModal').modal('hide');
        }).error(function(data, status, headersFn, config){
            $scope.waiverSaving = false;
            $scope.waiveAssignError = messages.getHttpErrorMessage({ status: status,  data: data });
        });       
    };    
  }]);
	
	policyViolationApp.controller('ViewWaiverController', ['$scope','$http','$q','PolicyViolationData', function($scope, $http, $q, policyViolationData) {
      function handleHttpError(data, statusCode, headerFn, config) {
          if (statusCode === 0) {
            $scope.appError = 'Failed to contact server';
          } else if (headerFn()['content-type'].indexOf('text/html') === -1) {
            $scope.appError = data;
          } else {
            console.log(data);
            $scope.appError = 'Error: CI server may be unable to reach CLM server';
          }
        }
      
      //after dialog is shown, make sure to apply the angular stuff
      $('#componentExistingWaiverModal').on('shown',function(){
        $scope.setupModal();
      });
      
      $scope.setupModal = function() {
        $scope.waiversLoading = true;
        function processResults(results) {
          $scope.waiversLoading = false;
          $scope.waivers = [];
  
          $.each(results[0].data.waiversByOwner, function(ownerIndex, waiversByOwner) {
            $.each(waiversByOwner.waivers, function(waiverIndex, waiver) {
              waiver.type = waiversByOwner.ownerType;
              waiver.ownerName = waiversByOwner.ownerName;
              $scope.waivers.push(waiver);
            });
          });
        }
        // get the waivers from the server
        var policyWaiverPromise = $http.get(CLM.path + 'rest/policyWaiver/application/' + policyViolationData.appId
                + '/component/' + policyViolationData.hash + '?timestamp=' + new Date().getTime());
  
        $q.all([policyWaiverPromise]).then(function(results) {
          processResults(results);
        }, function() {
          handleHttpError(arguments[0].data, arguments[0].status, arguments[0].headers, arguments[0].config);
        });
      }
      
      //move the dialog onto the body in the dom, so the backdrop shows properly
      relocateModal('#componentExistingWaiverModal');
      
      $scope.close = function() {
        $('#componentExistingWaiverModal').modal('hide');
      };
  
      $scope.remove = function (waiver) {
        $scope.confirmDelete = waiver;
      };
  
      $scope.removeWaiver = function () {
        var waiver = $scope.confirmDelete;
        $scope.confirmDelete = null;
        $scope.appError = null;
  
        $http['delete'](CLM.path + 'rest/policyWaiver/' + waiver.type + '/' + waiver.ownerId + '/' + waiver.id).success(function () {
          $scope.waivers.splice($scope.waivers.indexOf(waiver), 1);
        }).error(handleHttpError);
      };
    } ]);
}());