/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

    var licenseGroupModule = angular.module('LicenseGroup', ['AngularCommon']);

    licenseGroupModule.controller('InsightLicenseGroupController', ['$scope', '$http', 'CLMLocations', 'CLMAppLocations', function ($scope, $http, clmLocations, clmAppLocations) {
        function sortLicense(a, b) {
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

		function deselect() {
			delete $scope.selectedGroup;
		}

        $scope.editorUrl = 'components/license-threat-group-editor.html?' + clmBuildTimestamp;

        if (typeof($scope.features) === 'undefined') {
            $scope.features = {};
        }
        $scope.features.licenseGroup = true;
        $scope.allLicenses = null;


        $http.get(clmAppLocations.getLicenseGroupsUrl(), {
            params: { timestamp: new Date().getTime() }
        }).success(function (data) {
            $scope.licenseGroups = data;
            angular.forEach($scope.licenseGroups, function (group, index) {
                $http.get(clmAppLocations.getLicenseGroupLicensesUrl(group), {
                    params: { timestamp: new Date().getTime() }
                }).success(function (data) {
                        group.licenses = data;
                }).error(function () { $scope.$broadcast('showServerError', arguments); });
            });
        }).error(function () { $scope.$broadcast('showServerError', arguments); });

        $http.get(clmLocations.getLicensesUrl(), {
            params: { timestamp: new Date().getTime() }
        }).success(function (data) {
                // Keep sorted for setLicenses
                $scope.allLicenses = data.sort(sortLicense);
            }).error(function () { $scope.$broadcast('showServerError', arguments); });

        $scope.editLicenseGroup = function (group) {

            $scope.selectedGroup = { id: null, applicationId: null, licenses: [], name: '', threatLevel: 5 };
            if (group) {
                $scope.selectedGroup = angular.extend($scope.selectedGroup, group);
            }

            // Build a list of all existing licenses to exclude from selection
            var existingLicenses = [];
            angular.forEach($scope.licenseGroups, function (licenseGroup, index) {
                if (licenseGroup.id != $scope.selectedGroup.id) {
                    $.merge(existingLicenses, licenseGroup.licenses);
                }
            });
            // Reset master license list
            angular.forEach($scope.allLicenses, function (license, index) {
                license.isApplied = false;
            });
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
        };

        $scope.confirmDeleteLicenseGroup = function (group) {
            $scope.selectedGroup = angular.extend({ id: null, applicationId: null, name: '', threatLevel: 5 }, group);
            $scope.deletedEnabled = true;
            $('#deleteLicenseGroupModal').modal('show');
        };

        $scope.deleteLicenseGroup = function () {
            $scope.deletedEnabled = false;
            $http['delete'](clmAppLocations.getDeleteLicenseGroupUrl($scope.selectedGroup)).success(function () {
                angular.forEach($scope.licenseGroups, function (licenseCandidate, key) {
                    if (licenseCandidate.id === $scope.selectedGroup.id) {
                        $scope.licenseGroups.splice(key, 1);
                        return false;
                    }
                });
                deselect();
                $('#deleteLicenseGroupModal').modal('hide');
            }).error(function () { $scope.$broadcast('showServerError', arguments); });
        };

		$scope.$on('license.cancelLicenseGroupEdit', function (event, licenseGroup) {
			if (!licenseGroup || licenseGroup.id === $scope.selectedGroup.id) {
				deselect();
			}
		});
    }]);

    licenseGroupModule.controller('InsightLicenseGroupEditorController', ['$scope', '$filter', '$http', 'hudson', 'CLMLocations', 'CLMAppLocations', function ($scope, $filter, $http, hudson, clmLocations, clmAppLocations) {
        $scope.threatLevels = [
            {'value': 10, 'name': '10'},
            {'value': 9, 'name': '9'},
            {'value': 8, 'name': '8'},
            {'value': 7, 'name': '7'},
            {'value': 6, 'name': '6'},
            {'value': 5, 'name': '5'},
            {'value': 4, 'name': '4'},
            {'value': 3, 'name': '3'},
            {'value': 2, 'name': '2'},
            {'value': 1, 'name': '1'},
            {'value': 0, 'name': 'No Threat'}
        ];

        function onError(data, status, headersFn, config) {
            $scope.submitActive = false;
            var header = headersFn();
            if ($scope.licenseGroupEditor) {
                if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
                    $scope.licenseGroupEditor.editErrorResponse = 'Server Error';
                } else {
                    $scope.licenseGroupEditor.editErrorResponse = data;
                }
            }
        }

        $scope.searchEnter = function () {
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
            return valid && !$scope.submitActive && $scope.selectedGroup != null && $scope.selectedGroup.name;
        };

        $scope.saveClick = function () {
            if (!$scope.canSaveEdit($scope.licenseGroupEditor.$valid)) {
                return;
            }

            (function (licenseGroup) {
                $scope.submitActive = true;

                var filter = $filter('filterLicenses');
                var licenseIds = filter($scope.licenses, { isApplied: true }).map(function (l) {
                    return l.id;
                });

                if (licenseGroup.id == null) {
                    hudson.post(clmAppLocations.getLicenseGroupsUrl(), licenseGroup).success(function (group) {
                        $http.put(clmAppLocations.getLicenseGroupLicensesUrl(group), licenseIds).success(function (licenses) {
                            group.licenses = licenses;
                            $scope.licenseGroups.push(group);
                        }).error(onError);
                        // Modal will close regardless of whether licenses are persisted or not. This will prevent creating two of the same group.
                        $scope.$emit('license.cancelLicenseGroupEdit');
                    }).error(onError);
                } else {
                    $http.put(clmAppLocations.getLicenseGroupsUrl(), licenseGroup).success(function (group) {
                        $http.put(clmAppLocations.getLicenseGroupLicensesUrl(licenseGroup), licenseIds).success(function (licenses) {
                            angular.forEach($scope.licenseGroups, function (licenseCandidate, key) {
                                if (group.id === licenseCandidate.id) {
                                    $scope.licenseGroups[key] = group;
                                    $scope.licenseGroups[key].licenses = licenses;
                                    return false;
                                }
                            });
                            $scope.$emit('license.cancelLicenseGroupEdit', licenseGroup);
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

		$scope.cancelLicenseGroupEdit = function () {
			$scope.$emit('license.cancelLicenseGroupEdit');
		};
		$scope.$watch('selectedLabel', function (newValue) {
			if (newValue) {
				$scope.submitActive = false;
			}
		});
    }]);

    licenseGroupModule.filter('filterLicenses', function () {
        return function (items, filter) {
			if (!angular.isArray(items)) {
				return;
			}
            var isApplied = filter.isApplied;
            var searchLicense = filter.searchLicense;

            var arrayToReturn = [];
            for (var i = 0; i < items.length; i++) {
                if ((typeof(isApplied) === 'undefined' || items[i].isApplied == isApplied)
                    && (!searchLicense || ~items[i].shortDisplayName.toLowerCase().indexOf(searchLicense.toLowerCase()))) {
                    arrayToReturn.push(items[i]);
                }
            }

            return arrayToReturn;
        };
    });

    licenseGroupModule.directive('enterDown', function () {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                if (attrs.preventSubmit !== undefined) {
                    $(element).bind('keypress keydown keyup', function (e) {
                        if (e.keyCode == 13) {
                            e.preventDefault();
                        }
                    });
                }

                $(element).bind('keydown', function (e) {
                    if (e.keyCode == 13) {
                        scope.$apply(attrs.enterDown || angular.noop);
                    }
                });
            }
        };
    });
}());