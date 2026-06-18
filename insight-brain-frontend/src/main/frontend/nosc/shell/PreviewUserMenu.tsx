/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Avatar, Box, DropdownMenu, Flex, Text } from '@radix-ui/themes';
import { selectCurrentUser } from 'MainRoot/user/userSessionSelectors';
import { logout } from 'MainRoot/user/userSessionSlice';
import ShellDropdownRoot from 'MainRoot/nosc/shell/ShellDropdownRoot';

const MENU_WIDTH = 260;

/**
 * Shape of the user-session data this menu reads. `userSessionSelectors` is
 * still plain JS with no exported type, so we declare the slice of it we
 * consume here rather than casting inline at the call site.
 */
interface CurrentUser {
  readonly displayName?: string;
  readonly username?: string;
}

function resolveDisplayName(user: CurrentUser | null): string {
  return user?.displayName?.trim() || user?.username?.trim() || 'User';
}

/**
 * Radix-native user account dropdown for the Nexus One TopNav. Replaces
 * the non-interactive avatar placeholder. Shows the current user and a
 * Logout action wired to the shared `logout` thunk Classic uses.
 *
 * Open/close behavior is owned by {@link ShellDropdownRoot} (controlled +
 * non-modal). See that component for why there is no onClick toggle here.
 */
export default function PreviewUserMenu(): JSX.Element {
  const dispatch = useDispatch();
  const user = useSelector(selectCurrentUser) as CurrentUser | null;

  const displayName = resolveDisplayName(user);
  const fallback = displayName[0].toUpperCase();

  return (
    <Flex align="center" data-testid="nexus-one-top-nav-user-menu-slot">
      <ShellDropdownRoot>
        <DropdownMenu.Trigger>
          <button
            type="button"
            aria-label="User menu"
            style={{
              display: 'flex',
              alignItems: 'center',
              background: 'transparent',
              border: 'none',
              padding: 0,
              cursor: 'pointer',
              borderRadius: 'var(--radius-full)',
            }}
          >
            <Avatar size="2" radius="full" fallback={fallback} />
          </button>
        </DropdownMenu.Trigger>

        <DropdownMenu.Content align="end" sideOffset={6} style={{ width: MENU_WIDTH }}>
          <Box px="3" py="2">
            <Text size="1" color="gray" as="div">
              Current User
            </Text>
            <Text size="2" weight="medium" as="div" data-testid="nexus-one-top-nav-user-name">
              {displayName}
            </Text>
          </Box>
          <DropdownMenu.Separator />
          <DropdownMenu.Item
            color="red"
            data-testid="nexus-one-top-nav-user-logout"
            onSelect={() => {
              dispatch(logout());
            }}
          >
            Log Out
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </ShellDropdownRoot>
    </Flex>
  );
}
