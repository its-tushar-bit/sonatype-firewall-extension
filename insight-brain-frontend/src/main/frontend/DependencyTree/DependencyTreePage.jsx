/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NxLoadWrapper, NxPageMain, NxTile, NxFilterInput, NxP, NxTextLink } from '@sonatype/react-shared-components';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import ComponentDetailsReportInfo from 'MainRoot/componentDetails/ComponentDetailsHeader/ComponentDetailsReportInfo';
import DependencyTree from './DependencyTree';
import {
  loadReportIfNeeded,
  setDependencyTreeSearchTerm,
  setDependencyTreeRouterParamsForBackButton,
  toggleTreePathAction,
} from 'MainRoot/applicationReport/applicationReportActions';
import { selectApplicationInfo, selectComponentMetaData } from '../componentDetails/componentDetailsSelectors';
import {
  selectIsDependenciesLoading,
  selectDependencyTreeIsAvailable,
  selectDisplayedDependencyTree,
  selectDependencyTreeSearchTerm,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

export default function DependencyTreePage() {
  const dispatch = useDispatch();

  const setCurrentRouterParams = () => dispatch(setDependencyTreeRouterParamsForBackButton());
  const updateDependencyTreeSearchTerm = (searchTerm) => dispatch(setDependencyTreeSearchTerm(searchTerm));
  const dependencyTreeSearchTerm = useSelector(selectDependencyTreeSearchTerm);
  const dependencyTree = useSelector(selectDisplayedDependencyTree),
    applicationInfo = useSelector(selectApplicationInfo),
    metadata = useSelector(selectComponentMetaData),
    loading = useSelector(selectIsDependenciesLoading),
    dependencyTreeIsAvailable = useSelector(selectDependencyTreeIsAvailable),
    loadReport = () => dispatch(loadReportIfNeeded());

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
      <NxP>
        Only supported ecosystem components are displayed in dependency tree.{' '}
        <NxTextLink external href="http://links.sonatype.com/products/nxiq/doc/dependency-tree">
          View more details here.
        </NxTextLink>
      </NxP>
      <NxTile data-testid="dependency-tree-tile">
        <NxTile.Content>
          <NxLoadWrapper
            loading={loading}
            retryHandler={loadReport}
            error={!loading && !dependencyTreeIsAvailable ? 'Dependency tree not available.' : null}
          >
            <NxFilterInput
              id="iq-dependency-tree-component-name-filter-input"
              placeholder="component name"
              value={dependencyTreeSearchTerm}
              onChange={updateDependencyTreeSearchTerm}
            />
            {dependencyTreeSearchTerm && isNilOrEmpty(dependencyTree) ? (
              <p className="iq-dependency-tree__empty">No matching components</p>
            ) : (
              <DependencyTree
                items={dependencyTree}
                rootName={applicationInfo?.applicationName}
                treePathToggleAction={toggleTreePathAction}
                searchTerm={dependencyTreeSearchTerm}
              />
            )}
          </NxLoadWrapper>
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
}
