/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxModal } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function InnerSourceProducerReportModal({ reportUrl, onClose }) {
  function openReport() {
    window.open(reportUrl, '_blank');
    onClose();
  }

  return (
    <NxModal id="innersource-producer-report-modal" onClose={onClose}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Newer Component Version Found in Report</h2>
      </header>
      <div className="nx-modal-content">
        A newer version of the InnerSource component is being used in the latest report.
      </div>
      <footer className="nx-footer">
        <div className="nx-btn-bar">
          <NxButton id="innersource-producer-report-modal-cancel" onClick={onClose}>
            Cancel
          </NxButton>
          <NxButton id="innersource-producer-report-modal-continue-to-report" variant="primary" onClick={openReport}>
            Continue to Report
          </NxButton>
        </div>
      </footer>
    </NxModal>
  );
}

InnerSourceProducerReportModal.propTypes = {
  reportUrl: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
};
