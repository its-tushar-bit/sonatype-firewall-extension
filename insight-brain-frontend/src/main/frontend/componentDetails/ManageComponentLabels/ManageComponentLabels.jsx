/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { selectHasCustomComponentLabels } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { NxLoadWrapper, NxSubmitMask } from '@sonatype/react-shared-components';

import { componentDetailsTagsPropTypes } from '../ComponentDetailsHeader';
import TransferList from '../TransferList/TransferList';
import RemoveLabelModal from './RemoveLabelModal/RemoveLabelModalContainer';
import ApplyLabelModalContainer from './ApplyLabelModal/ApplyLabelModalContainer';
import EnterprisePopover from 'MainRoot/shared/enterpriseTier/EnterprisePopover';
import './_ManageComponentLabels.scss';

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

  const hasCustomComponentLabels = useSelector(selectHasCustomComponentLabels);
  const labelsToUse = applicableLabels;
  const available = labelsToUse.filter((item) => !selectedLabelsSet.has(item.id)) || [];
  const selected = selectedLabels || [];

  useEffect(() => {
    loadApplicableLabels();
  }, []);

  const handleViewCustomLabels = () => {
    // ROOT_ORGANIZATION_ID is the actual backend ID for the root org (see Organization.java)
    window.location.href = '/assets/#/management/edit/organization/ROOT_ORGANIZATION_ID/label';
  };

  const transferListContent = (
    <div>
      <TransferList
        available={available}
        selected={selected}
        onAddItem={handleAddLabelTag}
        onRemoveItem={handleRemoveLabelTag}
      />
    </div>
  );

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
              {!hasCustomComponentLabels ? (
                <EnterprisePopover
                  featureId="labels"
                  highlightText="Go beyond default labels"
                  content="—create your own labels to organize components and prioritize what matters most."
                  linkText="View custom component labels"
                  onLinkClick={handleViewCustomLabels}
                >
                  {transferListContent}
                </EnterprisePopover>
              ) : (
                transferListContent
              )}
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
