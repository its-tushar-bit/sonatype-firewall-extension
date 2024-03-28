/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxModal } from '@sonatype/react-shared-components';

export default function DeleteModal({
  sbom,
  deleteSbomFromTable,
  deleteError,
  deleteMaskState,
  onCancel,
  applicationName,
}) {
  return (
    <NxModal onCancel={onCancel} variant="narrow" id="delete-sbom-version-modal">
      <NxStatefulForm
        className="nx-form"
        onSubmit={() => deleteSbomFromTable(sbom.applicationVersion)}
        submitMaskState={deleteMaskState}
        onCancel={onCancel}
        submitBtnText="Delete"
        submitError={deleteError}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Delete SBOM for application {applicationName}</h2>
        </header>
        <div className="nx-modal-content">Are you sure you want to delete {sbom.applicationVersion}?</div>
      </NxStatefulForm>
    </NxModal>
  );
}

DeleteModal.propTypes = {
  onCancel: PropTypes.func.isRequired,
  sbom: PropTypes.object,
  deleteSbomFromTable: PropTypes.func.isRequired,
  deleteError: PropTypes.string,
  deleteMaskState: PropTypes.bool,
  applicationName: PropTypes.string.isRequired,
};
