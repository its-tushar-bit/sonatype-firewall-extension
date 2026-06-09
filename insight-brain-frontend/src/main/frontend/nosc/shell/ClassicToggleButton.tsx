/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Button, Flex, Theme } from '@radix-ui/themes';
import { ActionIcons } from 'MainRoot/nosc/icons';
import { classicToNexusOneUrl } from 'MainRoot/nexus-one/classicToNexusOneUrl';
import { NEXUS_ONE_DEFAULT_PATH } from 'MainRoot/nosc/routing/classicPreviewMap';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { selectIsPreviewNexusOneUiEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { bundleIndexUrl, isNexusOneBundle } from 'MainRoot/util/urlUtil';

import '@radix-ui/themes/styles.css';
// Loaded here because this button is rendered inside the Classic bundle
// (App.jsx) where the Nexus One SPA's CSS is not loaded.
import '@sonatype/nexus-one-components';

function readHashPath(): string {
  const rawHash = typeof window !== 'undefined' ? window.location.hash : '';
  return rawHash.startsWith('#') ? rawHash.slice(1) : rawHash;
}

export function ClassicToggleButton() {
  const [hashPath, setHashPath] = useState<string>(readHashPath());
  const isPreviewEnabled = useSelector(selectIsPreviewNexusOneUiEnabled);
  const { effectiveTheme } = useNoscTheme();

  useEffect(() => {
    const onHashChange = () => setHashPath(readHashPath());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  if (!isPreviewEnabled || isNexusOneBundle()) {
    return null;
  }

  const handleClick = () => {
    const nexusOnePath = classicToNexusOneUrl(hashPath) ?? NEXUS_ONE_DEFAULT_PATH;
    window.location.assign(bundleIndexUrl('nexus-one', nexusOnePath));
  };

  return (
    <div
      style={{
        position: 'fixed',
        top: 14,
        right: 320,
        zIndex: 9999,
      }}
      data-testid="classic-toggle-button"
    >
      <Theme
        appearance={effectiveTheme}
        accentColor="green"
        grayColor="slate"
        radius="full"
        scaling="100%"
        hasBackground={false}
      >
        <Button
          size="2"
          variant="solid"
          color="green"
          highContrast
          onClick={handleClick}
          aria-label="Switch to Nexus One UI"
        >
          <Flex align="center" gap="2">
            <ActionIcons.Swap size={14} />
            <span>Switch to Nexus One UI</span>
          </Flex>
        </Button>
      </Theme>
    </div>
  );
}
