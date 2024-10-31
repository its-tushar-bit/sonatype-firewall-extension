/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxButton,
  NxButtonBar,
  NxCopyToClipboard,
  NxErrorAlert,
  NxFooter,
  NxH2,
  NxModal,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';
import { concat } from 'ramda';

import { selectImportSbomModalSlice } from './importSbomModalSelectors';

export default function ValidationErrorPage({ headerId, onCancel }) {
  const { errorMessage, validationErrors } = useSelector(selectImportSbomModalSlice);

  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>Your SBOM failed validation</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxWarningAlert role="alert">{errorMessage}</NxWarningAlert>
        <NxCopyToClipboard
          label="Validation Error Details"
          content={validationErrors?.map(concat('• '))?.join('\n') ?? ''}
        />
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

ValidationErrorPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
};
