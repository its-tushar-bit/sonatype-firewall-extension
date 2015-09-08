/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, clmBuildTimestamp */
(function() {
  'use strict';

  angular.module('OrganizationModule', ['ui.router', 'ManagementModule', 'Stores', 'OwnerModule'], [
    '$stateProvider', function($stateProvider) {
      $stateProvider.state('management.organization-view', {
        parent: 'management',
        url: '/organization/{organizationId}',
        templateUrl: 'components/owner-summary-view.html?' + clmBuildTimestamp
      }).state('management.organization.view.policies', {
        parent: 'management.organization.view',
        url: '/policies',
        controller: 'PolicyController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy/components/policy/policy.html?' + clmBuildTimestamp
      }).state('management.organization.view.policies.new', {
        parent: 'management.organization.view.policies',
        url: '/new',
        controller: 'PolicyController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy/components/policy/policy.html?' + clmBuildTimestamp
      }).state('management.organization.view.labels', {
        parent: 'management.organization.view',
        url: '/labels',
        controller: 'LabelController',
        templateUrl: '../policy/components/label-editor/labels.html?' + clmBuildTimestamp
      }).state('management.organization.view.labels.new', {
        parent: 'management.organization.view.labels',
        url: '/new',
        controller: 'LabelController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy/components/label-editor/labels.html?' + clmBuildTimestamp
      }).state('management.organization.view.licenses', {
        parent: 'management.organization.view',
        url: '/licenses',
        controller: 'LicenseThreatGroupController',
        templateUrl: '../policy/components/license-threat-group/license-threat-group.html?' +
            clmBuildTimestamp
      }).state('management.organization.view.licenses.new', {
        parent: 'management.organization.view.licenses',
        url: '/new',
        controller: 'LicenseThreatGroupController',
        data: {
          passThroughAlerts: []
        },
        templateUrl: '../policy/components/license-threat-group/license-threat-group.html?' +
        clmBuildTimestamp
      }).state('management.organization.view.security', {
        parent: 'management.organization.view',
        url: '/security',
        controller: 'AppSecurityController',
        templateUrl: '../policy/components/app-security/app-security.html?' + clmBuildTimestamp,
        resolve : {
          isAuthorized : function () {
            return true;
          }
        }
      }).state('management.organization.view.tags', {
        parent: 'management.organization.view',
        url: '/tags',
        controller: 'TagController',
        templateUrl: '../policy/components/tag-editor/tags.html?' + clmBuildTimestamp
      }).state('management.organization.view.tags.new', {
        parent: 'management.organization.view.tags',
        url: '/new',
        controller: 'TagController',
        templateUrl: '../policy/components/tag-editor/tags.html?' + clmBuildTimestamp
      });
    }
  ]);
}());
