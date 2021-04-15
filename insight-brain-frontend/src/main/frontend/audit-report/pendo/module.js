/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as angular from 'angular';

import commonServicesModule from '../../util/CommonServices';
import clmLocationModule from '../../util/CLMLocation';
import pendoService from '../../pendo/pendoService';
import sanitizeUrlService from './sanitizeUrlService';

export default angular
  .module('auditReportPendoModule', [
    commonServicesModule.name,
    clmLocationModule.name,
  ])
  .service('pendoService', pendoService)
  .service('sanitizeUrlService', sanitizeUrlService);
