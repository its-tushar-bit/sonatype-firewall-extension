/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import { bindActionCreators } from 'redux';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import { sort } from 'ramda';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import { waiverType, populateViolationsWithApplicableWaivers } from 'MainRoot/util/waiverUtils';
import FirewallPolicyViolationsTableRow from './FirewallPolicyViolationsTableRow';
import FirewallExistingWaiversPopover from './FirewallExistingWaiversPopover';
import * as WaiverActionCreators from 'MainRoot/waivers/waiverActions';
import { selectSelectedPolicyViolation } from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsSelectors';

// exporting function for testing
export const sortPolicyByThreat = sort((threatA, threatB) => {
  return threatB.policyThreatLevel - threatA.policyThreatLevel || threatA.policyName.localeCompare(threatB.policyName);
});

export default function FirewallPolicyViolationsTable({
  violations,
  showProxyState = false,
  error,
  showPolicyWaiversPopover,
  setShowComponentWaiversPopover,
  componentName,
  componentNameWithoutVersion,
  loadPolicyViolationsInformation,
  waivers,
  waiverToDelete,
  loading,
}) {
  const dispatch = useDispatch();
  const mutatedViolations = populateViolationsWithApplicableWaivers(waivers, violations);
  const orderedViolations = violations ? sortPolicyByThreat(mutatedViolations) : [];
  const selectedPolicyViolation = useSelector(selectSelectedPolicyViolation);
  const boundSetWaiverToDelete = useMemo(() => bindActionCreators(WaiverActionCreators.setWaiverToDelete, dispatch), [
    dispatch,
  ]);

  return (
    <>
      <NxTable className="iq-policy-violations-table firewall-policy-violation-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Threat</NxTableCell>
            <NxTableCell>Policy/Action</NxTableCell>
            <NxTableCell>Constraint Name</NxTableCell>
            <NxTableCell>Condition</NxTableCell>
            <NxTableCell />
            <NxTableCell chevron />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          emptyMessage="No policy violations"
          error={error}
          isLoading={loading}
          retryHandler={loadPolicyViolationsInformation}
        >
          {orderedViolations.map((violation) => (
            <FirewallPolicyViolationsTableRow
              key={violation.policyViolationId}
              orderedViolations={orderedViolations}
              violation={violation}
              selectPolicyId={selectedPolicyViolation?.policyViolationId}
              showProxyState={showProxyState}
            />
          ))}
        </NxTableBody>
      </NxTable>
      {showPolicyWaiversPopover && (
        <FirewallExistingWaiversPopover
          componentName={componentName}
          componentNameWithoutVersion={componentNameWithoutVersion}
          setShowComponentWaiversPopover={setShowComponentWaiversPopover}
          waivers={waivers}
          setWaiverToDelete={boundSetWaiverToDelete}
          waiverToDelete={waiverToDelete}
        />
      )}
    </>
  );
}

FirewallPolicyViolationsTable.propTypes = {
  violations: PropTypes.arrayOf(FirewallPolicyViolationsTableRow.propTypes.violation),
  showProxyState: FirewallPolicyViolationsTableRow.propTypes.showProxyState,
  showPolicyWaiversPopover: PropTypes.bool,
  setShowComponentWaiversPopover: PropTypes.func,
  componentName: PropTypes.string,
  loadPolicyViolationsInformation: PropTypes.func.isRequired,
  componentNameWithoutVersion: PropTypes.string,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  waiverToDelete: PropTypes.shape(waiverType),
  loading: PropTypes.bool,
  error: PropTypes.string,
};
