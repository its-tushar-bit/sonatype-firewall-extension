/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
(function () {
    'use strict';

    var licenseGroupModule = angular.module('LicenseThreatGroup', ['AngularCommon', 'ResourceModule', 'CLMAppLocation']);

    licenseGroupModule.service('licenseGroupStore', function ($q, $http, CLMAppLocations, CLMResource, ApplicationId) {
		var currentStoreAppId = null, licenseGroupStore = null;

		function refreshLicenseStore() {
			var isNew = !licenseGroupStore || currentStoreAppId !== ApplicationId.encoded(); 
			if (isNew) {
				currentStoreAppId = ApplicationId.encoded();
				licenseGroupStore = CLMResource.getStore(angular.extend({ url : CLMAppLocations.getLicenseGroupsUrl() }, licenseGroupStoreTemplate));
				licenseGroupStore.objectMethods.push('licenses');
				licenseGroupStore.objectMethods.push('$saveGroup');
			}
			return isNew;
		}

    	function populateGroupLicenses(licenseGroups) {
			var deferred = $q.defer();
			var licenseCount = licenseGroups.length;

			if (licenseGroups.length > 0) {
				angular.forEach(licenseGroups, function (group, index) {
	                $http.get(CLMAppLocations.getLicenseGroupLicensesUrl(group), {
	                    params: { timestamp: new Date().getTime() }
	                }).success(function (data) {
	                    group.licenses = data;
	                    group.$saveGroup = saveGroup;
	                    licenseCount--;
	                    if (licenseCount <= 0) {
	                    	deferred.resolve(licenseGroups);
	                    }
	                }).error(function (data, status, headers, config) {
	        			deferred.reject({
	        				data: data,
	        				status : status,
	        				headers : headers,
	        				config : config
	        			});
	        		});
	            });
			} else {
				deferred.resolve(licenseGroups);
			}

			return deferred.promise;
		}

    	function saveGroup(licenseIds) {
    		var deferred = $q.defer();

    		var licenseGroup = this;
    		licenseGroup.$save().then(function() {
    			$http.put(CLMAppLocations.getLicenseGroupLicensesUrl(licenseGroup), licenseIds).success(function (licenses) {
                	licenseGroup.licenses = licenses;

                	deferred.resolve(licenseGroup);
                }).error(function (data, status, headers, config) {
        			deferred.reject({
        				data: data,
        				status : status,
        				headers : headers,
        				config : config
        			});
        		});
    		}, function(rejection) {
    			deferred.reject({
    				data: rejection.data,
    				status : rejection.status,
    				headers : rejection.headers,
    				config : rejection.config
    			});
            });

    		return deferred.promise;
    	}

		var licenseGroupStoreTemplate = {
			id : 'id',
			template : { id: null, applicationId: null, licenses: [], name: '', threatLevel: 5 },
			params : {
				timestamp : new Date().getTime()
			}
		};

		return {
			get: function() {
				if (refreshLicenseStore()) {
					return licenseGroupStore.get().then(populateGroupLicenses);
				} else {
					return licenseGroupStore.get();
				}
			},
			refresh: function() {
				refreshLicenseStore();
				return licenseGroupStore.refresh().then(populateGroupLicenses);
			},
			create: function() {
				refreshLicenseStore();
				var licenseGroup = licenseGroupStore.create();
				licenseGroup.saveGroup = saveGroup;
				return licenseGroup;
			}
		};
	});

    licenseGroupModule.service('licenseStore', function (CLMLocations, CLMResource) {
		var licenseStore = CLMResource.getStore({
			id : 'id',
			url : CLMLocations.getLicensesUrl(),
			params : {
				timestamp : new Date().getTime()
			}
		});
		return licenseStore;
    });

    licenseGroupModule.controller('LicenseThreatGroupController', function ($scope, $http, $q, CLMLocations, CLMAppLocations, licenseStore, licenseGroupStore) {
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

        $scope.editorUrl = '../policy-assets/components/license-threat-group/license-threat-group-editor.html?' + clmBuildTimestamp;
        $scope.allLicenses = null;
        $scope.allExpanded = false;

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

        $q.all([licenseStore.get(), licenseGroupStore.get()]).then(function (results) {
        	var licenses = results[0];
        	var licenseGroups = results[1];

        	$scope.allLicenses = licenses.sort(sortLicense);
        	$scope.licenseGroups = licenseGroups;
        }, function(error, a) {
        	alert(error);
        });

        $scope.getDisplayName = function(licenseId) {
        	for (var i = 0; i < $scope.allLicenses.length; i++) {
        		if ($scope.allLicenses[i].id === licenseId) {
        			return '(' + $scope.allLicenses[i].shortDisplayName + ') ' + $scope.allLicenses[i].longDisplayName;
        		}
        	}
        };

        $scope.editLicenseGroup = function (group) {
        	$scope.selectedGroup = licenseGroupStore.create();
        	if (group) {
        		$scope.selectedGroup = angular.extend($scope.selectedGroup, group);
        	}

            // Reset master license list
            angular.forEach($scope.allLicenses, function (license, index) {
                license.isApplied = false;
            });
            // Copy master license list
            var availableLicenses = $.merge([], $scope.allLicenses);
            $scope.selectedGroup.licenses.sort(sortGroupLicense);
            var j = 0;
            var k = availableLicenses.length;
            for (var i = 0; i < k; i++) {
                // If all licenses from this group have been processed, we are done
                if (j == $scope.selectedGroup.licenses.length) {
                    break;
                }
                // If the license exists in the group's licenses, set isApplied to true
                if (j < $scope.selectedGroup.licenses.length && availableLicenses[i].id == $scope.selectedGroup.licenses[j].licenseId) {
                    availableLicenses[i].isApplied = true;
                    j++;
                }
            }

            $scope.licenses = availableLicenses;

            angular.element('#licenseModal').modal('show');
        };

        $scope.inlineChangeThreatLevel = function(licenseGroup, threatLevel) {
        	licenseGroup.threatLevel = threatLevel.value;
        };

		$scope.hasInlineChanges = function() {
			if (!$scope.licenseGroups) {
				return false;
			}
			for (var i = 0; i < $scope.licenseGroups.length; i++) {
				if ($scope.licenseGroups[i].isDirty()) {
					return true;
				}
			}
		};

        $scope.inlineSaveLicenseGroup = function() {
    		for (var i = 0; i < $scope.licenseGroups.length; i++) {
				var licenseThreatGroup = $scope.licenseGroups[i];
				if (licenseThreatGroup.isDirty()) {
					licenseThreatGroup.$save().then(angular.noop, function(rejection) {
						$scope.alerts.push({ type: 'error', msg: rejection.data });
						$scope.inlineRevertLicenseGroup(licenseThreatGroup);
					});
				}
    		}
        };

        $scope.inlineRevertLicenseGroups = function() {
    		for (var i = 0; i < $scope.licenseGroups.length; i++) {
    			var licenseGroup = $scope.licenseGroups[i];
				$scope.inlineRevertLicenseGroup(licenseGroup);
    		}
        };

		$scope.inlineRevertLicenseGroup = function(licenseThreatGroup) {
			var original = licenseThreatGroup.$getOriginal();
			angular.extend(licenseThreatGroup, original);
		}

        $scope.toggleAll = function() {
        	var action = $scope.allExpanded ? 'hide' : 'show';
        	angular.element('.accordion-body').collapse(action);
        	$scope.allExpanded = !$scope.allExpanded;
        };

        $scope.confirmDeleteLicenseGroup = function (group) {
            $scope.selectedGroup = angular.extend({ id: null, applicationId: null, name: '', threatLevel: 5 }, group);
            $scope.deletedEnabled = true;
            $('#deleteLicenseGroupModal').modal('show');
        };

        $scope.deleteLicenseGroup = function () {
            $scope.deletedEnabled = false;
            $http['delete'](CLMAppLocations.getDeleteLicenseGroupUrl($scope.selectedGroup)).success(function () {
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
			deselect();
			delete $scope.newGroupName;
			angular.element('#licenseModal').modal('hide');
		});
    });

    licenseGroupModule.controller('LicenseThreatGroupEditorController', function ($scope, $filter, $http, hudson, CLMAppLocations, licenseGroupStore, Messages) {
        $scope.alerts = [];

        $scope.searchEnter = function () {
            var filter = $filter('filterLicenses');
            var licenses = filter($scope.licenses, { searchLicense: $scope.licenseSearch });
            // If only one license is applicable to the current search filter, set isApplied true when enter is pressed
            if (licenses.length == 1) {
            	$scope.setIsApplied(licenses[0], !licenses[0].isApplied);
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

                licenseGroup.saveGroup(licenseIds).then(function(licenseGroup) {
                	for (var i = 0; i < $scope.licenseGroups.length; i++) {
                		var licenseGroupIter = $scope.licenseGroups[i];
                		if (licenseGroup.id === licenseGroupIter.id) {
                			$scope.licenseGroups[i] = licenseGroup;
                		}
                	}

                	$scope.alerts = [];
                	$scope.$emit('license.cancelLicenseGroupEdit');
                }, function(rejection) {
                	$scope.alerts.push({
    					type : 'error',
    					msg : 'An error occurred while saving the license threat group. (' + Messages.getHttpErrorMessage(error) + ')'
    				});
                });

                $scope.submitActive = false;
            })($scope.selectedGroup);
        };

		$scope.cancelLicenseGroupEdit = function () {
			$scope.alerts = [];
			$scope.$emit('license.cancelLicenseGroupEdit');
		};
        $scope.$on('$destroy', function () {
            angular.element('.modal-backdrop').remove(); // Bootstrap modal creates elements at the document root
        });
    });

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