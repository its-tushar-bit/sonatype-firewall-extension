/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as angular from 'angular';

import clmLocationModule from '../util/CLMLocation';
import commonServicesModule from '../utilAngular/CommonServices';
import pendoService from './pendoService';
import sanitizeUrlService from './sanitizeUrlService';

export default angular
  .module('pendoModule', ['ui.router', clmLocationModule.name, commonServicesModule.name])
  .service('pendoService', pendoService)
  .service('sanitizeUrlService', sanitizeUrlService);
