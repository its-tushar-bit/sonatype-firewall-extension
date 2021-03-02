/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import StatusIndicatorIcon from '../../react/statusIndicatorIcon/StatusIndicatorIcon';
import * as PropTypes from 'prop-types';
import {NxOverflowTooltip} from '@sonatype/react-shared-components';

export default function FirewallPolicyConditionTypes(props) {
  // Actions
  const {
    openConfigurationModal
  } = props;

  return (
    <section id="firewall-policy-condition-types" className="nx-card">
      <header className="nx-card__header">
        <h3 className="nx-h3">Policies to be Auto Released from Quarantine</h3>
      </header>
      <div className="iq-firewall-policy-condition-types nx-card__content">
        <NxOverflowTooltip>
          <div className="iq-status-indicator">
            <StatusIndicatorIcon status={true}/>
            <span>Integrity-Suspicious</span>
          </div>
        </NxOverflowTooltip>
        <NxOverflowTooltip>
          <div className="iq-status-indicator">
            <StatusIndicatorIcon status={true}/>
            <span>License-Modified Weak CXXXXXX</span>
          </div>
        </NxOverflowTooltip>
        <NxOverflowTooltip>
          <div className="iq-status-indicator">
            <StatusIndicatorIcon status={true}/>
            <span>License-Commercial</span>
          </div>
        </NxOverflowTooltip>
        <NxOverflowTooltip>
          <div className="iq-status-indicator">
            <StatusIndicatorIcon status={true}/>
            <span>License-Non Standard</span>
          </div>
        </NxOverflowTooltip>
        <NxOverflowTooltip>
          <div className="iq-status-indicator">
            <StatusIndicatorIcon status={true}/>
            <span>Lincense-Threat Not AssiXXXXXX</span>
          </div>
        </NxOverflowTooltip>
        <a id="firewall-policy-condition-types-config-link"
           className="nx-text-link"
           onClick={openConfigurationModal}>more active policies</a>
      </div>
    </section>
  );
}

FirewallPolicyConditionTypes.propTypes = {
  openConfigurationModal: PropTypes.func.isRequired
};

