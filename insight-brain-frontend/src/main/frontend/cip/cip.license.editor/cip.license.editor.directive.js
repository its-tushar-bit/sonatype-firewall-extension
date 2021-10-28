/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './cip-license-editor.html';

/*global CLM */
export default function cipLicenseEditorDirective() {
  return {
    template,
    controllerAs: 'vm',
    controller: 'LicenseEditorController',
    scope: {
      closeCipModal: '&',
    },
  };
}
