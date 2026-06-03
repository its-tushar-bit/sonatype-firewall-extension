/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxPageTitle,
  NxH1,
  NxH3,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxOverflowTooltip,
} from '@sonatype/react-shared-components';
import { faDatabase } from '@fortawesome/pro-solid-svg-icons';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import { selectLoading, selectLoadError } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import {
  selectSelectedOwner,
  selectLoadError as selectLoadSelectedOwnerError,
  selectEntityId,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';
import NamespaceConfusionProtectionTile from 'MainRoot/OrgsAndPolicies/repositories/namespaceConfusionProtectionTile/NamespaceConfusionProtectionTile';
import ActionDropdown from 'MainRoot/OrgsAndPolicies/actionDropdown/ActionDropdown';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import RepositorySummaryPills from './RepositorySummaryPills';

export default function RepositorySummaryView() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const loadSelectedOwnerError = useSelector(selectLoadSelectedOwnerError);
  const repositoryId = useSelector(selectEntityId);
  const repository = useSelector(selectSelectedOwner);
  const isHostedRepository = repository.repositoryType === 'hosted';

  const loadOwnerSummary = () => dispatch(actions.loadOwnerSummary());

  useEffect(() => {
    if (repositoryId) loadOwnerSummary();
  }, [repositoryId]);

  return (
    <NxLoadWrapper loading={loading} error={loadError || loadSelectedOwnerError} retryHandler={loadOwnerSummary}>
      <div id="repository-page">
        <header>
          <NxPageTitle id="repositories-summary" className="iq-page-title">
            <NxOverflowTooltip placement="bottom">
              <NxH1>
                <NxFontAwesomeIcon icon={faDatabase} className="iq-page-title__repository-name-icon" />
                <span>{repository.publicId}</span>
              </NxH1>
            </NxOverflowTooltip>
            {!isHostedRepository && (
              <div className="nx-btn-bar">
                <ActionDropdown />
              </div>
            )}
          </NxPageTitle>
          <NxH3 className="iq-page-title__repository-format-type">
            ({repository.format} : {repository.repositoryType})
          </NxH3>
          <RepositorySummaryPills isHosted={isHostedRepository} />
        </header>
        <div
          className="iq-tile-scroll-container iq-tile-scroll-container--owner-summary-view nx-viewport-sized__scrollable"
          id="repositories-summary-sections"
        >
          <div id="scrollable-content">
            {!isHostedRepository && <PoliciesTile />}
            {isHostedRepository && (
              <NamespaceConfusionProtectionTile sortFilterSectionValues={`repository_${repository.publicId}`} />
            )}
            <AccessTile />
          </div>
        </div>
      </div>
    </NxLoadWrapper>
  );
}
