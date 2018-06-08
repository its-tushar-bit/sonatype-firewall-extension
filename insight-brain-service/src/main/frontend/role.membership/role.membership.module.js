/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import commonServicesModule from '../util/CommonServices';
import CLMAppLocationModule from '../util/CLMAppLocation';
import RoleMembershipDirective from './role.membership.directive';
import RoleMembershipController from './role.membership.controller';

export default angular.module('role.membership.module', [CLMAppLocationModule.name, commonServicesModule.name])
    .directive('roleMembership', RoleMembershipDirective)
    .controller('role.membership.controller', RoleMembershipController);
