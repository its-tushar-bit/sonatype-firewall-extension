/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import artifactoryRepositoryTile from './artifactory.repository.tile';
import clmContextLocationModule from '../../utilAngular/CLMContextLocation';
import utilityModule from '../../utility/utility.module';

export default angular
  .module('artifactoryRepositoryModule', [clmContextLocationModule.name, utilityModule.name])
  .component('artifactoryRepositoryTile', artifactoryRepositoryTile);
