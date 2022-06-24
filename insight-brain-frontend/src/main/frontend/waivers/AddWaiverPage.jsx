/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxSubmitMask } from '@sonatype/react-shared-components';

import LoadWrapper from '../react/LoadWrapper';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';
import AddWaiverForm, { waiverScopePropTypes } from './AddWaiverForm';
import { extractViolationDetails } from '../util/violationDetailsUtil';
import AddAndRequestWaiversBackButton from './AddAndRequestWaiversBackButton';

export default function AddWaiverPage(props) {
  const {
    // page state
    violationId,
    loading,
    loadError,
    submitMaskState,
    submitError,
    prevStateName,
    prevParams,
    //data
    waiverComments,
    availableWaiverScopes,
    selectedWaiverScope,
    componentMatcherStrategy,
    expiryTime,
    customExpiryTime,
    violationDetails,
    currentUser,
    //actions
    loadAddWaiverData,
    openVulnerabilityDetailsModal,
    closeVulnerabilityDetailsModal,
    saveWaiver,
    setWaiverComment,
    setWaiverScope,
    setComponentMatcherStrategy,
    setExpiryTime,
    setCustomExpiryTime,
    cancelAction,
  } = props;

  const backButtonProps = {
    violationId,
    prevStateName,
    prevParams,
  };

  function load() {
    if (violationId) {
      loadAddWaiverData(violationId);
    }
  }

  useEffect(load, [violationId]);

  const getFormProps = () => {
    if (!violationDetails) {
      return null;
    }

    return {
      componentMatcherStrategy,
      waiverComments,
      availableWaiverScopes,
      openVulnerabilityDetailsModal,
      closeVulnerabilityDetailsModal,
      selectedWaiverScope,
      expiryTime,
      customExpiryTime,
      submitError,
      setWaiverScope,
      setWaiverComment,
      setComponentMatcherStrategy,
      setExpiryTime,
      setCustomExpiryTime,
      saveWaiver,
      cancelAction,
      currentUser,
      ...extractViolationDetails(violationDetails),
    };
  };

  return (
    <main id="add-waiver-page" className="nx-page-main">
      <AddAndRequestWaiversBackButton {...backButtonProps} />
      <div className="nx-page-title">
        <h1 className="nx-h1">Add Waiver</h1>
      </div>
      <section className="nx-tile">
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
      </section>
    </main>
  );
}

AddWaiverPage.propTypes = {
  violationId: PropTypes.string,
  prevStateName: PropTypes.string,
  prevParams: AddAndRequestWaiversBackButton.propTypes.prevParams,
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
  componentMatcherStrategy: PropTypes.string,
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
  setComponentMatcherStrategy: PropTypes.func.isRequired,
  setExpiryTime: PropTypes.func.isRequired,
  setCustomExpiryTime: PropTypes.func.isRequired,
  cancelAction: PropTypes.func.isRequired,
  closeVulnerabilityDetailsModal: PropTypes.func.isRequired,
  currentUser: PropTypes.string,
};
