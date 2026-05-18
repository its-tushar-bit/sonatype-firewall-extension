/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useCallback } from 'react';
import { useAdapterPathname } from '@guide/ui-core';
import { Box, Flex, Tooltip, ScrollArea, Text } from '@radix-ui/themes';
import { tokens } from '@guide/ui-core/utils';
import { NavGroup, NavItem } from './types';
import { SIDEBAR_WIDTH_COLLAPSED, SIDEBAR_WIDTH_EXPANDED } from './constants';
import { useFeatureFlags } from '../feature-flags/FeatureFlagProvider';
import styles from './SidebarNavigation.module.css';

interface SidebarNavItemProps {
  item: NavItem;
  expanded: boolean;
  isMobile: boolean;
  onLinkClick?: () => void;
  isActive: boolean;
}

function SidebarNavItem({ item, expanded, isMobile, onLinkClick, isActive }: SidebarNavItemProps) {
  const Icon = item.icon;

  const handleClick = () => {
    if (isMobile && onLinkClick) {
      onLinkClick();
    }
  };

  const linkContent = (
    <a
      href={`#${item.href}`}
      aria-label={item.label}
      aria-current={isActive ? 'page' : undefined}
      className={styles.link}
      data-active={isActive}
      onClick={handleClick}
      style={!expanded ? { width: 'auto' } : undefined}
    >
      <Icon size={18} />
      {expanded && (
        <Text size={tokens.sizes.body.sm}>{item.label}</Text>
      )}
    </a>
  );

  if (!expanded) {
    return (
      <Tooltip key={item.id} content={item.label} side="right">
        {linkContent}
      </Tooltip>
    );
  }

  return linkContent;
}

interface SidebarNavigationProps {
  groups: NavGroup[];
  expanded: boolean;
  isMobile?: boolean;
  onLinkClick?: () => void;
}

export default function SidebarNavigation({
  groups,
  expanded,
  isMobile = false,
  onLinkClick,
}: SidebarNavigationProps) {
  const pathname = useAdapterPathname();
  const { isFeatureEnabled } = useFeatureFlags();

  const isItemActive = useCallback(
    (item: NavItem) => {
      if (pathname === item.href) return true;
      if (item.href !== '/' && pathname.startsWith(item.href + '/')) return true;
      return false;
    },
    [pathname]
  );

  const isItemVisible = useCallback(
    (item: NavItem) => !item.requiresFeatureFlag || isFeatureEnabled(item.requiresFeatureFlag),
    [isFeatureEnabled]
  );

  const containerClassName = isMobile
    ? `${styles.container} ${styles.containerEntering}`
    : styles.container;

  return (
    <Box
      data-testid="sidebar-navigation"
      width={isMobile ? undefined : !expanded ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH_EXPANDED}
      height="100%"
      className={containerClassName}
    >
      <Box asChild flexGrow="1" height="100%">
        <ScrollArea>
          <Box asChild px={expanded ? tokens.space.section : '0'} py={tokens.space.inline}>
            <nav aria-label="Sidebar">
              {groups.map((group) => {
                const visibleItems = group.items.filter(isItemVisible);
                if (visibleItems.length === 0) return null;
                return (
                  <Box key={group.id} mb={expanded ? tokens.space.section : '0'}>
                    {expanded && group.label && (
                      <Box mb={tokens.space.inline} ml={tokens.space.inline}>
                        <Text
                          size={tokens.sizes.body.xs}
                          color="gray"
                          style={{ textTransform: 'uppercase' }}
                        >
                          {group.label}
                        </Text>
                      </Box>
                    )}

                    <Flex width="100%" direction="column" align={!expanded ? 'center' : 'stretch'}>
                      {visibleItems.map((item) => (
                        <SidebarNavItem
                          key={item.id}
                          item={item}
                          expanded={expanded}
                          isMobile={isMobile}
                          onLinkClick={onLinkClick}
                          isActive={isItemActive(item)}
                        />
                      ))}
                    </Flex>
                  </Box>
                );
              })}
            </nav>
          </Box>
        </ScrollArea>
      </Box>
    </Box>
  );
}
