/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef, useState } from 'react';
import {
  NxButton,
  NxTooltip,
  NxFontAwesomeIcon,
  NxModal,
  NxH2,
  NxH4,
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
import { selectIsContainerImagesEvaluationEnabledAndProxyStage } from 'MainRoot/applicationReport/applicationReportSelectors';

const ReevaluationModal = () => {
  const isAutoWaiverEnabled = useSelector(selectIsAutoWaiversEnabled);
  const { reevaluating } = useSelector(selectApplicationReportSlice);
  const { scanId } = useSelector(selectRouterCurrentParams);
  const isLatestReportForStageRequestPending = useSelector(selectIsLatestReportForStageRequestPending);
  const latestReportId = useSelector(selectLatestReportForStageId);

  const isContainerImagesEvaluation = useSelector(selectIsContainerImagesEvaluationEnabledAndProxyStage);

  const dispatch = useDispatch();
  const [showDialog, setShowDialog] = useState(false);

  // Cancel an in-flight re-evaluation if the report (and this modal) unmounts mid-poll, so the long-running
  // poll chain doesn't resolve and reload the report into a view the user has navigated away from. A ref keeps
  // the latest reevaluating flag available to the unmount-only effect without re-running it on every change.
  const reevaluatingRef = useRef(reevaluating);
  reevaluatingRef.current = reevaluating;
  useEffect(
    () => () => {
      if (reevaluatingRef.current) {
        dispatch(reevaluateReportCancelled());
      }
    },
    [dispatch]
  );

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
            {isContainerImagesEvaluation ? <span>Re-Evaluate Container</span> : <span>Re-Evaluate Report</span>}
          </NxButton>
        </span>
      </NxTooltip>

      {showDialog && (
        <NxModal id="iq-reevaluation-options-modal" variant="narrow" onCancel={handleClose}>
          <NxModal.Header>
            <NxTile.HeaderTitle>
              <NxH2>Re-Evaluate Report</NxH2>
            </NxTile.HeaderTitle>
          </NxModal.Header>
          <NxModal.Content className="iq-reevaluation-modal-content">
            <NxH4>Re-Evaluate</NxH4>
            <NxP>
              Re-evaluate this scan against updated policies, apply new waivers, and/or auto-waive violations with no
              upgrade path.
            </NxP>
            <NxH4>Quick Re-Evaluate</NxH4>
            <NxP>Re-evaluate without updating auto-waived violations for faster results.</NxP>
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
