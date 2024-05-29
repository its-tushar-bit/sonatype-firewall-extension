/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxButton, NxFooter, NxH2, NxModal, NxFieldset, NxRadio } from '@sonatype/react-shared-components';

import { selectBillOfMaterialsPage } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';
import {
  actions,
  EXPORT_SBOM_FILE_FORMAT,
  EXPORT_SBOM_SPECIFICATION,
  EXPORT_SBOM_STATE,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';

export default function ExportAugmentedSbomModal() {
  const dispatch = useDispatch();
  const {
    exportAugmentedSbomModal: { showModal, sbomSpecification, sbomFileFormat },
  } = useSelector(selectBillOfMaterialsPage);

  const closeModal = () => dispatch(actions.setShowExportAugmentedSbomModal(false));
  const exportAndDownloadSbom = () => dispatch(actions.exportAndDownloadSbom({ state: EXPORT_SBOM_STATE.current }));

  const createRadioHandler = (name, currentValue, actionCreator) => (value) => ({
    name,
    value,
    onChange: () => dispatch(actionCreator(value)),
    isChecked: currentValue === value,
  });

  const createSbomSpecificationRadioHandler = createRadioHandler(
    'sbom-specification',
    sbomSpecification,
    actions.setExportAugmentedSbomSpecification
  );

  const createSbomFileFormatRadioHandler = createRadioHandler(
    'sbom-file-format',
    sbomFileFormat,
    actions.setExportAugmentedSbomFileFormat
  );

  return (
    <>
      {showModal && (
        <NxModal
          onCancel={closeModal}
          aria-labelledby="export-augmented-sbom-modal-title"
          id="export-augmented-sbom-modal"
        >
          <NxModal.Header>
            <NxH2 id="export-augmented-sbom-modal-title">Export Augmented SBOM</NxH2>
          </NxModal.Header>
          <NxModal.Content tabIndex={0}>
            <NxFieldset label="SBOM Specification">
              <NxRadio {...createSbomSpecificationRadioHandler(EXPORT_SBOM_SPECIFICATION.cyclonedx)}>
                Cyclone DX
              </NxRadio>
              <NxRadio {...createSbomSpecificationRadioHandler(EXPORT_SBOM_SPECIFICATION.spdx)}>SPDX</NxRadio>
            </NxFieldset>
            <NxFieldset label="SBOM Format">
              <NxRadio {...createSbomFileFormatRadioHandler(EXPORT_SBOM_FILE_FORMAT.json)}>JSON</NxRadio>
              <NxRadio {...createSbomFileFormatRadioHandler(EXPORT_SBOM_FILE_FORMAT.xml)}>XML</NxRadio>
            </NxFieldset>
          </NxModal.Content>
          <NxFooter>
            <div className="nx-btn-bar">
              <NxButton onClick={closeModal}>Cancel</NxButton>
              <NxButton variant="primary" onClick={exportAndDownloadSbom}>
                Export
              </NxButton>
            </div>
          </NxFooter>
        </NxModal>
      )}
    </>
  );
}
