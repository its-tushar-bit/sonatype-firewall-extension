/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import {
  NxButton,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxFooter,
  NxH2,
  NxLoadingSpinner,
  NxModal,
  NxP,
} from '@sonatype/react-shared-components';
import { SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components/components/NxSubmitMask/NxSubmitMask';
import { faCircleCheck } from '@fortawesome/pro-solid-svg-icons';

export default function ReevaluationStatusModal({ reevaluating }) {
  const [showReevaluationStatus, setShowReevaluationStatus] = useState(false);

  useEffect(() => {
    if (reevaluating) {
      setShowReevaluationStatus(true);
    } else {
      const timer = setTimeout(() => {
        setShowReevaluationStatus(false);
      }, SUCCESS_VISIBLE_TIME_MS); // Display content for the default NxSubmitMask time
      return () => clearTimeout(timer);
    }
  }, [reevaluating]);

  return (
    <>
      {showReevaluationStatus && (
        <NxModal id="iq-reevaluation-status-modal" variant="narrow" onCancel={() => setShowReevaluationStatus(false)}>
          {reevaluating ? (
            <>
              <NxModal.Header>
                <NxH2>Re-Evaluation Status</NxH2>
              </NxModal.Header>
              <NxModal.Content>
                <div
                  className="iq-reevaluation-status-modal__reevaluation-status-text
                 iq-reevaluation-status-modal__reevaluation-status-text--reevaluating"
                >
                  <NxLoadingSpinner>Re-Evaluating…</NxLoadingSpinner>
                </div>
                <NxP>
                  Closing this modal will not interrupt the Re-Evaluation. It will still progress until the process is
                  complete.
                </NxP>
              </NxModal.Content>
            </>
          ) : (
            <>
              <NxModal.Header>
                <NxH2>Re-Evaluation Complete</NxH2>
              </NxModal.Header>
              <NxModal.Content>
                <div
                  className="iq-reevaluation-status-modal__reevaluation-status-text
                                iq-reevaluation-status-modal__reevaluation-status-text--complete"
                >
                  <NxFontAwesomeIcon icon={faCircleCheck} />
                  <span>Success!</span>
                </div>
              </NxModal.Content>
            </>
          )}

          {reevaluating && (
            <NxFooter>
              <NxButtonBar>
                <NxButton onClick={() => setShowReevaluationStatus(false)}>Close</NxButton>
              </NxButtonBar>
            </NxFooter>
          )}
        </NxModal>
      )}
    </>
  );
}

ReevaluationStatusModal.propTypes = {
  reevaluating: PropTypes.bool.isRequired,
};
