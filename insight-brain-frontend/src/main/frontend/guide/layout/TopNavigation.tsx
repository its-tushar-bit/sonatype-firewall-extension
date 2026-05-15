/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { RefObject } from 'react';
import { Box, Flex, IconButton, Avatar, Tooltip } from '@radix-ui/themes';
import { Menu, LogOut } from 'lucide-react';
import { tokens } from '@guide/ui-core/utils';
import { GuideLogo } from './GuideLogo';
import { ThemeToggle } from './ThemeToggle';
import { useAuth } from '../auth/AuthProvider';

interface TopNavigationProps {
  onSidebarToggle: () => void;
  sidebarToggleRef?: RefObject<HTMLButtonElement | null>;
}

function getInitials(displayName?: string, username?: string): string {
  const name = displayName || username || '';
  if (!name) return '?';

  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  }
  return name.substring(0, 2).toUpperCase();
}

export function TopNavigation({ onSidebarToggle, sidebarToggleRef }: TopNavigationProps) {
  const { user, logout } = useAuth();

  return (
    <Box
      style={{
        backgroundColor: 'var(--color-background)',
        borderBottom: '1px solid var(--gray-6)',
      }}
    >
      <Flex align="center" justify="between" gap={tokens.space.section}>
        <Flex align="center" gap={tokens.space.section} pl={tokens.space.section} py={tokens.space.section}>
          <IconButton
            ref={sidebarToggleRef}
            variant="outline"
            color="gray"
            size={tokens.sizes.caption}
            aria-label="Toggle sidebar"
            onClick={onSidebarToggle}
          >
            <Menu size={20} />
          </IconButton>

          <GuideLogo />
        </Flex>

        <Flex align="center" gap={tokens.space.item} pr={tokens.space.section} py={tokens.space.section}>
          <ThemeToggle />
          <Avatar
            size={tokens.sizes.caption}
            radius="full"
            color="gray"
            fallback={getInitials(user?.displayName, user?.username)}
          />
          <Tooltip content="Log out" side="bottom">
            <IconButton
              variant="outline"
              size={tokens.sizes.caption}
              color="gray"
              aria-label="Log out"
              onClick={() => { void logout(); }}
            >
              <LogOut size={16} />
            </IconButton>
          </Tooltip>
        </Flex>
      </Flex>
    </Box>
  );
}
