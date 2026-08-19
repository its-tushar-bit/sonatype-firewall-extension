/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useSelector } from 'react-redux';
import { selectSbomExportSlice } from 'MainRoot/sbomManager/features/sbomExport/sbomExportSelectors';
import { NxStatefulSubmitMask } from '@sonatype/react-shared-components';
import React from 'react';

export default function ExportModalSubmitMask() {
  const {
    exportAndDownloadSbomSubmitMask: { success, showSubmitMask },
  } = useSelector(selectSbomExportSlice);

  if (showSubmitMask) {
    return (
      <NxStatefulSubmitMask
        success={success}
        successMessage="SBOM export completed successfully!"
        message="SBOM export in progress…"
      />
    );
  }
  return null;
}
