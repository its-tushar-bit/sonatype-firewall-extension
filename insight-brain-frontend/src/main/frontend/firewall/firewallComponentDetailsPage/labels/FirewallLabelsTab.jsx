/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxLoadWrapper, NxSubmitMask } from '@sonatype/react-shared-components';

import TransferList from 'MainRoot/componentDetails/TransferList/TransferList';
import RemoveLabelModal from 'MainRoot/componentDetails/ManageComponentLabels/RemoveLabelModal/RemoveLabelModalContainer';
import ApplyLabelModalContainer from 'MainRoot/componentDetails/ManageComponentLabels/ApplyLabelModal/ApplyLabelModalContainer';
import { actions } from 'MainRoot/componentDetails/componentDetailsSlice';

import { selectFirewallComponentDetailsIsLoading } from 'MainRoot/firewall/firewallSelectors';
import {
  selectApplicableLabels,
  selectApplyLabelMaskState,
  selectLabels,
  selectApplicableLabelsLoadError,
  selectIsApplicableLabelsLoading,
} from 'MainRoot/componentDetails/componentDetailsSelectors';

export default function FirewallLabelsTab() {
  const loadError = useSelector(selectApplicableLabelsLoadError);
  const isApplicableLabelsLoading = useSelector(selectIsApplicableLabelsLoading);
  const firewallComponentDetailsIsLoading = useSelector(selectFirewallComponentDetailsIsLoading);
  const applyLabelMaskState = useSelector(selectApplyLabelMaskState);
  const applicableLabels = useSelector(selectApplicableLabels) || [];
  const selectedLabels = useSelector(selectLabels) || [];
  const dispatch = useDispatch();
  const loadApplicableLabels = () => {
    dispatch(firewallLoadApplicableLabels());
  };

  useEffect(loadApplicableLabels, []);

  const loading = isApplicableLabelsLoading || firewallComponentDetailsIsLoading;
  const selectedLabelsSet = new Set(selectedLabels.map(({ id }) => id));
  const available = applicableLabels.filter((item) => !selectedLabelsSet.has(item.id)) || [];
  const selected = selectedLabels || [];
  const { firewallLoadApplicableLabels, handleAddLabelTag, handleRemoveLabelTag } = actions;

  return (
    <>
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
                onAddItem={(labelDetails, ownerType) => {
                  dispatch(handleAddLabelTag(labelDetails, ownerType));
                }}
                onRemoveItem={(labelDetails, ownerType) => dispatch(handleRemoveLabelTag(labelDetails, ownerType))}
              />
            </div>
          </div>
        )}
      </NxLoadWrapper>
    </>
  );
}
