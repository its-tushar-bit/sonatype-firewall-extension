/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
import commonServicesModule from '../util/CommonServices';
import storesModule from '../util/Stores';

angular.module('root.organization.migrate', [commonServicesModule.name, storesModule.name]);
