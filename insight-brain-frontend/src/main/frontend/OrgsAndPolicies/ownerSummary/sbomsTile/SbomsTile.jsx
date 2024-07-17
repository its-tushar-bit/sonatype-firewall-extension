/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { faPlus, faEllipsisV } from '@fortawesome/pro-solid-svg-icons';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxTile,
  NxH2,
  NxLoadWrapper,
  NxTable,
  NxTooltip,
  NxSmallThreatCounter,
  NxPagination,
  NxIconDropdown,
  NxTextLink,
} from '@sonatype/react-shared-components';
import moment from 'moment';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { actions as importSbomActions } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';
import {
  selectSbomsResults,
  selectError,
  selectCurrentPage,
  selectPageCount,
  selectVersionForActions,
  selectApplicationId,
  selectLoading,
  selectSortDir,
  selectDeleteError,
  selectDeleteMaskState,
  selectShowDeleteModal,
} from './sbomsTileSelectors.js';
import { actions } from './sbomsTileSlice.js';
import { getDownloadSbomFileUrl } from 'MainRoot/util/CLMLocation';
import DeleteModal from './DeleteModal.jsx';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function SbomsTile() {
  const dispatch = useDispatch();
  const openModal = () => dispatch(importSbomActions.setIsModalOpen(true));
  const doLoad = () => dispatch(actions.loadSbomTableData());
  const uiRouterState = useRouterState();
  const [selectedSbom, setSelectedSbom] = useState({});
  const sbomTableData = useSelector(selectSbomsResults);
  const sbomTableError = useSelector(selectError);
  const downloadSbomUrl = getDownloadSbomFileUrl;
  const currentPage = useSelector(selectCurrentPage);
  const pageCount = useSelector(selectPageCount);
  const deleteMaskState = useSelector(selectDeleteMaskState);
  const showDeleteModal = useSelector(selectShowDeleteModal);
  const sortDir = useSelector(selectSortDir);
  const deleteError = useSelector(selectDeleteError);
  const selectedVersionForActions = useSelector(selectVersionForActions);
  const applicationId = useSelector(selectApplicationId);
  const loading = useSelector(selectLoading);
  const selectedApplication = useSelector(selectSelectedOwner);

  const isActionsOpen = (version) => {
    return !isNilOrEmpty(selectedVersionForActions) && selectedVersionForActions === version;
  };

  const onDeleteModalClick = (sbom) => {
    dispatch(actions.setShowDeleteModal(true));
    setSelectedSbom(sbom);
  };

  const cancelDeleteModal = () => {
    dispatch(actions.setShowDeleteModal(false));
    setSelectedSbom({});
  };

  const onActionsToggleCollapse = (version) => dispatch(actions.setSelectedVersionForActions(version));

  const generateTableBodyRows = () => {
    if (!isNilOrEmpty(sbomTableData)) {
      return (
        <>
          {sbomTableData.map((sbom) => (
            <NxTable.Row key={sbom.applicationVersion}>
              <NxTable.Cell>
                <NxTooltip
                  title={sbom.applicationVersion}
                  className="sbom-manager-owner-summary-sboms-tile-table__version-link-tooltip"
                >
                  <NxTextLink
                    className="sbom-manager-owner-summary-sboms-tile-table__version-link"
                    href={uiRouterState.href('sbomManager.management.view.bom', {
                      applicationPublicId: selectedApplication.publicId,
                      versionId: sbom.applicationVersion,
                    })}
                  >
                    {sbom.applicationVersion}
                  </NxTextLink>
                </NxTooltip>
              </NxTable.Cell>
              <NxTable.Cell>
                <NxSmallThreatCounter
                  maxDigits={2}
                  criticalCount={sbom.critical}
                  severeCount={sbom.high}
                  moderateCount={sbom.medium}
                  lowCount={sbom.low}
                />
              </NxTable.Cell>
              <NxTable.Cell>
                {sbom.spec} {sbom.specVersion}
              </NxTable.Cell>
              <NxTable.Cell>{moment(sbom.importDate).format('YYYY-MM-DD HH:mm:ss')}</NxTable.Cell>
              <NxTable.Cell>
                <NxIconDropdown
                  isOpen={isActionsOpen(sbom.applicationVersion)}
                  onToggleCollapse={() => onActionsToggleCollapse(sbom.applicationVersion)}
                  icon={faEllipsisV}
                  title="Options"
                  aria-label={sbom.applicationVersion + '-options'}
                >
                  <button
                    onClick={() => window.open(downloadSbomUrl(applicationId, sbom.applicationVersion), '_blank')}
                    className="nx-dropdown-button"
                  >
                    Download SBOM report
                  </button>
                  <button onClick={() => onDeleteModalClick(sbom)} className="nx-dropdown-button delete-sbom">
                    Delete SBOM
                  </button>
                </NxIconDropdown>
              </NxTable.Cell>
            </NxTable.Row>
          ))}
        </>
      );
    }
  };

  const getCurrentPage = () => (pageCount === 0 ? null : currentPage);

  const handlePageChange = (page) => {
    dispatch(actions.setCurrentPage(page));
    doLoad();
  };

  const handleSortChange = () => {
    dispatch(actions.toggleSortDir());
    doLoad();
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxTile id="owner-pill-sboms">
      <NxLoadWrapper retryHandler={() => {}}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>SBOMs</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderActions>
              <NxButton id="import-sbom-button" variant="tertiary" onClick={openModal}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Import SBOM</span>
              </NxButton>
            </NxTile.HeaderActions>
          </NxTile.Headings>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable className="sbom-manager-owner-summary-sboms-tile-table">
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell>Versions</NxTable.Cell>
                <NxTable.Cell>Vulnerabilities</NxTable.Cell>
                <NxTable.Cell>BOM Format</NxTable.Cell>
                <NxTable.Cell isSortable sortDir={sortDir} onClick={handleSortChange}>
                  Import Date
                </NxTable.Cell>
                <NxTable.Cell>Actions</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body error={sbomTableError} emptyMessage="No SBOMs found" isLoading={loading}>
              {generateTableBodyRows()}
            </NxTable.Body>
          </NxTable>
          <div className="nx-table-container__footer">
            <NxPagination pageCount={pageCount} currentPage={getCurrentPage()} onChange={handlePageChange} />
          </div>
        </NxTile.Content>
      </NxLoadWrapper>
      {showDeleteModal && (
        <DeleteModal
          sbom={selectedSbom}
          deleteSbomFromTable={(applicationVersion) => dispatch(actions.deleteSbomFromTable(applicationVersion))}
          deleteError={deleteError}
          deleteMaskState={deleteMaskState}
          onCancel={cancelDeleteModal}
          applicationName={selectedApplication.name}
        ></DeleteModal>
      )}
    </NxTile>
  );
}
