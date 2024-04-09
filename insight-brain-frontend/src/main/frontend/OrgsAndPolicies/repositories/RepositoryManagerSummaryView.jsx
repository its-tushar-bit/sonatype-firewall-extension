/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { getAddIconUrl } from 'MainRoot/util/CLMLocation';
import { NxLoadWrapper, NxPageTitle, NxH1, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faCubes } from '@fortawesome/pro-solid-svg-icons';
import { selectLoading, selectLoadError } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import {
  selectSelectedOwner,
  selectLoadError as selectLoadSelectedOwnerError,
  selectEntityId,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';

import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import RepositoryManagerPills from 'MainRoot/OrgsAndPolicies/repositories/RepositoryManagerPills';
import NamespaceConfusionProtectionTile from '../repositories/namespaceConfusionProtectionTile/NamespaceConfusionProtectionTile';
import RepositoriesConfigurationTile from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesConfigurationTile';
import ActionDropdown from 'MainRoot/OrgsAndPolicies/actionDropdown/ActionDropdown';
import DeleteOwnerModal from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/DeleteOwnerModal';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';

export default function RepositoryManagerSummaryView() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const loadSelectedOwnerError = useSelector(selectLoadSelectedOwnerError);
  const entityId = useSelector(selectEntityId);
  const owner = useSelector(selectSelectedOwner);

  const doLoad = () => dispatch(actions.loadOwnerSummary());
  const getIconUrl = () => getAddIconUrl('repository_manager', owner.id) + `?${Math.random()}`;

  useEffect(() => {
    if (entityId) {
      doLoad();
    }
  }, [entityId]);

  return (
    <NxLoadWrapper loading={loading} error={loadError || loadSelectedOwnerError} retryHandler={doLoad}>
      <div id="repository-page">
        <header>
          <NxPageTitle id="repositories-summary" className="iq-page-title">
            <NxH1>
              <span className="nx-icon">
                <img src={getIconUrl()} />
              </span>
              <span>{owner.name}</span>
            </NxH1>
            <div className="nx-btn-bar">
              <ActionDropdown />
            </div>
          </NxPageTitle>
          <RepositoryManagerPills />
        </header>

        <div
          className="iq-tile-scroll-container iq-tile-scroll-container--owner-summary-view nx-viewport-sized__scrollable"
          id="repositories-summary-sections"
        >
          <div id="scrollable-content">
            <RepositoriesConfigurationTile />
            <PoliciesTile />
            <NamespaceConfusionProtectionTile sortFilterSectionValues={`repository-manager_${entityId}`} />
            <AccessTile />
          </div>
        </div>
      </div>
      <DeleteOwnerModal />
    </NxLoadWrapper>
  );
}
