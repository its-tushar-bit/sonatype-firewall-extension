/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { groupBy } from 'ramda';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

import { componentDetailsTagsPropTypes } from '../ComponentDetailsHeader';
import TransferList from '../TransferList/TransferList';

export default function ManageComponentLabels({
  applicableLabels = [],
  selectedLabels = [],
  loadError,
  loading,
  loadApplicableLabels,
}) {
  const selectedLabelsSet = new Set(selectedLabels.map(({ id }) => id));
  const groupedItems = groupBy((item) => (selectedLabelsSet.has(item.id) ? 'selected' : 'available'), applicableLabels);
  const available = groupedItems.available || [];
  const selected = groupedItems.selected || [];

  useEffect(() => {
    loadApplicableLabels();
  }, []);

  return (
    <NxLoadWrapper error={loadError} loading={loading} retryHandler={loadApplicableLabels}>
      {() => (
        <div className="nx-tile">
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2" id="iq-manage-labels__tile__title">
                Manage Labels
              </h2>
            </div>
          </header>
          <div className="nx-tile-content">
            <TransferList available={available} selected={selected} onChange={() => {}} />
          </div>
        </div>
      )}
    </NxLoadWrapper>
  );
}

ManageComponentLabels.propTypes = {
  applicableLabels: componentDetailsTagsPropTypes.labels,
  selectedLabels: componentDetailsTagsPropTypes.labels,
  loadApplicableLabels: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
};
