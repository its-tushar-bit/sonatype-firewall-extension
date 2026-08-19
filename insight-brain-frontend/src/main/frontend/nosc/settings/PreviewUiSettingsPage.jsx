/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import { Box, Card, Flex, Heading, Switch, Text, Theme } from '@radix-ui/themes';
import { AlertCircle } from 'lucide-react';
import { authErrorMessage } from 'MainRoot/util/authorizationUtil';
import { getConfigFeatureUrl, getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';

import '@radix-ui/themes/styles.css';

/**
 * The SystemConfigurationPropertyFeature sub-flag names mirrored on
 * nexus-internal's preview-ui Settings page. Listed in render order.
 *
 * Stays in sync with insight-brain-data/.../SystemConfigurationProperty.java
 * (the same constant strings).
 */
const TOGGLES = {
  PREVIEW_NEXUS_ONE_UI_ANONYMOUS_ENABLED: {
    label: 'Show Nexus One UI to anonymous (logged-out) users',
    description:
      'When enabled, users who are not logged in can see the new Preview UI. Requires the master Preview Nexus One UI feature flag to be ON.',
    section: 'access',
  },
  PREVIEW_NEXUS_ONE_UI_LOGGEDIN_ENABLED: {
    label: 'Show Nexus One UI to logged-in users',
    description: 'When enabled, logged-in users see an opt-in invitation in Classic and can switch to the Preview UI.',
    section: 'access',
  },
  PREVIEW_NEXUS_ONE_UI_DEFAULT_TO_PREVIEW: {
    label: 'Default new sessions to Preview UI',
    description:
      'When enabled (and at least one Access Control toggle is ON), users land in Preview by default. They can still switch back to Classic via the user menu.',
    section: 'rollout',
  },
  PREVIEW_NEXUS_ONE_UI_DISABLE_SWITCH_FEEDBACK: {
    label: 'Hide the "leave feedback" prompt when users switch UI',
    description:
      'When enabled, the feedback prompt that appears after switching between Classic and Preview is suppressed.',
    section: 'rollout',
  },
};

const ACCESS_TOGGLES = Object.keys(TOGGLES).filter((k) => TOGGLES[k].section === 'access');
const ROLLOUT_TOGGLES = Object.keys(TOGGLES).filter((k) => TOGGLES[k].section === 'rollout');

/**
 * The productFeatures slice keys are the kebab-cased Feature.getId() values.
 * SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI_ANONYMOUS_ENABLED
 * becomes 'preview-nexus-one-ui-anonymous-enabled'.
 */
function featureKey(toggle) {
  return toggle.toLowerCase().replace(/_/g, '-');
}

// JaxRsExceptionMapper returns plain-text error bodies (not JSON envelopes),
// so axios's err.response.data is the raw string for IQ API errors. Handle
// both shapes defensively in case any endpoint emits JSON.
function extractApiMessage(err) {
  const data = err?.response?.data;
  if (typeof data === 'string') return data;
  if (data && typeof data === 'object' && typeof data.message === 'string') return data.message;
  return '';
}

// /rest/product/features returns an array of kebab-case feature IDs that
// are CURRENTLY ENABLED. We project it to a presence-set keyed by the same
// kebab-case strings so the rest of the component can do membership tests.
function projectEnabledFeatures(arrayPayload) {
  const out = {};
  if (Array.isArray(arrayPayload)) {
    for (const id of arrayPayload) {
      out[id] = true;
    }
  }
  return out;
}

function applyToggleToFeatureSet(features, toggle, enabled) {
  const key = featureKey(toggle);
  const next = { ...features };
  if (enabled) {
    next[key] = true;
  } else {
    delete next[key];
  }
  return next;
}

export default function PreviewUiSettingsPage({ isAuthorized }) {
  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();
  // Page-local feature state (not Redux) so we can re-fetch on every toggle
  // without depending on slice cache semantics. The shared
  // productFeatures slice is read-only-ish (its IfNeeded thunk no-ops once
  // the cache is populated, which would silently swallow our updates).
  const [productFeatures, setProductFeatures] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [refreshError, setRefreshError] = useState(null);
  const [inflight, setInflight] = useState({});
  const [saveError, setSaveError] = useState(null);

  const refreshFeatures = useCallback(async ({ background = false } = {}) => {
    try {
      const response = await axios.get(getProductFeaturesUrl());
      setProductFeatures(projectEnabledFeatures(response.data));
      setLoadError(null);
      setRefreshError(null);
    } catch (err) {
      const message = extractApiMessage(err) || err?.message || 'Failed to load features';
      if (background) {
        setRefreshError(message);
      } else {
        setLoadError(message);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isAuthorized) {
      setIsLoading(false);
      return;
    }
    refreshFeatures();
  }, [isAuthorized, refreshFeatures]);

  const isToggleOn = useCallback((toggle) => Boolean(productFeatures[featureKey(toggle)]), [productFeatures]);

  const handleToggle = useCallback(
    async (toggle, nextOn) => {
      setSaveError(null);
      setInflight((prev) => ({ ...prev, [toggle]: true }));
      let saveSucceeded = false;
      try {
        const url = getConfigFeatureUrl(toggle);
        if (nextOn) {
          await axios.post(url);
        } else {
          await axios.delete(url);
        }
        saveSucceeded = true;
      } catch (err) {
        // ApiConfigFeaturesService throws FeatureAlreadyEnabledException /
        // FeatureAlreadyDisabledException (both 400) when the feature is
        // already in the requested state. The JaxRsExceptionMapper returns
        // these as plain-text bodies (not JSON), so the message lives in
        // err.response.data as a raw string. From the user's POV, a click
        // on a toggle that's "already correct" is a successful no-op —
        // re-sync page state from the server instead of surfacing a fake
        // error.
        const status = err?.response?.status;
        const apiMessage = extractApiMessage(err);
        const isAlreadyInDesiredState = status === 400 && /already (enabled|disabled)/i.test(apiMessage);
        if (isAlreadyInDesiredState) {
          saveSucceeded = true;
        } else {
          setSaveError(apiMessage || err?.message || 'Failed to save setting');
        }
      }
      if (saveSucceeded) {
        setProductFeatures((prev) => applyToggleToFeatureSet(prev, toggle, nextOn));
      }
      try {
        await refreshFeatures({ background: true });
      } finally {
        setInflight((prev) => ({ ...prev, [toggle]: false }));
      }
    },
    [refreshFeatures]
  );

  const renderToggleRow = (toggle) => (
    <Flex key={toggle} justify="between" align="center" gap="4">
      <Box style={{ flex: 1 }}>
        <Text weight="bold">{TOGGLES[toggle].label}</Text>
        <Text as="p" size="2" color="gray">
          {TOGGLES[toggle].description}
        </Text>
      </Box>
      <Switch
        data-testid={`preview-ui-toggle-${featureKey(toggle)}`}
        checked={isToggleOn(toggle)}
        onCheckedChange={(checked) => handleToggle(toggle, checked)}
        disabled={Boolean(inflight[toggle])}
      />
    </Flex>
  );

  // Compute the body (loading / error / normal). Wrap the whole thing in a
  // Radix Theme below so the page renders correctly even when no parent
  // Theme exists (e.g., when rendered inside Classic's <UIView/> at
  // /assets/#/previewUiSettings, where the Classic chrome doesn't provide
  // a Theme).
  let body;
  if (!isAuthorized) {
    body = (
      <Box p="6" data-testid="preview-ui-settings-unauthorized">
        <Text color="red">{authErrorMessage}</Text>
      </Box>
    );
  } else if (isLoading && Object.keys(productFeatures).length === 0) {
    body = (
      <Box p="6" data-testid="preview-ui-settings-loading">
        <Text color="gray">Loading…</Text>
      </Box>
    );
  } else if (loadError && Object.keys(productFeatures).length === 0) {
    body = (
      <Box p="6" data-testid="preview-ui-settings-error">
        <Flex gap="2" align="center">
          <AlertCircle size={16} />
          <Text color="red">Failed to load Preview UI settings: {loadError}</Text>
        </Flex>
      </Box>
    );
  } else {
    body = (
      <Box p="6" style={{ maxWidth: '720px' }} data-testid="preview-ui-settings-page">
        <Heading size="6" mb="3">
          Preview — Nexus One UI Settings
        </Heading>
        <Text as="p" color="gray" mb="5" size="3">
          Configure how the Nexus One Preview UI is offered to users in this IQ Server instance. Changes take effect on
          the next page load.
        </Text>

        {refreshError && (
          <Flex
            gap="2"
            align="center"
            mb="4"
            p="3"
            style={{ backgroundColor: 'var(--amber-3)', borderRadius: 'var(--radius-2)' }}
            data-testid="preview-ui-settings-refresh-error"
          >
            <AlertCircle size={14} />
            <Text size="2">
              Could not refresh settings from the server: {refreshError}. Your last change was saved; reload the page to
              sync.
            </Text>
          </Flex>
        )}

        {saveError && (
          <Flex
            gap="2"
            align="center"
            mb="4"
            p="3"
            style={{ backgroundColor: 'var(--red-3)', borderRadius: 'var(--radius-2)' }}
            data-testid="preview-ui-settings-save-error"
          >
            <AlertCircle size={14} />
            <Text size="2" color="red">
              {saveError}
            </Text>
          </Flex>
        )}

        <Flex direction="column" gap="4">
          <Card>
            <Box p="4">
              <Heading size="3" mb="3">
                Access Control
              </Heading>
              <Flex direction="column" gap="3">
                {ACCESS_TOGGLES.map(renderToggleRow)}
              </Flex>
            </Box>
          </Card>

          <Card>
            <Box p="4">
              <Heading size="3" mb="3">
                Rollout
              </Heading>
              <Flex direction="column" gap="3">
                {ROLLOUT_TOGGLES.map(renderToggleRow)}
              </Flex>
            </Box>
          </Card>
        </Flex>
      </Box>
    );
  }

  // Position the Theme container as a fixed overlay so it sits in the visible
  // viewport area below the 56px TopNav and right of the 256px LeftNav,
  // regardless of how Classic's <UIView/> places this component in its own
  // flow. Mirrors PreviewPagePlaceholder's positioning so the F4 admin page
  // and the placeholder pages appear in the same visual frame.
  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--gray-1)',
      }}
    >
      {body}
    </Theme>
  );
}

PreviewUiSettingsPage.propTypes = {
  isAuthorized: PropTypes.bool.isRequired,
};
