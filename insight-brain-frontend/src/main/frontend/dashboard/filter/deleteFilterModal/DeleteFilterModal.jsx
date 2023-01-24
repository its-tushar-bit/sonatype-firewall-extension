/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { NxFontAwesomeIcon, NxModal, NxWarningAlert, NxStatefulForm } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons/index';
import * as PropTypes from 'prop-types';
import useEscapeKeyStack from '../../../react/useEscapeKeyStack';

export default function DeleteFilterModal(props) {
  const { deleteFilter, hideDeleteFilterModal, filterToDelete, deleteFilterError, deleteFilterMaskState } = props;

  useEscapeKeyStack(filterToDelete != null, hideDeleteFilterModal);

  const handleDeleteFilter = (evt) => {
    if (evt) {
      evt.preventDefault();
    }

    deleteFilter(filterToDelete);
  };

  return filterToDelete ? (
    <NxModal id="delete-filter-modal">
      <NxStatefulForm
        className="nx-form"
        onSubmit={handleDeleteFilter}
        onCancel={hideDeleteFilterModal}
        submitBtnText="Continue"
        submitError={deleteFilterError}
        submitErrorTitleMessage="An error occurred deleting data."
        submitMaskState={deleteFilterMaskState}
        submitMaskMessage="Removing…"
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Delete Filter</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert id="delete-filter-confirmation">
            You are about to delete &quot;{filterToDelete}&quot; filter. This action can not be undone.
          </NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  ) : null;
}

DeleteFilterModal.propTypes = {
  deleteFilter: PropTypes.func.isRequired,
  hideDeleteFilterModal: PropTypes.func.isRequired,
  filterToDelete: PropTypes.string,
  deleteFilterError: PropTypes.string,
  deleteFilterMaskState: PropTypes.bool,
};
