(function () {
	'use strict';

	var applicationModule = angular.module('ApplicationModule', ['ui.compat', 'ManagementModule', 'Policy', 'LicenseThreatGroup', 'Labels'], ['$stateProvider', function ($stateProvider) {
		$stateProvider.state('management.application', {
			parent : 'management',
			url : '/application',
			controller : 'applicationController',
			templateUrl : '../application-assets/components/application-navigator.html'
		}).state('management.application.view', {
			parent : 'management.application',
			url : '/{applicationPublicId}',
			controller : 'applicationEditorController',
			templateUrl : '../application-assets/components/application-editor.html'
		}).state('management.application.view.policies', {
			parent : 'management.application.view',
			url : '/policies',
			controller : 'PolicyController',
			templateUrl : '../policy-assets/components/policy/policy.html'
		}).state('management.application.view.policies.edit', {
			parent : 'management.application.view',
			url : '/policies/{policyId}',
			controller : 'PolicyEditorController',
			templateUrl : '../assets/components/policy-editor/policy-editor.html'
		}).state('management.application.view.labels', {
			parent : 'management.application.view',
			url : '/labels',
			controller : 'LabelController',
			templateUrl : '../policy-assets/components/label-editor/labels.html'
		}).state('management.application.view.licenses', {
			parent : 'management.application.view',
			url : '/licenses',
			controller : 'LicenseThreatGroupController',
			templateUrl : '../policy-assets/components/license-threat-group/license-threat-group.html'
		});
	}]);

	applicationModule.controller('applicationController', function($scope, $state, $timeout, $location, applicationStore) {
		function switchApplication() {
			$scope.selectedApplication = null;
			if ($scope.$state.params.applicationPublicId !== null && $scope.applications) {
				for (var i = 0; i < $scope.applications.length; i++) {
					if ($scope.$state.params.applicationPublicId === $scope.applications[i].publicId) {
						$timeout(function () {
							$scope.selectedApplication = $scope.applications[i];
						}, 200);
						return;
					}
				}
				// TODO We might want to consider reloading the store at this point?
			}
		}
		$scope.location =  $location;

		$scope.$state = $state;
		$scope.isCurrentTab = function (tabName) {
			return $state.current.name.lastIndexOf(tabName) === $state.current.name.length - tabName.length;
		};

		applicationStore.get().then(function(applications) {
			$scope.applications = applications;
			switchApplication();
			$scope.$watch('$state.params.applicationPublicId', switchApplication);
		}, function (error) {
            // TODO Error handling
			alert(error.data);
		});
	});

	applicationModule.controller('applicationEditorController', function($scope, $state, applicationStore) {
		$scope.$state = $state;

		$scope.encodeURIComponent = window.encodeURIComponent;
	});

	applicationModule.service('applicationStore', ['CLMLocations', 'CLMResource', function (clmLocations, clmResource) {
		var applicationStore = clmResource.getStore({
			id : 'id',
			url : clmLocations.getApplicationsUrl(),
			template : { id: null, publicId: null, name: null },
			params : {
				timestamp : new Date().getTime()
			}
		});
		return applicationStore;
	}]);

	applicationModule.service('ApplicationId', ['commonCodeFactory', '$state', function (commonCodeFactory, $state) {
		// TODO Are ui-router parameters encoded or decoded?
		return {
			encoded : function () {
				var applicationPublicId = $state.params.applicationPublicId;
				return applicationPublicId ? encodeURI(applicationPublicId) : null;
			}
		};
	}]);
}());