/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { fireEvent, render, screen, axiosMockAdapter, waitFor } from 'TestRoot/SpecUtil';
import DeleteAutoWaiverModal from 'MainRoot/waivers/DeleteAutoWaiverModal';
import { getAutoWaiverRevocationsUrl } from 'MainRoot/util/CLMLocation';
import * as automatedWaiversRevocationsSelector from 'MainRoot/OrgsAndPolicies/automatedWaiversRevocationsSelector';

describe('DeleteAutoWaiverModal', () => {
  let onCloseMock = jest.fn();
  let setShowModalMock = jest.fn();
  let axiosMock;
  let defaultPreloadedState;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    defaultPreloadedState = (isRoot) => {
      return {
        violation: {
          autoWaiver: {
            ownerType: isRoot ? 'root_organization' : 'application',
            ownerId: 'application-owner-id',
            autoPolicyWaiverId: 'example-auto-waiver-id',
          },
          violationDetails: {
            applicationPublicId: 'application-public-id',
            stageData: {
              build: {
                mostRecentScanId: 'example-scan-id',
              },
            },
          },
        },
      };
    };
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('does not render modal without being open', () => {
    renderComponent(false, {
      showModal: false,
      onClose: onCloseMock,
    });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('render modal content correctly', () => {
    renderComponent();

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Remove Auto-Waiver' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Do not auto-waive this violation' })).toBeVisible();
    expect(screen.getByText('Remove waiver and exclude from future automations')).toBeVisible();

    const confirmationCheckbox = screen.getByRole('checkbox', { name: 'Remove auto-waiver' });
    expect(confirmationCheckbox).toBeVisible();
    expect(confirmationCheckbox).not.toBeChecked();

    expect(screen.getByText('Removing this waiver does not disable all automated waivers.')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Submit' })).toBeVisible();
  });

  it('close modal on cancel button click', () => {
    renderComponent();

    const closeButton = screen.getByRole('button', { name: 'Cancel' });
    expect(closeButton).toBeVisible();
    expect(closeButton).not.toBeDisabled();
    fireEvent.click(closeButton);
    expect(onCloseMock).toHaveBeenCalledTimes(1);
  });

  it('close modal on escape button typed', () => {
    renderComponent();

    const modal = screen.getByRole('dialog');
    fireEvent.keyDown(modal, { key: 'Escape' });
    expect(onCloseMock).toHaveBeenCalledTimes(1);
  });

  it('shows validation error when submitting without confirmation and then removes it after fixing issues', async () => {
    renderComponent();

    const submitButton = screen.getByRole('button', { name: 'Submit' });
    fireEvent.click(submitButton);
    const alert = screen.getByRole('alert');
    expect(alert).toBeVisible();
    expect(alert).toHaveTextContent('There were validation errors. You must confirm the removal of the auto-waiver');

    const confirmationCheckbox = screen.getByRole('checkbox', { name: 'Remove auto-waiver' });
    fireEvent.click(confirmationCheckbox);

    expect(alert).not.toBeVisible();
  });

  it('shows validation error when submitting without confirmation', async () => {
    renderComponent();

    const submitButton = screen.getByRole('button', { name: 'Submit' });
    fireEvent.click(submitButton);
    const alert = screen.getByRole('alert');
    expect(alert).toBeVisible();
    expect(alert).toHaveTextContent('There were validation errors. You must confirm the removal of the auto-waiver');
  });

  it('submits correct information when clicking submit and having confirmation', async () => {
    renderComponent();

    const confirmationCheckbox = screen.getByRole('checkbox', { name: 'Remove auto-waiver' });
    fireEvent.click(confirmationCheckbox);

    const submitButton = screen.getByRole('button', { name: 'Submit' });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
      expect(axiosMock.history.post[0].data).toBe(
        JSON.stringify({
          ownerId: 'application-owner-id',
          applicationPublicId: 'application-public-id',
          scanId: 'example-scan-id',
          policyViolationId: undefined,
          autoPolicyWaiverId: 'example-auto-waiver-id',
          matchStrategy: 'POLICY_VIOLATION',
        })
      );
      expect(axiosMock.history.post[0].url).toBe(
        '/api/v2/autoPolicyWaiverRevocations/application/application-owner-id'
      );
    });
  });

  it('submits correct information when its root organization', async () => {
    renderComponent(true);

    const confirmationCheckbox = screen.getByRole('checkbox', { name: 'Remove auto-waiver' });
    fireEvent.click(confirmationCheckbox);

    const submitButton = screen.getByRole('button', { name: 'Submit' });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
      expect(axiosMock.history.post[0].data).toBe(
        JSON.stringify({
          ownerId: 'application-owner-id',
          applicationPublicId: 'application-public-id',
          scanId: 'example-scan-id',
          policyViolationId: undefined,
          autoPolicyWaiverId: 'example-auto-waiver-id',
          matchStrategy: 'POLICY_VIOLATION',
        })
      );
      expect(axiosMock.history.post[0].url).toBe(
        '/api/v2/autoPolicyWaiverRevocations/organization/application-owner-id'
      );
    });
  });

  it('displays error message when creating revocation fails', async () => {
    axiosMock.onPost(getAutoWaiverRevocationsUrl('application', 'application-owner-id')).reply(500, {
      message: 'Failed to save configuration',
    });

    renderComponent();

    const confirmationCheckbox = screen.getByRole('checkbox', { name: 'Remove auto-waiver' });
    fireEvent.click(confirmationCheckbox);

    const submitButton = screen.getByRole('button', { name: 'Submit' });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
      expect(axiosMock.history.post[0].data).toBe(
        JSON.stringify({
          ownerId: 'application-owner-id',
          applicationPublicId: 'application-public-id',
          scanId: 'example-scan-id',
          policyViolationId: undefined,
          autoPolicyWaiverId: 'example-auto-waiver-id',
          matchStrategy: 'POLICY_VIOLATION',
        })
      );
      expect(axiosMock.history.post[0].url).toBe(
        '/api/v2/autoPolicyWaiverRevocations/application/application-owner-id'
      );
      expect(screen.getByText('An error occurred saving data. Failed to save configuration')).toBeVisible();
    });
  });

  it('closes the modal after saving', async () => {
    renderComponent();

    const confirmationCheckbox = screen.getByRole('checkbox', { name: 'Remove auto-waiver' });
    fireEvent.click(confirmationCheckbox);

    const submitButton = screen.getByRole('button', { name: 'Submit' });
    fireEvent.click(submitButton);

    jest.spyOn(automatedWaiversRevocationsSelector, 'selectAutomatedWaiversRevocationSlice').mockReturnValue({
      submitMaskState: true,
      submitError: null,
    });

    await waitFor(() => {
      expect(axiosMock.history.post.length).toBe(1);
      expect(axiosMock.history.post[0].data).toBe(
        JSON.stringify({
          ownerId: 'application-owner-id',
          applicationPublicId: 'application-public-id',
          scanId: 'example-scan-id',
          policyViolationId: undefined,
          autoPolicyWaiverId: 'example-auto-waiver-id',
          matchStrategy: 'POLICY_VIOLATION',
        })
      );
      expect(axiosMock.history.post[0].url).toBe(
        '/api/v2/autoPolicyWaiverRevocations/application/application-owner-id'
      );

      expect(setShowModalMock).toHaveBeenCalledWith(false);
      expect(setShowModalMock).toHaveBeenCalledTimes(1);
    });
  });

  function renderComponent(
    isRoot = false,
    props = { showModal: true, onClose: onCloseMock, setShowModal: setShowModalMock }
  ) {
    return render(<DeleteAutoWaiverModal {...props} />, { preloadedState: defaultPreloadedState(isRoot) });
  }
});
