/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxLoadWrapper, NxSubmitMask } from '@sonatype/react-shared-components';

import { componentDetailsTagsPropTypes } from '../ComponentDetailsHeader';
import TransferList from '../TransferList/TransferList';
import RemoveLabelModal from './RemoveLabelModal/RemoveLabelModalContainer';
import ApplyLabelModalContainer from './ApplyLabelModal/ApplyLabelModalContainer';

export default function ManageComponentLabels({
  applicableLabels = [],
  loadError,
  loading,
  loadApplicableLabels,
  handleRemoveLabelTag,
  handleAddLabelTag,
  selectedLabels = [],
  applyLabelMaskState,
}) {
  const selectedLabelsSet = new Set(selectedLabels.map(({ id }) => id));
  const available = applicableLabels.filter((item) => !selectedLabelsSet.has(item.id)) || [];
  const selected = selectedLabels || [];

  useEffect(() => {
    loadApplicableLabels();
  }, []);

  return (
    <Fragment>
      {applyLabelMaskState !== null && (
        <NxSubmitMask success={applyLabelMaskState} message="Applying label…" successMessage="Success!" />
      )}
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
              <RemoveLabelModal />
              <ApplyLabelModalContainer />
              <TransferList
                available={available}
                selected={selected}
                onAddItem={handleAddLabelTag}
                onRemoveItem={handleRemoveLabelTag}
              />
            </div>
          </div>
        )}
      </NxLoadWrapper>
    </Fragment>
  );
}

ManageComponentLabels.propTypes = {
  applicableLabels: componentDetailsTagsPropTypes.labels,
  selectedLabels: componentDetailsTagsPropTypes.labels,
  loadApplicableLabels: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  handleRemoveLabelTag: PropTypes.func.isRequired,
  handleAddLabelTag: PropTypes.func.isRequired,
  applyLabelMaskState: PropTypes.bool,
};
