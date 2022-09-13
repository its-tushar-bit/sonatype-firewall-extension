/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectDeleteOwnerSlice } from './deleteOwnerSelectors';
import { selectIsApplication } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { NxModal, NxWarningAlert, NxH2, NxFontAwesomeIcon, NxForm } from '@sonatype/react-shared-components';
import { actions } from './deleteOwnerSlice';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';

export default function DeleteOwnerModal() {
  const dispatch = useDispatch();

  const { isModalOpen, submitMaskState, submitError } = useSelector(selectDeleteOwnerSlice);
  const isApp = useSelector(selectIsApplication);
  const ownerName = useSelector(selectSelectedOwnerName);

  const closeModal = () => dispatch(actions.closeModal());
  const deleteOwner = () => dispatch(actions.removeOwner());

  useEffect(() => {
    return () => closeModal();
  }, []);

  return isModalOpen ? (
    <NxModal id="owner-delete-modal" onCancel={closeModal}>
      <NxForm
        onSubmit={deleteOwner}
        onCancel={closeModal}
        submitMaskState={submitMaskState}
        submitBtnText="Delete"
        submitError={submitError}
      >
        <NxModal.Header>
          <NxH2>
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete {isApp ? 'Application' : 'Organization'}</span>
          </NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>
            You are about to permanently remove {ownerName}. This action cannot be undone.
          </NxWarningAlert>
        </NxModal.Content>
      </NxForm>
    </NxModal>
  ) : null;
}
