/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { nxFileUploadStateHelpers, nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import ImportSbomModal from 'MainRoot/OrgsAndPolicies/importSbomModal/ImportSbomModal';
import { IMPORT_STATE } from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSlice';
import { getCommitImportedSbomUrl, getImportSbomUrl } from 'MainRoot/util/CLMLocation';

import { axiosMockAdapter, fireEvent, render, screen } from 'TestRoot/SpecUtil';

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
            fileInputState: nxFileUploadStateHelpers.initialState(null),
            sbomSummary: {
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
        expect(document.querySelector('input[type=file]')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Import/i })).toBeInTheDocument();
      });
    });

    describe('Upload form validation', () => {
      it('shows an error if the Import button is clicked when a file is not selected', async () => {
        const user = userEvent.setup();
        renderComponent();
        const importButton = screen.getByRole('button', { name: /Import/i });
        const fileUpload = document.querySelector('input[type=file]');

        expect(fileUpload).not.toHaveAccessibleErrorMessage();
        expect(importButton).toBeEnabled();

        await user.click(importButton);

        expect(fileUpload).toHaveAccessibleErrorMessage('This field is required!');
        expect(fileUpload).toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByRole('alert', { name: /validation error/ })).toHaveTextContent(
          'There were validation errors. Please select a file to upload.'
        );

        setFileUploadValue(fileUpload, createTestFile());

        expect(fileUpload).not.toHaveAccessibleErrorMessage();
        expect(fileUpload).not.toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByRole('button', { name: /Import/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Import/i })).toBeEnabled();
      });
    });

    describe('Uploading', () => {
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

        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const importButton = screen.getByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        expect(await screen.findByRole('progressbar')).toBeVisible();
        expect(screen.getByText('Import in progress…')).toBeVisible();
        expect(screen.getByText('Importing [test-file.json]…')).toBeVisible();

        expect(screen.queryByRole('button', { name: /Cancel/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Close/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Import/i })).not.toBeInTheDocument();
      });
    });

    describe('VERSION_CONFIRM/COMMITTING', () => {
      beforeEach(() => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
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
          savedVersion: '1.2.3_2024',
          scanType: 'SBOM',
          errorMessage: null,
        });

        renderComponent();

        // Set up initial state by uploading file
        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const initialImportButton = screen.getByRole('button', { name: /import/i });
        fireEvent.click(initialImportButton);
      });

      it('shows the version confirmation page with correct content', async () => {
        expect(screen.getByRole('dialog')).toHaveAccessibleName('File Uploaded. Import in Progress…');

        const applicationNameLabel = (await screen.findByText('Application Name')).closest('dt');
        expect(applicationNameLabel).toBeVisible();
        expect(applicationNameLabel.nextElementSibling.tagName).toBe('DD');
        expect(applicationNameLabel.nextElementSibling).toHaveTextContent('testApplicationName');

        const versionInput = screen.getByRole('textbox', { name: 'Application Version' });
        expect(versionInput).toHaveValue('1.2.3_2024');

        expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Import/i })).toBeInTheDocument();
      });

      it('shows validation error when trying to import with empty version', async () => {
        const versionInput = screen.getByPlaceholderText('Enter version');
        const importButton = screen.getByRole('button', { name: /Import/i });

        fireEvent.change(versionInput, { target: { value: '' } });
        fireEvent.click(importButton);

        expect(
          screen.getByText('There were validation errors. Invalid version input. Please enter a valid version format.')
        );
        expect(screen.getByText('Must be non-empty')).toBeInTheDocument();
      });

      it('allows retry with new version after conflict', async () => {
        axiosMock
          .onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024'))
          .replyOnce(409, 'Version 1.2.3_2024 already exists');

        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024', '2.0.0')).replyOnce(201, {});

        let versionInput = screen.getByRole('textbox', { name: 'Application Version' });
        expect(versionInput).toHaveValue('1.2.3_2024');

        let importButton = screen.getByRole('button', { name: /Import/i });

        fireEvent.click(importButton);
        expect(
          await screen.findByText('An error occurred saving data. Version 1.2.3_2024 already exists')
        ).toBeInTheDocument();

        versionInput = screen.getByRole('textbox', { name: 'Application Version' });
        fireEvent.change(versionInput, { target: { value: '2.0.0' } });
        importButton = screen.getByRole('button', { name: /Import/i });
        fireEvent.click(importButton);

        expect(
          screen.queryByText('An error occurred saving data. Version 1.2.3_2024 already exists')
        ).not.toBeInTheDocument();

        //ensure we are on the summary page
        const versionIdTextBox = await screen.findByRole('textbox', { name: /version id/i });
        expect(versionIdTextBox).toHaveValue('2.0.0');
        expect(versionIdTextBox).toBeDisabled();
      });

      it('allows user to override version', async () => {
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024', '2.0.0')).replyOnce(201, {});

        await screen.findByText('File Uploaded. Import in Progress…');

        const versionInput = screen.getByPlaceholderText('Enter version');
        expect(versionInput).toHaveValue('1.2.3_2024');

        fireEvent.change(versionInput, { target: { value: '2.0.0' } });
        expect(versionInput).toHaveValue('2.0.0');

        const importButton = screen.getByRole('button', { name: 'Import' });
        fireEvent.click(importButton);

        //make sure we are on the summary page
        const versionIdTextBox = await screen.findByRole('textbox', { name: /version id/i });
        expect(versionIdTextBox).toHaveValue('2.0.0');
        expect(versionIdTextBox).toBeDisabled();

        expect(axiosMock.history.post).toHaveLength(2);
        const commitFileCall = axiosMock.history.post[1];
        expect(commitFileCall.url).toEqual(
          '/rest/sbom/commit/testApplicationId/1.2.3_2024?applicationVersionOverride=2.0.0'
        );
      });

      it('closes modal when Cancel is clicked', () => {
        const cancelButton = screen.getByRole('button', { name: /Cancel/i });

        fireEvent.click(cancelButton);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    describe('Summary', () => {
      it('shows post upload details for SBOM', async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
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
          savedVersion: '1.2.3_2024',
          scanType: 'SBOM',
          errorMessage: null,
        });
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).reply(201, {});

        renderComponent();

        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const importButton = screen.getByRole('button', { name: /import/i });

        fireEvent.click(importButton);

        // version confirm page
        expect(await screen.findByText('File Uploaded. Import in Progress…')).toBeInTheDocument();

        const confirmImportButton = screen.getByRole('button', { name: /import/i });
        fireEvent.click(confirmImportButton);

        const applicationNameLabel = (await screen.findByText('Application Name')).closest('dt');
        expect(applicationNameLabel).toBeVisible();
        expect(applicationNameLabel.parentElement.querySelector('dd')).toHaveTextContent('testApplicationName');

        const versionIdTextBox = screen.getByRole('textbox', { name: /version id/i });
        expect(versionIdTextBox).toHaveValue('1.2.3_2024');
        expect(versionIdTextBox).toBeDisabled();

        const totalComponentsLabel = screen.getByText('Total Components').closest('dt');
        expect(totalComponentsLabel).toBeVisible();
        expect(totalComponentsLabel.parentElement.querySelector('dd')).toHaveTextContent('1');

        const totalVulnsLabel = screen.getByText('Total Vulnerabilities').closest('dt');
        expect(totalVulnsLabel).toBeVisible();
        expect(totalVulnsLabel.parentElement.querySelector('dd')).toHaveTextContent('2');

        expect(
          screen.getByText(
            'Closing the modal will not interrupt the evaluation; it will still be in progress until completed. ' +
              'Once the evaluation is complete, you can view the SBOM in the SBOM table.'
          )
        ).toBeInTheDocument();

        expect(screen.queryByRole('button', { name: 'Import' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument();

        const closeButton = screen.getByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();

        fireEvent.click(closeButton);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });

      it('shows post upload details for BINARY', async () => {
        axiosMock.onPost(getImportSbomUrl('testApplicationId')).reply(200, {
          savedVersion: '1.2.3',
          sbomSummary: null,
          scanType: 'BINARY',
          errorMessage: null,
        });
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3')).reply(201, {});

        renderComponent();

        setFileUploadValue(document.querySelector('input[type=file]'), createTestFile());
        const importButton = screen.getByRole('button', { name: /import/i });

        fireEvent.click(importButton);

        //version confirm page
        expect(await screen.findByText('File Uploaded. Import in Progress…')).toBeInTheDocument();

        const confirmImportButton = screen.getByRole('button', { name: /import/i });
        fireEvent.click(confirmImportButton);

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
      it('should show an error message when an error is generated prior to starting the validation', () => {
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
        const importButton = screen.getByRole('button', { name: 'Import' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toBeDisabled();
      });

      it('should show an alert message of warning type when there are ignorable validation errors', () => {
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: true,
              },
            },
          },
        });

        expect(screen.getByRole('dialog', { name: 'Your SBOM failed validation' })).toBeInTheDocument();
        const alert = screen.getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('Something went wrong.');
        expect(alert).toHaveClass('nx-alert--warning');
        const skipCheckbox = screen.getByText('Skip validation and import anyway');
        expect(skipCheckbox).toBeInTheDocument();
        expect(skipCheckbox).not.toBeChecked();
        expect(screen.queryByRole('button', { name: 'Copy to Clipboard' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Validation Error Details' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
        const importButton = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toBeDisabled();
      });

      it('should show an alert message of error type when validation errors are not ignorable', () => {
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: false,
              },
            },
          },
        });

        expect(screen.getByRole('dialog', { name: 'Your SBOM failed validation' })).toBeInTheDocument();
        const alert = screen.getByRole('alert');
        expect(alert).toBeInTheDocument();
        expect(alert).toHaveTextContent('Something went wrong.');
        expect(alert).toHaveClass('nx-alert--error');
        expect(screen.queryByRole('button', { name: 'Copy to Clipboard' })).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: 'Validation Error Details' })).toBeInTheDocument();
        expect(screen.queryByText('Skip validation and import anyway')).not.toBeInTheDocument();
        const cancelButton = screen.getByRole('button', { name: 'Cancel' });
        expect(cancelButton).toBeInTheDocument();
        expect(cancelButton).toBeEnabled();
        const importButton = screen.getByRole('button', { name: 'Import' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toHaveClass('disabled');
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
                isSkipValidation: false,
                isValidationErrorIgnorable: false,
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
                isSkipValidation: false,
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

      it('should allow/disallow import if skipValidation selected when there are ignorable validation errors', () => {
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: true,
              },
            },
          },
        });

        const importButton = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toBeDisabled();

        const checkbox = screen.getByText('Skip validation and import anyway');
        expect(checkbox).toBeInTheDocument();
        expect(checkbox).not.toBeChecked();

        fireEvent.click(checkbox);

        const importButtonUpdate1 = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButtonUpdate1).toBeInTheDocument();
        expect(importButtonUpdate1).toBeEnabled();

        fireEvent.click(checkbox);

        const importButtonUpdate2 = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButtonUpdate2).toBeInTheDocument();
        expect(importButtonUpdate2).toBeDisabled();
      });

      it('should show a tooltip in import button when there are non ignorable validation errors', async () => {
        const user = userEvent.setup();
        renderComponent({
          orgsAndPolicies: {
            ownerActions: {
              importSbomModal: {
                isModalOpen: true,
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: false,
              },
            },
          },
        });

        const importButton = screen.getByText('Import');
        expect(importButton).toBeInTheDocument();
        await user.hover(importButton);

        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent(
          'Import cannot proceed due to a critical error in the file. Please correct the file and try again.'
        );
      });

      it('should execute import if skipValidation selected when there are ignorable validation errors', async () => {
        axiosMock.onPost(getCommitImportedSbomUrl('testApplicationId', '1.2.3_2024')).reply(201, {});

        const file = createTestFile();
        renderComponent({
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
                importState: IMPORT_STATE.ERROR,
                errorMessage: 'Something went wrong.',
                validationErrors: ['error reason 1', 'error reason 2'],
                isSkipValidation: false,
                isValidationErrorIgnorable: true,
                scanType: 'SBOM',
                isValid: false,
                fileInputState: nxFileUploadStateHelpers.initialState(fakeFileList(file)),
                uploadProgress: 0,
                sbomSummary: {
                  specification: 'CycloneDx',
                  format: 'json',
                  version: '1.4',
                  totalComponents: 1,
                  totalVulnerabilities: 2,
                  applicationName: 'Application Name',
                  applicationVersion: '1.2.3',
                  serialNumber: 'urn:uuid:3e671687-395b-41f5-a30f-a58921a69b79',
                  creationDetails: null,
                },
                savedVersion: '1.2.3_2024',
                versionTextInput: nxTextInputStateHelpers.initialState('1.2.3_2024'),
              },
            },
          },
        });

        const checkbox = await screen.getByText('Skip validation and import anyway');
        expect(checkbox).toBeInTheDocument();
        expect(checkbox).not.toBeChecked();

        fireEvent.click(checkbox);

        const importButton = screen.getByRole('button', { name: 'Import Anyway' });
        expect(importButton).toBeInTheDocument();
        expect(importButton).toBeEnabled();

        fireEvent.click(importButton);

        //version confirm page
        expect(await screen.findByText('File Uploaded. Import in Progress…')).toBeInTheDocument();

        const confirmImportButton = screen.getByRole('button', { name: /import/i });
        fireEvent.click(confirmImportButton);

        const applicationNameLabel = (await screen.findByText('Application Name')).closest('dt');
        expect(applicationNameLabel).toBeVisible();
        expect(applicationNameLabel.parentElement.querySelector('dd')).toHaveTextContent('testApplicationName');

        const versionIdTextBox = screen.getByRole('textbox', { name: /version id/i });
        expect(versionIdTextBox).toHaveValue('1.2.3_2024');
        expect(versionIdTextBox).toBeDisabled();

        const totalComponentsLabel = screen.getByText('Total Components').closest('dt');
        expect(totalComponentsLabel).toBeVisible();
        expect(totalComponentsLabel.parentElement.querySelector('dd')).toHaveTextContent('1');

        const totalVulnsLabel = screen.getByText('Total Vulnerabilities').closest('dt');
        expect(totalVulnsLabel).toBeVisible();
        expect(totalVulnsLabel.parentElement.querySelector('dd')).toHaveTextContent('2');

        expect(
          screen.getByText(
            'Closing the modal will not interrupt the evaluation; it will still be in progress until completed. ' +
              'Once the evaluation is complete, you can view the SBOM in the SBOM table.'
          )
        ).toBeInTheDocument();

        const closeButton = screen.getByRole('button', { name: 'Close' });
        expect(closeButton).toBeVisible();

        fireEvent.click(closeButton);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });
});
