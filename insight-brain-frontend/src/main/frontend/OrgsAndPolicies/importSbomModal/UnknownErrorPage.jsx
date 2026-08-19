/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxButtonBar, NxErrorAlert, NxFooter, NxH2, NxModal } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';

import { selectImportSbomModalSlice } from './importSbomModalSelectors';

export default function UnknownErrorPage({ headerId, onCancel }) {
  const { errorMessage } = useSelector(selectImportSbomModalSlice);

  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>Error Importing SBOM</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxErrorAlert>{errorMessage}</NxErrorAlert>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onCancel}>Cancel</NxButton>
          <NxButton variant="primary" disabled={true}>
            Import
          </NxButton>
        </NxButtonBar>
      </NxFooter>
    </>
  );
}

UnknownErrorPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
};
