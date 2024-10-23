/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxCopyToClipboard,
  NxDescriptionList,
  NxErrorAlert,
  NxFileUpload,
  nxFileUploadStateHelpers,
  NxFooter,
  NxFormGroup,
  NxH2,
  NxInfoAlert,
  NxModal,
  NxP,
  NxProgressBar,
  NxTextInput,
  NxTextLink,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { always, complement, compose, is, isNil, toString, when } from 'ramda';

import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

import { actions, IMPORT_STATE } from './importSbomModalSlice';
import { selectImportSbomModalSlice } from './importSbomModalSelectors';
import { selectSelectedOwnerName } from '../orgsAndPoliciesSelectors';

import './ImportSbomModal.scss';

const POST_IMPORT_TOAST_MESSAGE =
  'The file you uploaded is currently being evaluated and will be available on this page shortly. ' +
  'Please refresh the page after few minutes to see it.';

const ensureString = compose(when(complement(is(String)), toString), when(isNil, always('')));

export default function ImportSbomModal() {
  const applicationName = useSelector(selectSelectedOwnerName);
  const dispatch = useDispatch();
  const [fileUploadState, setFileUploadState] = useState(nxFileUploadStateHelpers.initialState(null));
  const [selectedFilename, setSelectedFilename] = useState('');
  const [modalTitle, setModalTitle] = useState('Import File for Application ' + applicationName);
  // Store the File (non-serializable)
  const fileRef = useRef(null);

  const {
    isModalOpen,
    importState,
    uploadProgress,
    sbomSummary,
    errorMessage,
    validationErrors,
    scanType,
  } = useSelector(selectImportSbomModalSlice);

  useEffect(() => {
    switch (importState) {
      case IMPORT_STATE.INITIAL:
        setModalTitle('Import File for Application ' + applicationName);
        break;
      case IMPORT_STATE.UPLOADING_COMMITTING:
        setModalTitle('Import in progress...');
        break;
      case IMPORT_STATE.ERROR:
        setModalTitle(validationErrors?.length ? 'Your SBOM failed validation' : 'Error Importing SBOM');
        break;
      case IMPORT_STATE.SUMMARY:
        setModalTitle('Import completed. Evaluating...');
        break;
      default:
        setModalTitle('');
    }
  }, [importState]);

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
    setSelectedFilename(files?.[0].name);
  };

  const handleImportSBOM = () => dispatch(actions.uploadFile(fileRef.current));

  const formSubLabel = (
    <>
      <NxP>
        Supported file types: SBOMs (e.g. CycloneDX, SPDX), Files (e.g. .jar, .exe, .dll), Archives (e.g. .zip, .tar,
        .gz).{' '}
        <NxTextLink external href="https://links.sonatype.com/products/sbom/docs/supported-formats">
          Read about supported formats.
        </NxTextLink>
      </NxP>
    </>
  );

  const initialContent =
    importState === IMPORT_STATE.INITIAL || importState === IMPORT_STATE.ERROR ? (
      errorMessage ? (
        validationErrors?.length ? (
          <>
            <NxWarningAlert role="alert">{errorMessage}</NxWarningAlert>
            <NxCopyToClipboard
              label="Validation Error Details"
              content={validationErrors?.map((e) => '• ' + e).join('\n') || ''}
            />
          </>
        ) : (
          <NxErrorAlert>{errorMessage}</NxErrorAlert>
        )
      ) : (
        <NxFormGroup label="Import a file to evaluate" sublabel={formSubLabel} isRequired>
          <NxFileUpload
            {...fileUploadState}
            data-testid="import-sbom-modal-file-upload"
            onChange={handleSelectFile}
            isRequired
          />
        </NxFormGroup>
      )
    ) : null;

  const uploadingAndCommittingContent =
    importState === IMPORT_STATE.UPLOADING_COMMITTING ? (
      <NxProgressBar
        value={uploadProgress}
        showSteps
        max={10}
        variant="full"
        label={'Importing [' + selectedFilename + ']...'}
        className={'import-sbom-modal__progress-bar'}
      />
    ) : null;

  const summaryContent = () => {
    if (importState === IMPORT_STATE.SUMMARY) {
      if (scanType === 'SBOM') {
        return (
          <>
            <dl className="import-sbom-modal__application-name">
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
                <NxDescriptionList.Description
                  id="import-sbom-modal-summary-total-components"
                  data-testid="import-sbom-modal-total-components"
                >
                  {sbomSummary.totalComponents}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>
              <NxDescriptionList.Item>
                <NxDescriptionList.Term>Total Vulnerabilities:</NxDescriptionList.Term>
                <NxDescriptionList.Description
                  id="import-sbom-modal-summary-total-vulnerabilities"
                  data-testid="import-sbom-modal-total-vulnerabilities"
                >
                  {sbomSummary.totalVulnerabilities}
                </NxDescriptionList.Description>
              </NxDescriptionList.Item>
            </NxDescriptionList>

            <NxInfoAlert>
              Closing the modal will not interrupt the evaluation; it will still be in progress until completed. Once
              the evaluation is complete, you can view the SBOM in the SBOM table.
            </NxInfoAlert>
          </>
        );
      } else if (scanType === 'BINARY') {
        return (
          <>
            <NxP>
              We are now evaluating your file in the background and you can close this window safely. Refresh the page
              in a few minutes to see your new SBOM in the list.
            </NxP>

            <dl className="sbom-manager-import-sbom-modal__binary-summary">
              <dt>File</dt>
              <dd className="filename">{selectedFilename}</dd>

              <dt className="application-name-title">Application Name</dt>
              <dd className="application-name">{applicationName}</dd>
            </dl>
          </>
        );
      } else {
        return null;
      }
    } else {
      return null;
    }
  };

  return isModalOpen ? (
    <NxModal
      id="import-sbom-modal"
      className="sbom-manager-import-sbom-modal"
      aria-labelledby="import-sbom-modal-header"
      onCancel={closeModal}
    >
      <NxModal.Header>
        <NxH2 id="import-sbom-modal-header">{modalTitle}</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        {initialContent}
        {uploadingAndCommittingContent}
        {summaryContent()}
      </NxModal.Content>
      <NxFooter>
        <div className="nx-btn-bar">
          {importState !== IMPORT_STATE.UPLOADING_COMMITTING ? (
            <NxButton onClick={closeModal}>{importState === IMPORT_STATE.SUMMARY ? 'Close' : 'Cancel'}</NxButton>
          ) : null}
          {importState === IMPORT_STATE.INITIAL || importState === IMPORT_STATE.ERROR ? (
            <NxButton
              variant="primary"
              onClick={handleImportSBOM}
              disabled={!fileUploadState.files || importState === IMPORT_STATE.ERROR}
            >
              Import
            </NxButton>
          ) : null}
        </div>
      </NxFooter>
    </NxModal>
  ) : null;
}
