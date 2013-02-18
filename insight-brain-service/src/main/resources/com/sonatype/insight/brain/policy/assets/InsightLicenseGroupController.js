/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

(function () {
	'use strict';

	var licenseGroupModule = angular.module('LicenseGroup', []);
	
	licenseGroupModule.controller('InsightLicenseGroupController', ['$scope', '$http', 'CLMLocations', function ($scope, $http, clmLocations) {
		if (typeof($scope.features) === 'undefined') {
			$scope.features = {};
		}
		$scope.features.licenseGroup = true;
		$scope.allLicenses = null;
				
		function onError (data, status, headersFn, config) {
			$('#deleteLicenseGroupModal').modal('hide');
			var header = headersFn();
			if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
			    $scope.errorResponse = 'Server Error';
			} else {
			    $scope.errorResponse = data;
			}
			$('#licenseGroupErrorModal').modal('show');
		};
		
		function sortLicense (a, b) {
			if (a.id < b.id) {
				return -1;
			}
			if (a.id > b.id) {
				return 1;
			}
			return 0;
		}
		
		function sortGroupLicense(a, b) {
			if (a.licenseId < b.licenseId) {
				return -1;
			}
			if (a.licenseId > b.licenseId) {
				return 1;
			}
			return 0;
		}
		
		$http.get(clmLocations.getLicenseGroupsUrl(), {
            params : { timestamp : new Date().getTime() }
		}).success(function (data) {
			$scope.licenseGroups = data;
			for (var i = 0; i < $scope.licenseGroups.length; i++) {
				(function(group) {
					$http.get(clmLocations.getLicenseGroupLicensesUrl(group), {
						params : { timestamp : new Date().getTime() }
					}).success(function (data) {
						group.licenses = data;
					}).error(onError);
				})($scope.licenseGroups[i]);
			}
			
		}).error(onError);
		
		$http.get(clmLocations.getLicensesUrl(), {
            params : { timestamp : new Date().getTime() }
		}).success(function (data) {
			// Keep sorted for setLicenses
			$scope.allLicenses = data.sort(sortLicense);
			for (var i = 0; i < $scope.allLicenses.length; i++) {
				$scope.allLicenses[i].isApplied = false;
			}
		}).error(onError);
		
		$scope.editLicenseGroup = function(group) {
			$scope.editorUrl = 'components/license-group-editor.html?' + new Date(); // + clmBuildTimestamp;
			
			$scope.selectedGroup = { id : null, applicationId : null, licenses : [], name : '', threatLevel : 5 };
			if (group) {
				$scope.selectedGroup = angular.extend($scope.selectedGroup, group);
			}
			
			// Build a list of all existing licenses to exclude from selection
			var existingLicenses = [];
			for (var i = 0; i < $scope.licenseGroups.length; i++) {
				if ($scope.licenseGroups[i].id != $scope.selectedGroup.id) {
					$.merge(existingLicenses, $scope.licenseGroups[i].licenses);
				}
			}
			// Copy master license list
			var availableLicenses = $.merge([], $scope.allLicenses);
			existingLicenses.sort(sortGroupLicense);
			$scope.selectedGroup.licenses.sort(sortGroupLicense);
			var j = 0;
			var k = availableLicenses.length;
			for (var i = 0; i < k; i++) {
				// If all existing licenses and licenses from this group have been processed, we are done
				if (existingLicenses.length == 0 && j == $scope.selectedGroup.licenses.length) {
					break;
				}
				// If the license exists in the group's licenses, set isApplied to true
				if (j < $scope.selectedGroup.licenses.length && availableLicenses[i].id == $scope.selectedGroup.licenses[j].licenseId) {
					availableLicenses[i].isApplied = true;
					j++;
				}
				// If the license exists in another group's licenses, remove the license from the available licenses
				if (existingLicenses.length > 0 && availableLicenses[i].id == existingLicenses[0].licenseId) {
					availableLicenses.splice(i, 1);
					existingLicenses.shift();
					i--;
					k--;
				}
			}
			
			$scope.licenses = availableLicenses;
		
			$('#licenseGoupEditModal').modal('show');
		};
		
		$scope.confirmDeleteLicenseGroup = function (group) {
			$scope.selectedGroup = angular.extend({ id : null, applicationId : null, name : '', threatLevel : 5 }, group);
			$scope.deletedEnabled = true;
			$('#deleteLicenseGroupModal').modal('show');
		};

		$scope.deleteLicenseGroup = function () {
		    $scope.deletedEnabled = false;
		    $http['delete'](clmLocations.getDeleteLicenseGroupUrl($scope.selectedGroup)).success(function () {
		        angular.forEach($scope.licenseGroups, function (licenseCandidate, key) {
		            if (licenseCandidate.id === $scope.selectedGroup.id) {
				        $scope.licenseGroups.splice(key, 1);
		                return false;
		            }
		        });
		        $('#deleteLicenseGroupModal').modal('hide');
		    }).error(onError);
		};
	}]);
	
	licenseGroupModule.controller('InsightLicenseGroupEditorController', ['$scope', '$filter', '$http', 'hudson', 'CLMLocations', function ($scope, $filter, $http, hudson, clmLocations) {
		$scope.submitActive = false;
		
		$scope.threatLevels = [{'value':10, 'name':'10'},{'value':9, 'name':'9'},{'value':8, 'name':'8'},{'value':7, 'name':'7'},{'value':6, 'name':'6'},{'value':5, 'name':'5'},{'value':4, 'name':'4'},{'value':3, 'name':'3'},{'value':2, 'name':'2'},{'value':1, 'name':'1'},{'value':0, 'name':'No Threat'}];
		
		function onError (data, status, headersFn, config) {
			$scope.submitActive = false;
			var header = headersFn();
			if ($scope.licenseGroupEditor) {
			    if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
			        $scope.licenseGroupEditor.editErrorResponse = 'Server Error';
			    } else {
					$scope.licenseGroupEditor.editErrorResponse = data;
			    }
			}
		};
		
		$scope.searchEnter = function() {
			var filter = $filter('filterLicenses');
			var licenses = filter($scope.licenses, { searchLicense: $scope.licenseSearch });
			// If only one license is applicable to the current search filter, set isApplied true when enter is pressed
			if (licenses.length == 1) {
				licenses[0].isApplied = !licenses[0].isApplied;
				$scope.licenseSearch = null;
			}
		};
		
		$scope.setIsApplied = function (license, value) {
			license.isApplied = value;
		};
		
		$scope.canSaveEdit = function (valid) {
			return valid && !$scope.submitActive && $scope.selectedGroup != null && $scope.selectedGroup.name.length > 0;
		};
		
		$scope.saveClick = function () {
			if (!$scope.canSaveEdit($scope.licenseGroupEditor.$valid)) {
				return;
            }
			
			(function (licenseGroup) {
				$scope.submitActive = true;
				
				var filter = $filter('filterLicenses');
				var licenseIds = filter($scope.licenses, { isApplied: true }).map(function(l) {
					return l.id;
				});
				
				if (licenseGroup.id == null) {
					hudson.post(clmLocations.getLicenseGroupsUrl(), licenseGroup).success(function (group) {
						$http.put(clmLocations.getLicenseGroupLicensesUrl(licenseGroup), licenseIds).success(function (licenses) {
							group.licenses = licenses;
							$scope.licenseGroups.push(group);
						}).error(onError);
						// Modal will close regardless of whether licenses are persisted or not. This will prevent creating two of the same group.
			            $('#licenseGoupEditModal').modal('hide');
			        }).error(onError);
				} else {
					$http.put(clmLocations.getLicenseGroupsUrl(), licenseGroup).success(function (group) {
						$http.put(clmLocations.getLicenseGroupLicensesUrl(licenseGroup), licenseIds).success(function (licenses){
				            angular.forEach($scope.licenseGroups, function (licenseCandidate, key) {
				                if (group.id === licenseCandidate.id) {
				                    $scope.licenseGroups[key] = group;
				                    $scope.licenseGroups[key].licenses = licenses;
				                    return false;
				                }
				            });
				            $('#licenseGoupEditModal').modal('hide');
						}).error(onError);
			        }).error(onError);
				}
				
				$scope.submitActive = false;
			})($scope.selectedGroup);
			
			$scope.clearEditError = function () {
				if ($scope.licenseGroupEditor) {
					$scope.licenseGroupEditor.editErrorResponse = null;
				}
			};
		};
	}]);
	
	licenseGroupModule.filter('filterLicenses', function() {
		return function(items, filter) {
			var isApplied = filter.isApplied;
			var searchLicense = filter.searchLicense;
			
			var arrayToReturn = [];        
		    for (var i=0; i<items.length; i++){
		        if ((typeof(isApplied) === 'undefined' || items[i].isApplied == isApplied) 
		        		&& (!searchLicense || ~items[i].shortDisplayName.toLowerCase().indexOf(searchLicense.toLowerCase()))) {
		            arrayToReturn.push(items[i]);
		        }
		    }

		    return arrayToReturn;
		};
	});
	
	licenseGroupModule.directive('enterDown', function() {
		return {
			restrict: 'A',
			link : function(scope, element, attrs) {
				if (attrs.preventSubmit !== undefined) {
					$(element).bind('keypress keydown keyup', function(e) {
						if (e.keyCode == 13) {
							e.preventDefault();
						}
					});
				}
				
				$(element).bind('keydown', function(e) {
					if (e.keyCode == 13) {
						scope.$apply(attrs.enterDown || angular.noop);
					}
				});
			}
		};
	});
}());