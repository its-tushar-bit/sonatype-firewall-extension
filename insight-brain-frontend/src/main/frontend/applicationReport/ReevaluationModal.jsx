/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import {
  NxButton,
  NxTooltip,
  NxFontAwesomeIcon,
  NxModal,
  NxH2,
  NxP,
  NxLoadWrapper,
  NxTile,
} from '@sonatype/react-shared-components';
import { faSync } from '@fortawesome/pro-solid-svg-icons';
import { useSelector, useDispatch } from 'react-redux';
import { selectIsAutoWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectApplicationReportSlice } from 'MainRoot/applicationReport/applicationReportSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsLatestReportForStageRequestPending,
  selectLatestReportForStageId,
} from 'MainRoot/applicationReport/latestReportForStageSelectors';
import { reevaluateReport, reevaluateReportCancelled } from './applicationReportActions';

const ReevaluationModal = () => {
  const isAutoWaiverEnabled = useSelector(selectIsAutoWaiversEnabled);
  const { reevaluating } = useSelector(selectApplicationReportSlice);
  const { scanId } = useSelector(selectRouterCurrentParams);
  const isLatestReportForStageRequestPending = useSelector(selectIsLatestReportForStageRequestPending);
  const latestReportId = useSelector(selectLatestReportForStageId);
  const dispatch = useDispatch();
  const [showDialog, setShowDialog] = useState(false);

  const handleButtonClick = () => {
    isAutoWaiverEnabled ? setShowDialog(true) : handleReevaluate();
  };

  const handleClose = () => {
    setShowDialog(false);
    if (reevaluating) {
      dispatch(reevaluateReportCancelled());
    }
  };

  const handleQuickReevaluate = () => {
    dispatch(reevaluateReport(true));
    setShowDialog(false);
  };

  const handleReevaluate = () => {
    dispatch(reevaluateReport());
    setShowDialog(false);
  };

  const isSameAsCurrentScan = () => latestReportId === scanId;

  const shouldDisableReevaluation = () => isLatestReportForStageRequestPending || !isSameAsCurrentScan();

  const tooltipMessage = shouldDisableReevaluation()
    ? 'Re-Evaluation is only allowed on the latest scan of a given stage.'
    : null;

  return (
    <>
      <NxTooltip title={tooltipMessage}>
        <span>
          <NxButton
            id="reevaluate-report-button"
            className="nx-btn--tertiary"
            onClick={handleButtonClick}
            disabled={shouldDisableReevaluation() || reevaluating}
          >
            <NxFontAwesomeIcon icon={faSync} />
            <span>Re-Evaluate Report</span>
          </NxButton>
        </span>
      </NxTooltip>

      {showDialog && (
        <NxModal id="iq-reevaluation-options-modal" variant="narrow" onCancel={handleClose}>
          <NxModal.Header>
            <NxTile.HeaderTitle>
              <NxH2>Re-Evaluate Options</NxH2>
            </NxTile.HeaderTitle>
          </NxModal.Header>
          <NxModal.Content className="iq-reevaluation-modal-content">
            <NxP className="iq-reevaluation-modal-tile-p">
              Click on Re-evaluate for a full re-evaluation or after enabling the Automatic Waivers feature. To skip
              re-evaluation of violations with automatic waivers applied, click Quick Re-evaluate.
            </NxP>
          </NxModal.Content>
          <footer className="nx-footer">
            <div className="nx-btn-bar">
              <NxButton type="button" variant="tertiary" onClick={handleClose} disabled={reevaluating}>
                Cancel
              </NxButton>
              <NxLoadWrapper loading={reevaluating} retryHandler={handleQuickReevaluate}>
                <NxTooltip title={tooltipMessage}>
                  <NxButton
                    id="quick-reevaluate-report-button"
                    type="button"
                    onClick={handleQuickReevaluate}
                    disabled={shouldDisableReevaluation() || reevaluating}
                  >
                    Quick Re-Evaluate
                  </NxButton>
                </NxTooltip>
              </NxLoadWrapper>
              <NxLoadWrapper loading={reevaluating} retryHandler={handleReevaluate}>
                <NxTooltip title={tooltipMessage}>
                  <NxButton
                    id="full-reevaluate-report-button"
                    type="button"
                    variant="primary"
                    onClick={handleReevaluate}
                    disabled={shouldDisableReevaluation() || reevaluating}
                  >
                    Re-Evaluate
                  </NxButton>
                </NxTooltip>
              </NxLoadWrapper>
            </div>
          </footer>
        </NxModal>
      )}
    </>
  );
};

export default ReevaluationModal;
