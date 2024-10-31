/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxH2, NxModal, NxProgressBar } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';

import { selectImportSbomModalSlice } from './importSbomModalSelectors';

export default function UploadProgressPage({ headerId }) {
  const { uploadProgress, fileInputState } = useSelector(selectImportSbomModalSlice),
    selectedFilename = fileInputState?.files[0]?.name;

  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>Import in progress…</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxProgressBar
          className="import-sbom-modal__progress-bar"
          value={uploadProgress}
          showSteps
          max={10}
          variant="full"
          label={`Importing${selectedFilename ? ` [${selectedFilename}]` : ''}…`}
        />
      </NxModal.Content>
    </>
  );
}

UploadProgressPage.propTypes = {
  headerId: PropTypes.string.isRequired,
};
