/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, clmBuildTimestamp*/
(function() {
  'use strict';

  function RootOrganizationMigrateModalService($modal) {
    var service = {
      openModal: openModal
    };

    function openModal() {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'clm-modal',
        controller: 'RootOrganizationMigrateModalController as vm',
        templateUrl: 'components/angular.common/root.organization.migrate.modal.html?' + clmBuildTimestamp
      }).result;
    }

    return service;
  }

  RootOrganizationMigrateModalService.$inject = ['$modal'];

  angular //
      .module('root.organization.migrate') //
      .service('RootOrganizationMigrateModalService', RootOrganizationMigrateModalService);

}());
