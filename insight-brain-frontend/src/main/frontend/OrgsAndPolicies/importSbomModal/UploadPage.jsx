/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxErrorAlert,
  NxFileUpload,
  NxFooter,
  NxFormGroup,
  NxH2,
  NxModal,
  NxStatefulForm,
  NxTextLink,
} from '@sonatype/react-shared-components';

import { selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';
import { selectImportSbomModalSlice, selectUploadValidationErrors } from './importSbomModalSelectors';
import { actions, IMPORT_STATE } from './importSbomModalSlice';

export default function UploadPage({ headerId, onCancel }) {
  const dispatch = useDispatch();
  const { fileInputState, evaluationError } = useSelector(selectImportSbomModalSlice);
  const applicationName = useSelector(selectSelectedOwnerName);
  const validationErrors = useSelector(selectUploadValidationErrors);
  const setSelectedFile = (fileList) => {
      dispatch(actions.setSelectedFile(fileList));
    },
    uploadFile = () => {
      dispatch(actions.uploadFile());
    };

  const fileUploadSublabel = (
    <>
      Supported file types: SBOMs (e.g. CycloneDX, SPDX), Files (e.g. .jar, .exe, .dll), Archives (e.g. .zip, .tar,
      .gz).{' '}
      <NxTextLink external href="https://links.sonatype.com/products/sbom/docs/supported-formats">
        Read about supported formats.
      </NxTextLink>
    </>
  );

  const errorAlert = evaluationError ? (
    <NxFooter>
      <NxErrorAlert>
        We were unable to process your SBOM:{' '}
        {evaluationError.trim().endsWith('.') ? evaluationError : `${evaluationError}.`} Please re-import your SBOM.
      </NxErrorAlert>
    </NxFooter>
  ) : null;

  return (
    <NxStatefulForm
      onSubmit={uploadFile}
      submitBtnText="Import"
      onCancel={onCancel}
      validationErrors={validationErrors}
    >
      <NxModal.Header>
        <NxH2 id={headerId}>Import File for Application {applicationName}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxFormGroup label="Import a file to evaluate" sublabel={fileUploadSublabel} isRequired>
          <NxFileUpload {...fileInputState} onChange={setSelectedFile} isRequired />
        </NxFormGroup>
      </NxModal.Content>
      {errorAlert}
    </NxStatefulForm>
  );
}

UploadPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
};
