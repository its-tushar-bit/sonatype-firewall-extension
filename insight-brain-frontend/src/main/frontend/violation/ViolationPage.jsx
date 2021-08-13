/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import LoadWrapper from '../react/LoadWrapper';
import ViolationDetailsTile, { violationDetailsPropTypes } from './ViolationDetailsTile';
import PolicyViolationConstraintInfoTile, { constraintViolationsPropType } from './PolicyViolationConstraintInfoTile';
import SecurityVulnerabilityDetailsTile from './SecurityVulnerabilityDetailsTile';

export default function ViolationPage(props) {
  const {
    $state,
    loadViolation,
    loadVulnerabilityDetails,
    stateGo,
    fetchStageTypes,
    loading,
    violationDetails,
    stageTypes,
    vulnerabilityDetailsLoading,
    vulnerabilityDetails,
    vulnerabilityDetailsError,
    activeWaivers,
    selectedViolationId,
    goToWaivers,
    isFromPolicyViolations,
  } = props;

  const error = props.violationDetailsError || props.stageTypesError;

  const constraintViolations = violationDetails ? violationDetails.constraintViolations : [];

  // eslint-disable-next-line react/prop-types
  const isSecurityVulnerability =
    // eslint-disable-next-line react/prop-types
    violationDetails && violationDetails.policyThreatCategory === 'security';

  useEffect(() => {
    load();
  }, [selectedViolationId]);

  function load() {
    loadViolation(selectedViolationId);
    fetchStageTypes('dashboard');
  }

  return (
    <div id="violation-page">
      <LoadWrapper error={error} loading={loading || !(violationDetails && stageTypes)} retryHandler={load}>
        <ViolationDetailsTile
          {...{
            $state,
            stageTypes,
            violationDetails,
            stateGo,
            activeWaivers,
            goToWaivers,
            selectedViolationId,
            isFromPolicyViolations,
          }}
        />
        <PolicyViolationConstraintInfoTile constraintViolations={constraintViolations} />
        {isSecurityVulnerability && (
          <SecurityVulnerabilityDetailsTile
            showTitle={!isFromPolicyViolations}
            vulnerabilityDetails={vulnerabilityDetails}
            error={vulnerabilityDetailsError}
            loading={vulnerabilityDetailsLoading}
            retryLoad={loadVulnerabilityDetails}
          />
        )}
      </LoadWrapper>
    </div>
  );
}

export const violationPageTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
  }).isRequired,
  selectedViolationId: PropTypes.string.isRequired,
  loadViolation: PropTypes.func.isRequired,
  loadVulnerabilityDetails: PropTypes.func.isRequired,
  fetchStageTypes: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  violationDetailsError: LoadWrapper.propTypes.error,
  stageTypesError: LoadWrapper.propTypes.error,
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
  }),
  stageTypes: ViolationDetailsTile.propTypes.stageTypes,
  vulnerabilityDetailsLoading: PropTypes.bool.isRequired,
  vulnerabilityDetails: PropTypes.object,
  vulnerabilityDetailsError: LoadWrapper.propTypes.error,
  activeWaivers: ViolationDetailsTile.propTypes.activeWaivers,
  goToWaivers: PropTypes.func.isRequired,
  isFromPolicyViolations: PropTypes.bool,
};

ViolationPage.propTypes = violationPageTypes;
