/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef, useState } from 'react';
import { useSelector } from 'react-redux';
import { NxButton, NxFontAwesomeIcon, NxFormGroup } from '@sonatype/react-shared-components';
import { faCopy, faEye, faEyeSlash } from '@fortawesome/pro-solid-svg-icons';
import {
  selectRelayWebhookSecret,
  selectShouldShowRelayWebhookSecret,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';

/**
 * Click-to-reveal masked input for the per-customer HMAC webhook signing secret. Hidden when
 * the underlying selector says the secret is not applicable (no PAT-mode registration, App-mode
 * registration, no provider chosen, GitHub App auth, or feature gate closed).
 *
 * Renders a password-masked input by default; an eye icon toggles to plaintext. A copy button
 * works in either state so admins don't have to reveal the secret to paste it into the SCM
 * provider's webhook configuration.
 */
const RelayWebhookSecretField = () => {
  const secret = useSelector(selectRelayWebhookSecret);
  const shouldShow = useSelector(selectShouldShowRelayWebhookSecret);
  const [revealed, setRevealed] = useState(false);
  const [justCopied, setJustCopied] = useState(false);
  // Track the "Copied" hint timer so we can clear it on unmount and prevent a stray setState
  // call after the component is gone (no user-visible impact in React 18, but keeps things tidy).
  const copiedTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (copiedTimerRef.current !== null) {
        clearTimeout(copiedTimerRef.current);
      }
    };
  }, []);

  if (!shouldShow) {
    return null;
  }

  const onToggleReveal = () => setRevealed((v) => !v);

  const onCopy = () => {
    if (!secret) return;
    // navigator.clipboard is the standard browser path; the rest of this app already assumes it
    // (see NxCopyToClipboard's tests). No fallback needed for our supported browsers, but the
    // second optional chain guards against undefined clipboard (non-HTTPS / older test envs)
    // so a TypeError on .then() never reaches the user.
    navigator.clipboard?.writeText(secret)?.then(
      () => {
        setJustCopied(true);
        // Reset the "Copied" hint after a couple seconds so repeated copies still feel
        // responsive. The timer id is captured in a ref so the unmount cleanup effect can
        // clear it; clearing any prior pending timer here also keeps repeated rapid copies
        // from resetting on the OLD timer's schedule.
        if (copiedTimerRef.current !== null) {
          clearTimeout(copiedTimerRef.current);
        }
        copiedTimerRef.current = setTimeout(() => {
          setJustCopied(false);
          copiedTimerRef.current = null;
        }, 2000);
      },
      () => {
        // Swallow clipboard rejections silently; the user will notice nothing got pasted and
        // we don't want to throw an unhandled promise.
      }
    );
  };

  return (
    <NxFormGroup
      id="source-control-relay-webhook-secret"
      className="iq-relay-webhook-secret"
      label="Webhook Signing Secret"
      sublabel="Paste this secret into your SCM provider's webhook configuration so the relay can verify the signature on each delivery."
    >
      <div className="iq-relay-webhook-secret__row">
        <input
          id="source-control-relay-webhook-secret-input"
          className="nx-text-input__input"
          type={revealed ? 'text' : 'password'}
          readOnly
          value={secret ?? ''}
          aria-label="Webhook Signing Secret"
        />
        <NxButton
          type="button"
          variant="tertiary"
          onClick={onToggleReveal}
          aria-label={revealed ? 'Hide webhook signing secret' : 'Show webhook signing secret'}
          aria-pressed={revealed}
          title={revealed ? 'Hide' : 'Show'}
        >
          <NxFontAwesomeIcon icon={revealed ? faEyeSlash : faEye} />
        </NxButton>
        <NxButton
          type="button"
          variant="tertiary"
          onClick={onCopy}
          aria-label="Copy webhook signing secret to clipboard"
          title={justCopied ? 'Copied' : 'Copy to Clipboard'}
        >
          <NxFontAwesomeIcon icon={faCopy} />
          <span className="nx-button__text">{justCopied ? 'Copied' : 'Copy to Clipboard'}</span>
        </NxButton>
      </div>
    </NxFormGroup>
  );
};

export default RelayWebhookSecretField;
