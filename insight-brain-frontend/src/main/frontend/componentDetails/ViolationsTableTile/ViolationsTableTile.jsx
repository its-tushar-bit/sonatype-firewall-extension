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
  <NxButton id="component-details-view-waivers" variant="tertiary" onClick={toggleComponentWaiversPopover}>
    <span>View All Component Waivers</span>
  </NxButton>
);
ViewAllComponentWaiversButton.propTypes = {
  toggleComponentWaiversPopover: PropTypes.func.isRequired,
};

export default function ViolationsTableTile({
  isLoadingComponentDetails,
  componentDetailsLoadError,
  loadComponentDetails,
  violationType,
  setViolationType,
  title,
  showViewAllComponents,
  ...tableProps
}) {
  useEffect(() => {
    setViolationType(violationType);
  }, [violationType]);

  return (
    <section className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2" id="violations__tile__title">
            {title}
          </h2>
        </div>
        {showViewAllComponents && (
          <div className="nx-tile__actions">
            <ViewAllComponentWaiversButton toggleComponentWaiversPopover={tableProps.toggleComponentWaiversPopover} />
          </div>
        )}
      </header>
      <div className="nx-tile-content">
        <NxLoadWrapper
          loading={isLoadingComponentDetails || !tableProps.componentName}
          error={componentDetailsLoadError}
          retryHandler={loadComponentDetails}
        >
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
  title: PropTypes.string,
  ...PolicyViolationsTable.propTypes,
};
