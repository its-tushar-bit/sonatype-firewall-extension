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
			'LabelEditor' : function (node, applicationId, hash) {
				var timestamp = (new Date()).getTime(),
					container = $('<div clm-include="\'' + CLM.path + 'cip/cip-label-editor.html\'"></div>');
				node.empty();
				container.appendTo(node);

				angular.module('labelEditor' + timestamp, []).service('ComponentLabelEditorGAV', function () {
					return {
						applicationId : applicationId,
						hash : hash
					};
				});
				angular.bootstrap(container[0], ['ComponentLabelEditor', 'labelEditor' + timestamp, 'AngularCommon']);
			}
		}
	});

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
	
	labelsApp.controller('LabelAddController', ['$scope', 'CurrentLabelData', 'ComponentLabelEditorGAV', 'hudson', 'Messages', function($scope, currentLabelData, componentLabelEditorGAV, hudson, messages){
	  $scope.decline = function() {
      $('#labelAssignScopeModal').modal('hide');
    };
    
    $scope.accept = function() {
      $scope.labelSaving = true;
      $scope.labelAddError = null;
      var parts = $scope.selectedOwner.split('$$');
      hudson.post(CLM.path + 'rest/label/component/' + parts[1] + '/' + parts[0] + '/' + componentLabelEditorGAV.hash, currentLabelData.get()).success(function(responseData){
        $scope.labelSaving = false;
        $('#labelAssignScopeModal').modal('hide');
      }).error(function(data, status, headersFn, config){
        $scope.labelSaving = false;
        $scope.labelAddError = messages.getHttpErrorMessage({ status: status,  data: data });
      });
    };
    
    $('#labelAssignScopeModal').on('shown',function(){
        $scope.$apply(function(){
          var label = currentLabelData.get();
          $scope.selectedOwner = componentLabelEditorGAV.applicationId + '$$application';
          //if label is owned by an app, simply do the add with no further input from user
          $scope.labelOwners = [{
            ownerId: componentLabelEditorGAV.applicationId,
            ownerName: componentLabelEditorGAV.applicationId,
            ownerType: 'application'
          },{
            ownerId: label.ownerId,
            ownerName: label.ownerName,
            ownerType: label.ownerType
          }];  
        });
    });
    
    $("body > #labelAssignScopeModal").remove();
    $("#labelAssignScopeModal").appendTo("body");
	}]);
	
	labelsApp.controller('LabelRemoveController', ['$scope', '$http', 'CurrentLabelData', 'ComponentLabelEditorGAV', 'Messages', function($scope, $http, currentLabelData, componentLabelEditorGAV, messages){
    $scope.decline = function() {
      $('#labelRemoveModal').modal('hide');
    };
    
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
    
    $("body > #labelRemoveModal").remove();
    $("#labelRemoveModal").appendTo("body");
	}]);

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