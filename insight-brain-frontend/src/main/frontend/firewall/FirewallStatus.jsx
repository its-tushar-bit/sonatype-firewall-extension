/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import classnames from 'classnames';
import * as PropTypes from 'prop-types';

export default function FirewallStatus(props) {
  const { totalComponentCount, repositoryCount, quarantineEnabledRepositoryCount } = props;

  const allRepositoriesAreProtected = quarantineEnabledRepositoryCount === repositoryCount;
  const statusIndicator = (
    <span
      role="status"
      className={classnames('iq-firewall-status__status-indicator', 'nx-status-indicator', {
        'nx-status-indicator--positive': allRepositoriesAreProtected,
        'nx-status-indicator--intermediate': !allRepositoriesAreProtected,
      })}
    >
      <strong>{quarantineEnabledRepositoryCount.toLocaleString('en-US')}</strong> of{' '}
      <strong>{repositoryCount.toLocaleString('en-US')}</strong> repositories protected
    </span>
  );

  return (
    <section id="firewall-status" className="iq-firewall-status">
      <div className="iq-firewall-status__status">
        {statusIndicator}
        <div className="iq-firewall-status__components-monitored">
          <span>{totalComponentCount.toLocaleString('en-US')}</span> components monitored
        </div>
      </div>
    </section>
  );
}

FirewallStatus.propTypes = {
  totalComponentCount: PropTypes.number.isRequired,
  repositoryCount: PropTypes.number.isRequired,
  quarantineEnabledRepositoryCount: PropTypes.number.isRequired,
};
