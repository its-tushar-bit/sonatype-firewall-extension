/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxH2,
  NxH3,
  NxDivider,
  NxTile,
  NxButton,
  NxFontAwesomeIcon,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import { faPen } from '@fortawesome/free-solid-svg-icons';
import { selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectEntityId, selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectLoading,
  selectLoadError,
  selectApplicationReportsStages,
  selectSuccessMetrics,
} from 'MainRoot/OrgsAndPolicies/retentionSelectors';
import { actions, NOT_ENABLED } from 'MainRoot/OrgsAndPolicies/retentionSlice';
import RetentionTable from './RetentionTable';
import { selectIsDataRetentionEnabled, selectTenantMode } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function RetentionTile() {
  const dispatch = useDispatch();
  const selectedOwner = useSelector(selectSelectedOwner);
  const entityId = useSelector(selectEntityId);
  const isOrg = useSelector(selectIsOrganization);
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const successMetrics = useSelector(selectSuccessMetrics);
  const stages = useSelector(selectApplicationReportsStages);

  const isDataRetentionEnabled = useSelector(selectIsDataRetentionEnabled);
  const isMultiTenant = useSelector(selectTenantMode) === 'multi-tenant';
  const isFeatureEnabledForLicense = isDataRetentionEnabled && !isMultiTenant;

  const goToEditRetention = () => dispatch(actions.goToEditRetention());
  const doLoad = () => dispatch(actions.loadRetention());

  useEffect(() => {
    if (isOrg) {
      doLoad();
    }
  }, [entityId]);

  function getSuccessMetricsMaxAge() {
    const age = successMetrics.maxAge?.trimmedValue;
    const unit = successMetrics.maxAgeUnit;
    const ageUnit = age === '1' ? unit.slice(0, unit.length - 1) : unit;
    return (
      <div className="retention-tile__success-metrics">
        {successMetrics.enablePurging ? (
          <>
            <span className="retention-tile__max-age">Max Age: </span>
            {age} {ageUnit}
          </>
        ) : (
          NOT_ENABLED
        )}
      </div>
    );
  }

  return (
    isOrg &&
    isFeatureEnabledForLicense && (
      <NxTile id="owner-pill-retention" className="retention-tile">
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
          <NxTile.Header>
            <NxTile.Headings>
              <NxTile.HeaderTitle>
                <NxH2>Data Retention</NxH2>
              </NxTile.HeaderTitle>
              <NxTile.HeaderSubtitle>applying to {selectedOwner.name}</NxTile.HeaderSubtitle>
            </NxTile.Headings>
            <NxTile.HeaderActions>
              <NxButton variant="tertiary" id="edit-retention-button" onClick={goToEditRetention}>
                <NxFontAwesomeIcon icon={faPen} />
                <span>Edit</span>
              </NxButton>
            </NxTile.HeaderActions>
          </NxTile.Header>
          <NxTile.Content>
            <NxTile.Subsection>
              <NxTile.SubsectionHeader>
                <NxH3>Application Reports</NxH3>
              </NxTile.SubsectionHeader>
              <RetentionTable stages={stages} />
            </NxTile.Subsection>
            <NxTile.Subsection aria-labelledby="sucess-metrics-heading">
              <NxTile.SubsectionHeader>
                <NxH3 id="sucess-metrics-heading">Success Metrics</NxH3>
              </NxTile.SubsectionHeader>
              <NxDivider />
              {getSuccessMetricsMaxAge()}
            </NxTile.Subsection>
          </NxTile.Content>
        </NxLoadWrapper>
      </NxTile>
    )
  );
}
