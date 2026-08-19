/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import WaiverExpirationNotificationEditor from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationEditor/WaiverExpirationNotificationEditor';
import * as selectors from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as orgsSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/waiverExpirationNotificationSlice';

describe('WaiverExpirationNotificationEditor', () => {
  beforeEach(() => {
    jest.spyOn(routerSelectors, 'selectIsOrganization').mockReturnValue(true);
    jest.spyOn(orgsSelectors, 'selectSelectedOwnerParentId').mockReturnValue('parent-org-id');
    jest.spyOn(selectors, 'selectLoading').mockReturnValue(false);
    jest.spyOn(selectors, 'selectLoadError').mockReturnValue(null);
    jest.spyOn(selectors, 'selectIsDirty').mockReturnValue(true);
    jest.spyOn(selectors, 'selectInheritConfig').mockReturnValue(false);
    jest.spyOn(selectors, 'selectNotificationDays').mockReturnValue([7]);
    jest.spyOn(selectors, 'selectDirectEmails').mockReturnValue([]);
    jest.spyOn(selectors, 'selectRoleIds').mockReturnValue([]);
    jest.spyOn(selectors, 'selectSubmitMaskState').mockReturnValue(null);
    jest.spyOn(selectors, 'selectSubmitError').mockReturnValue(null);
    jest.spyOn(selectors, 'selectAvailableRoles').mockReturnValue([]);
    jest.spyOn(selectors, 'selectServerData').mockReturnValue({
      inheritConfig: false,
      notificationDays: [7],
      directEmails: [],
      roleIds: [],
    });
    jest.spyOn(actions, 'loadConfig').mockReturnValue({ type: 'waiverExpirationNotification/loadConfig/pending' });
    jest.spyOn(actions, 'loadRoles').mockReturnValue({ type: 'waiverExpirationNotification/loadRoles/pending' });
  });

  it('shows invalid email format error when draft email is a plain string without @', async () => {
    const user = userEvent.setup();

    render(<WaiverExpirationNotificationEditor />);

    await user.type(screen.getByPlaceholderText('Enter email address'), 'notanemail');

    expect(screen.getByText('Invalid email address')).toBeInTheDocument();
  });

  it('shows invalid email format error when draft email domain is missing', async () => {
    const user = userEvent.setup();

    render(<WaiverExpirationNotificationEditor />);

    await user.type(screen.getByPlaceholderText('Enter email address'), 'foo@');

    expect(screen.getByText('Invalid email address')).toBeInTheDocument();
  });

  it('does not show error for a valid draft email address', async () => {
    const user = userEvent.setup();

    render(<WaiverExpirationNotificationEditor />);

    await user.type(screen.getByPlaceholderText('Enter email address'), 'alice@example.com');

    expect(screen.queryByText('Invalid email address')).not.toBeInTheDocument();
  });

  it('always shows the recipients table with empty state message when no recipients added', () => {
    render(<WaiverExpirationNotificationEditor />);

    expect(screen.getByText('RECIPIENTS')).toBeInTheDocument();
    expect(
      screen.getByText('Add people or groups who should receive waiver expiration reminders.')
    ).toBeInTheDocument();
  });

  it('shows both emails and roles in the same table when both are added', () => {
    jest.spyOn(selectors, 'selectDirectEmails').mockReturnValue(['alice@example.com']);
    jest.spyOn(selectors, 'selectRoleIds').mockReturnValue(['role-1']);
    jest.spyOn(selectors, 'selectAvailableRoles').mockReturnValue([{ roleId: 'role-1', roleName: 'Developer' }]);

    render(<WaiverExpirationNotificationEditor />);

    expect(screen.getByText('alice@example.com')).toBeInTheDocument();
    expect(screen.getByText('Developer')).toBeInTheDocument();
  });

  it('switching recipient type does not clear the table', async () => {
    jest.spyOn(selectors, 'selectDirectEmails').mockReturnValue(['alice@example.com']);
    const user = userEvent.setup();

    render(<WaiverExpirationNotificationEditor />);

    expect(screen.getByText('alice@example.com')).toBeInTheDocument();

    await user.selectOptions(screen.getByRole('combobox', { name: /recipient type/i }), 'Role');

    // Email added before switching should still be in the table
    expect(screen.getByText('alice@example.com')).toBeInTheDocument();
  });

  it('Update button remains visible after clicking submit when form is dirty', async () => {
    const user = userEvent.setup();

    render(<WaiverExpirationNotificationEditor />);

    await user.click(screen.getByRole('button', { name: /update/i }));

    expect(screen.getByRole('button', { name: /update/i })).toBeInTheDocument();
  });

  it('always renders a Back button', () => {
    render(<WaiverExpirationNotificationEditor />);

    expect(screen.getByRole('button', { name: /back/i })).toBeInTheDocument();
  });

  it('clicking Back calls window.history.back', async () => {
    const user = userEvent.setup();
    const backSpy = jest.spyOn(window.history, 'back').mockImplementation(() => {});

    render(<WaiverExpirationNotificationEditor />);

    await user.click(screen.getByRole('button', { name: /back/i }));

    expect(backSpy).toHaveBeenCalledTimes(1);
    backSpy.mockRestore();
  });

  it('does not render Delete Config button when serverData is null', () => {
    jest.spyOn(selectors, 'selectServerData').mockReturnValue(null);

    render(<WaiverExpirationNotificationEditor />);

    expect(screen.queryByRole('button', { name: /delete config/i })).not.toBeInTheDocument();
  });

  it('does not render Delete Config button when serverData has inheritConfig true (owner inherits)', () => {
    jest.spyOn(selectors, 'selectServerData').mockReturnValue({ inheritConfig: true, notificationDays: [7], directEmails: [], roleIds: [] });

    render(<WaiverExpirationNotificationEditor />);

    expect(screen.queryByRole('button', { name: /delete config/i })).not.toBeInTheDocument();
  });

  it('does not render Delete Config button when serverData has no data (root org with no saved config)', () => {
    jest.spyOn(selectors, 'selectServerData').mockReturnValue({ inheritConfig: false, notificationDays: [], directEmails: [], roleIds: [] });

    render(<WaiverExpirationNotificationEditor />);

    expect(screen.queryByRole('button', { name: /delete config/i })).not.toBeInTheDocument();
  });

  it('renders Delete Config button when serverData has inheritConfig false (own row exists in DB)', () => {
    jest.spyOn(selectors, 'selectServerData').mockReturnValue({ inheritConfig: false, notificationDays: [7], directEmails: [], roleIds: [] });

    render(<WaiverExpirationNotificationEditor />);

    expect(screen.getByRole('button', { name: /delete config/i })).toBeInTheDocument();
  });

  it('clicking Delete Config opens confirmation modal', async () => {
    const user = userEvent.setup();

    render(<WaiverExpirationNotificationEditor />);

    await user.click(screen.getByRole('button', { name: /delete config/i }));

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/permanently remove this custom configuration/i)).toBeInTheDocument();
  });

  it('clicking Cancel in the delete modal closes it without dispatching deleteConfig', async () => {
    const user = userEvent.setup();
    const deleteConfigSpy = jest.spyOn(actions, 'deleteConfig').mockReturnValue({ type: 'waiverExpirationNotification/deleteConfig/pending' });

    render(<WaiverExpirationNotificationEditor />);

    await user.click(screen.getByRole('button', { name: /delete config/i }));
    await user.click(screen.getByRole('button', { name: /cancel/i }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(deleteConfigSpy).not.toHaveBeenCalled();
  });

  it('clicking Delete in the modal dispatches deleteConfig action', async () => {
    const user = userEvent.setup();
    const deleteConfigSpy = jest.spyOn(actions, 'deleteConfig').mockReturnValue({ type: 'waiverExpirationNotification/deleteConfig/pending' });

    render(<WaiverExpirationNotificationEditor />);

    await user.click(screen.getByRole('button', { name: /delete config/i }));
    await user.click(screen.getByRole('button', { name: /^delete$/i }));

    expect(deleteConfigSpy).toHaveBeenCalledTimes(1);
  });
});
