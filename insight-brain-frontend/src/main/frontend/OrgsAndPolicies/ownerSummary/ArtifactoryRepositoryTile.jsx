/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxH2,
  NxTile,
  NxList,
  NxLoadWrapper,
  NxH3,
  NxButton,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { faPen } from '@fortawesome/pro-solid-svg-icons';

import { actions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';
import {
  selectIsArtifactoryRepositorySupported,
  selectLoadErrorFeatures,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectInheritedFromOrganizationName,
  selectArtifactoryRepositoriesEnabled,
  selectLoadError,
  selectLoading,
  selectArtifactoryConnection,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSelectors';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function ArtifactoryRepositoryTile() {
  const dispatch = useDispatch();

  const load = (data) => dispatch(actions.load(data));

  const isLoading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);

  const isArtifactoryRepositorySupported = useSelector(selectIsArtifactoryRepositorySupported);
  const ownerId = useSelector(selectSelectedOwnerId);
  const loadingFeatures = useSelector(selectLoadingFeatures);
  const loadFeaturesError = useSelector(selectLoadErrorFeatures);
  const artifactoryRepositories = useSelector(selectArtifactoryConnection);
  const artifactoryRepositoriesInheritedFrom = useSelector(selectInheritedFromOrganizationName);
  const artifactoryRepositoriesEnabled = useSelector(selectArtifactoryRepositoriesEnabled);

  const loading = loadingFeatures || isLoading;
  const error = loadFeaturesError || loadError;

  useEffect(() => {
    if (isArtifactoryRepositorySupported) {
      load({ ownerId: ownerId, inherit: true });
    }
  }, [ownerId, isArtifactoryRepositorySupported]);

  const goToEditArtifactoryRepositoryPage = () => {
    dispatch(actions.goToEditPage());
  };

  if (!isArtifactoryRepositorySupported) {
    return null;
  }

  return (
    <NxTile id="owner-pill-artifactory-repository">
      <NxLoadWrapper loading={loading} error={error} retryHandler={load}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>Artifactory Repository</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderSubtitle>Configure an Artifactory repository to identify components.</NxTile.HeaderSubtitle>
          </NxTile.Headings>
          <NxTile.HeaderActions>
            <NxButton
              variant="tertiary"
              onClick={goToEditArtifactoryRepositoryPage}
              id="artifactory-repositories-edit"
              title="edit"
            >
              <NxFontAwesomeIcon icon={faPen} />
              <span>Edit</span>
            </NxButton>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>
          <NxH3>
            {artifactoryRepositoriesInheritedFrom ? `Inherited from ${artifactoryRepositoriesInheritedFrom}` : 'Local'}
          </NxH3>
          <NxList>
            {artifactoryRepositoriesEnabled && artifactoryRepositories && (
              <NxList.Item>
                <NxList.Text>{artifactoryRepositories.baseUrl}</NxList.Text>
              </NxList.Item>
            )}
            {artifactoryRepositoriesEnabled && !artifactoryRepositories && (
              <NxList.Item>
                <NxList.Text>No Artifactory repository connection is configured</NxList.Text>
              </NxList.Item>
            )}
            {!artifactoryRepositoriesEnabled && (
              <NxList.Item>
                <NxList.Text>Artifactory repository connection is disabled</NxList.Text>
              </NxList.Item>
            )}
          </NxList>
        </NxTile.Content>
      </NxLoadWrapper>
    </NxTile>
  );
}
