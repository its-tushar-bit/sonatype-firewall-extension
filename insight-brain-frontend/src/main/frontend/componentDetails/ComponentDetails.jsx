/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

export default function ComponentDetails({
  selectedComponent,
  publicId,
  scanId,
  unknownjs,
  hash,
  loadReportAndSelectComponentByHash,
}) {
  useEffect(() => {
    if (!selectedComponent) {
      loadReportAndSelectComponentByHash(publicId, scanId, hash, unknownjs);
    }
  }, [selectedComponent, publicId, scanId, hash, unknownjs]);

  return (
    <main className="nx-page-main nx-viewport-sized">
      {selectedComponent && <h1>{selectedComponent.derivedComponentName}</h1>}
    </main>
  );
}

ComponentDetails.propTypes = {
  loadReportAndSelectComponentByHash: PropTypes.func.isRequired,
  selectedComponent: PropTypes.object,
  unknownjs: PropTypes.bool,
  // the following 3 should be required but marking them as such causes proptype errors when navigating away
  hash: PropTypes.string,
  publicId: PropTypes.string,
  scanId: PropTypes.string,
};
