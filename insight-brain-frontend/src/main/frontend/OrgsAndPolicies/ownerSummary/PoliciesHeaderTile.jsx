/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import { NxH2, NxTile, NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';

export default function PoliciesHeaderTile() {
  const dispatch = useDispatch();
  const ownerName = useSelector(selectSelectedOwnerName);

  const goToCreatePolicy = () => dispatch(actions.goToCreatePolicy());

  return (
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
  );
}
