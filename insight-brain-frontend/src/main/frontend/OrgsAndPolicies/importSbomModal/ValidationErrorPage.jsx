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
  NxCheckbox,
  NxCopyToClipboard,
  NxErrorAlert,
  NxFooter,
  NxH2,
  NxModal,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { concat } from 'ramda';

import { selectImportSbomModalSlice } from './importSbomModalSelectors';
import { actions } from './importSbomModalSlice';

export default function ValidationErrorPage({ headerId, onCancel }) {
  const { errorMessage, validationErrors, isSkipValidation, isValidationErrorIgnorable } = useSelector(
    selectImportSbomModalSlice
  );

  const dispatch = useDispatch();
  const importAnyway = () => {
    dispatch(actions.versionConfirm());
  };

  const handleSkipValidation = () => {
    dispatch(actions.setIsSkipValidation(!isSkipValidation));
  };

  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>Your SBOM failed validation</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        {isValidationErrorIgnorable ? (
          <NxWarningAlert role="alert">{errorMessage}</NxWarningAlert>
        ) : (
          <NxErrorAlert>{errorMessage}</NxErrorAlert>
        )}
        <NxCopyToClipboard
          label="Validation Error Details"
          content={validationErrors?.map(concat('• '))?.join('\n') ?? ''}
        />
        {isValidationErrorIgnorable && (
          <NxCheckbox onChange={handleSkipValidation} isChecked={isSkipValidation}>
            Skip validation and import anyway
          </NxCheckbox>
        )}
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onCancel}>Cancel</NxButton>
          {isValidationErrorIgnorable ? (
            <NxButton variant="primary" disabled={!isSkipValidation} onClick={importAnyway}>
              Import Anyway
            </NxButton>
          ) : (
            <NxButton
              variant="primary"
              className="disabled"
              title="Import cannot proceed due to a critical error in the file. Please correct the file and try again."
            >
              Import
            </NxButton>
          )}
        </NxButtonBar>
      </NxFooter>
    </>
  );
}

ValidationErrorPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
};
