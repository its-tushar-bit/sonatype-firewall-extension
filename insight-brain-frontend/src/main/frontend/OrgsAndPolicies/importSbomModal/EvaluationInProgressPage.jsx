/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import {
  NxButton,
  NxButtonBar,
  NxFooter,
  NxH2,
  NxLoadingSpinner,
  NxModal,
  NxP,
} from '@sonatype/react-shared-components';

export default function EvaluationInProgressPage({ headerId, onCancel }) {
  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>File Imported</NxH2>
      </NxModal.Header>
      <NxModal.Content id="import-sbom-modal-evaluation-in-progress-content">
        <div className="import-sbom-modal__evaluation-status-text import-sbom-modal__evaluation-status-text--evaluating">
          <NxLoadingSpinner>Evaluating…</NxLoadingSpinner>
        </div>
        <NxP>
          Feel free to close this modal and continue working. Your evaluation will continue to process in the background
          and should be done within a few minutes.
        </NxP>
      </NxModal.Content>
      <NxFooter>
        <NxButtonBar>
          <NxButton onClick={onCancel}>Close</NxButton>
        </NxButtonBar>
      </NxFooter>
    </>
  );
}

EvaluationInProgressPage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
};
