/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import {
  NxH2,
  NxTile,
  NxButton,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxList,
  NxTableContainer,
} from '@sonatype/react-shared-components';
import { always, compose, propEq, reject, when, complement, isNil } from 'ramda';

import { selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectEntityId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectPoliciesByOwner,
  selectPolicyTileLoading,
  selectPolicyTileLoadError,
  selectPolicyTileSortingCollapsible,
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

  const isSbomManager = useSelector(selectIsSbomManager);
  const policiesByOwner = useSelector(selectPoliciesByOwner);
  const actionStages = compose(
    when(complement(isNil), reject(propEq('stageTypeId', 'compliance'))),
    when(always(isSbomManager), always([]))
  )(useSelector(selectActionStageTypes));

  const isEnforcementSupported = useSelector(selectIsEnforcementSupported);
  const isFirewallSupported = useSelector(selectIsFirewallSupported);

  const loading = useSelector(selectPolicyTileLoading);
  const loadError = useSelector(selectPolicyTileLoadError);
  const entityId = useSelector(selectEntityId);
  const collapsibleSorting = useSelector(selectPolicyTileSortingCollapsible);

  const doLoad = () => dispatch(actions.loadPolicyTile());

  useEffect(() => {
    doLoad();
  }, [entityId]);

  const goToCreatePolicy = () => dispatch(actions.goToCreatePolicy());

  const stagesNumber = `policy-tile__stages-num--${actionStages ? actionStages.length : 7}`;

  const isNoPoliciesDefined = !policiesByOwner?.some((owner) => {
    return owner.policies.length > 0;
  });

  return (
    <NxTile id="owner-pill-policy" data-testid="policies-tile">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>Policies</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Headings>
          <NxTile.HeaderActions>
            {isSbomManager ? null : (
              <NxButton variant="tertiary" id="add-policy-button" onClick={goToCreatePolicy}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Add a Policy</span>
              </NxButton>
            )}
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content className={stagesNumber}>
          <NxTile.Subsection>
            {isNoPoliciesDefined ? (
              <NxList emptyMessage="No local policies defined" />
            ) : (
              <NxTableContainer>
                <PoliciesTable
                  ariaLabel="Policy tile"
                  policiesByOwner={policiesByOwner ?? []}
                  stages={actionStages}
                  isFirewallSupported={isFirewallSupported}
                  isEnforcementSupported={isEnforcementSupported}
                  collapsibleSorting={collapsibleSorting}
                />
              </NxTableContainer>
            )}
          </NxTile.Subsection>
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
