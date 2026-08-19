/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { Grid, Box } from '@radix-ui/themes';
import { FocusScope } from '@radix-ui/react-focus-scope';
import { useAdapterPathname } from '@guide/ui-core';
import { TopNavigation } from './TopNavigation';
import SidebarNavigation from './SidebarNavigation';
import { SIDEBAR_GROUPS, SIDEBAR_STORAGE_KEY } from './constants';
import sidebarStyles from './SidebarNavigation.module.css';

function useLocalStorageState(key: string, defaultValue: boolean): [boolean, (v: boolean) => void] {
  const [value, setValue] = useState(() => {
    try {
      const stored = localStorage.getItem(key);
      return stored !== null ? JSON.parse(stored) : defaultValue;
    } catch {
      return defaultValue;
    }
  });

  const setStoredValue = useCallback(
    (v: boolean) => {
      setValue(v);
      try {
        localStorage.setItem(key, JSON.stringify(v));
      } catch {
        // ignore
      }
    },
    [key]
  );

  return [value, setStoredValue];
}

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    function update() {
      setIsMobile(window.innerWidth < 1024);
    }
    update();
    window.addEventListener('resize', update);
    return () => window.removeEventListener('resize', update);
  }, []);

  return isMobile;
}

interface AppShellProps {
  children: React.ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  const pathname = useAdapterPathname();
  const isMobile = useIsMobile();

  const sidebarToggleRef = useRef<HTMLButtonElement>(null);

  const [sidebarCollapsed, setSidebarCollapsed] = useLocalStorageState(SIDEBAR_STORAGE_KEY, false);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  // Auto-close mobile sidebar on navigation
  useEffect(() => {
    if (isMobile && mobileSidebarOpen) {
      setMobileSidebarOpen(false);
    }
  }, [pathname]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSidebarToggle = () => {
    if (isMobile) {
      setMobileSidebarOpen(!mobileSidebarOpen);
    } else {
      setSidebarCollapsed(!sidebarCollapsed);
    }
  };

  const handleSidebarClose = () => {
    if (isMobile && mobileSidebarOpen) {
      setMobileSidebarOpen(false);
      sidebarToggleRef.current?.focus();
    }
  };

  // Close mobile sidebar on Escape key
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && mobileSidebarOpen) {
        handleSidebarClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mobileSidebarOpen]);

  const shouldShowMobileSidebar = isMobile && mobileSidebarOpen;
  const sidebarExpanded = !sidebarCollapsed;

  return (
    <>
      <Grid
        rows="auto 1fr"
        columns={!isMobile ? 'auto 1fr' : 'minmax(0, 1fr)'}
        width="100%"
        style={{ overflow: 'hidden', height: '100dvh' }}
      >
        <Box style={{ gridColumn: '1 / -1' }}>
          <TopNavigation onSidebarToggle={handleSidebarToggle} sidebarToggleRef={sidebarToggleRef} />
        </Box>

        {!isMobile && (
          <Box>
            <SidebarNavigation
              groups={SIDEBAR_GROUPS}
              expanded={sidebarExpanded}
            />
          </Box>
        )}

        <Box asChild style={{ overflow: 'auto' }}>
          <main id="main-content" tabIndex={0}>
            {children}
          </main>
        </Box>
      </Grid>

      {shouldShowMobileSidebar && (
        <>
          <div
            className={`${sidebarStyles.overlay} ${sidebarStyles.overlayEntering}`}
            onClick={handleSidebarClose}
            aria-hidden
          />
          <FocusScope trapped loop>
            {/* eslint-disable-next-line jsx-a11y/prefer-tag-over-role */}
            <Box role="dialog" aria-modal="true" aria-label="Navigation menu">
              <SidebarNavigation
                groups={SIDEBAR_GROUPS}
                expanded={true}
                onLinkClick={handleSidebarClose}
                isMobile={true}
              />
            </Box>
          </FocusScope>
        </>
      )}
    </>
  );
}
