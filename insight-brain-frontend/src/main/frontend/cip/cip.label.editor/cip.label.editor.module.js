/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
import currentLabelDataService from './current.label.data.service';
import labelAddController from './label.add.controller';
import labelRemoveController from './label.remove.controller';
import labelsController from './labels.controller';
import cipLabelEditor from './cip.label.editor.directive';
import labelModificationService from './label.modification.service';

export default angular
  .module('cip.label.editor', [
    'CommonServices',
    'HttpInterceptors',
    'UnauthenticatedResponseHttpInterceptor',
    'ui.bootstrap',
  ])
  .service('CurrentLabelData', currentLabelDataService)
  .controller('LabelAddController', labelAddController)
  .controller('LabelRemoveController', labelRemoveController)
  .controller('LabelsController', labelsController)
  .directive('cipLabelEditor', cipLabelEditor)
  .service('LabelModification', labelModificationService);
