/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp, window */
(function () {
	'use strict';

	var organizationModule = angular.module('OrganizationModule', ['ui.compat', 'ManagementModule', 'Organization'], ['$stateProvider', function ($stateProvider) {
		$stateProvider.state('management.organization', {
			parent : 'management',
			url : '/organization',
			controller : 'OrganizationController',
			templateUrl : '../organization-assets/components/organization-navigator.html'
		}).state('management.organization.view', {
			parent : 'management.organization',
			url : '/{organizationId}',
			controller : 'OrganizationEditorController',
			templateUrl : '../organization-assets/components/organization-editor.html'
		}).state('management.organization.view.licenses', {
			parent : 'management.organization.view',
			url : '/licenses',
			controller : 'LicenseThreatGroupController',
			templateUrl : '../policy-assets/components/license-threat-group/license-threat-group.html'
		});
	}]);
}());

(function() {
    'use strict';

    var organizationModule = angular.module('Organization', [ 'AngularCommon', 'ui.compat', 'CLMAppLocation', 'ResourceModule', 'EditorTools' ]);

    organizationModule.controller('OrganizationController', [ '$scope', '$state', '$http', '$location', '$timeout', 'hudson', 'CLMLocations', 'OrganizationStore', function($scope, $state, $http, $location, $timeout, hudson, CLMLocations, OrganizationStore) {
        function switchOrganization() {
            $scope.selectedOrganization = null;
            $scope.userIconSource = null;
            if ('_new_' == $scope.$state.params.organizationId) {
                $timeout(function() {
                    $scope.selectedOrganization = OrganizationStore.create();
                    $scope.origUserIconSource = $scope.userIconSource = '../assets/img/defaulticon_organization.png';
                }, 100);
            }
            if ($scope.$state.params.organizationId !== null && $scope.organizations) {
                for ( var i = 0; i < $scope.organizations.length; i++) {
                    if ($scope.$state.params.organizationId === $scope.organizations[i].id) {
                        $timeout(function() {
                            // don't want to infect the original data
                            $scope.selectedOrganization = $scope.organizations[i].$clone();
                            $scope.origUserIconSource = $scope.userIconSource = '../rest/organization/icon/' + encodeURIComponent($scope.selectedOrganization.id);
                        }, 100);
                        return;
                    }
                }
            }
        }

        $scope.$state = $state;

        OrganizationStore.get().then(function(results) {
            $scope.organizations = results;
            $scope.$watch('$state.params.organizationId', switchOrganization);
            switchOrganization();
        }, function() {
            $scope.$broadcast('showServerError', arguments);
        });
    } ]);

    organizationModule.controller('OrganizationEditorController', [ '$scope', '$state', '$location', 'regexFactory', 'CLMLocations', 'hudson', 'editorTools', 'CLMAppLocations', function($scope, $state, $location, regexFactory, CLMLocations, hudson, editorTools, clmAppLocations) {
        $scope.$state = $state;
        $scope.submitActive = false;
        $scope.addOrganizationSync = clmAppLocations.addIconSync();
        $scope.hasRobotSource = false;
        $scope.alerts = [];

        $scope.validateName = function(value) {
            $scope.organizationEditor.$invalid = false;
            
            var result = editorTools.validateName(value, $scope.selectedOrganization, $scope.organizations);
            
            if (result !== true) {
                $scope.organizationEditor.$invalid = true;
                return result;
            }
        }

        $scope.closeAlert = function(index) {
            $scope.alerts.splice(index, 1);
        };

        $scope.generateIcon = function() {
            $scope.robotHash = editorTools.generateIcon($scope.selectedOrganization.name);
            $scope.hasRobotSource = true;
            $scope.iconChanged = true;
        };

        $scope.fileChanged = function(element) {
            $scope.$apply(function() {
                $scope.userIconSource = editorTools.getIconSource(element, '../assets/img/defaulticon_organization.png');
                $scope.hasRobotSource = false;
                $scope.iconChanged = true;
            });
        };

        $scope.encodeURIComponent = window.encodeURIComponent;

        $scope.canSaveEdit = function() {
            return !$scope.organizationEditor.$invalid && !$scope.submitActive;
        };

        $scope.cancelClick = function() {
            $scope.selectedOrganization.$revert();
            if ($scope.iconChanged) {
                $scope.userIconSource = $scope.origUserIconSource;
                $scope.iconChanged = false;
            }
        };

        $scope.isFormDirty = function() {
            if (!$scope.selectedOrganization) {
                return false;
            }
            var originalOrganization = $scope.selectedOrganization.$getOriginal();
            var currentOrganization = $scope.selectedOrganization;
            return currentOrganization.name !== originalOrganization.name || $scope.iconChanged;
        };

        // This needs to be invoked by onsubmit rather than ng-submit to
        // suppress submit when necessary
        $scope.saveClick = function() {
            if ($scope.submitActive) {
                return true;
            }

            if ($scope.organizationEditor.$invalid) {
                return false;
            }

            if (window.FormData) {
                var icon = angular.element('#file')[0];
                if (icon.files.length > 0) {
                    if (icon.files[0].size > 5242880) {
                        $scope.$apply(function() {
                            $scope.alerts.push({
                                type : 'error',
                                msg : 'Icon file size must be smaller than 5 MB.'
                            });
                        });
                        return false;
                    }
                }
            }

            $scope.submitActive = true;

            $scope.selectedOrganization.$save().then(function(data) {
                if ($scope.iconChanged) {
                    saveIcon();
                } else {
                    $scope.submitActive = false;
                }

                $state.params.organizationId = data.id;

                var path = $location.path();
                $location.path(path.substring(0, path.lastIndexOf('/')) + '/' + $state.params.organizationId);
            }, function(data) {
                $scope.alerts.push({
                    type : 'error',
                    msg : data
                })
            });

            return false;
        };

        function saveIcon() {
            // Angular modal does not adjust value of form element so when
            // posting these values need to be set
            angular.element('[name=organizationId]').val($scope.selectedOrganization.id);
            angular.element('[name=hasRobotSource]').val($scope.hasRobotSource);
            angular.element('[name=robotHash]').val($scope.robotHash);

            var form = angular.element('#organizationEditor');

            if (window.FormData) {
                $scope.isUploadingIcon = true;

                var formData = new FormData(form[0]);
                var icon = angular.element('#file')[0];
                if (icon.files.length > 0) {
                    formData.append('file', icon.files[0]);
                }

                hudson.ajaxPost({
                    url : clmAppLocations.addIcon(),
                    data : formData,
                    success : function(data, status, jqXHR) {
                        $scope.$apply(function() {
                            // We need to regrab the icon here because it
                            // doesn't exist when the browser first requests
                            var iconSource = "../rest/organization/icon/" + encodeURIComponent($scope.selectedOrganization.id);
                            angular.element("img[ng-src='" + iconSource + "']").attr('src', iconSource + '?' + new Date().getTime());
                            $scope.submitActive = false;
                            $scope.isUploadingIcon = false;
                            $scope.origUserIconSource = $scope.userIconSource;
                        });
                    },
                    error : function(jqXHR) {
                        $scope.$apply(function() {
                            $scope.isUploadingIcon = false;
                            $scope.submitActive = false;
                            $scope.$broadcast('postAlert', jqXHR);
                        });
                    }
                });
            } else {
                form.submit();
            }
        }
    } ]);

    organizationModule.service('OrganizationStore', [ 'CLMLocations', 'CLMResource', '$q', function(CLMLocations, clmResource, $q) {
        return clmResource.getStore({
            id : 'id',
            url : CLMLocations.getOrganizationsUrl(),
            template : {
                id : null,
                name : null
            }
        });
    } ]);


	organizationModule.service('OrganizationId', function ($state) {
		return {
			encoded : function () {
				var organizationId = $state.params.organizationId;
				return organizationId ? encodeURI(organizationId) : null;
			}
		};
	});
}());