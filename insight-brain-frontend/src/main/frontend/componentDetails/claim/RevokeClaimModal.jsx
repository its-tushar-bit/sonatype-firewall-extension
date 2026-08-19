/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulForm, NxFontAwesomeIcon, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import { faTrashAlt } from '@fortawesome/pro-solid-svg-icons';

export default function RevokeClaimModal({ revokeMaskState, revokeError, closeModal, revoke }) {
  return (
    <NxModal onClose={closeModal} variant="narrow" id="component-details-revoke-claim-modal">
      <NxStatefulForm
        onSubmit={revoke}
        submitMaskState={revokeMaskState}
        onCancel={closeModal}
        submitMaskMessage="Revoking…"
        submitBtnText="Revoke"
        submitError={revokeError}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">
            <NxFontAwesomeIcon icon={faTrashAlt} />
            <span>Revoke Claim</span>
          </h2>
        </header>
        <div className="nx-modal-content">
          <NxWarningAlert>
            Are you sure you want to revoke the claim on this component? This change will not be reflected until a new
            policy evaluation is triggered.
          </NxWarningAlert>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

export const revokeClaimModalPropTypes = {
  revokeMaskState: PropTypes.bool,
  revokeError: PropTypes.string,
  revoke: PropTypes.func.isRequired,
};

RevokeClaimModal.propTypes = {
  closeModal: PropTypes.func.isRequired,
  ...revokeClaimModalPropTypes,
};
