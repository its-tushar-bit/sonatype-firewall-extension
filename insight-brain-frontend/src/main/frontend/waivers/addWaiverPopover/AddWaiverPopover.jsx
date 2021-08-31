/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { NxSubmitMask } from '@sonatype/react-shared-components';

import IqPopover from '../../react/IqPopover/IqPopover';
import { extractViolationDetails } from '../../util/violationDetailsUtil';
import AddWaiverForm, { waiverScopePropTypes } from '../AddWaiverForm';
import { violationDetailsPropTypes } from '../../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../../violation/PolicyViolationConstraintInfoTile';
import LoadWrapper from '../../react/LoadWrapper';

const AddWaiversPopover = (props) => {
  const {
    violationId,
    loading,
    loadError,
    applyToAllComponents,
    waiverComments,
    availableWaiverScopes,
    openVulnerabilityDetailsModal,
    selectedWaiverScope,
    expiryTime,
    submitError,
    setWaiverScope,
    setWaiverComment,
    setApplyToAllComponents,
    setExpiryTime,
    saveWaiver,
    violationDetails,
    loadAddWaiverData,
    onClose,
    submitMaskState,
  } = props;

  const load = () => {
    loadAddWaiverData(violationId);
  };

  const closeWhensubmitFinish = () => {
    if (submitMaskState) {
      onClose();
    }
  };

  useEffect(load, [violationId]);

  useEffect(closeWhensubmitFinish, [submitMaskState]);

  const getFormProps = () => {
    if (!violationDetails) {
      return null;
    }

    return {
      applyToAllComponents,
      waiverComments,
      availableWaiverScopes,
      openVulnerabilityDetailsModal,
      selectedWaiverScope,
      expiryTime,
      submitError,
      setWaiverScope,
      setWaiverComment,
      setApplyToAllComponents,
      setExpiryTime,
      saveWaiver,
      cancelAction: onClose,
      ...extractViolationDetails(violationDetails),
    };
  };

  return (
    <IqPopover size="large" onClose={onClose} id="add-waiver-popover">
      <IqPopover.Header className="add-waiver-popover-header">
        <div className="add-waiver-popover-header__title">
          <h2 className="nx-h2 add-waivers-popover-header__title-text">Add Waiver</h2>
          <NxButton onClick={onClose} variant="icon-only" title="Close" id="add=waiver=popover-close-button">
            <NxFontAwesomeIcon icon={faArrowToRight} />
          </NxButton>
        </div>
      </IqPopover.Header>
      {submitMaskState !== null && (
        <NxSubmitMask success={submitMaskState} message="Creating waiver…" successMessage="Success!" />
      )}
      <LoadWrapper
        loading={loading || !violationDetails || !availableWaiverScopes}
        error={loadError}
        retryHandler={load}
      >
        {() => <AddWaiverForm {...getFormProps()} />}
      </LoadWrapper>
    </IqPopover>
  );
};

AddWaiversPopover.propTypes = {
  violationId: PropTypes.string.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: LoadWrapper.propTypes.error,
  submitMaskState: PropTypes.bool,
  submitError: PropTypes.instanceOf(Error),
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
    displayName: PropTypes.shape({
      parts: PropTypes.arrayOf(PropTypes.object),
    }),
    filename: PropTypes.string,
    policyViolationId: PropTypes.string.isRequired,
  }),
  waiverComments: PropTypes.shape({
    value: PropTypes.string.isRequired,
    isPristine: PropTypes.bool.isRequired,
  }).isRequired,
  availableWaiverScopes: PropTypes.arrayOf(PropTypes.shape(waiverScopePropTypes)),
  selectedWaiverScope: PropTypes.shape(waiverScopePropTypes),
  applyToAllComponents: PropTypes.bool,
  expiryTime: PropTypes.string,
  loadAddWaiverData: PropTypes.func.isRequired,
  openVulnerabilityDetailsModal: PropTypes.func.isRequired,
  saveWaiver: PropTypes.func.isRequired,
  setWaiverComment: PropTypes.func.isRequired,
  setWaiverScope: PropTypes.func.isRequired,
  setApplyToAllComponents: PropTypes.func.isRequired,
  setExpiryTime: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};

export default AddWaiversPopover;
