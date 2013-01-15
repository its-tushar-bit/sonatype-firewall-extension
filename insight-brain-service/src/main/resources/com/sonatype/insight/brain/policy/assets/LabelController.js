/*global insightApp, angular, $ */
(function () {
	'use strict';

	angular.module('Labels', []).controller('LabelController', ['$scope', '$http', function ($scope, $http) {
		// TODO Failure?
		$http.get(insightApp.getLabelsUrl()).success(function (data) {
			$scope.labels = data;
		});

		$scope.colors = [null, 'white', 'grey', 'black', 'green', 'yellow', 'orange', 'red', 'blue'];

		$scope.editLabel = function (label) {
		    if (label) {
		        $scope.selectedLabel = angular.extend({id : null, applicationId : null, label : null, labelLowercase : null, color : null}, label);
		    } else {
		        $scope.selectedLabel = {id : null, applicationId : null, label : null, labelLowercase : null, color : null};
		    }
		    $scope.editSave = true;
		    // show modal?
		    $('#labelEditModal').modal('show');
		};

		$scope.setColor = function (color) {
		    $scope.selectedLabel.color = color;
		};

		$scope.canSaveLabelEditor = function (formValid) {
		    return !(formValid && $scope.editSave);
		};

		$scope.saveLabelClick = function () {
		    var label = $scope.selectedLabel;
		    $scope.editSave = false;
		    if (label.id == null) {
		        // TODO Failure?
		        $http.post(insightApp.getLabelsUrl(), label).success(function (data) {
		            $scope.labels.push(data);
		            $('#labelEditModal').modal('hide');
		        });
		    } else {
		        $http.put(insightApp.getLabelsUrl(), label).success(function (data) {
		            angular.forEach($scope.labels, function (labelCandidate, key) {
		                if (data.id === labelCandidate.id) {
		                    $scope.labels[key] = data;
		                    return false;
		                }
		            });
		            $('#labelEditModal').modal('hide');
		        });
		    }
		};

		$scope.confirmDeleteLabel = function (label) {
		    $scope.selectedLabel = angular.extend({id : null, applicationId : null, label : null, labelLowercase : null, color : null}, label);
		    $scope.deletedEnabled = true;
		    $('#deleteLabelModal').modal('show');
		};

		$scope.deleteLabel = function () {
		    $scope.deletedEnabled = false;
		    $http.delete(insightApp.getDeleteLabelsUrl($scope.selectedLabel)).success(function () {
		        var index = null;
		        angular.forEach($scope.labels, function (candidate, key) {
		            if (candidate.id === $scope.selectedLabel.id) {
		                index = key;
		                return false;
		            }
		        });
		        $scope.labels.splice(index, 1);
		        $('#deleteLabelModal').modal('hide');
		    });
		};
	}]);
}());
