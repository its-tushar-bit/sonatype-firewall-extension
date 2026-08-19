/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import { NxSubmitMask } from '@sonatype/react-shared-components';

import IqPopover from '../../react/IqPopover/IqPopover';
import { extractViolationDetails } from '../../util/violationDetailsUtil';
import AddWaiverForm, { waiverScopePropTypes } from '../AddWaiverForm';
import { violationDetailsPropTypes } from '../../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../../violation/PolicyViolationConstraintInfo';
import LoadWrapper from '../../react/LoadWrapper';
import UnsavedChangesModal from '../../modals/unsavedChangesModal/UnsavedChangesModal';

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
    customExpiryTime,
    submitError,
    setWaiverScope,
    setWaiverComment,
    setApplyToAllComponents,
    setExpiryTime,
    setCustomExpiryTime,
    saveWaiver,
    violationDetails,
    loadAddWaiverData,
    onClose,
    submitMaskState,
    showUnsavedChangesModal,
    setShowUnsavedChangesModal,
    resetAddWaiverData,
    isDirty,
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

  const openUnsavedChangesModal = () => {
    if (isDirty) {
      setShowUnsavedChangesModal(true);
    } else {
      onClose();
    }
  };

  const closeUnsavedChangesModal = () => {
    setShowUnsavedChangesModal(false);
  };

  const closeAddWaiverPopover = () => {
    if (isDirty) {
      resetAddWaiverData();
    }
    onClose();
  };

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
      customExpiryTime,
      submitError,
      setWaiverScope,
      setWaiverComment,
      setApplyToAllComponents,
      setExpiryTime,
      setCustomExpiryTime,
      saveWaiver,
      cancelAction: openUnsavedChangesModal,
      ...extractViolationDetails(violationDetails),
    };
  };

  return (
    <IqPopover size="large" onClose={closeAddWaiverPopover} id="add-waiver-popover">
      {showUnsavedChangesModal && (
        <UnsavedChangesModal onContinue={closeAddWaiverPopover} onClose={closeUnsavedChangesModal} />
      )}
      <IqPopover.Header
        className="add-waiver-popover-header"
        buttonId="add-waiver-popover-close-button"
        onClose={openUnsavedChangesModal}
        headerTitle="Add Waiver"
      />
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
  customExpiryTime: PropTypes.shape({
    value: PropTypes.string,
    isPristine: PropTypes.bool,
  }).isRequired,
  loadAddWaiverData: PropTypes.func.isRequired,
  openVulnerabilityDetailsModal: PropTypes.func.isRequired,
  saveWaiver: PropTypes.func.isRequired,
  setWaiverComment: PropTypes.func.isRequired,
  setWaiverScope: PropTypes.func.isRequired,
  setApplyToAllComponents: PropTypes.func.isRequired,
  setExpiryTime: PropTypes.func.isRequired,
  setCustomExpiryTime: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
  showUnsavedChangesModal: PropTypes.bool.isRequired,
  setShowUnsavedChangesModal: PropTypes.func.isRequired,
  resetAddWaiverData: PropTypes.func.isRequired,
  isDirty: PropTypes.bool.isRequired,
};

export default AddWaiversPopover;
