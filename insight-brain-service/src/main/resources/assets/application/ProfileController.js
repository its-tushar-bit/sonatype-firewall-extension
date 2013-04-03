/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window */
(function () {
	'use strict';

	var profileModule = angular.module('Profile', ['AngularCommon', 'Hudson', 'CLMLocation']);

	profileModule.controller('ProfileController', ['$scope', '$location', '$http', 'hudson', 'CLMLocations', function ($scope, $location, $http, hudson, clmLocations) {
		$http.get(clmLocations.getProfilesUrl()).success(function (data) {
			$scope.profiles = data;
		}).error(function () {
			window.alert('hi!');
		});

		$scope.editProfile = function (profile) {
			$scope.edit = true;
			if (profile) {
			    $scope.selectedProfile = angular.extend({}, profile);
			} else {
				$scope.selectedProfile = { id : null };
			}
		};
		$scope.confirmDeleteProfile = function (profile) {
			// open modal
			$scope.selectedProfile = profile;
			$('#deleteProfileModal').modal('show');
		};
		$scope.deleteProfile = function () {
			$http['delete'](clmLocations.getDeleteProfileUrl($scope.selectedProfile)).success(function () {
				var index = null;
                angular.forEach($scope.profiles, function (candidate, key) {
                    if (candidate.id === $scope.selectedProfile.id) {
                        index = key;
                        return false;
                    }
                });
                if (index !== null) {
                    $scope.profiles.splice(index, 1);
                }
                delete $scope.selectedProfile;
                $('#deleteProfileModal').modal('hide');
			});
		};
		$scope.deselectProfile = function () {
			delete $scope.edit;
			delete $scope.selectedProfile;
		};
		$scope.submitProfile = function () {
			var selectedProfile = $scope.selectedProfile;
			if (selectedProfile.id === null) {
				hudson.post(clmLocations.getProfilesUrl(), $scope.selectedProfile).success(function (data) {
					$scope.profiles.push(data);
	                $scope.deselectProfile();
				});
			} else {
				$http.put(clmLocations.getProfilesUrl(), $scope.selectedProfile).success(function (data) {
					angular.forEach($scope.profiles, function (profile, index) {
						if (profile.id === selectedProfile.id) {
							$scope.profiles[index] = data;
							return false;
						}
					});
	                $scope.deselectProfile();
				});
			}
		};
	}]);
}());