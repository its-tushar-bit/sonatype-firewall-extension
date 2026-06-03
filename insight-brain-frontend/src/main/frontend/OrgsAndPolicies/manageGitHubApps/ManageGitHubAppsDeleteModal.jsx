/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxModal, NxH2, NxP, NxWarningAlert, NxFooter, NxButton } from '@sonatype/react-shared-components';
import { selectDeleteModal } from './manageGitHubAppsSelectors';
import { closeDeleteModal, deleteGitHubApp } from './manageGitHubAppsSlice';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function ManageGitHubAppsDeleteModal() {
  const dispatch = useDispatch();
  const { isOpen, app, isDeleting } = useSelector(selectDeleteModal);
  const owner = useSelector(selectSelectedOwner);

  if (!isOpen || !app) {
    return null;
  }

  const handleConfirm = () => {
    dispatch(deleteGitHubApp({ githubAppId: app.id, ownerId: owner.id }));
  };

  const handleCancel = () => {
    dispatch(closeDeleteModal());
  };

  return (
    <NxModal onCancel={handleCancel} aria-labelledby="delete-github-app-modal-header">
      <NxModal.Header>
        <NxH2 id="delete-github-app-modal-header">Remove GitHub App configuration?</NxH2>
      </NxModal.Header>
      <NxModal.Content>
        <NxWarningAlert>
          Are you sure you want to remove {app.slug} from Lifecycle? This action cannot be undone.
        </NxWarningAlert>
        <NxP>
          This removes the GitHub App configuration from Lifecycle. To fully remove the integration, you may also need
          to delete or uninstall the GitHub App in GitHub.
        </NxP>
      </NxModal.Content>
      <NxFooter>
        <NxButton onClick={handleCancel}>Cancel</NxButton>
        <NxButton variant="primary" onClick={handleConfirm} disabled={isDeleting}>
          {isDeleting ? 'Deleting...' : 'Confirm Deletion'}
        </NxButton>
      </NxFooter>
    </NxModal>
  );
}
