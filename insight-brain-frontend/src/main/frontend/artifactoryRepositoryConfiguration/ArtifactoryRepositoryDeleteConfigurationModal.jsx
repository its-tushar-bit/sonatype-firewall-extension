/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxStatefulForm, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';

import { actions } from './artifactoryRepositoryDeleteConfigurationModalSlice';

import { selectArtifactoryRepositoryDeleteConfigurationModalSlice } from './artifactoryRepositoryDeleteConfigurationModalSelectors';

import { useDispatch, useSelector } from 'react-redux';

export default function ArtifactoryRepositoryDeleteConfigurationModal() {
  const dispatch = useDispatch();

  const { deleteConfigurationError, deleteSubmitMaskState, showModal } = useSelector(
    selectArtifactoryRepositoryDeleteConfigurationModalSlice
  );

  const hideModal = () => dispatch(actions.hideDeleteConfigurationModal());
  const deleteConfiguration = () => dispatch(actions.deleteConfiguration());

  return (
    showModal && (
      <NxModal
        id="artifactory-repository-configuration-delete-modal"
        onCancel={() => hideModal()}
        aria-labelledby="artifactory-repository-configuration-delete-modal-header"
      >
        <NxStatefulForm
          onSubmit={deleteConfiguration}
          onCancel={() => hideModal()}
          submitError={deleteConfigurationError}
          submitBtnText="OK"
          submitErrorTitleMessage="Unable to delete the configured repository."
          submitMaskState={deleteSubmitMaskState}
          submitMaskMessage="Deleting Configuration"
        >
          <header className="nx-modal-header">
            <h2 className="nx-h2" id="artifactory-repository-configuration-delete-modal-header">
              Delete Repository Configuration?
            </h2>
          </header>
          <div className="nx-modal-content">
            <NxWarningAlert>This will disable querying your configured repository.</NxWarningAlert>
          </div>
        </NxStatefulForm>
      </NxModal>
    )
  );
}
