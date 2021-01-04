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

export default function AddWaiverPage(props) {
  const {
    // page state
    violationId,
    loading,
    loadError,
    submitMaskState,
    submitError,
    //data
    waiverComments,
    availableWaiverScopes,
    selectedWaiverScope,
    applyToAllComponents,
    expiryTime,
    violationDetails,
    //actions
    loadAddWaiverData,
    openVulnerabilityDetailsModal,
    saveWaiver,
    setWaiverComment,
    setWaiverScope,
    setApplyToAllComponents,
    setExpiryTime,
    cancelAction
  } = props;

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
      cancelAction,
      ...extractViolationDetails(violationDetails)
    };
  };

  return (
    <main id="add-waiver-page" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">Add Waiver</h1>
      </div>

      <section className="nx-tile">
        { submitMaskState !== null &&
          <NxSubmitMask success={ submitMaskState }
                        message="Creating waiver…"
                        successMessage="Success!" />
        }

        <LoadWrapper loading={ loading || !violationDetails || !availableWaiverScopes }
                     error={loadError}
                     retryHandler={load}>
          {() =>
            <AddWaiverForm {...getFormProps()} />
          }
        </LoadWrapper>
      </section>
    </main>
  );
}

AddWaiverPage.propTypes = {
  violationId: PropTypes.string,
  loading: PropTypes.bool.isRequired,
  loadError: LoadWrapper.propTypes.error,
  submitMaskState: PropTypes.bool,
  submitError: PropTypes.instanceOf(Error),
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
    displayName: PropTypes.shape({
      parts: PropTypes.arrayOf(PropTypes.object)
    }),
    filename: PropTypes.string,
    policyViolationId: PropTypes.string.isRequired
  }),
  waiverComments: PropTypes.shape({
    value: PropTypes.string.isRequired,
    isPristine: PropTypes.bool.isRequired
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
  cancelAction: PropTypes.func.isRequired
};
