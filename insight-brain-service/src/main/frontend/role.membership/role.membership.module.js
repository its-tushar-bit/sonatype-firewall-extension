/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import commonServicesModule from '../util/CommonServices';
import CLMAppLocationModule from '../util/CLMAppLocation';

export default angular.module('role.membership.module', [CLMAppLocationModule.name, commonServicesModule.name]);
