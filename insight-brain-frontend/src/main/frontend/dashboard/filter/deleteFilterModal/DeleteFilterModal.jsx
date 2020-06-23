/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';

import {
  NxButton,
  NxErrorAlert,
  NxFontAwesomeIcon,
  NxModal,
  NxSubmitMask,
  NxWarningAlert
} from '@sonatype/react-shared-components';
import { faTrashAlt, faSync } from '@fortawesome/free-solid-svg-icons/index';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

export default function DeleteFilterModal(props) {

  const {
    deleteFilter,
    hideDeleteFilterModal,
    filterToDelete,
    deleteFilterError,
    deleteFilterSaving,
    deleteFilterSuccess
  } = props;

  const handleDeleteFilter = event => {
    event.preventDefault();
    deleteFilter(filterToDelete);
  };

  const handleCancelButtonClick = event => {
    // needs this to keep filters dropdown open
    event.nativeEvent.stopImmediatePropagation();
    hideDeleteFilterModal();
  };

  return (
    <NxModal id="delete-filter-modal"
             onClose={hideDeleteFilterModal}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">
          <NxFontAwesomeIcon icon={faTrashAlt}/>
          <span>Delete Filter</span>
        </h2>
      </header>
      <form className="nx-form nx-form--simple" onSubmit={handleDeleteFilter} noValidate>
        { (deleteFilterSaving || deleteFilterSuccess) &&
          <NxSubmitMask message="Removing…" success={deleteFilterSuccess} />
        }
        { !deleteFilterError &&
          <div className="nx-modal-content">
            <NxWarningAlert id="delete-filter-confirmation">
              You are about to delete &quot;{filterToDelete}&quot; filter. This action can not be undone.
            </NxWarningAlert>
          </div>
        }
        <footer className={classnames('nx-modal-footer', { 'nx-error': deleteFilterError })}>
          { deleteFilterError &&
            <NxErrorAlert>{deleteFilterError}</NxErrorAlert>
          }
          <div className="nx-btn-bar">
            <NxButton variant={ deleteFilterError ? 'error' : 'primary' }
                      id="delete-filter-modal-continue-button"
                      type="submit">
              { deleteFilterError ?
                <Fragment>
                  <NxFontAwesomeIcon icon={faSync}/>
                  <span>Retry</span>
                </Fragment>
                :
                'Continue'
              }
            </NxButton>
            <NxButton id="delete-filter-modal-cancel-button"
                      type="button"
                      onClick={handleCancelButtonClick}>
              Cancel
            </NxButton>
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
