/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
import template from './cip-label-editor.html';

export default function CIPLabelEditor() {
  return {
    template,
    controllerAs: 'vm',
    controller: 'LabelsController',
  };
}
