/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import InnerSourceRepositoryService from './innersource.repository.service';
import innerSourceRepositoryTile from './innersource.repository.tile';
import clmContextLocationModule from '../../utilAngular/CLMContextLocation';
import utilityModule from '../../utility/utility.module';

export default angular
  .module('innerSourceRepositoryModule', [clmContextLocationModule.name, utilityModule.name])
  .service('InnerSourceRepositoryService', InnerSourceRepositoryService)
  .component('innerSourceRepositoryTile', innerSourceRepositoryTile);
