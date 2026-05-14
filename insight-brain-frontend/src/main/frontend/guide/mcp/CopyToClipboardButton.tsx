/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useState } from 'react';
import { Button, Tooltip } from '@radix-ui/themes';
import { Copy, Check } from 'lucide-react';
import { tokens } from '@guide/ui-core/utils';

interface CopyToClipboardButtonProps {
  text: string;
  size?: '1' | '2' | '3';
  iconSize?: number;
  position?: { top?: string; right?: string; bottom?: string; left?: string };
}

export function CopyToClipboardButton({
  text,
  size = tokens.button.small,
  iconSize = 14,
  position = { top: '8px', right: '8px' },
}: CopyToClipboardButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => {
        setCopied(false);
      }, 2000);
    } catch (err) {
      console.error('Failed to copy to clipboard:', err);
    }
  };

  return (
    <Tooltip content={copied ? 'Copied!' : 'Copy'}>
      <Button
        aria-label={copied ? 'Copied to clipboard' : 'Copy to clipboard'}
        variant="soft"
        size={size}
        style={{ position: 'absolute', ...position }}
        onClick={handleCopy}
      >
        {copied ? <Check size={iconSize} /> : <Copy size={iconSize} />}
      </Button>
    </Tooltip>
  );
}
