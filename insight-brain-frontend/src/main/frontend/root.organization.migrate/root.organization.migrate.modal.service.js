/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './root.organization.migrate.modal.html';

/*global angular, clmBuildTimestamp*/
export default function RootOrganizationMigrateModalService(Modal) {
  var service = {
    openModal: openModal
  };

  function openModal() {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'RootOrganizationMigrateModalController as vm',
      template
    }).result;
  }

  return service;
}

RootOrganizationMigrateModalService.$inject = ['Modal'];
