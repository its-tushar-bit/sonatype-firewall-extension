/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper, NxButton } from '@sonatype/react-shared-components';

import PolicyViolationsTable from './PolicyViolationsTable';

export const ViewAllComponentWaiversButton = ({ toggleComponentWaiversPopover }) => (
  <NxButton id="component-details-view-waivers" variant="tertiary" onClick={() => toggleComponentWaiversPopover()}>
    <span>View Existing Waivers</span>
  </NxButton>
);
ViewAllComponentWaiversButton.propTypes = {
  toggleComponentWaiversPopover: PropTypes.func.isRequired,
};

export const ViewTransitiveViolationsButton = ({ stateGo, ownerType, ownerId, scanId, hash }) => (
  <NxButton
    id="component-details-view-transitive-violations"
    variant="tertiary"
    onClick={() => {
      stateGo('transitiveViolations', {
        ownerType: ownerType,
        ownerId: ownerId,
        scanId: scanId,
        hash: hash,
      });
    }}
  >
    <span>View Transitive Violations</span>
  </NxButton>
);
ViewTransitiveViolationsButton.propTypes = {
  stateGo: PropTypes.func.isRequired,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string,
  scanId: PropTypes.string,
  hash: PropTypes.string,
};

export default function ViolationsTableTile({
  isLoadingComponentDetails,
  componentDetailsLoadError,
  loadComponentDetails,
  violationType,
  setViolationType,
  title,
  showViewAllComponents,
  showViewTransitiveViolations,
  stateGo,
  ownerType,
  ownerId,
  scanId,
  hash,
  ...tableProps
}) {
  useEffect(() => {
    setViolationType(violationType);
  }, [violationType]);

  const loading = isLoadingComponentDetails || !tableProps.componentName;

  return (
    <section className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2" id="violations__tile__title">
            {title}
          </h2>
        </div>
        {(showViewAllComponents || showViewTransitiveViolations) && (
          <div className="nx-tile__actions">
            {showViewTransitiveViolations && (
              <ViewTransitiveViolationsButton
                stateGo={stateGo}
                ownerType={ownerType}
                ownerId={ownerId}
                scanId={scanId}
                hash={hash}
              />
            )}
            {showViewAllComponents && !loading && (
              <ViewAllComponentWaiversButton toggleComponentWaiversPopover={tableProps.toggleComponentWaiversPopover} />
            )}
          </div>
        )}
      </header>
      <div className="nx-tile-content">
        <NxLoadWrapper loading={loading} error={componentDetailsLoadError} retryHandler={loadComponentDetails}>
          {() => <PolicyViolationsTable {...tableProps} />}
        </NxLoadWrapper>
      </div>
    </section>
  );
}

ViolationsTableTile.propTypes = {
  isLoadingComponentDetails: PropTypes.bool.isRequired,
  componentDetailsLoadError: PropTypes.string,
  loadComponentDetails: PropTypes.func.isRequired,
  violationType: PropTypes.string,
  setViolationType: PropTypes.func.isRequired,
  showViewAllComponents: PropTypes.bool,
  showViewTransitiveViolations: PropTypes.bool.isRequired,
  stateGo: PropTypes.func.isRequired,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  scanId: PropTypes.string.isRequired,
  hash: PropTypes.string.isRequired,
  title: PropTypes.string,
  ...PolicyViolationsTable.propTypes,
};
