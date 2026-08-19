/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxModal, NxButton, NxTextLink } from '@sonatype/react-shared-components';
import './_EnterpriseWaiverFeaturesModal.scss';

export default function EnterpriseWaiverFeaturesModal({ isOpen, onClose }) {
  if (!isOpen) {
    return null;
  }

  return (
    <NxModal onClose={onClose} className="iq-enterprise-waiver-features-modal">
      <div className="nx-modal-content">
        <div className="iq-enterprise-waiver-features-modal__header">
          <span className="iq-enterprise-waiver-features-modal__title">70% of your work is still manual</span>
        </div>

        <ul className="iq-enterprise-waiver-features-modal__list">
          <li className="iq-enterprise-waiver-features-modal__list-item">
            <NxTextLink href="https://help.sonatype.com/en/bulk-waivers.html" external>
              Bulk Waivers:
            </NxTextLink>{' '}
            Waive multiple policy violations for a component or an application at once—reducing manual effort and speed
            up remediation workflows.
          </li>
          <li className="iq-enterprise-waiver-features-modal__list-item">
            <NxTextLink href="https://help.sonatype.com/en/automated-waivers.html" external>
              Auto-Waivers:
            </NxTextLink>{' '}
            Automatically apply waivers to low-risk, non-reachable or known issues so teams can stay unblocked while
            maintaining control over risk and policy enforcement.
          </li>
        </ul>

        <div className="iq-enterprise-waiver-features-modal__links">
          <NxTextLink href="https://www.sonatype.com/products/request-demo" external>
            Request Demo
          </NxTextLink>
          .
        </div>
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton variant="tertiary" onClick={onClose}>
            Close
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

EnterpriseWaiverFeaturesModal.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
};
