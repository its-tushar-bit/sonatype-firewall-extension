/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import {
  NxModal,
  NxH2,
  NxFontAwesomeIcon,
  NxButtonBar,
  NxButton,
  NxFooter,
  NxP,
} from '@sonatype/react-shared-components';
import { faCircleCheck } from '@fortawesome/pro-solid-svg-icons';

export default function EvaluationCompletePage({ headerId, onCancel }) {
  return (
    <>
      <NxModal.Header>
        <NxH2 id={headerId}>Evaluation Complete</NxH2>
      </NxModal.Header>
      <NxModal.Content id="import-sbom-modal-evaluation-complete-content">
        <div
          role="status"
          className="import-sbom-modal__evaluation-status-text import-sbom-modal__evaluation-status-text--success"
        >
          <NxFontAwesomeIcon icon={faCircleCheck} />
          <span>Success!</span>
        </div>
        <NxP className="import-sbom-modal__evaluation-description-text">
          Your SBOM has been evaluated and is ready for viewing.
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

EvaluationCompletePage.propTypes = {
  headerId: PropTypes.string.isRequired,
  onCancel: PropTypes.func.isRequired,
};
