/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { map, path, prop } from 'ramda';
import { NxSubmitMask } from '@sonatype/react-shared-components';
import { categoryByPolicyThreatLevel } from '@sonatype/react-shared-components/util/threatLevels';

import MaximizedContainer from '../react/MaximizedContainer';
import LoadWrapper from '../react/LoadWrapper';
import { getComponentName, getArtifactName } from '../util/componentNameUtils';
import { violationDetailsPropTypes } from '../violation/ViolationDetailsTile';
import { constraintViolationsPropType } from '../violation/PolicyViolationConstraintInfoTile';
import AddWaiverForm, { waiverScopePropTypes } from './AddWaiverForm';

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
    violationDetails,
    //actions
    loadAddWaiverData,
    openVulnerabilityDetailsModal,
    saveWaiver,
    setWaiverComment,
    setWaiverScope,
    setApplyToAllComponents
  } = props;

  useEffect(() => {
    if (violationId) {
      loadAddWaiverData(violationId);
    }
  }, [violationId]);

  const getFormProps = () => {
    if (!violationDetails) {
      return null;
    }

    const {
      constraintViolations,
      policyName,
      policyViolationId,
      threatLevel
    } = violationDetails;

    const { constraintName, reasons } = constraintViolations[0],
        vulnerabilityId = path([0, 'reference', 'value'], reasons),
        threatLevelCategory = categoryByPolicyThreatLevel[threatLevel],
        componentName = getComponentName(violationDetails),
        artifactName = getArtifactName(violationDetails);

    return {
      applyToAllComponents,
      artifactName,
      componentName,
      constraintName,
      policyName,
      policyViolationId,
      reasons: map(prop('reason'), reasons),
      threatLevelCategory,
      waiverComments,
      availableWaiverScopes,
      openVulnerabilityDetailsModal,
      selectedWaiverScope,
      submitError,
      setWaiverScope,
      setWaiverComment,
      setApplyToAllComponents,
      saveWaiver,
      vulnerabilityId
    };
  };

  return (
    <MaximizedContainer id="add-waiver-page" className="nx-page-content">
      <div className="nx-page-main">
        <div className="nx-page-title">
          <h1 className="nx-h1">Add Waiver</h1>
        </div>

        <div className="nx-tile">
          { submitMaskState !== null &&
            <NxSubmitMask success={ submitMaskState }
                          message="Creating waiver…"
                          successMessage="Success!" />
          }

          <LoadWrapper loading={ loading || !violationDetails || !availableWaiverScopes } error={loadError}>
            {() =>
              <AddWaiverForm {...getFormProps()} />
            }
          </LoadWrapper>
        </div>
      </div>
    </MaximizedContainer>
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
  loadAddWaiverData: PropTypes.func.isRequired,
  openVulnerabilityDetailsModal: PropTypes.func.isRequired,
  saveWaiver: PropTypes.func.isRequired,
  setWaiverComment: PropTypes.func.isRequired,
  setWaiverScope: PropTypes.func.isRequired,
  setApplyToAllComponents: PropTypes.func.isRequired
};
