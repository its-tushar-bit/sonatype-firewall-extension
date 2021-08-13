/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faArrowFromLeft } from '@fortawesome/pro-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import React from 'react';
import { useRouterState } from '../../react/RouterStateContext';
import IqPopover from '../../react/IqPopover/IqPopover';
import ViolationPageContainer from '../../violation/ViolationPageContainer';

export default function PolicyViolationDetailsPopover({ onClose }) {
  const uiRouterState = useRouterState();
  return (
    <IqPopover size="automatic" onClose={onClose}>
      <IqPopover.Header id="policy-violation-detail-header" className="policy-violation-detail-header">
        <div className="policy-violation-detail-header__title">
          <h2 className="nx-h2">Violation Detail</h2>
          <NxButton id="policy-violation-close-btn" onClick={onClose} variant="icon-only" title="Close">
            <NxFontAwesomeIcon icon={faArrowFromLeft} />
          </NxButton>
        </div>
      </IqPopover.Header>
      <ViolationPageContainer $state={uiRouterState} isFromPolicyViolations />
    </IqPopover>
  );
}

PolicyViolationDetailsPopover.propTypes = {
  onClose: PropTypes.func.isRequired,
};
