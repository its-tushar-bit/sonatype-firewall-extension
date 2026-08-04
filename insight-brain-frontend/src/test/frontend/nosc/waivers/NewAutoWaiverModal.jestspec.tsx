/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { render, userEvent } from 'TestRoot/SpecUtil';
import { installRadixJsdomShims } from 'TestRoot/nosc/shell/radixJsdomShims';
import NewAutoWaiverModal from 'MainRoot/nosc/waivers/NewAutoWaiverModal';
import * as autoWaiversApi from 'MainRoot/nosc/waivers/autoWaiversApi';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';

jest.mock('MainRoot/nosc/waivers/autoWaiversApi', () => {
  const actual = jest.requireActual('MainRoot/nosc/waivers/autoWaiversApi');
  return {
    ...actual,
    createAutoPolicyWaiver: jest.fn(),
  };
});

const mockedCreate = autoWaiversApi.createAutoPolicyWaiver as jest.MockedFunction<
  typeof autoWaiversApi.createAutoPolicyWaiver
>;

describe('NewAutoWaiverModal', () => {
  let addToastSpy: jest.SpyInstance;

  beforeAll(() => {
    installRadixJsdomShims();
  });

  beforeEach(() => {
    mockedCreate.mockReset().mockResolvedValue({
      autoPolicyWaiverId: 'aw-1',
      ownerId: 'ROOT_ORGANIZATION_ID',
      ownerType: 'organization',
    });
    addToastSpy = jest.spyOn(toastActions, 'addToast');
  });

  afterEach(() => {
    addToastSpy.mockRestore();
  });

  it('requires a condition before create', async () => {
    const onSaved = jest.fn();
    render(
      <Theme>
        <NewAutoWaiverModal
          open
          onOpenChange={jest.fn()}
          ownerType="organization"
          ownerId="ROOT_ORGANIZATION_ID"
          canManage
          onSaved={onSaved}
        />
      </Theme>,
    );

    await userEvent.click(screen.getByTestId('new-auto-waiver-submit'));
    expect(await screen.findByTestId('new-auto-waiver-error')).toHaveTextContent(/at least one condition/i);
    expect(mockedCreate).not.toHaveBeenCalled();
  });

  it('creates with threat + condition payload', async () => {
    const onSaved = jest.fn();
    const onOpenChange = jest.fn();
    render(
      <Theme>
        <NewAutoWaiverModal
          open
          onOpenChange={onOpenChange}
          ownerType="organization"
          ownerId="ROOT_ORGANIZATION_ID"
          canManage
          onSaved={onSaved}
        />
      </Theme>,
    );

    await userEvent.click(screen.getByTestId('new-auto-waiver-reachability'));
    await userEvent.clear(screen.getByTestId('new-auto-waiver-threat'));
    await userEvent.type(screen.getByTestId('new-auto-waiver-threat'), '4');
    await userEvent.click(screen.getByTestId('new-auto-waiver-submit'));

    await waitFor(() => {
      expect(mockedCreate).toHaveBeenCalledWith({
        ownerType: 'organization',
        ownerId: 'ROOT_ORGANIZATION_ID',
        body: expect.objectContaining({
          threatLevel: 4,
          reachability: true,
          pathForward: false,
        }),
      });
    });
    expect(onSaved).toHaveBeenCalled();
    expect(onOpenChange).toHaveBeenCalledWith(false);
    expect(addToastSpy).toHaveBeenCalledWith({ type: 'success', message: 'Auto-waiver created' });
  });
});
