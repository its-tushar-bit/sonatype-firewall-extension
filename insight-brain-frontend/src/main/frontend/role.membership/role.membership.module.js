/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import commonServicesModule from '../util/CommonServices';
import CLMContextLocationModule from '../util/CLMContextLocation';
import RoleMembershipDirective from './role.membership.directive';
import RoleMembershipController from './role.membership.controller';

export default angular
  .module('role.membership.module', [CLMContextLocationModule.name, commonServicesModule.name])
  .directive('roleMembership', RoleMembershipDirective)
  .controller('role.membership.controller', RoleMembershipController);
