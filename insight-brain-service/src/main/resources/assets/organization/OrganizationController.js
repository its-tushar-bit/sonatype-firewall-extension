/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp, window */
(function() {
    'use strict';

    var organizationModule = angular.module('Organization', [ 'AngularCommon', 'ui.compat', 'CLMLocation', 'ResourceModule' ]);

    organizationModule.controller('OrganizationController', [ '$scope', '$state', '$http', '$location', '$timeout', 'hudson', 'CLMLocations', 'OrganizationStore', function($scope, $state, $http, $location, $timeout, hudson, clmLocations, organizationStore) {
        function switchOrganization() {
            $scope.selectedOrganization = null;
            if ('_new_' == $scope.$state.params.organizationId) {
                $timeout(function () {
                    $scope.selectedOrganization = organizationStore.create();
                }, 100);
            }
            if ($scope.$state.params.organizationId !== null && $scope.organizations) {
                for ( var i = 0; i < $scope.organizations.length; i++) {
                    if ($scope.$state.params.organizationId === $scope.organizations[i].id) {
                        $timeout(function () {
                            //don't want to infect the original data
	                        $scope.selectedOrganization = angular.copy($scope.organizations[i]);
                        }, 100);
                        return;
                    }
                }
            }
        }

        $scope.$state = $state;

        organizationStore.get().then(function(results) {
            $scope.organizations = results;
            $scope.$watch('$state.params.organizationId', switchOrganization);
            switchOrganization();
        }, function() {
            $scope.$broadcast('showServerError', arguments);
        });
    }]);

    organizationModule.controller('OrganizationEditorController', [ '$scope', '$state', '$location', 'regexFactory', function($scope, $state, $location, regexFactory) {
        $scope.$state = $state;
        
        $scope.validateName = function(value) {
            //field is required, alphanumeric, and no unnecessary spaces
            if (!value) {
                $scope.organizationEditor.$invalid = true;
                return 'Name is required';
            } else if (value.match(new RegExp('[^-' + regexFactory.allLetters().source + '0-9 ]', 'i'))) {
                $scope.organizationEditor.$invalid = true;
                return 'Must be alpha numeric';
            } else if (value.match(/^ | {2,}|\t| $/)) {
                $scope.organizationEditor.$invalid = true;
                return 'No leading, trailing or double spaces or tabs';
            }
            
            //check for uniqueness
            for (var i = 0 ; i < $scope.organizations.length ; i++) {
                if ($scope.organizations[i].name === value && $scope.organizations[i].id !== $scope.selectedOrganization.id) {
                    $scope.organizationEditor.$invalid = true;
                    return 'Name is already in use';
                }
            }
            
            $scope.organizationEditor.$invalid = false;
        }

        $scope.cancelClick = function() {
            $scope.selectedOrganization = null;
        };

        $scope.saveClick = function() {
            $scope.selectedOrganization.$save().then(function(data) {
                $state.params.organizationId = data.id;

                var path = $location.path();
                $location.path(path.substring(0, path.lastIndexOf('/')) + '/' + $state.params.organizationId);
            }, function() {
                $scope.$broadcast('showServerError', arguments);
            });
        };
    }]);

    organizationModule.service('OrganizationStore', [ 'CLMLocations', 'CLMResource', '$q', function(clmLocations, clmResource, $q) {
        var organizationStore = clmResource.getStore({
            id : 'id',
            url : clmLocations.getOrganizationsUrl(),
            template : { id : null, name : null }
        });

        return organizationStore;
    }]);
}());