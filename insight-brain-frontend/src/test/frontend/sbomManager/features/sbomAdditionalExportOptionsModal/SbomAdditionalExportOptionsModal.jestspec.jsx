/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { assocPath } from 'ramda';
import userEvent from '@testing-library/user-event';

import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';

import SbomAdditionalExportOptionsModal from 'MainRoot/sbomManager/features/sbomExport/SbomAdditionalExportOptionsModal';
import {
  EXPORT_SBOM_SPECIFICATION,
  EXPORT_SBOM_FILE_FORMAT,
  exportAndDownloadSbomSubmitMaskInitialState,
  sbomAdditionalExportOptionsModalInitialState,
} from 'MainRoot/sbomManager/features/sbomExport/sbomExportSlice';
import { getDownloadSbomFileUrl } from 'MainRoot/util/CLMLocation';

describe('SbomAdditionalExportOptionsModal', () => {
  const APPLICATION_INTERNAL_ID = 'APPLICATION-INTERNAL-ID';
  const SBOM_VERSION = 'SBOM-VERSION';

  const initialState = Object.freeze({
    sbomExport: {
      ...sbomAdditionalExportOptionsModalInitialState,
      showModal: true,
      applicationId: APPLICATION_INTERNAL_ID,
      sbomVersion: SBOM_VERSION,
      exportAndDownloadSbomSubmitMask: { ...exportAndDownloadSbomSubmitMaskInitialState },
    },
  });

  const renderComponent = (state) => render(<SbomAdditionalExportOptionsModal />, { preloadedState: { ...state } });

  let axiosMock;
  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  it('should render the correct content', () => {
    renderComponent(initialState);

    expect(screen.getByText(/Additional Export Options/)).toBeVisible();

    expect(screen.getByText(/SBOM Specification/)).toBeVisible();
    expect(screen.getByLabelText('CycloneDX 1.7')).toBeVisible();
    expect(screen.getByLabelText('CycloneDX 1.7')).toBeChecked();
    expect(screen.getByLabelText('CycloneDX 1.6')).toBeVisible();
    expect(screen.getByLabelText('CycloneDX 1.6')).not.toBeChecked();
    expect(screen.getByLabelText(/SPDX 2\.3/)).toBeVisible();
    expect(screen.getByLabelText(/SPDX 3\.0/)).toBeVisible();

    expect(screen.getByText(/SBOM Format/)).toBeVisible();
    expect(screen.getByLabelText(/JSON/)).toBeVisible();
    expect(screen.getByLabelText(/JSON/)).toBeChecked();
    expect(screen.getByLabelText(/XML/)).toBeVisible();

    expect(screen.getByRole('button', { name: /Cancel/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Export SBOM/ })).toBeVisible();
  });

  it('downloads an SBOM from the additional export options modal with the default settings', async () => {
    const user = userEvent.setup();
    const downloadSbomFileUrl = getDownloadSbomFileUrl(
      APPLICATION_INTERNAL_ID,
      SBOM_VERSION,
      'current',
      'cyclonedx1.7'
    );
    axiosMock.onGet(downloadSbomFileUrl).reply(200, {});

    renderComponent(initialState);
    expect(screen.getByText(/Additional Export Options/)).toBeVisible();

    await user.click(screen.getByRole('button', { name: /Export SBOM/ }));
    expect(axiosMock.history.get[0].url).toBe(downloadSbomFileUrl);
    expect(axiosMock.history.get[0].headers).toHaveProperty('Accept', 'application/json');
  });

  it('downloads an SBOM from the additional export options modal with SPDX 2.3', async () => {
    const user = userEvent.setup();
    const downloadSbomFileUrl = getDownloadSbomFileUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION, 'current', 'spdx2.3');
    axiosMock.onGet(downloadSbomFileUrl).reply(200, {});

    renderComponent(initialState);
    expect(screen.getByText(/Additional Export Options/)).toBeVisible();

    const spdxRadio = screen.getByLabelText(/SPDX 2\.3/);
    await user.click(spdxRadio);
    expect(spdxRadio).toBeChecked();

    const xmlRadio = screen.getByLabelText(/^XML$/);
    await user.click(xmlRadio);
    expect(xmlRadio).toBeChecked();

    await user.click(screen.getByRole('button', { name: /Export SBOM/ }));
    expect(axiosMock.history.get[0].url).toBe(downloadSbomFileUrl);
    expect(axiosMock.history.get[0].headers).toHaveProperty('Accept', 'application/xml');
  });

  it('downloads an SBOM with SPDX 3.0 specification', async () => {
    const user = userEvent.setup();
    const downloadSbomFileUrl = getDownloadSbomFileUrl(APPLICATION_INTERNAL_ID, SBOM_VERSION, 'current', 'spdx3.0');
    axiosMock.onGet(downloadSbomFileUrl).reply(200, {});

    renderComponent(initialState);

    const spdx30Radio = screen.getByLabelText(/SPDX 3\.0/);
    await user.click(spdx30Radio);
    expect(spdx30Radio).toBeChecked();

    await user.click(screen.getByRole('button', { name: /Export SBOM/ }));
    expect(axiosMock.history.get[0].url).toBe(downloadSbomFileUrl);
    expect(axiosMock.history.get[0].headers).toHaveProperty('Accept', 'application/json');
  });

  it('disables XML format when SPDX 3.0 is selected', async () => {
    const user = userEvent.setup();
    renderComponent(initialState);

    const spdx30Radio = screen.getByLabelText(/SPDX 3\.0/);
    await user.click(spdx30Radio);

    const xmlRadio = screen.getByLabelText(/XML/);
    expect(xmlRadio).toBeDisabled();
  });

  it('auto-selects JSON when SPDX 3.0 is selected while XML was chosen', async () => {
    const user = userEvent.setup();
    const preloadedState = assocPath(
      ['sbomExport'],
      {
        showModal: true,
        sbomSpecification: EXPORT_SBOM_SPECIFICATION.spdx23,
        sbomFileFormat: EXPORT_SBOM_FILE_FORMAT.xml,
        exportAndDownloadSbomSubmitMask: { ...exportAndDownloadSbomSubmitMaskInitialState },
      },
      initialState
    );

    renderComponent(preloadedState);

    expect(screen.getByLabelText(/^XML$/)).toBeChecked();

    const spdx30Radio = screen.getByLabelText(/SPDX 3\.0/);
    await user.click(spdx30Radio);

    expect(screen.getByLabelText(/JSON/)).toBeChecked();
  });

  it('re-enables XML with plain label when switching away from SPDX 3.0', async () => {
    const user = userEvent.setup();
    renderComponent(initialState);

    const spdx30Radio = screen.getByLabelText(/SPDX 3\.0/);
    await user.click(spdx30Radio);

    expect(screen.getByLabelText(/XML \(not available for SPDX 3\.0\)/)).toBeDisabled();

    const cycloneDxRadio = screen.getByLabelText('CycloneDX 1.7');
    await user.click(cycloneDxRadio);

    const xmlRadio = screen.getByLabelText(/^XML$/);
    expect(xmlRadio).toBeEnabled();
    expect(xmlRadio).not.toBeChecked();
  });

  it('render the correct radios state', () => {
    const preloadedState = assocPath(
      ['sbomExport'],
      {
        showModal: true,
        sbomSpecification: EXPORT_SBOM_SPECIFICATION.spdx23,
        sbomFileFormat: EXPORT_SBOM_FILE_FORMAT.xml,
      },
      initialState
    );

    renderComponent(preloadedState);

    expect(screen.getByLabelText(/SPDX 2\.3/)).toBeChecked();
    expect(screen.getByLabelText(/^XML$/)).toBeChecked();
  });

  it('should handle specification and format selection', async () => {
    const user = userEvent.setup();
    renderComponent(initialState);

    expect(screen.getByLabelText('CycloneDX 1.7')).toBeChecked();
    expect(screen.getByLabelText(/JSON/)).toBeChecked();

    const spdxRadio = screen.getByLabelText(/SPDX 2\.3/);
    const xmlRadio = screen.getByLabelText(/^XML$/);

    await user.click(spdxRadio);

    expect(spdxRadio).toBeChecked();
    expect(screen.getByLabelText('CycloneDX 1.7')).not.toBeChecked();
    expect(screen.getByLabelText('CycloneDX 1.6')).not.toBeChecked();

    await user.click(xmlRadio);

    expect(xmlRadio).toBeChecked();
    expect(screen.getByLabelText(/JSON/)).not.toBeChecked();
  });

  it('should close the modal when you click "Cancel"', async () => {
    const user = userEvent.setup();
    renderComponent(initialState);

    expect(screen.getByText(/Additional Export Options/)).toBeVisible();

    const cancelButton = screen.getByRole('button', { name: /Cancel/ });

    await user.click(cancelButton);

    expect(screen.queryByText(/Additional Export Options/)).not.toBeInTheDocument();
  });
});
