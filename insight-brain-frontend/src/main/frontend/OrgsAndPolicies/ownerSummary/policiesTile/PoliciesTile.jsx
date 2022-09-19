/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import { find, propEq, filter, isEmpty } from 'ramda';
import {
  NxH2,
  NxH3,
  NxTile,
  NxButton,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxList,
} from '@sonatype/react-shared-components';
import { selectSelectedOwnerName, selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectPoliciesByOwner,
  selectPolicyTileLoading,
  selectPolicyTileLoadError,
  selectPolicyTileSorting,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectActionStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import PoliciesTable from './PoliciesTable';

export default function PoliciesTile() {
  const dispatch = useDispatch();

  const ownerName = useSelector(selectSelectedOwnerName);
  const policiesByOwner = useSelector(selectPoliciesByOwner);
  const actionStages = useSelector(selectActionStageTypes);
  const isEnforcementSupported = useSelector(selectIsEnforcementSupported);
  const isFirewallSupported = useSelector(selectIsFirewallSupported);
  const loading = useSelector(selectPolicyTileLoading);
  const loadError = useSelector(selectPolicyTileLoadError);
  const selectedOwner = useSelector(selectSelectedOwner);
  const sorting = useSelector(selectPolicyTileSorting);

  const doLoad = () => dispatch(actions.loadPolicyTile());

  useEffect(() => {
    doLoad();
  }, [selectedOwner]);

  const goToCreatePolicy = () => dispatch(actions.goToCreatePolicy());

  const local = find(propEq('inherited', false), policiesByOwner ?? []);
  const inherited = filter(propEq('inherited', true), policiesByOwner ?? []);
  const stagesNumber = `policy-tile__stages-num--${actionStages?.length || 7}`;

  return (
    <NxTile id="owner-pill-policy">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>Policies</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderSubtitle>applying to {ownerName}</NxTile.HeaderSubtitle>
          </NxTile.Headings>
          <NxTile.HeaderActions>
            <NxButton variant="tertiary" id="add-policy-button" onClick={goToCreatePolicy}>
              <NxFontAwesomeIcon icon={faPlus} />
              <span>Add a Policy</span>
            </NxButton>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content className={stagesNumber}>
          <NxTile.Subsection>
            <NxTile.SubsectionHeader>
              <NxH3>Local</NxH3>
            </NxTile.SubsectionHeader>
            {isEmpty(local?.policies) ? (
              <NxList emptyMessage="No local policies defined" />
            ) : (
              <PoliciesTable
                ariaLabel="Policy tile local policies"
                owner={local}
                stages={actionStages}
                isFirewallSupported={isFirewallSupported}
                isEnforcementSupported={isEnforcementSupported}
                sorting={sorting}
              />
            )}
          </NxTile.Subsection>
          {inherited?.map((owner) => {
            if (!isEmpty(owner.policies)) {
              return (
                <NxTile.Subsection key={owner.ownerId}>
                  <NxTile.SubsectionHeader>
                    <NxH3>Inherited from {owner.ownerName}</NxH3>
                  </NxTile.SubsectionHeader>
                  <PoliciesTable
                    ariaLabel={`Policy tile inherited from ${owner.ownerName} policies`}
                    emptyMessage="No policies defined"
                    owner={owner}
                    stages={actionStages}
                    isFirewallSupported={isFirewallSupported}
                    isEnforcementSupported={isEnforcementSupported}
                    sorting={sorting}
                  />
                </NxTile.Subsection>
              );
            }
          })}
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
