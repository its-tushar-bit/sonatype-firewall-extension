/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxFileUpload,
  NxFormGroup,
  NxH2,
  NxInfoAlert,
  NxModal,
  NxProgressBar,
  NxStatefulForm,
  NxTextInput,
} from '@sonatype/react-shared-components';
import { equals, cond, always } from 'ramda';
import classNames from 'classnames';

import { actions } from './importSbomModalSlice';
import { selectImportSbomModalSlice } from './importSbomModalSelectors';
import { selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';

import './importSbomModal.scss';

export default function ImportSbomModal() {
  const dispatch = useDispatch();

  const {
    isModalOpen,

    file,
    uploadState,
    uploadFileProgress,

    versionId,
    componentCount,
    vulnerabilityCount,

    submitMaskState,
    submitMaskMessage,
    submitError,
  } = useSelector(selectImportSbomModalSlice);
  const applicationName = useSelector(selectSelectedOwnerName);
  const isFileUploadSuccessful = uploadState === 1;

  const closeModal = () => dispatch(actions.setIsModalOpen(false));
  const uploadFile = (file) => dispatch(actions.uploadFile(file));
  const submitImport = () => {
    if (isFileUploadSuccessful) dispatch(actions.submitImport());
  };
  const versionIdHandler = (value) => dispatch(actions.setVersionId(value));

  const progressBarLabel = cond([
    [equals(-1), always({ label: 'Upload failed', labelError: 'Upload failed' })],
    [equals(0), always({ label: `Uploading ${file.files?.[0]?.name} file` })],
    [equals(1), always({ label: 'Upload successful!', labelSuccess: 'Upload successful!' })],
  ])(uploadState);

  const progressBar = () =>
    uploadState !== null ? (
      <NxProgressBar id="import-sbom-modal-progress-bar" value={uploadFileProgress} {...progressBarLabel} />
    ) : null;

  const postUploadContent = () =>
    isFileUploadSuccessful ? (
      <>
        <NxFormGroup label="Application Name" sublabel={`SBOM linked to Application ${applicationName}`}>
          <NxTextInput
            id="import-sbom-modal-application-name-input"
            value={applicationName}
            isPristine={true}
            disabled
          />
        </NxFormGroup>

        <NxFormGroup label="Version Id" sublabel="Version value cannot be edited">
          <NxTextInput
            id="import-sbom-modal-version-id-input"
            value={versionId}
            isPristine={true}
            onChange={versionIdHandler}
            disabled
          />
        </NxFormGroup>

        <NxInfoAlert id="import-sbom-modal-info-alert" data-testid="import-sbom-modal-info-alert">
          <strong>{componentCount} components</strong> and <strong>{vulnerabilityCount} vulnerabilities</strong> will be
          included with uploaded file.
        </NxInfoAlert>
      </>
    ) : null;

  return isModalOpen ? (
    <NxModal id="import-sbom-modal" className="import-sbom-modal" onCancel={closeModal}>
      <NxStatefulForm
        id="import-sbom-modal-form"
        submitBtnClasses={classNames('import-sbom-modal__submit-button', {
          disabled: !isFileUploadSuccessful,
        })}
        onSubmit={submitImport}
        onCancel={closeModal}
        submitBtnText="Finish Import"
        submitMaskState={submitMaskState}
        submitMaskMessage={submitMaskMessage}
        submitError={submitError}
        validationErrors={undefined}
      >
        <NxModal.Header>
          <NxH2>Import SBOM for Application {applicationName}</NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxFormGroup label="Upload SBOM File" isRequired>
            <NxFileUpload data-testid="import-sbom-modal-file-upload" onChange={uploadFile} {...file} isRequired />
          </NxFormGroup>
          {progressBar()}
          {postUploadContent()}
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
}
