/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global CLM */
export default function CIPLicenseEditor() {
  return {
    templateUrl: CLM.assetsPath + 'cip/cip-license-editor.html',
    controllerAs: 'vm',
    controller: 'LicenseEditorController'
  };
}
