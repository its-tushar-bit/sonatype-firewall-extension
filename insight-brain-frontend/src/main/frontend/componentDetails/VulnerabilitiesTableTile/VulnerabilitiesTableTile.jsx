/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';

import VulnerabilitiesTable, { vulnerabilitiesPropTypes } from './VulnerabilitiesTable';
import { VulnerabilityDetailsPopoverContainer } from './VulnerabilityDetailsPopoverContainer';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

export default function VulnerabilitiesTableTile(props) {
  const { isLoadingComponentDetails, componentDetailsLoadError, loadComponentDetails, ...tableProps } = props;
  return (
    <section className="nx-tile">
      <VulnerabilityDetailsPopoverContainer />
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2" id="component-details-vulnerabilities-title">
            Vulnerabilities
          </h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <NxLoadWrapper
          loading={isLoadingComponentDetails}
          error={componentDetailsLoadError}
          retryHandler={loadComponentDetails}
        >
          {() => <VulnerabilitiesTable {...tableProps} />}
        </NxLoadWrapper>
      </div>
    </section>
  );
}

VulnerabilitiesTableTile.propTypes = {
  isLoadingComponentDetails: PropTypes.bool.isRequired,
  componentDetailsLoadError: PropTypes.string,
  loadComponentDetails: PropTypes.func.isRequired,
  loadVulnerabilities: PropTypes.func.isRequired,
  vulnerabilities: PropTypes.shape(vulnerabilitiesPropTypes),
  toggleVulnerabilityPopoverWithEffects: PropTypes.func.isRequired,
};
