/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxModal } from '@sonatype/react-shared-components';

import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

import { actions, IMPORT_STATE } from './importSbomModalSlice';
import { selectImportSbomModalSlice } from './importSbomModalSelectors';
import UploadPage from './UploadPage';
import UploadProgressPage from './UploadProgressPage';
import ValidationErrorPage from './ValidationErrorPage';
import UnknownErrorPage from './UnknownErrorPage';
import SbomSummaryPage from './SbomSummaryPage';
import BinarySummaryPage from './BinarySummaryPage';
import VersionConfirmPage from 'MainRoot/OrgsAndPolicies/importSbomModal/VersionConfirmPage';

const POST_IMPORT_TOAST_MESSAGE =
  'The file you uploaded is currently being evaluated and will be available on this page shortly. ' +
  'Please refresh the page after few minutes to see it.';

const headerId = 'import-sbom-modal-header';

export default function ImportSbomModal() {
  const dispatch = useDispatch();

  const { isModalOpen, importState, validationErrors, scanType } = useSelector(selectImportSbomModalSlice);

  const closeModal = () => {
    if (importState === IMPORT_STATE.SUMMARY) {
      dispatch(toastActions.addToast({ type: 'info', message: POST_IMPORT_TOAST_MESSAGE }));
    }
    dispatch(actions.reset());
  };

  let page = null;
  switch (importState) {
    case IMPORT_STATE.INITIAL:
      page = <UploadPage headerId={headerId} onCancel={closeModal} />;
      break;
    case IMPORT_STATE.UPLOADING:
      page = <UploadProgressPage headerId={headerId} />;
      break;
    case IMPORT_STATE.VERSION_CONFIRM:
      page = <VersionConfirmPage headerId={headerId} onCancel={closeModal} />;
      break;
    case IMPORT_STATE.ERROR:
      if (validationErrors?.length) {
        page = <ValidationErrorPage headerId={headerId} onCancel={closeModal} />;
      } else {
        page = <UnknownErrorPage headerId={headerId} onCancel={closeModal} />;
      }
      break;
    case IMPORT_STATE.SUMMARY:
      switch (scanType) {
        case 'SBOM':
          page = <SbomSummaryPage headerId={headerId} onClose={closeModal} />;
          break;
        case 'BINARY':
          page = <BinarySummaryPage headerId={headerId} onClose={closeModal} />;
          break;
      }
      break;
  }

  return isModalOpen ? (
    <NxModal
      id="import-sbom-modal"
      className="sbom-manager-import-sbom-modal"
      aria-labelledby={headerId}
      onCancel={closeModal}
    >
      {page}
    </NxModal>
  ) : null;
}
