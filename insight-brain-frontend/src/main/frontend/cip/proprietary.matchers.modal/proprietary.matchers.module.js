/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
import proprietaryMatchersService from './proprietary.matchers.service';
import proprietaryMatchersModalController from './proprietary.matchers.modal.controller';
import proprietaryMatchersModalService from './proprietary.matchers.modal';

export default angular
  .module('proprietary.matchers', ['CommonServices', 'utility.directives'])
  .service('proprietary.matchers.service', proprietaryMatchersService)
  .controller(
    'proprietary.matchers.modal.controller',
    proprietaryMatchersModalController
  )
  .service('proprietary.matchers.modal', proprietaryMatchersModalService);
