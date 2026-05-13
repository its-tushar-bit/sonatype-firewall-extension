/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useState } from 'react';
import { Button, Card, Flex, Heading, Text, TextField, Callout } from '@radix-ui/themes';
import { tokens } from '@guide/ui-core/utils';
import type { SsoConfig } from './loginApi';
import styles from './LoginPage.module.css';

interface LoginPageProps {
  login: (username: string, password: string) => Promise<void>;
  ssoConfig: SsoConfig | null;
}

export function LoginPage({ login, ssoConfig }: LoginPageProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const canSubmit = username.length > 0 && password.length > 0 && !submitting;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!canSubmit) return;

    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setSubmitting(false);
    }
  }

  function handleInputChange(setter: (v: string) => void) {
    return (e: React.ChangeEvent<HTMLInputElement>) => {
      setError(null);
      setter(e.target.value);
    };
  }

  function handleSsoClick() {
    if (ssoConfig) {
      const target = new URL(ssoConfig.loginUrl, window.location.origin);
      if (target.protocol !== 'https:' && target.protocol !== 'http:') return;
      // Matches LandingService.ORIGIN_GUIDE and OidcLoginFilter.ORIGIN_GUIDE in backend
      target.searchParams.set('origin', 'guide');
      if (window.location.hash) {
        target.searchParams.set('hash', window.location.hash);
      }
      window.location.assign(target.href);
    }
  }

  return (
    <div className={styles.container}>
      <Card className={styles.card} size={tokens.space.section}>
        <form onSubmit={handleSubmit}>
          <Flex direction="column" gap={tokens.space.section}>
            <Heading as="h1" size={tokens.sizes.sectionTitle} align="center">
              Sign in to Sonatype Guide
            </Heading>

            {error && (
              <Callout.Root color="red" role="alert">
                <Callout.Text>{error}</Callout.Text>
              </Callout.Root>
            )}

            <Flex direction="column" gap={tokens.space.tight}>
              <Text as="label" size={tokens.sizes.body.sm} weight="medium" htmlFor="login-username">
                Username
              </Text>
              <TextField.Root
                id="login-username"
                value={username}
                onChange={handleInputChange(setUsername)}
                autoComplete="username"
                required
              />
            </Flex>

            <Flex direction="column" gap={tokens.space.tight}>
              <Text as="label" size={tokens.sizes.body.sm} weight="medium" htmlFor="login-password">
                Password
              </Text>
              <TextField.Root
                id="login-password"
                type="password"
                value={password}
                onChange={handleInputChange(setPassword)}
                autoComplete="current-password"
                required
              />
            </Flex>

            <Button type="submit" disabled={!canSubmit} size={tokens.sizes.body.md}>
              {submitting ? 'Signing in…' : 'Sign in'}
            </Button>

            {ssoConfig && (
              <>
                <div className={styles.divider}>or</div>
                <Button type="button" variant="outline" size={tokens.sizes.body.md} onClick={handleSsoClick}>
                  Sign in with SSO
                </Button>
              </>
            )}
          </Flex>
        </form>
      </Card>
    </div>
  );
}
