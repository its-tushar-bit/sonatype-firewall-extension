/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import ImportSbomModal from 'MainRoot/OrgsAndPolicies/importSbomModal/ImportSbomModal';
import { IMPORT_STATE } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';
import { getCommitImportedSbomUrl, getImportSbomUrl } from 'MainRoot/util/CLMLocation';

import { axiosMockAdapter, fireEvent, render, screen } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';

describe('ImportSbomModal', () => {
  let renderComponent, axiosMock, defaultPreloadedState;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    defaultPreloadedState = {
      productFeatures: {
        productFeatures: {
          'sbom-manager': true,
        },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: 'testApplicationId',
            name: 'testApplicationName',
          },
        },
        ownerActions: {
          importSbomModal: {
            isModalOpen: true,
            importState: IMPORT_STATE.INITIAL,
            uploadProgress: 0,
            errorMessage: null,
            sbomSummary: {
              versionId: null,
              totalComponents: null,
              totalVulnerabilities: null,
            },
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<ImportSbomModal />, { preloadedState: { ...defaultPreloadedState, ...preloadedState } });
  });

  describe('show/hide Modal', () => {
    it('should hide modal when it is not open', () => {
      renderComponent({
        orgsAndPolicies: {
          ownerActions: {
            importSbomModal: {
              isModalOpen: false,
            },
          },
        },
      });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('should show the modal when it is open', () => {
      renderComponent({
        orgsAndPolicies: {
          ownerActions: {
            importSbomModal: {
              isModalOpen: true,
            },
          },
        },
      });
      expect(screen.queryByRole('dialog')).toBeInTheDocument();
    });

    it('closes the modal on cancel', () => {
      async () => {
        renderComponent();
        expect(screen.queryByRole('dialog')).toBeInTheDocument();
        const cancelButton = await screen.findByRole('button', { name: 'Cancel' });
        fireEvent.click(cancelButton);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      };
    });
  });

  describe('Modal Content', () => {
    const createTestFile = () => {
      const blob = new Blob([JSON.stringify([{ name: 'file-content' }])]);
      return new File([blob], 'test-file.json', {
        type: 'application/JSON',
      });
    };

    const fakeFileList = (...files) => {
      const retval = {
        ...files,
        item(i) {
          return files[i];
        },
        length: files.length,
      };
      Object.setPrototypeOf(retval, FileList.prototype);
      return retval;
    };

    const setFileUploadValue = (fileUpload, ...files) => {
      fireEvent.change(fileUpload, {
        target: {
          files: fakeFileList(...files),
        },
      });
    };

    describe('Initial State', () => {
      it('shows the correct content', () => {
        renderComponent();
        expect(screen.getByText('Import File for Application testApplicationName')).toBeInTheDocument();
        expect(screen.getByTestId('import-sbom-modal-file-upload')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Import/i })).toBeInTheDocument();
      });

      it('should disable import SBOM until a file is selected', async () => {
        renderComponent();
        const importButton = screen.getByRole('button', { name: /Import/i });
        expect(importButton).toBeVisible();
        expect(importButton).toBeDisabled();
        setFileUploadValue(await screen.findByTestId('import-sbom-modal-file-upload'), createTestFile());
        expect(importButton).not.toBeDisabled();
      });
    });

    describe('Uploading/Committing', () => {
      it('shows the correct content', async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply((config) => {
          const total = 1024; // Mocked file size
          const progress = 0.5;
          if (config.onUploadProgress) {
            config.onUploadProgress({ loaded: total * progress, total });
          }
          return new Promise(() => {});
        });

        renderComponent();

        setFileUploadValue(await screen.findByTestId('import-sbom-modal-file-upload'), createTestFile());
        const importButton = screen.getByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        expect(await screen.findByRole('progressbar')).toBeVisible();
        expect(await screen.findByText('Import in progress...')).toBeVisible();
        expect(await screen.findByText('Importing [test-file.json]...')).toBeVisible();

        expect(screen.queryByRole('button', { name: /Cancel/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Close/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Import/i })).not.toBeInTheDocument();
      });
    });

    describe('Summary', () => {
      it('shows post upload details for SBOM', async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          requestId: 'request-id',
          sbomSummary: {
            specification: 'CycloneDx',
            format: 'json',
            version: '1.4',
            componentCount: 1,
            vulnerabilityCount: 2,
            applicationName: null,
            applicationVersion: '1.2.3',
            serialNumber: 'urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79',
            creationDetails: null,
          },
          scanType: 'SBOM',
          errorMessage: null,
        });
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', 'request-id')).reply(201, {});

        renderComponent();

        setFileUploadValue(await screen.findByTestId('import-sbom-modal-file-upload'), createTestFile());
        const importButton = screen.getByRole('button', { name: /import/i });

        fireEvent.click(importButton);

        expect(await screen.findByText('Application Name')).toBeVisible();
        expect(await screen.findByText('testApplicationName')).toBeVisible();

        const versionIdTextBox = await screen.findByRole('textbox', { name: /version id/i });
        expect(versionIdTextBox).toHaveValue('1.2.3');
        expect(versionIdTextBox).toBeDisabled();

        expect(await screen.findByText('Total Components:')).toBeVisible();
        expect(await screen.findByTestId('import-sbom-modal-total-components')).toHaveTextContent('1');

        expect(await screen.findByText('Total Vulnerabilities:')).toBeVisible();
        expect(await screen.findByTestId('import-sbom-modal-total-vulnerabilities')).toHaveTextContent('2');

        expect(
          await screen.findByText(
            'Closing the modal will not interrupt the evaluation; it will still be in progress until completed. ' +
              'Once the evaluation is complete, you can view the SBOM in the SBOM table.'
          )
        ).toBeInTheDocument();

        expect(screen.queryByRole('button', { name: 'Import' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();

        const closeButton = await screen.findByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();

        fireEvent.click(closeButton);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });

      it('shows post upload details for BINARY', async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          requestId: 'request-id',
          sbomSummary: null,
          scanType: 'BINARY',
          errorMessage: null,
        });
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', 'request-id')).reply(201, {});

        renderComponent();

        setFileUploadValue(await screen.findByTestId('import-sbom-modal-file-upload'), createTestFile());
        const importButton = screen.getByRole('button', { name: /import/i });

        fireEvent.click(importButton);

        expect(await screen.findByText('Application Name')).toBeVisible();
        expect(await screen.findByText('testApplicationName')).toBeVisible();

        expect(await screen.findByText('File')).toBeVisible();
        expect(await screen.findByText('test-file.json')).toBeVisible();

        expect(screen.queryByRole('button', { name: 'Import' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();

        const closeButton = await screen.findByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();

        fireEvent.click(closeButton);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    describe('Error', () => {
      it('should show an error message', () => {
        // We're not testing mock responses here, due to js-dom FileList error with NxFileUpload.
        // When NxFileUpload is initially mounted with a fake "FileList" passed into the files prop, in js-dom, it throws an error:
        // Failed to set the 'files' property on 'HTMLInputElement': The provided value is not of type 'FileList'
        // This does not occur on other implementations because they were initially mounted with a null value.
        // So far there is no way to construct a proper mock "real" FileList object (DataTransfer is not supported) in js-dom.
        // Instead, we will be testing this in the functional tests.
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
              },
            },
          },
        });

        expect(screen.getByRole('dialog', { name: 'Error Importing SBOM' })).toBeInTheDocument();
        const alert = screen.getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('Something went wrong.');
        expect(screen.queryByRole('button', { name: 'Copy to Clipboard' })).not.toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Validation Error Details' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Import' })).toBeInTheDocument();
      });
    });

    it('should show the list of errors', () => {
      renderComponent({
        orgsAndPolicies: {
          ownerActions: {
            importSbomModal: {
              isModalOpen: true,
              importState: IMPORT_STATE.ERROR,
              errorMessage: 'Something went wrong.',
              validationErrors: ['error reason 1', 'error reason 2'],
            },
          },
        },
      });

      expect(screen.getByRole('dialog', { name: 'Your SBOM failed validation' })).toBeInTheDocument();
      const alert = screen.getByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent('Something went wrong.');
      expect(screen.getByRole('button', { name: 'Copy to Clipboard' })).toBeInTheDocument();
      const validationErrors = screen.getByRole('textbox', { name: 'Validation Error Details' });
      expect(validationErrors).toBeInTheDocument();
      expect(validationErrors).toHaveValue('• error reason 1\n• error reason 2');
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Import' })).toBeInTheDocument();
    });

    it('should copy the list of errors when clicking the copy to clipboard button', async () => {
      renderComponent({
        orgsAndPolicies: {
          ownerActions: {
            importSbomModal: {
              isModalOpen: true,
              importState: IMPORT_STATE.ERROR,
              errorMessage: 'Something went wrong.',
              validationErrors: ['error reason 1', 'error reason 2'],
            },
          },
        },
      });
      const user = userEvent.setup();
      const copyToClipboardButton = screen.getByRole('button', { name: 'Copy to Clipboard' });
      expect(copyToClipboardButton).toBeInTheDocument();

      await user.click(copyToClipboardButton);

      const clipboardText = await navigator.clipboard.readText();
      expect(clipboardText).toEqual('• error reason 1\n• error reason 2');
    });
  });
});
