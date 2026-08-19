/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  NxLoadWrapper,
  NxPageMain,
  NxTile,
  NxP,
  NxTextLink,
  NxButton,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { debounce } from 'debounce';

import BackButton from 'MainRoot/applicationReport/BackButton';
import ComponentDetailsReportInfo from 'MainRoot/componentDetails/ComponentDetailsHeader/ComponentDetailsReportInfo';
import DependencyTree from './DependencyTree';
import { isFlatDependencyTree } from './dependencyTreeUtil';
import {
  loadReportIfNeeded,
  setDependencyTreeSearchTerm,
  setDependencyTreeRouterParamsForBackButton,
  toggleTreePathAction,
  collapseAllDependencyTreeNodes,
  expandAllDependencyTreeNodes,
} from 'MainRoot/applicationReport/applicationReportActions';
import { selectApplicationInfo, selectComponentMetaData } from '../componentDetails/componentDetailsSelectors';
import {
  selectIsDependenciesLoading,
  selectDependencyTreeIsAvailable,
  selectDisplayedDependencyTree,
  selectDependencyTreeSearchTerm,
  selectLoadError,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import IqStatefulFilterInput from 'MainRoot/react/IqStatefulFilterInput';

const INPUT_DEBOUNCE_TIME = 500;

export default function DependencyTreePage() {
  const dispatch = useDispatch();

  const setCurrentRouterParams = () => dispatch(setDependencyTreeRouterParamsForBackButton());
  const collapseAllNodes = () => dispatch(collapseAllDependencyTreeNodes());
  const expandAllNodes = () => dispatch(expandAllDependencyTreeNodes());
  const dependencyTreeSearchTerm = useSelector(selectDependencyTreeSearchTerm);
  const dependencyTree = useSelector(selectDisplayedDependencyTree);
  const applicationInfo = useSelector(selectApplicationInfo);
  const metadata = useSelector(selectComponentMetaData);
  const loading = useSelector(selectIsDependenciesLoading);
  const error = useSelector(selectLoadError);
  const dependencyTreeIsAvailable = useSelector(selectDependencyTreeIsAvailable);
  const isFlatTree = isFlatDependencyTree(dependencyTree);
  const loadReport = () => dispatch(loadReportIfNeeded());
  const debouncedSetDependencyTreeSearchTerm = useCallback(
    debounce((value) => dispatch(setDependencyTreeSearchTerm(value)), INPUT_DEBOUNCE_TIME),
    []
  );

  useEffect(() => {
    loadReport();
    setCurrentRouterParams();
  }, []);

  return (
    <NxPageMain className="iq-dependency-tree-page">
      <BackButton />
      <header className="nx-page-title">
        <h1 className="nx-h1 iq-dependency-tree__title">Dependency Tree</h1>
        <ComponentDetailsReportInfo className="nx-page-title__description" {...(metadata || {})} />
      </header>
      <NxP>
        Only supported ecosystem components are displayed in dependency tree.{' '}
        <NxTextLink external href="http://links.sonatype.com/products/nxiq/doc/dependency-tree">
          View more details here.
        </NxTextLink>
      </NxP>
      {dependencyTreeIsAvailable || error || loading ? (
        <NxTile>
          <NxLoadWrapper loading={loading} retryHandler={loadReport} error={error}>
            <NxTile.Header>
              <IqStatefulFilterInput
                id="iq-dependency-tree-component-name-filter-input"
                placeholder="component name"
                onChange={debouncedSetDependencyTreeSearchTerm}
                defaultValue={dependencyTreeSearchTerm}
              />
              <div className="nx-tile__actions">
                <NxButton
                  id="iq-dependency-tree__expand-all-button"
                  variant="tertiary"
                  disabled={isFlatTree}
                  onClick={expandAllNodes}
                >
                  Expand All
                </NxButton>
                <NxButton
                  id="iq-dependency-tree__collapse-all-button"
                  variant="tertiary"
                  disabled={isFlatTree}
                  onClick={collapseAllNodes}
                >
                  Collapse All
                </NxButton>
              </div>
            </NxTile.Header>
            <NxTile.Content>
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
            </NxTile.Content>
          </NxLoadWrapper>
        </NxTile>
      ) : (
        <NxWarningAlert>Dependency tree not available.</NxWarningAlert>
      )}
    </NxPageMain>
  );
}
