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

import {
  selectIsInnerSourceRepositoriesEnabled,
  selectIsInnerSourceRepositorySupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';
import {
  selectInheritedFromOrganizationName,
  selectInnerSourceRepositoriesEnabled,
  selectLoadError,
  selectLoading,
  selectRepositoryConnections,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function InnerSourceRepositoryTile() {
  const dispatch = useDispatch();
  const isInnerSourceRepositorySupported = useSelector(selectIsInnerSourceRepositorySupported);
  const isInnerSourceRepositoriesEnabled = useSelector(selectIsInnerSourceRepositoriesEnabled);

  const currentOwner = useSelector(selectSelectedOwner);

  const load = () => dispatch(actions.load({ inherit: true }));

  useEffect(() => {
    if (isInnerSourceRepositorySupported && isInnerSourceRepositoriesEnabled) {
      load();
    }
  }, [currentOwner, isInnerSourceRepositorySupported]);

  const isLoading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const innerSourceRepositories = useSelector(selectRepositoryConnections);
  const innerSourceRepositoriesInheritedFrom = useSelector(selectInheritedFromOrganizationName);
  const listHeader = innerSourceRepositoriesInheritedFrom
    ? `Inherited from ${innerSourceRepositoriesInheritedFrom}`
    : 'Local';
  const innerSourceRepositoriesEnabled = useSelector(selectInnerSourceRepositoriesEnabled);
  const innerSourceRepositoriesIsEmpty = innerSourceRepositoriesEnabled && !innerSourceRepositories.length;

  const goToEditInnerSourceRepositoryPage = () => {
    dispatch(actions.goToEditPage());
  };

  if (!isInnerSourceRepositorySupported || !isInnerSourceRepositoriesEnabled) {
    return null;
  }

  return (
    isInnerSourceRepositoriesEnabled && (
      <NxTile id="owner-pill-innersource-repository">
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={load}>
          <NxTile.Header>
            <NxTile.Headings>
              <NxTile.HeaderTitle>
                <NxH2>InnerSource Repositories</NxH2>
              </NxTile.HeaderTitle>
              <NxTile.HeaderSubtitle>Configure repositories to identify InnerSource components.</NxTile.HeaderSubtitle>
            </NxTile.Headings>
            <NxTile.HeaderActions>
              <NxButton
                variant="tertiary"
                onClick={goToEditInnerSourceRepositoryPage}
                id="innersource-repositories-edit"
                title="edit"
              >
                <NxFontAwesomeIcon icon={faPen} />
                <span>Edit</span>
              </NxButton>
            </NxTile.HeaderActions>
          </NxTile.Header>
          <NxTile.Content>
            <NxH3>{listHeader}</NxH3>
            <NxList>
              {innerSourceRepositoriesEnabled &&
                innerSourceRepositories.map(({ repositoryConnectionId, baseUrl, format }) => (
                  <NxList.Item key={repositoryConnectionId}>
                    <NxList.Text>{baseUrl}</NxList.Text>
                    <NxList.Subtext>{format}</NxList.Subtext>
                  </NxList.Item>
                ))}
              {innerSourceRepositoriesIsEmpty && (
                <NxList.Item>
                  <NxList.Text>No InnerSource repository connections are configured</NxList.Text>
                </NxList.Item>
              )}
              {!innerSourceRepositoriesEnabled && (
                <NxList.Item>
                  <NxList.Text>InnerSource repository connections are disabled</NxList.Text>
                </NxList.Item>
              )}
            </NxList>
          </NxTile.Content>
        </NxLoadWrapper>
      </NxTile>
    )
  );
}
