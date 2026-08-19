/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import PropTypes from 'prop-types';

import { useSelector } from 'react-redux';
import { NxStatefulForm, NxH2, NxModal, NxFontAwesomeIcon, NxWarningAlert } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { isEmpty } from 'ramda';

import { selectUnSortedAddedUsers, selectOwnerType, selectAccessSlice } from './accessSelectors';
import { selectIsRepositoryContainer } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function DeleteAccessModal({ removeRole, closeDeleteModal }) {
  const accessProps = useSelector(selectAccessSlice);
  const { role, deleteMaskState, deleteError } = accessProps;
  const ownerType = useSelector(selectOwnerType);
  const isRepositoryContainer = useSelector(selectIsRepositoryContainer);
  const addedUsers = useSelector(selectUnSortedAddedUsers);
  const deleteRoleModalOwnerText = isRepositoryContainer ? `${ownerType}` : `this ${ownerType}`;
  const deleteRoleModalText = `
      You are about to remove the ${role.roleName} role from ${deleteRoleModalOwnerText}.
 `;
  const deleteRoleFromUpdateModalText = `
      You are about to remove the ${role.roleName} role from ${deleteRoleModalOwnerText}. Next time, consider using the
      "Delete" button; it will save you some clicks!
  `;

  return (
    <NxModal id="role-config-delete-modal" aria-labelledby="role-config-delete-modal-header" onClose={closeDeleteModal}>
      <NxStatefulForm
        onSubmit={removeRole}
        submitMaskState={deleteMaskState}
        onCancel={closeDeleteModal}
        submitBtnText="Continue"
        submitMaskMessage="Deleting…"
        submitError={deleteError}
      >
        <NxModal.Header>
          <NxH2 id="role-config-delete-modal-header">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete Role</span>
          </NxH2>
        </NxModal.Header>
        <NxModal.Content>
          <NxWarningAlert>{isEmpty(addedUsers) ? deleteRoleFromUpdateModalText : deleteRoleModalText}</NxWarningAlert>
        </NxModal.Content>
      </NxStatefulForm>
    </NxModal>
  );
}

DeleteAccessModal.propTypes = {
  removeRole: PropTypes.func,
  closeDeleteModal: PropTypes.func,
};
