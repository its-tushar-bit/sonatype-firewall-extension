/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import StatusIndicatorIcon from '../react/statusIndicatorIcon/StatusIndicatorIcon';
import * as PropTypes from 'prop-types';
import { NxTextLink } from '@sonatype/react-shared-components';

export default function FirewallAutoUnquarantineStatus(props) {
  // Actions
  const { openConfigurationModal } = props;

  //viewState
  const { enabledPolicyConditionTypesCount, totalPolicyConditionTypesCount } = props;

  //configurationState
  const { autoUnquarantineEnabled } = props;

  return (
    <section id="firewall-auto-unquarantine-status" className="nx-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Auto Release from Quarantine Status</h3>
      </header>
      <div className="nx-card__content">
        <div className="iq-status-indicator">
          <StatusIndicatorIcon status={autoUnquarantineEnabled} />
          <span>{autoUnquarantineEnabled ? 'Active' : 'Inactive'}</span>
        </div>
        <div className="nx-card__text">
          releasing {enabledPolicyConditionTypesCount} of {totalPolicyConditionTypesCount} policy condition types
        </div>
      </div>
      <footer className="nx-card__footer">
        <NxTextLink onClick={openConfigurationModal}>Configure</NxTextLink>
      </footer>
    </section>
  );
}

FirewallAutoUnquarantineStatus.propTypes = {
  autoUnquarantineEnabled: PropTypes.bool.isRequired,
  enabledPolicyConditionTypesCount: PropTypes.number.isRequired,
  totalPolicyConditionTypesCount: PropTypes.number.isRequired,
  openConfigurationModal: PropTypes.func.isRequired,
};
