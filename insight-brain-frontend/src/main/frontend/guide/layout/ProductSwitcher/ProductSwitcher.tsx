/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { DropdownMenu, IconButton } from '@radix-ui/themes';
import { LayoutGrid } from 'lucide-react';
import { tokens } from '@guide/ui-core/utils';
import { useTheme } from 'GuideRoot/layout/ThemeProvider';
import { useLicensedProducts } from './useLicensedProducts';
import {
  getExploreProducts,
  PRODUCT_METADATA,
  type ExploreProduct,
  type LicensedProduct,
  type SolutionId,
} from './productMetadata';
import styles from './ProductSwitcher.module.css';

function openInNewTab(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer');
}

interface ProductIconProps {
  productId: SolutionId;
}

function ProductIcon({ productId }: ProductIconProps) {
  const { resolvedTheme } = useTheme();
  const meta = PRODUCT_METADATA[productId];
  const src = resolvedTheme === 'dark' ? meta.iconDark : meta.iconLight;
  return <img src={src} alt="" className={styles.productIcon} />;
}

function ProductMenuEntry({ product }: { product: LicensedProduct }) {
  if ('url' in product) {
    return (
      <DropdownMenu.Item onSelect={() => openInNewTab(product.url)}>
        <span className={styles.itemLabel}>
          <ProductIcon productId={product.id} />
          {product.displayName}
        </span>
      </DropdownMenu.Item>
    );
  }
  return (
    <DropdownMenu.Sub>
      <DropdownMenu.SubTrigger>
        <span className={styles.itemLabel}>
          <ProductIcon productId={product.id} />
          {product.displayName}
        </span>
      </DropdownMenu.SubTrigger>
      <DropdownMenu.SubContent>
        {product.instances.map((instance) => (
          <DropdownMenu.Item
            key={instance.url}
            onSelect={() => openInNewTab(instance.url)}
          >
            {instance.url}
          </DropdownMenu.Item>
        ))}
      </DropdownMenu.SubContent>
    </DropdownMenu.Sub>
  );
}

function ExploreMenuEntry({ product }: { product: ExploreProduct }) {
  return (
    <DropdownMenu.Item onSelect={() => openInNewTab(product.url)}>
      <span className={styles.itemLabel}>
        <ProductIcon productId={product.id} />
        {product.displayName}
      </span>
    </DropdownMenu.Item>
  );
}

export function ProductSwitcher() {
  const { products, loading, error } = useLicensedProducts();

  if (error) return null;

  const exploreProducts = getExploreProducts(products);
  const showLicensedSection = loading || products.length > 0;
  const showExploreSection = !loading && exploreProducts.length > 0;

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger>
        <IconButton
          variant="outline"
          size={tokens.sizes.caption}
          color="gray"
          aria-label="Sonatype Solutions"
        >
          <LayoutGrid size={16} />
        </IconButton>
      </DropdownMenu.Trigger>
      <DropdownMenu.Content align="end">
        {showLicensedSection && (
          <>
            <DropdownMenu.Label>My Sonatype Solutions</DropdownMenu.Label>
            {loading ? (
              <DropdownMenu.Item disabled>Loading…</DropdownMenu.Item>
            ) : (
              products.map((product) => (
                <ProductMenuEntry key={product.id} product={product} />
              ))
            )}
          </>
        )}
        {showLicensedSection && showExploreSection && <DropdownMenu.Separator />}
        {showExploreSection && (
          <>
            <DropdownMenu.Label>Explore</DropdownMenu.Label>
            {exploreProducts.map((product) => (
              <ExploreMenuEntry key={product.id} product={product} />
            ))}
          </>
        )}
      </DropdownMenu.Content>
    </DropdownMenu.Root>
  );
}

export default ProductSwitcher;
