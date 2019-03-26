/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import retentionService from './retentionService';
import retentionTile from './retentionTile';
import retentionEditor from './retentionEditor';
import clmContextLocationModule from '../../util/CLMContextLocation';
import utilityModule from '../../utility/utility.module';

export default angular.module('retentionModule', [clmContextLocationModule.name, utilityModule.name])
    .service('retentionService', retentionService)
    .component('retentionTile', retentionTile)
    .component('retentionEditor', retentionEditor);
