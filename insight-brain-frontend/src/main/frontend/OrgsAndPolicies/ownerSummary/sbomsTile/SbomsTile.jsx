/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { faPlus, faEllipsisV } from '@fortawesome/pro-solid-svg-icons';
import {
  NxBinaryDonutChart,
  NxButton,
  NxFontAwesomeIcon,
  NxH2,
  NxIconDropdown,
  NxLoadWrapper,
  NxPagination,
  NxSmallThreatCounter,
  NxTable,
  NxTextLink,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import debounce from 'debounce';
import moment from 'moment';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectSbomsTile } from './sbomsTileSelectors.js';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from './sbomsTileSlice.js';
import { actions as importSbomActions } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';
import { actions as sbomExportActions } from 'MainRoot/sbomManager/features/sbomExport/sbomExportSlice';
import { getDownloadSbomFileUrl, getSbomDownloadPdfUrl } from 'MainRoot/util/CLMLocation';
import {
  additionalExportOptionsDisabledDueToValidationErrors,
  exportPDFIsDisabledDueToValidationErrors,
} from 'MainRoot/sbomManager/features/messages';

import DeleteModal from './DeleteModal.jsx';
import SbomAdditionalExportOptionsModal from 'MainRoot/sbomManager/features/sbomExport/SbomAdditionalExportOptionsModal';
import ExportModalSubmitMask from 'MainRoot/sbomManager/features/sbomExport/ExportModalSubmitMask';
import InvalidSbomIndicator from 'MainRoot/sbomManager/features/InvalidSbomIndicator';
import InvalidSbomTooltipWrapper from 'MainRoot/sbomManager/features/sbomExport/InvalidSbomTooltipWrapper';

import { SORT_BY_FIELDS, SORT_DIRECTION } from './sbomsTileSlice';

const LOAD_SBOMS_DEBOUNCE_TIMEOUT_MS = 300;

export default function SbomsTile() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const openModal = () => dispatch(importSbomActions.setIsModalOpen(true));
  const loadSboms = () => dispatch(actions.loadSbomTableData());
  const resetConfigurations = () => dispatch(actions.resetConfigurations());

  const debouncedLoadSboms = useCallback(debounce(loadSboms, LOAD_SBOMS_DEBOUNCE_TIMEOUT_MS), []);
  const loadSortedSboms = (sortBy) => {
    dispatch(actions.setSortByAndCycleDirection(sortBy));
    debouncedLoadSboms();
  };

  const [selectedSbom, setSelectedSbom] = useState({});

  const {
    applicationId,
    currentPage,
    deleteError,
    deleteMaskState,
    error,
    loading,
    pageCount,
    sboms,
    sbomsTotalCount,
    selectedVersionForActions,
    showDeleteModal,
    sortConfiguration,
  } = useSelector(selectSbomsTile);

  const selectedApplication = useSelector(selectSelectedOwner);

  useEffect(() => {
    resetConfigurations();
    loadSboms();
  }, []);

  const showSbomAdditionalExportOptionsModal = (applicationId, sbomVersion) =>
    dispatch(
      sbomExportActions.setShowSbomAdditionalExportOptionsModal({
        applicationId,
        sbomVersion,
      })
    );

  const onDeleteModalClick = (sbom) => {
    dispatch(actions.setShowDeleteModal(true));
    setSelectedSbom(sbom);
  };

  const cancelDeleteModal = () => {
    dispatch(actions.setShowDeleteModal(false));
    setSelectedSbom({});
  };

  const isActionsOpen = (version) => !isNilOrEmpty(selectedVersionForActions) && selectedVersionForActions === version;
  const onActionsToggleCollapse = (version) => dispatch(actions.setSelectedVersionForActions(version));

  const hasSboms = !isNilOrEmpty(sboms);
  const showTableContent = !loading && !error && hasSboms;

  const getCurrentPage = () => (pageCount === 0 ? null : currentPage);

  const handlePageChange = (page) => {
    dispatch(actions.setCurrentPage(page));
    loadSboms();
  };

  const createColumnSortHandler = (field) =>
    showTableContent && sbomsTotalCount > 1
      ? {
          sortDir: field === sortConfiguration.sortBy ? sortConfiguration.sortDirection : SORT_DIRECTION.DEFAULT,
          onClick: () => loadSortedSboms(field),
          isSortable: true,
        }
      : {};

  const tableRows = () =>
    hasSboms
      ? sboms.map((sbom) => (
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
              {!sbom.isValid && <InvalidSbomIndicator />}
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
              {typeof sbom.releaseStatusPercentage === 'number' ? (
                <div className="sbom-manager-owner-summary-sboms-tile-table__releaseStatusPercentage">
                  <NxBinaryDonutChart
                    className="sbom-manager-owner-summary-sboms-tile-table__releaseStatusPercentageDonut"
                    value={sbom.releaseStatusPercentage}
                    aria-label={`${sbom.releaseStatusPercentage}% release ready`}
                    innerRadiusPercent={80}
                  />
                  <span>{sbom.releaseStatusPercentage}%</span>
                </div>
              ) : null}
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
                  onClick={() => window.open(getDownloadSbomFileUrl(applicationId, sbom.applicationVersion), '_blank')}
                  className="nx-dropdown-button"
                >
                  Export Original SBOM
                </button>
                <InvalidSbomTooltipWrapper
                  isValid={sbom.isValid}
                  text={additionalExportOptionsDisabledDueToValidationErrors}
                >
                  <button
                    onClick={() => showSbomAdditionalExportOptionsModal(applicationId, sbom.applicationVersion)}
                    className={`nx-dropdown-button${sbom.isValid ? '' : ' disabled'}`}
                    disabled={!sbom.isValid}
                  >
                    Additional Export Options
                  </button>
                </InvalidSbomTooltipWrapper>
                <InvalidSbomTooltipWrapper isValid={sbom.isValid} text={exportPDFIsDisabledDueToValidationErrors}>
                  <NxTextLink
                    className="nx-dropdown-button"
                    href={getSbomDownloadPdfUrl(applicationId, sbom.applicationVersion)}
                    disabled={!sbom.isValid}
                  >
                    Export PDF
                  </NxTextLink>
                </InvalidSbomTooltipWrapper>
                <button onClick={() => onDeleteModalClick(sbom)} className="nx-dropdown-button delete-sbom">
                  Delete SBOM
                </button>
              </NxIconDropdown>
            </NxTable.Cell>
          </NxTable.Row>
        ))
      : null;

  return (
    <NxTile id="owner-pill-sboms">
      <ExportModalSubmitMask />
      <SbomAdditionalExportOptionsModal />
      <NxLoadWrapper retryHandler={() => {}}>
        <NxTile.Header>
          <NxTile.Headings>
            <NxTile.HeaderTitle>
              <NxH2>SBOMs</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderActions>
              <NxButton id="import-sbom-button" variant="tertiary" onClick={openModal}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Import</span>
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
                <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.releaseStatus)}>Release Status</NxTable.Cell>
                <NxTable.Cell>BOM Format</NxTable.Cell>
                <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.importDate)}>Import Date</NxTable.Cell>
                <NxTable.Cell>Actions</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body error={error} emptyMessage="No SBOMs found" isLoading={loading}>
              {tableRows()}
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
