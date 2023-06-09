/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo, useState } from 'react';
import { bindActionCreators } from 'redux';
import { useDispatch } from 'react-redux';
import * as PropTypes from 'prop-types';
import { sort } from 'ramda';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import { waiverType, mapApplicableWaiversToViolations } from 'MainRoot/util/waiverUtils';
import FirewallPolicyViolationsTableRow from './FirewallPolicyViolationsTableRow';
import PolicyViolationDetailsPopover from './FirewallPolicyViolationDetailsPopover';
import FirewallExistingWaiversPopover from './FirewallExistingWaiversPopover';
import * as WaiverActionCreators from 'MainRoot/waivers/waiverActions';

// exporting function for testing
export const sortPolicyByThreat = sort((threatA, threatB) => {
  return threatB.policyThreatLevel - threatA.policyThreatLevel || threatA.policyName.localeCompare(threatB.policyName);
});

export default function FirewallPolicyViolationsTable({
  violations,
  showProxyState = false,
  showPolicyWaiversPopover,
  setShowComponentWaiversPopover,
  componentName,
  componentNameWithoutVersion,
  waivers,
  waiverToDelete,
  componentHash,
  tabId,
  repositoryId,
}) {
  const mutatedViolations = mapApplicableWaiversToViolations(waivers, violations);
  const orderedViolations = violations ? sortPolicyByThreat(mutatedViolations) : [];
  const [showViolationsDetailPopover, showPopover] = useState(false);
  const [selectPolicyId, savePolicyId] = useState('');
  const dispatch = useDispatch();
  const boundSetWaiverToDelete = useMemo(
    () => bindActionCreators(WaiverActionCreators.setWaiverToDelete, dispatch),
    [dispatch]
  );

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
        <NxTableBody emptyMessage="No policy violations">
          {orderedViolations.map((violation) => (
            <FirewallPolicyViolationsTableRow
              key={violation.policyViolationId}
              showPopover={showPopover}
              orderedViolations={orderedViolations}
              violation={violation}
              selectPolicyId={selectPolicyId}
              savePolicyId={savePolicyId}
              showProxyState={showProxyState}
            />
          ))}
        </NxTableBody>
      </NxTable>
      {showViolationsDetailPopover && (
        <PolicyViolationDetailsPopover
          showViolationsDetailPopover={showViolationsDetailPopover}
          selectPolicyId={selectPolicyId}
          savePolicyId={savePolicyId}
          showPopover={showPopover}
          componentHash={componentHash}
          tabId={tabId}
          repositoryId={repositoryId}
        />
      )}
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
  componentNameWithoutVersion: PropTypes.string,
  waivers: PropTypes.arrayOf(PropTypes.shape(waiverType)),
  waiverToDelete: PropTypes.shape(waiverType),
  setWaiverToDelete: PropTypes.func,
  componentHash: PropTypes.string,
  tabId: PropTypes.string,
  repositoryId: PropTypes.string,
};
