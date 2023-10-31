/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { faPlus } from '@fortawesome/free-solid-svg-icons';
import { NxButton, NxTile, NxLoadWrapper, NxH2, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { prop } from 'ramda';

import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import {
  selectApplicableLicenseThreatGroup,
  selectIsLoading,
  selectLicenseThreatGroupLoadError,
} from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import { selectOwnerProperties, selectEntityId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import ApplicableLicenseThreatGroupTable from 'MainRoot/OrgsAndPolicies/ownerSummary/licenseThreatGroupSummaryTile/ApplicableLicenseThreatGroupTable';

export default function LicenseThreatGroupSummaryTile() {
  const dispatch = useDispatch();
  const doLoad = () => dispatch(actions.loadApplicableLicenseThreatGroups());
  const goToNewLTG = () => dispatch(actions.goToCreateLTG());

  const applicableLTGs = useSelector(selectApplicableLicenseThreatGroup);
  const entityId = useSelector(selectEntityId);
  const selectedOwnerProperties = useSelector(selectOwnerProperties);
  const loading = useSelector(selectIsLoading);
  const error = useSelector(selectLicenseThreatGroupLoadError);
  const currentOwnerType = prop('ownerType', selectedOwnerProperties);

  useEffect(() => {
    doLoad();
  }, [entityId]);

  return (
    <NxTile id="owner-pill-ltgs">
      <NxLoadWrapper loading={loading} error={error} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>License Threat Groups</NxH2>
          </NxTile.HeaderTitle>
          {currentOwnerType === 'organization' ? (
            <NxTile.HeaderActions>
              <NxButton variant="tertiary" onClick={goToNewLTG} id="add-ltg-button">
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Add a Threat Group</span>
              </NxButton>
            </NxTile.HeaderActions>
          ) : null}
        </NxTile.Header>
        <NxTile.Content>
          <ApplicableLicenseThreatGroupTable applicableLTGs={applicableLTGs} />
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
