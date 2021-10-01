/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';
import { useRouterState } from '../../react/RouterStateContext';
import IqPopover from '../../react/IqPopover/IqPopover';
import ViolationPageContainer from '../../violation/ViolationPageContainer';

export default function PolicyViolationDetailsPopover({ onClose }) {
  const uiRouterState = useRouterState();
  return (
    <IqPopover size="extra-large" onClose={onClose} id="component-details-policy-violations-popover">
      <IqPopover.Header
        id="policy-violation-detail-header"
        className="policy-violation-detail-header"
        buttonId="policy-violation-close-btn"
        onClose={onClose}
        headerTitle="Violation Detail"
      />
      <ViolationPageContainer $state={uiRouterState} isFromPolicyViolations />
    </IqPopover>
  );
}

PolicyViolationDetailsPopover.propTypes = {
  onClose: PropTypes.func.isRequired,
};
