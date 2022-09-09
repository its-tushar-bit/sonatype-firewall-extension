/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import IqPopover from 'MainRoot/react/IqPopover/IqPopover';
import ViolationPageContainer from 'MainRoot/violation/ViolationPageContainer';
export default function PolicyViolationDetailsPopover({
  showPopover,
  showViolationsDetailPopover,
  selectPolicyId,
  savePolicyId,
}) {
  const uiRouterState = useRouterState();

  return (
    <IqPopover size="extra-large" onClose={() => showPopover(false)} id="component-details-policy-violations-popover">
      <IqPopover.Header
        id="policy-violation-detail-header"
        className="policy-violation-detail-header"
        buttonId="policy-violation-close-btn"
        onClose={() => showPopover(false)}
        headerTitle="Violation Details"
      />
      <ViolationPageContainer
        $state={uiRouterState}
        showViolationsDetailPopover={showViolationsDetailPopover}
        selectPolicyId={selectPolicyId}
        savePolicyId={savePolicyId}
        isFromPolicyViolations
      />
    </IqPopover>
  );
}

PolicyViolationDetailsPopover.propTypes = {
  showPopover: PropTypes.func,
  showViolationsDetailPopover: PropTypes.bool,
  selectPolicyId: PropTypes.string,
  savePolicyId: PropTypes.func,
};
