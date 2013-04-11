/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window */
(function () {
	'use strict';

	var profileModule = angular.module('Profile', ['AngularCommon', 'Hudson', 'CLMLocation', 'PolicyEditor']);

	profileModule.service('ProfileStore', ['$q', 'CLMLocations', 'CLMResource', function ($q, clmLocations, clmResource) {
		var profileStore = clmResource.getStore({
			id : 'id',
			url : clmLocations.getProfilesUrl(),
			template : {
				id : null,
				name : null
			},
			params : {
				timestamp : new Date().getTime()
			}
		});
		return profileStore;
	}]);

	function httpErrorFn() {
		// TODO Adequate Error Handling ;)
		console.log(arguments);
		window.alert('hi!');
	}

	profileModule.controller('ProfilePageController', ['$scope', '$timeout', '$q', '$http', 'hudson', 'CLMLocations', 'ProfileStore', 'PolicyStore', function ($scope, $timeout, $q, $http, hudson, clmLocations, profileStore, policyStore) {
		function errorLoading() {
		}

		$scope.errorSaving = function (error) {
			if (arguments.length === 0) {
				$scope.alerts.push('An unexpected error occurred');
			} else {
				angular.forEach(arguments, function (error) {
					if (error.status === 0) {
						$scope.alerts.push('Unable to contact server');
					} else {
						$scope.alerts.push(error.data + ' (' + error.status + ')');
					}
				});
			}
		};

		$scope.alerts = [];

		$scope.hideAlert = function (index) {
			$scope.alerts.splice(index,1);
		};

		$scope.editProfile = function (profile) {
			if (profile) {
				$scope.selectedProfile = angular.copy(profile);
			} else {
				$scope.selectedProfile = profileStore.create();
			}
		};

		$scope.confirmDeleteProfile = function (profile) {
			// open modal
			$('#deleteProfileModal').modal('show');
		};

		$scope.deleteProfile = function () {
			var profileToDelete = $scope.selectedProfile;
			$http['delete'](clmLocations.getDeleteProfileUrl(profileToDelete)).success(function () {
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
				if ($scope.selectedProfile && profileToDelete.id === $scope.selectedProfile.id) {
					delete $scope.selectedProfile;
				}
				$('#deleteProfileModal').modal('hide');
			}).error(function () {
				var args = arguments
				$timeout(function () {
					// Asynchronous for aesthetic reasons
					$scope.errorSaving({
						status : args[1],
						data : args[0]
					});
				}, 150);
				$('#deleteProfileModal').modal('hide');
			});
		};

		$scope.deselectProfile = function () {
			delete $scope.selectedProfile;
		};

		$scope.editPolicy = function () {
			$scope.policyEdit = true;
		};

		$scope.$on('editPolicyComplete', function (event) {
			// Occurs when policy editing has finished, reshow profile editor
			event.stopPropagation();
			$scope.policyEdit = false;
		});
		profileStore.get().then(function (result) {
			$scope.profiles = result;
		}, httpErrorFn);
	}]);

	profileModule.controller('ProfileController', ['$scope',  '$q', '$http', 'hudson', 'CLMLocations', 'ProfileStore', 'PolicyStore', function ($scope, $q, $http, hudson, clmLocations, profileStore, policyStore) {
		function createProfileIdMap() {
			$scope.profileIdMap = {};
			angular.forEach($scope.policies, function (policy) {
				$scope.profileIdMap[policy.id] = policy;
			});
		}

		$scope.submitProfile = function () {
			var selectedProfile = $scope.selectedProfile,
				profilePolicies = $scope.profilePolicies,
				promise;

			if (selectedProfile.id === null) {
				promise = $scope.selectedProfile.$save().then(function (data) {
					if (profilePolicies.length > 0) {
						return $http.put(clmLocations.getApplicationProfilePoliciesUrl(data.id), profilePolicies);
					}
				});
			} else {
				promise = $q.all([$scope.selectedProfile.$save(), $http.put(clmLocations.getApplicationProfilePoliciesUrl($scope.selectedProfile.id), $scope.profilePolicies)]);
			}
			promise.then(function () {
				$scope.deselectProfile();
			}, $scope.errorSaving);
		};

		$scope.filterName = function (profilePolicyId) {
			return (!$scope.policyFilter || $scope.profileIdMap[profilePolicyId].name.indexOf($scope.policyFilter) !== -1)
		};
		$scope.filterAvailablePolicies = function (policy) {
			var located = false;
			angular.forEach($scope.profilePolicies, function (candidate) {
				if (candidate === policy.id) {
					located = true;
					return false;
				}
			});
			return !located && (!$scope.policyFilter || policy.name.indexOf($scope.policyFilter) !== -1);
		};

		$scope.addProfilePolicy = function (policy) {
			$scope.profilePolicies.push(policy.id);
		};
		$scope.removeProfilePolicy = function (policyId) {
			var index = -1;
			angular.forEach($scope.profilePolicies, function (candidate, key) {
				if (policyId === candidate) {
					index = key;
					return false;
				}
			});
			if (index !== -1) {
				$scope.profilePolicies.splice(index, 1);
			}
		};

		$scope.isNameUnique = function () {
			var unique = true;
			angular.forEach($scope.profiles, function (profile) {
				if (profile.name === $scope.selectedProfile.name && $scope.selectedProfile.id !== profile.id) {
					unique = false;
					return false;
				}
			});
			return unique;
		};

		$scope.$watch('policies', createProfileIdMap);
		$scope.$watch('selectedProfile', function (newValue) {
			delete $scope.profilePolicies;
			if (newValue !== null && angular.isDefined(newValue)) {
				$http.get(clmLocations.getApplicationProfilePoliciesUrl(newValue.id)).success(function (data) {
					if ($scope.selectedProfile && $scope.selectedProfile.id === newValue.id) {
						$scope.profilePolicies = [];
						angular.forEach(data, function (applicationProfilePolicy) {
							$scope.profilePolicies.push(applicationProfilePolicy.policyId);
						});
					}
				}).error($scope.errorSaving);
			}
		});

		// Load Data
		policyStore.get().then(function (policies) {
			$scope.policies = policies;
		}, httpErrorFn);
	}]);

	profileModule.directive('uniqueProfileName', function () {
		return {
			require: 'ngModel',
			link: function (scope, element, attrs, ctrl) {
				var validator = function (newValue) {
					if (!newValue) {
						return undefined;
					}

					var unique = true;
					angular.forEach(scope.profiles, function (profile) {
						if (scope.selectedProfile.id !== profile.id && profile.name === newValue) {
							unique = false;
							return false;
						}
					});
					ctrl.$setValidity('unique', unique);

					return (unique && newValue.length > 0) ? newValue : undefined;
				};
				ctrl.$formatters.push(validator);
				ctrl.$parsers.unshift(validator);
			}
		};
	});
}());