/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import SourceControlService from './source.control.service';
import inheritableEnableDisable from './inheritableEnableDisable';
import sourceControlTile from './source.control.tile';
import sourceControlEditor from './source.control.editor';
import clmContextLocationModule from '../../utilAngular/CLMContextLocation';
import utilityModule from '../../utility/utility.module';
import UpdateSourceControlModalService from './update.source.control.modal.service';
import UpdateSourceControlModalController from './update.source.control.modal.controller';

export default angular
  .module('sourceControlModule', [clmContextLocationModule.name, utilityModule.name])
  .service('SourceControlService', SourceControlService)
  .component('inheritableEnableDisable', inheritableEnableDisable)
  .component('sourceControlTile', sourceControlTile)
  .component('sourceControlEditor', sourceControlEditor)
  .controller('UpdateSourceControlModalController', UpdateSourceControlModalController)
  .service('UpdateSourceControlModalService', UpdateSourceControlModalService);
