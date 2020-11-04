/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxModal,
  NxSubmitMask,
  NxWarningAlert,
  NxLoadError
} from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons/index';
import * as PropTypes from 'prop-types';

export default function DeleteFilterModal(props) {

  const {
    deleteFilter,
    hideDeleteFilterModal,
    filterToDelete,
    deleteFilterError,
    deleteFilterSaving,
    deleteFilterSuccess
  } = props;

  const handleDeleteFilter = evt => {
    if (evt) {
      evt.preventDefault();
    }

    deleteFilter(filterToDelete);
  };

  const handleCancelButtonClick = evt => {
    // needs this to keep filters dropdown open
    evt.nativeEvent.stopImmediatePropagation();
    hideDeleteFilterModal();
  };

  return (
    <NxModal id="delete-filter-modal"
             onClose={hideDeleteFilterModal}>
      <form className="nx-form" onSubmit={handleDeleteFilter} noValidate>
        { (deleteFilterSaving || deleteFilterSuccess) &&
          <NxSubmitMask message="Removing…" success={deleteFilterSuccess} />
        }
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <NxFontAwesomeIcon icon={faTrashAlt}/>
            <span>Delete Filter</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert id="delete-filter-confirmation">
            You are about to delete &quot;{filterToDelete}&quot; filter. This action can not be undone.
          </NxWarningAlert>
        </div>
        <footer className="nx-footer">
          { deleteFilterError &&
            <NxLoadError error={deleteFilterError}
                         retryHandler={handleDeleteFilter}
                         titleMessage="An error occurred deleting data." />
          }
          <div className="nx-btn-bar">
            <NxButton id="delete-filter-modal-cancel-button"
                      type="button"
                      onClick={handleCancelButtonClick}>
              Cancel
            </NxButton>
            { !deleteFilterError &&
              <NxButton variant="primary" id="delete-filter-modal-continue-button" type="submit">
                Continue
              </NxButton>
            }
          </div>
        </footer>
      </form>
    </NxModal>
  );
}

DeleteFilterModal.propTypes = {
  deleteFilter: PropTypes.func.isRequired,
  hideDeleteFilterModal: PropTypes.func.isRequired,
  filterToDelete: PropTypes.string,
  deleteFilterError: PropTypes.string,
  deleteFilterSaving: PropTypes.bool,
  deleteFilterSuccess: PropTypes.bool
};
