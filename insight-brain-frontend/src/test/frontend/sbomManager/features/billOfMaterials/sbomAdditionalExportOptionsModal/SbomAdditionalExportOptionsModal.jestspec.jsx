/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { assocPath } from 'ramda';

import { fireEvent, render, screen } from 'TestRoot/SpecUtil';

import SbomAdditionalExportOptionsModal from 'MainRoot/sbomManager/features/billOfMaterials/sbomAdditionalExportOptionsModal/SbomAdditionalExportOptionsModal';
import {
  EXPORT_SBOM_SPECIFICATION,
  EXPORT_SBOM_FILE_FORMAT,
  exportAndDownloadSbomSubmitMaskInitialState,
  sbomAdditionalExportOptionsModalInitialState,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';

describe('SbomAdditionalExportOptionsModal', () => {
  const APPLICATION_PUBLIC_ID = 'APPLICATION-PUBLIC-ID';
  const SBOM_VERSION = 'SBOM-VERSION';

  const initialState = Object.freeze({
    router: {
      currentParams: {
        applicationPublicId: APPLICATION_PUBLIC_ID,
        versionId: SBOM_VERSION,
      },
    },
    billOfMaterialsPage: {
      sbomAdditionalExportOptionsModal: {
        ...sbomAdditionalExportOptionsModalInitialState,
        showModal: true,
      },
      exportAndDownloadSbomSubmitMask: { ...exportAndDownloadSbomSubmitMaskInitialState },
    },
  });

  const renderComponent = (state) => render(<SbomAdditionalExportOptionsModal />, { preloadedState: { ...state } });

  it('should render the correct content', () => {
    renderComponent(initialState);

    expect(screen.getByText(/Additional Export Options/)).toBeVisible();

    expect(screen.getByText(/SBOM Specification/)).toBeVisible();
    expect(screen.getByLabelText(/Cyclone DX/)).toBeVisible();
    expect(screen.getByLabelText(/Cyclone DX/)).toBeChecked();
    expect(screen.getByLabelText(/SPDX/)).toBeVisible();

    expect(screen.getByText(/SBOM Format/)).toBeVisible();
    expect(screen.getByLabelText(/JSON/)).toBeVisible();
    expect(screen.getByLabelText(/JSON/)).toBeChecked();
    expect(screen.getByLabelText(/XML/)).toBeVisible();

    expect(screen.getByRole('button', { name: /Cancel/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Export SBOM/ })).toBeVisible();
  });

  it('render the correct radios state', () => {
    const preloadedState = assocPath(
      ['billOfMaterialsPage', 'sbomAdditionalExportOptionsModal'],
      {
        showModal: true,
        sbomSpecification: EXPORT_SBOM_SPECIFICATION.spdx,
        sbomFileFormat: EXPORT_SBOM_FILE_FORMAT.xml,
      },
      initialState
    );

    renderComponent(preloadedState);

    expect(screen.getByLabelText(/SPDX/)).toBeChecked();
    expect(screen.getByLabelText(/XML/)).toBeChecked();
  });

  it('should render the correct content', () => {
    renderComponent(initialState);

    expect(screen.getByLabelText(/Cyclone DX/)).toBeChecked();
    expect(screen.getByLabelText(/JSON/)).toBeChecked();

    const spdxRadio = screen.getByLabelText(/SPDX/);
    const xmlRadio = screen.getByLabelText(/XML/);

    fireEvent.click(spdxRadio);

    expect(spdxRadio).toBeChecked();
    expect(screen.getByLabelText(/Cyclone DX/)).not.toBeChecked();

    fireEvent.click(xmlRadio);

    expect(xmlRadio).toBeChecked();
    expect(screen.getByLabelText(/JSON/)).not.toBeChecked();
  });

  it('should close the modal when you click "Cancel"', async () => {
    renderComponent(initialState);

    expect(screen.getByText(/Additional Export Options/)).toBeVisible();

    const cancelButton = screen.getByRole('button', { name: /Cancel/ });

    fireEvent.click(cancelButton);

    expect(await screen.queryByText(/Additional Export Options/)).not.toBeInTheDocument();
  });
});
