/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp, window */
(function() {
    'use strict';

    var organizationModule = angular.module('Organization', [ 'AngularCommon', 'ui.compat', 'CLMLocation' ]);

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
	                        $scope.selectedOrganization = $scope.organizations[i];
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

    organizationModule.controller('OrganizationEditorController', [ '$scope', '$state', '$location', function($scope, $state, $location) {
        $scope.$state = $state;

        $scope.cancelOrgClick = function() {
            $scope.selectedOrganization = null;
        };

        $scope.saveOrgClick = function() {
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