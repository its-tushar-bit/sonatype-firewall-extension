/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxDescriptionList,
  NxErrorAlert,
  NxFileUpload,
  nxFileUploadStateHelpers,
  NxFooter,
  NxFormGroup,
  NxH2,
  NxInfoAlert,
  NxModal,
  NxProgressBar,
  NxTextInput,
} from '@sonatype/react-shared-components';
import { always, complement, compose, is, isNil, toString, when } from 'ramda';

import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

import { actions } from './importSbomModalSlice';
import { selectImportSbomModalSlice } from './importSbomModalSelectors';
import { selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';
import { IMPORT_STATE } from './importSbomModalSlice';

import './ImportSbomModal.scss';

const POST_IMPORT_TOAST_MESSAGE =
  'SBOM is currently being evaluated and will be available in the SBOM table shortly.\n' +
  'Please refresh the page after few minutes to see newly imported SBOM.';

const ensureString = compose(when(complement(is(String)), toString), when(isNil, always('')));

export default function ImportSbomModal() {
  const dispatch = useDispatch();
  const [fileUploadState, setFileUploadState] = useState(nxFileUploadStateHelpers.initialState(null));
  // Store the File (non-serializable)
  const fileRef = useRef(null);

  const { isModalOpen, importState, uploadProgress, sbomSummary, errorMessage } = useSelector(
    selectImportSbomModalSlice
  );
  const applicationName = useSelector(selectSelectedOwnerName);

  const closeModal = () => {
    if (importState === IMPORT_STATE.SUMMARY) {
      dispatch(toastActions.addToast({ type: 'info', message: POST_IMPORT_TOAST_MESSAGE }));
    }
    fileRef.current = null;
    setFileUploadState(nxFileUploadStateHelpers.initialState(null));
    dispatch(actions.reset());
  };

  const handleSelectFile = (files) => {
    if (!isNil(files?.[0])) {
      fileRef.current = files?.[0];
    }
    setFileUploadState(nxFileUploadStateHelpers.userInput(files));
  };

  const handleImportSBOM = () => dispatch(actions.uploadFile(fileRef.current));

  const initialOrErrorImportState = [IMPORT_STATE.INITIAL, IMPORT_STATE.ERROR].includes(importState);
  const initialContent = initialOrErrorImportState ? (
    <>
      <NxFormGroup label="Upload SBOM File" isRequired>
        <NxFileUpload
          {...fileUploadState}
          data-testid="import-sbom-modal-file-upload"
          onChange={handleSelectFile}
          isRequired
        />
      </NxFormGroup>
      {errorMessage ? <NxErrorAlert>{errorMessage}</NxErrorAlert> : null}
    </>
  ) : null;

  const uploadingAndCommittingContent =
    importState === IMPORT_STATE.UPLOADING_COMMITTING ? (
      <NxProgressBar value={uploadProgress} showSteps max={10} variant="full" label="Uploading..." />
    ) : null;

  const summaryContent =
    importState === IMPORT_STATE.SUMMARY ? (
      <>
        <dl className="sbom-manager-import-sbom-modal__application-name">
          <dt>Application Name</dt>
          <dd>{applicationName}</dd>
        </dl>

        <NxFormGroup
          label="Version Id"
          sublabel="The import time is used when the version id cannot be located in the file."
        >
          <NxTextInput
            name="version-id"
            title="Version Id"
            value={ensureString(sbomSummary.versionId)}
            isPristine={true}
            disabled
          />
        </NxFormGroup>

        <NxDescriptionList>
          <NxDescriptionList.Item>
            <NxDescriptionList.Term>Total Components:</NxDescriptionList.Term>
            <NxDescriptionList.Description data-testid="import-sbom-modal-total-components">
              {sbomSummary.totalComponents}
            </NxDescriptionList.Description>
          </NxDescriptionList.Item>
          <NxDescriptionList.Item>
            <NxDescriptionList.Term>Total Vulnerabilities:</NxDescriptionList.Term>
            <NxDescriptionList.Description data-testid="import-sbom-modal-total-vulnerabilities">
              {sbomSummary.totalVulnerabilities}
            </NxDescriptionList.Description>
          </NxDescriptionList.Item>
        </NxDescriptionList>

        <NxInfoAlert>
          Closing the modal will not interrupt the evaluation; it will still be in progress until completed. Once the{' '}
          evaluation is complete, you can view the SBOM in the SBOM table.
        </NxInfoAlert>
      </>
    ) : null;

  return isModalOpen ? (
    <NxModal id="import-sbom-modal" className="sbom-manager-import-sbom-modal" onCancel={closeModal}>
      <NxModal.Header>
        <NxH2>Import SBOM for Application {applicationName}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        {initialContent}
        {uploadingAndCommittingContent}
        {summaryContent}
      </NxModal.Content>
      <NxFooter>
        <div className="nx-btn-bar">
          {importState !== IMPORT_STATE.UPLOADING_COMMITTING ? (
            <NxButton onClick={closeModal}>{importState === IMPORT_STATE.SUMMARY ? 'Close' : 'Cancel'}</NxButton>
          ) : null}
          {initialOrErrorImportState ? (
            <NxButton variant="primary" onClick={handleImportSBOM} disabled={!fileUploadState.files}>
              Import SBOM
            </NxButton>
          ) : null}
        </div>
      </NxFooter>
    </NxModal>
  ) : null;
}
