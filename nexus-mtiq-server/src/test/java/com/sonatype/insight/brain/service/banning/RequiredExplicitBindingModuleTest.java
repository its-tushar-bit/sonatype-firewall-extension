/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementVisitor;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.UntargettedBinding;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequiredExplicitBindingModuleTest
{
  private RequiredExplicitBindingModule underTest;

  @Mock
  private Binder binder;

  @Mock
  private BannedImplementationService banned;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_EnsureExplicitBindingsEnabled() {

    underTest = new RequiredExplicitBindingModule(Collections.emptyList(), null);
    underTest.configure(binder);

    verify(binder).requireExplicitBindings();
  }

  @Test
  public void test_WhenElementUntargettedAndBannedApplyToSkipped() {
    List<Element> elements = Arrays.asList(
        new TestUntargettedBinding<String>()
    );

    when(banned.isBanned(any())).thenReturn(true);
    underTest = new RequiredExplicitBindingModule(elements, banned);

    underTest.configure(binder);

    verify(banned).isBanned(any());
  }

  @Test
  public void test_WhenElementUntargettedAndNotBannedApplyToCalled() {
    List<Element> elements = Collections.singletonList(
        new TestUntargettedBinding<String>()
    );

    // Verifies that the element.applyTo is called which means the element is not skipped
    expectedException.expect(TestAppliedToException.class);

    when(banned.isBanned(any())).thenReturn(false);
    underTest = new RequiredExplicitBindingModule(elements, banned);

    underTest.configure(binder);
  }

  @Test
  public void test_WhenElementDefinedAsInterfaceAndBannedApplyToSkipped() {
    List<Element> elements = Collections.singletonList(
        new TestLinkedKeyBinding<String>()
    );

    when(banned.isBanned(any())).thenReturn(true);
    underTest = new RequiredExplicitBindingModule(elements, banned);

    underTest.configure(binder);

    verify(banned).isBanned(any());
  }

  @Test
  public void test_WhenElementDefinedAsInterfaceAndNotBannedApplyToCalled() {
    List<Element> elements = Collections.singletonList(
        new TestLinkedKeyBinding<String>()
    );

    // Verifies that the element.applyTo is called which means the element is not skipped
    expectedException.expect(TestAppliedToException.class);

    when(banned.isBanned(any())).thenReturn(false);
    underTest = new RequiredExplicitBindingModule(elements, banned);

    underTest.configure(binder);
  }

  private class TestUntargettedBinding<T>
      implements UntargettedBinding<T>
  {
    @Override
    public Key<T> getKey() {
      return (Key<T>) Key.get(RequiredExplicitBindingModuleTest.class);
    }

    @Override
    public Provider<T> getProvider() {
      return null;
    }

    @Override
    public <V> V acceptTargetVisitor(final BindingTargetVisitor<? super T, V> visitor) {
      return null;
    }

    @Override
    public <V> V acceptScopingVisitor(final BindingScopingVisitor<V> visitor) {
      return null;
    }

    @Override
    public Object getSource() {
      return null;
    }

    @Override
    public <T> T acceptVisitor(final ElementVisitor<T> visitor) {
      return null;
    }

    @Override
    public void applyTo(final Binder binder) {
      throw new TestAppliedToException();
    }
  }

  private class TestLinkedKeyBinding<T>
      implements LinkedKeyBinding<T>
  {
    @Override
    public Key<? extends T> getLinkedKey() {
      return (Key<T>) Key.get(RequiredExplicitBindingModuleTest.class);
    }

    @Override
    public Key<T> getKey() {
      return null;
    }

    @Override
    public Provider<T> getProvider() {
      return null;
    }

    @Override
    public <V> V acceptTargetVisitor(final BindingTargetVisitor<? super T, V> visitor) {
      return null;
    }

    @Override
    public <V> V acceptScopingVisitor(final BindingScopingVisitor<V> visitor) {
      return null;
    }

    @Override
    public Object getSource() {
      return null;
    }

    @Override
    public <T> T acceptVisitor(final ElementVisitor<T> visitor) {
      return null;
    }

    @Override
    public void applyTo(final Binder binder) {
      throw new TestAppliedToException();
    }
  }

  private class TestAppliedToException
      extends RuntimeException
  {
  }
}
