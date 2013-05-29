/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved. Includes
 *          the third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular, $, clmBuildTimestamp, window */
(function() {
    'use strict';

    var organizationModule = angular.module('Organization', [ 'ui.compat' ]);

    organizationModule.controller('OrganizationController', function($scope, $state, $timeout) {
        function switchOrganization() {
            $scope.selectedOrganization = null;
            if ('_new_' == $scope.$state.params.organizationId) {
                $scope.selectedOrganization = {
                    id : null,
                    name : null
                };
            }
            if ($scope.$state.params.organizationId !== null && $scope.organizations) {
                for ( var i = 0; i < $scope.organizations.length; i++) {
                    if ($scope.$state.params.organizationId === $scope.organizations[i].id) {
                        $scope.selectedOrganization = $scope.organizations[i];
                        return;
                    }
                }
            }
        }

        $scope.$state = $state;

        // TODO: need to load store
        $scope.organizations = [ {
            name : 'a',
            id : '1'
        }, {
            name : 'b',
            id : '2'
        }, {
            name : 'c',
            id : '3'
        }, {
            name : 'd',
            id : '4'
        } ];
        
        switchOrganization();
        
        $scope.$watch('$state.params.organizationId', switchOrganization);
    });

    organizationModule.controller('OrganizationEditorController', function($scope, $state) {
        $scope.$state = $state;
    });
}());