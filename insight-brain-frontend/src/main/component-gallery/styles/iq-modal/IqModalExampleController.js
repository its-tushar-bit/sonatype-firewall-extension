/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function IqModalExampleController($modal) {
  this.openModal = function() {
    $modal.open({
      templateUrl: 'styles/iq-modal/modal-content.html',
      windowClass: 'iq-modal', // NOTE: this is how the iq-modal CSS class gets added to the window
      backdropClass: 'iq-modal-backdrop'
    });
  };
}
