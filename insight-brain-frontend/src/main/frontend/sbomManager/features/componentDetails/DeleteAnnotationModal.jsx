/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxModal } from '@sonatype/react-shared-components';
import { transformAnalysisStatus } from './componentDetailsUtils';

export default function DeleteAnnotationModal({
  vulnerability,
  deleteAnnotationFromTable,
  deleteError,
  deleteMaskState,
  onCancel,
}) {
  return (
    <NxModal onCancel={onCancel} variant="narrow" id="delete-vex-annotation-modal">
      <NxStatefulForm
        className="nx-form"
        onSubmit={deleteAnnotationFromTable}
        submitMaskState={deleteMaskState}
        onCancel={onCancel}
        submitBtnText="Delete"
        submitError={deleteError}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Delete annotation for {vulnerability.issue}</h2>
        </header>
        <div className="nx-modal-content">
          Are you sure you want to delete &quot;{transformAnalysisStatus(vulnerability.analysisStatus)}&quot; annotation
          for {vulnerability.issue}?
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

DeleteAnnotationModal.propTypes = {
  onCancel: PropTypes.func.isRequired,
  vulnerability: PropTypes.object,
  deleteAnnotationFromTable: PropTypes.func.isRequired,
  deleteError: PropTypes.string,
  deleteMaskState: PropTypes.bool,
};
