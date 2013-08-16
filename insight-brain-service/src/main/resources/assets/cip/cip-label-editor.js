/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, CLM, setTimeout */
(function () {
    'use strict';
	$.extend(true, window, {
		'Insight' : {
			'LabelEditor' : function (node, applicationId, hash, gav) {
				var timestamp = (new Date()).getTime(),
					container = $('<div clm-include="\'' + CLM.path + 'cip/cip-label-editor.html\'"></div>');
				node.empty();
				container.appendTo(node);
				gav = gav ? gav : {};

				angular.module('labelEditor' + timestamp, []).service('ComponentLabelEditorGAV', function () {
					return {
						applicationId : applicationId,
						hash : hash,
						groupId : gav.groupId,
						artifactId : gav.artifactId,
						version : gav.version
					};
				});
				angular.bootstrap(container[0], ['ComponentLabelEditor', 'labelEditor' + timestamp, 'AngularCommon']);
			}
		}
	});
	
	function relocateModal(selector) {
	  $("body > " + selector).remove();
    $(selector).appendTo("body");
	}

	//create the app, and a service we can use to transfer data between our controllers
	var labelsApp = angular.module('ComponentLabelEditor', ['CommonServices', 'Hudson']).service('CurrentLabelData', function() {
	  var currentLabel = null;
	  var currentError = null;
	  return {
	    get: function(){
	      return currentLabel;
	    },
	    set: function(label){
	      currentLabel = label;
	    },
	    getError: function(){
	      return currentError;
	    },
	    setError: function(error){
	      currentError = error;
	    }
	  }
	});
	
	//the add controller, controlling the add modal 
	labelsApp.controller('LabelAddController', ['$scope', 'CurrentLabelData', 'ComponentLabelEditorGAV', 'hudson', 'Messages', '$http', function($scope, currentLabelData, componentLabelEditorGAV, hudson, messages, $http){
	  $scope.groupId = componentLabelEditorGAV.groupId;
	  $scope.artifactId = componentLabelEditorGAV.artifactId;
	  $scope.version = componentLabelEditorGAV.version;
	  
	  //decline to add, just dump the modal and move on
	  $scope.decline = function() {
      $('#labelAssignScopeModal').modal('hide');
    };
    
    //they accept, update the server
    $scope.accept = function() {
      $scope.labelSaving = true;
      $scope.labelAddError = null;
      var parts = $scope.label.selectedOwner.split('$$');
      hudson.post(CLM.path + 'rest/label/component/' + parts[1] + '/' + parts[0] + '/' + componentLabelEditorGAV.hash, currentLabelData.get()).success(function(responseData){
        $scope.labelSaving = false;
        $('#labelAssignScopeModal').modal('hide');
      }).error(function(data, status, headersFn, config){
        $scope.labelSaving = false;
        $scope.labelAddError = messages.getHttpErrorMessage({ status: status,  data: data });
      });
    };
    
    //after dialog is shown, make sure to apply the angular stuff
    $('#labelAssignScopeModal').on('shown',function(){
      $scope.labelLoading = true;
      $scope.labelAddError = null;
      var label = currentLabelData.get();
      $scope.label = {
        selectedOwner: componentLabelEditorGAV.applicationId + '$$application'
      };
      
      $scope.labelOwners = [];
      
      //purposefully not wrapping the changes in $apply so that i can check if its already running first
      $http.get(CLM.path + 'rest/label/' + label.ownerType + '/' + label.ownerId + '/applicable/context/' + label.id).success(function(data){
        $scope.labelLoading = false;
        
        function processItem(item) {
          if (item.type === 'application' && item.id === componentLabelEditorGAV.applicationId) {
            $scope.labelOwners.splice(0,0, item);    
          } else if (item.type === 'organization') {
            $scope.labelOwners.push(item);
            
            angular.forEach(item.children, function(child, childIndex){
              processItem(child);
            });
          }
        }
        
        processItem(data)
      }).error(function(data, status){
        $scope.labelLoading = false;
        $scope.labelAddError = messages.getHttpErrorMessage({ status: status,  data: data });
      });
      
      if(!$scope.$$phase) {
        $scope.$apply();
      }
    });
    
    //move the dialog onto the body in the dom, so the backdrop shows properly
    relocateModal('#labelAssignScopeModal');
	}]);
	
	//the remove controller, controlling the remove modal
	labelsApp.controller('LabelRemoveController', ['$scope', '$http', 'CurrentLabelData', 'ComponentLabelEditorGAV', 'Messages', function($scope, $http, currentLabelData, componentLabelEditorGAV, messages){
    //decline to remove, just dump the dialog
	  $scope.decline = function() {
      $('#labelRemoveModal').modal('hide');
    };
    
    //accept, send delete request to server
    $scope.accept = function() {
      $scope.labelDeleting = true;
      $scope.labelRemoveError = null;
      
      var label = currentLabelData.get();
      
      $http['delete'](CLM.path + 'rest/label/component/' + label.ownerType + '/' + label.ownerId + '/' + componentLabelEditorGAV.hash + '/' + label.id).success(function(responseData){
        $scope.labelDeleting = false;
        $('#labelRemoveModal').modal('hide');
      }).error(function(data, status, headersFn, config){
        $scope.labelDeleting = false;
        $scope.labelRemoveError = messages.getHttpErrorMessage({ status: status,  data: data });
      });
    };
    
    $('#labelAssignScopeModal').on('shown',function(){
      $scope.labelRemoveError = null;
      
      //purposefully not wrapping the above changes in $apply so that i can check if its already running first
      if(!$scope.$$phase) {
        $scope.$apply();
      }
    });
    
    //move the dialog onto the body in the dom, so the backdrop shows properly
    relocateModal('#labelRemoveModal');
	}]);

	//main label controller handling the main view, and launching the other modals when necessary
	labelsApp.controller('LabelsController', ['$http', '$scope', 'CurrentLabelData', 'ComponentLabelEditorGAV', 'hudson', 'Messages', function ($http, $scope, currentLabelData, componentLabelEditorGAV, hudson, messages) {
		function errorFn(data, status, headersFn, config) {
			$scope.alerts.push({
				type : 'error',
				msg : messages.getHttpErrorMessage({ status: status,  data: data })
			});
		}
		function flattenLabelList(data) {
		  var list = [];
      angular.forEach(data.labelsByOwner,function(labelOwner,labelOwnerIndex){
        angular.forEach(labelOwner.labels,function(label, labelIndex){
          label.ownerId = labelOwner.ownerId;
          label.ownerType = labelOwner.ownerType;
          label.ownerName = labelOwner.ownerName;
          list.push(label);
        });
      });
      return list;
		}

		$scope.reloadLabels = function () {
			$http.get(CLM.path + 'rest/label/component/application/' + componentLabelEditorGAV.applicationId + '/' + componentLabelEditorGAV.hash, { params : { timestamp : new Date().getTime() } }).success(function (data) {
				$scope.itemLabels = flattenLabelList(data);
			}).error(errorFn);
		};

		$scope.reloadAppLabels = function () {
			$http.get(CLM.path + 'rest/label/application/' + componentLabelEditorGAV.applicationId + '/applicable', { params : { timestamp : new Date().getTime() } }).success(function (data) {
			  $scope.availableLabels = flattenLabelList(data);
			}).error(errorFn);
		};

		$scope.removeLabel = function (label) {
		  currentLabelData.set(label);
		  $('#labelRemoveModal').modal('show');
		};

		//for labels owned by the app, we simply do the add here, as there is no need to view the dialog to select the owner, app is the only option
		$scope.addLabel = function (label) { 
		  if (label.ownerType === 'application') {
		    hudson.post(CLM.path + 'rest/label/component/application/' + componentLabelEditorGAV.applicationId + '/' + componentLabelEditorGAV.hash, label).success(function(responseData){
		      $scope.reloadLabels();
		      $scope.reloadAppLabels();
	      }).error(errorFn);  
		  } else {
		    currentLabelData.set(label);
	      $('#labelAssignScopeModal').modal('show');  
		  }
		};

    $scope.isWhite = function (label) {
      return label.color === "green" || label.color === "black" || label.color === "orange" || label.color === "red" || label.color === "blue";
    };
    
    $scope.isApplied = function (label) {
      var duplicate = false;
      angular.forEach($scope.itemLabels, function (candidate, key) {
        duplicate = duplicate || (candidate.label === label.label);
        return !duplicate;
      });
      return !duplicate;
    };

    $scope.alerts = [];
    $scope.reloadLabels(); // do initial load
    $scope.reloadAppLabels(); // do initial load
    
    //when either of the modals go away, refresh the content
    $('#labelAssignScopeModal').on('hide',function(){
      $scope.reloadLabels();
      $scope.reloadAppLabels();
    });
    $('#labelRemoveModal').on('hide',function(){
      $scope.reloadLabels();
      $scope.reloadAppLabels();
    });
	}]);
  
  /**
   * Enables tipsy tooltip on an element(with fixed parameters)
   */
  labelsApp.directive('tip', function () {
    return function (scope, element, attrs) {
      $(element).tipsy({fade: true, gravity: $.fn.tipsy.autoWE, html: true, opacity: 1.0, delayOut: 0});
    };
  });

	labelsApp.directive('spinner', function () {
		var properties = ['-ms-transform', '-webkit-transform', '-moz-transform', 'transform'];

		function setElement(element, value) {
			angular.forEach(properties, function (prop, key) {
				element.css(prop, value);
			});
			return element;
		}

		return function (scope, element, attrs) {
			element.bind('click', function (e) {
				setElement(element, '').prop('rotate', null).animate({ rotate : '+360'}, {
					step : function (now, fx) {
						now = now % 360;
						setElement(element, 'rotate(' + now + 'deg)');
					}
				});
			});
		};
	});
}());