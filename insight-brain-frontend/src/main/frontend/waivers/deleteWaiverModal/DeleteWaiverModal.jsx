/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import {
  NxButton,
  NxLoadError,
  NxModal,
  NxSubmitMask
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

import { waiverType } from '../ListWaiversTable';

export default function DeleteWaiverModal(props) {
  const {
    waiverToDelete,
    deleteWaiver,
    hideDeleteWaiverModal,
    deleteWaiverError,
    deleteWaiverSaving
  } = props;

  const handleDeleteWaiver = () => {
    const { scopeOwnerId, scopeOwnerType, policyWaiverId } = waiverToDelete,
        ownerType = scopeOwnerType === 'root_organization' ? 'organization' : scopeOwnerType;

    deleteWaiver(ownerType, scopeOwnerId, policyWaiverId);
  };

  return (
    <NxModal id="delete-waiver-modal"
             onClose={hideDeleteWaiverModal}>
      { (deleteWaiverSaving != null) &&
        <NxSubmitMask message="Removing…" success={deleteWaiverSaving} />
      }
      <header className="nx-modal-header">
        <h2 className="nx-h2"><span>Delete Waiver</span></h2>
      </header>
      <div className="nx-modal-content">
        <span>
          Are you sure you want to remove this waiver?
        </span>
      </div>
      <footer className="nx-modal-footer">
        { deleteWaiverError &&
          <NxLoadError error={deleteWaiverError}
                       retryHandler={handleDeleteWaiver}
                       titleMessage="An error occurred deleting the waiver." />
        }
        <div className="nx-btn-bar">
          <NxButton id="delete-waiver-modal-cancel-button"
                    type="button"
                    onClick={hideDeleteWaiverModal}>
            No
          </NxButton>
          { !deleteWaiverError &&
            <NxButton variant="primary"
                      id="delete-waiver-modal-continue-button"
                      onClick={handleDeleteWaiver}>
              Yes
            </NxButton>
          }
        </div>
      </footer>
    </NxModal>
  );
}

DeleteWaiverModal.propTypes = {
  waiverToDelete: PropTypes.shape(waiverType),
  deleteWaiver: PropTypes.func.isRequired,
  hideDeleteWaiverModal: PropTypes.func.isRequired,
  deleteWaiverError: PropTypes.string,
  deleteWaiverSaving: PropTypes.bool,
  deleteWaiverSuccess: PropTypes.bool
};
