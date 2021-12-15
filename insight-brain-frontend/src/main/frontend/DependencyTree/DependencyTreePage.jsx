/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxLoadWrapper, NxPageMain, NxTile, NxErrorAlert } from '@sonatype/react-shared-components';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import ComponentDetailsReportInfo from 'MainRoot/componentDetails/ComponentDetailsHeader/ComponentDetailsReportInfo';
import DependencyTree from './DependencyTree';
import {
  loadReportIfNeeded,
  setDependencyTreeRouterParamsForBackButton,
} from 'MainRoot/applicationReport/applicationReportActions';
import { selectApplicationInfo, selectComponentMetaData } from '../componentDetails/componentDetailsSelectors';
import {
  selectIsDependenciesLoading,
  selectDependencyTreeData,
  selectDependencyTreeIsAvailable,
} from 'MainRoot/applicationReport/applicationReportSelectors';

export default function DependencyTreePage() {
  const dispatch = useDispatch();

  const dependencyTree = useSelector(selectDependencyTreeData),
    applicationInfo = useSelector(selectApplicationInfo),
    metadata = useSelector(selectComponentMetaData),
    loading = useSelector(selectIsDependenciesLoading),
    dependencyTreeIsAvailable = useSelector(selectDependencyTreeIsAvailable),
    loadReport = () => dispatch(loadReportIfNeeded());
  const setCurrentRouterParams = () => dispatch(setDependencyTreeRouterParamsForBackButton());

  useEffect(() => {
    loadReport();
    setCurrentRouterParams();
  }, []);

  return (
    <NxPageMain className="iq-dependency-tree-page">
      <MenuBarBackButton stateName="applicationReport.policy" />
      <header className="nx-page-title">
        <h1 className="nx-h1 iq-dependency-tree__title">Dependency Tree</h1>
        {dependencyTreeIsAvailable && (
          <ComponentDetailsReportInfo
            data-testid="dependency-tree-page-header-breadcrumbs"
            className="nx-page-title__description"
            {...(metadata || {})}
          />
        )}
      </header>
      {dependencyTreeIsAvailable ? (
        <NxTile data-testid="dependency-tree-tile">
          <NxTile.Content>
            <NxLoadWrapper loading={loading} retryHandler={loadReport}>
              <DependencyTree dependencyTree={dependencyTree} rootName={applicationInfo?.applicationName} />
            </NxLoadWrapper>
          </NxTile.Content>
        </NxTile>
      ) : (
        <NxErrorAlert>Dependency tree not available.</NxErrorAlert>
      )}
    </NxPageMain>
  );
}
