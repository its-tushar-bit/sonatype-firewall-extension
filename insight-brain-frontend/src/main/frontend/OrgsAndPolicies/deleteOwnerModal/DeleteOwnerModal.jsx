/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';

import { selectDeleteOwnerSlice } from './deleteOwnerSelectors';
import {
  selectIsApplication,
  selectIsOrganization,
  selectIsRepositoryManager,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectTotalDescendantsCount,
  selectAllDescendants,
} from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { NxModal, NxWarningAlert, NxH2, NxFontAwesomeIcon, NxStatefulForm } from '@sonatype/react-shared-components';
import { actions } from './deleteOwnerSlice';

export default function DeleteOwnerModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError } = useSelector(selectDeleteOwnerSlice);
  const isApp = useSelector(selectIsApplication);
  const isOrganization = useSelector(selectIsOrganization);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);
  const ownerName = useSelector(selectSelectedOwnerName);
  const descendantsCount = useSelector(selectTotalDescendantsCount);
  const descendants = useSelector(selectAllDescendants);

  const closeModal = () => dispatch(actions.closeModal());
  const deleteOwner = () => dispatch(actions.removeOwner());

  useEffect(() => {
    return () => closeModal();
  }, []);

  const getDescendantsMessage = () => {
    if (isApp) {
      return '';
    }
    if (isOrganization) {
      return ` and ${descendantsCount} descendant${descendantsCount > 1 || descendantsCount === 0 ? 's' : ''}`;
    }
    if (isRepositoryManager) {
      const repoCount = descendants.repositoryIds.length;
      return ` and ${repoCount} descendant${repoCount > 1 || repoCount === 0 ? 's' : ''}`;
    }
  };

  const getHeaderTitle = () => {
    if (isApp) {
      return 'Delete Application';
    }
    if (isOrganization) {
      return 'Delete Organization';
    }
    if (isRepositoryManager) {
      return 'Delete Repository Manager';
    }
  };

  return isModalOpen ? (
    <NxModal id="owner-delete-modal" onCancel={closeModal}>
      <NxStatefulForm
        onSubmit={deleteOwner}
        onCancel={closeModal}
        submitMaskState={submitMaskState}
        submitBtnText="Delete"
        submitError={submitError}
      >
        <NxModal.Header>
          <NxH2>
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>{getHeaderTitle()}</span>
          </NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            You are about to permanently remove {ownerName}
            {getDescendantsMessage()}. This action cannot be undone.
          </NxWarningAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  ) : null;
}
