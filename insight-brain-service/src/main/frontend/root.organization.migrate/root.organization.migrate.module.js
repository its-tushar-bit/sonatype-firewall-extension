/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import commonServicesModule from '../util/CommonServices';
import storesModule from '../util/Stores';
import RootOrganizationMigrateDirective from './root.organization.migrate.directive';
import RootOrganizationMigrateModalController from './root.organization.migrate.modal.controller';
import RootOrganizationMigrateModalService from './root.organization.migrate.modal.service';

export default angular.module('root.organization.migrate', [commonServicesModule.name, storesModule.name])
    .directive('rootOrganizationMigrate', RootOrganizationMigrateDirective)
    .controller('RootOrganizationMigrateModalController', RootOrganizationMigrateModalController)
    .service('RootOrganizationMigrateModalService', RootOrganizationMigrateModalService);
