/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxFontAwesomeIcon, NxLoadError, NxModal, NxSubmitMask } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { normalizeFirewallOwnerType } from 'MainRoot/firewall/bulkWaive/firewallWaiverUtils';

export default function FirewallDeleteWaiverModal({
  waiverToDelete,
  deleteFirewallWaiver,
  hideFirewallDeleteWaiverModal,
  deleteWaiverError,
  deleteWaiverSaving,
}) {
  if (!waiverToDelete) {
    return null;
  }

  const handleDelete = () => {
    const { ownerId, ownerType, id: waiverId } = waiverToDelete;
    deleteFirewallWaiver(normalizeFirewallOwnerType(ownerType), ownerId, waiverId);
  };

  return (
    <NxModal id="firewall-delete-waiver-modal" onClose={hideFirewallDeleteWaiverModal}>
      {deleteWaiverSaving != null && <NxSubmitMask message="Removing…" success={deleteWaiverSaving} />}
      <header className="nx-modal-header">
        <h2 className="nx-h2">
          <NxFontAwesomeIcon icon={faTrashAlt} />
          <span>Delete Waiver</span>
        </h2>
      </header>
      <div className="nx-modal-content">Are you sure you want to delete this waiver?</div>
      <footer className="nx-footer">
        {deleteWaiverError && (
          <NxLoadError
            error={deleteWaiverError}
            retryHandler={handleDelete}
            titleMessage="An error occurred deleting the waiver."
          />
        )}
        <div className="nx-btn-bar">
          <NxButton type="button" onClick={hideFirewallDeleteWaiverModal}>
            Cancel
          </NxButton>
          {!deleteWaiverError && (
            <NxButton variant="primary" onClick={handleDelete}>
              Delete Waiver
            </NxButton>
          )}
        </div>
      </footer>
    </NxModal>
  );
}

FirewallDeleteWaiverModal.propTypes = {
  waiverToDelete: PropTypes.shape({
    id: PropTypes.string.isRequired,
    ownerId: PropTypes.string.isRequired,
    ownerType: PropTypes.string.isRequired,
  }),
  deleteFirewallWaiver: PropTypes.func.isRequired,
  hideFirewallDeleteWaiverModal: PropTypes.func.isRequired,
  deleteWaiverError: PropTypes.string,
  deleteWaiverSaving: PropTypes.bool,
};
