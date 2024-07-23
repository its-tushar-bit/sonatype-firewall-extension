/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxModal, NxStatefulForm } from '@sonatype/react-shared-components';
import { transformAnalysisStatus } from './componentDetailsUtils';

export default function CopyAnnotationModal({
  vulnerability,
  copyAnnotationFromTable,
  copyError,
  copyMaskState,
  onCancel,
}) {
  return (
    <NxModal onCancel={onCancel} variant="narrow" id="copy-vex-annotation-modal">
      <NxStatefulForm
        className="nx-form"
        onSubmit={copyAnnotationFromTable}
        submitMaskState={copyMaskState}
        onCancel={onCancel}
        submitBtnText="Copy"
        submitError={copyError}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Copy annotation for {vulnerability.issue}</h2>
        </header>
        <div className="nx-modal-content">
          Are you sure you want to copy &quot;
          {transformAnalysisStatus(vulnerability.latestPreviousAnnotation.analysisStatus)}&quot; annotation for{' '}
          {vulnerability.issue} from previous version {vulnerability.latestPreviousAnnotation.sbomVersion}?
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

CopyAnnotationModal.propTypes = {
  onCancel: PropTypes.func.isRequired,
  vulnerability: PropTypes.object,
  copyAnnotationFromTable: PropTypes.func.isRequired,
  copyError: PropTypes.string,
  copyMaskState: PropTypes.bool,
};
