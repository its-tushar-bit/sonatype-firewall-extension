/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { faPlus } from '@fortawesome/free-solid-svg-icons';
import { NxButton, NxTile, NxLoadWrapper, NxH2, NxFontAwesomeIcon, NxH3 } from '@sonatype/react-shared-components';
import { map, prop } from 'ramda';

import { actions } from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSlice';
import {
  selectApplicableLicenseThreatGroup,
  selectIsLoading,
  selectLicenseThreatGroupLoadError,
} from 'MainRoot/OrgsAndPolicies/licenseThreatGroupSelectors';
import { selectSelectedOwner, selectOwnerProperties } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import ApplicableLicenseThreatGroupTable from 'MainRoot/OrgsAndPolicies/ownerSummary/licenseThreatGroupSummaryTile/ApplicableLicenseThreatGroupTable';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

export default function LicenseThreatGroupSummaryTile() {
  const dispatch = useDispatch();
  const doLoad = () => dispatch(actions.loadApplicableLicenseThreatGroups());
  const goToNewLTG = () => dispatch(actions.goToCreateLTG());

  const applicableLTGs = useSelector(selectApplicableLicenseThreatGroup);
  const selectedOwner = useSelector(selectSelectedOwner);
  const selectedOwnerProperties = useSelector(selectOwnerProperties);
  const loading = useSelector(selectIsLoading);
  const error = useSelector(selectLicenseThreatGroupLoadError);
  const currentOwnerType = prop('ownerType', selectedOwnerProperties);
  const currentOwnerName = prop('name', selectedOwner);

  useEffect(() => {
    doLoad();
  }, [selectedOwner]);

  const renderApplicableLicenseThreatGroup = (props) => {
    const isLocalForApp =
      !props.inherited && props.ownerType === 'application' && isNilOrEmpty(props.licenseThreatGroups);
    const isEmptyForOrg =
      isNilOrEmpty(props.licenseThreatGroups) && props.inherited && props.ownerType === 'organization';
    if (isLocalForApp || isEmptyForOrg) {
      return null;
    }

    const name = props.inherited ? props.ownerName : 'local';
    const title = props.inherited ? `Inherited from ${name}` : 'Local';

    return (
      <NxTile.Subsection key={props.ownerId}>
        <NxTile.SubsectionHeader>
          <NxH3>{title}</NxH3>
        </NxTile.SubsectionHeader>
        <ApplicableLicenseThreatGroupTable {...props} key={props.ownerId} />
      </NxTile.Subsection>
    );
  };

  const renderContent = () => {
    return map(renderApplicableLicenseThreatGroup, applicableLTGs);
  };

  return (
    <NxTile id="owner-pill-ltgs">
      <NxLoadWrapper loading={loading} error={error} retryHandler={doLoad}>
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>License Threat Groups</NxH2>
          </NxTile.HeaderTitle>
          <NxTile.HeaderSubtitle>available to {currentOwnerName} policies</NxTile.HeaderSubtitle>
          {currentOwnerType === 'organization' ? (
            <NxTile.HeaderActions>
              <NxButton variant="tertiary" onClick={goToNewLTG} id="add-ltg-button">
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Add a Threat Group</span>
              </NxButton>
            </NxTile.HeaderActions>
          ) : null}
        </NxTile.Header>
        <NxTile.Content>{renderContent()}</NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
