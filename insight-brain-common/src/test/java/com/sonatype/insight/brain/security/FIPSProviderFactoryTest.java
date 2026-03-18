/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.security.Provider;
import java.util.Arrays;

import org.junit.Test;

import static com.sonatype.insight.brain.security.FIPSProviderFactory.FIPS_PROVIDER_CLASS_NAME;
import static java.nio.file.Files.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FIPSProviderFactoryTest
{
  private static final String[] EXPECTED_EMBEDDED_FIPS_JARS = {
    "lib/bc-fips.jar",
    "lib/bcpkix-fips.jar",
    "lib/bctls-fips.jar"
  };

  private static final String EXPECTED_BCFIPS_NAME = "BCFIPS";

  @Test
  public void testCreateFipsProvider_LoadsFromClasspath() {
    Provider provider = FIPSProviderFactory.createFipsProvider();
    assertThat(provider).isNotNull();
    assertThat(provider.getName()).contains(EXPECTED_BCFIPS_NAME);
  }

  @Test
  public void testCreateFipsProvider_LoadsFromEmbeddedJars_WhenClasspathFails() throws Exception {
    Provider provider = createFipsProviderWithClassLoader(createClassLoaderWithEmbeddedFipsJars());
    assertThat(provider).isNotNull();
    assertThat(provider.getName()).contains(EXPECTED_BCFIPS_NAME);
  }

  @Test
  public void testCreateFipsProvider_WhenFipsNotAvailable_ThrowsException() {
    assertThatThrownBy(() -> createFipsProviderWithClassLoader(createClassLoaderWithFipsProviderExcluded()))
        .isInstanceOf(InvocationTargetException.class)
        .cause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to create FIPS provider");
  }

  @Test
  public void testCreateFipsProvider_WhenEmbeddedJarsNotFound_ThrowsException() {
    assertThatThrownBy(() -> createFipsProviderWithClassLoader(createClassLoaderWithFipsProviderAndEmbedJardExcluded()))
        .isInstanceOf(InvocationTargetException.class)
        .cause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to create FIPS provider")
        .cause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Incomplete FIPS JAR extraction")
        .hasMessageContaining("Expected 3 JARs but found 0")
        .hasMessageContaining("Expected: [lib/bc-fips.jar, lib/bcpkix-fips.jar, lib/bctls-fips.jar]");
  }

  @Test
  public void testCreateFipsProvider_WhenPartialEmbeddedJarsFound_ThrowsException() {
    assertThatThrownBy(() -> createFipsProviderWithClassLoader(createClassLoaderWithPartialEmbeddedJars()))
        .isInstanceOf(InvocationTargetException.class)
        .cause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Failed to create FIPS provider")
        .cause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Incomplete FIPS JAR extraction")
        .hasMessageContaining("Expected 3 JARs but found 1")
        .hasMessageContaining("Expected: [lib/bc-fips.jar, lib/bcpkix-fips.jar, lib/bctls-fips.jar]")
        .hasMessageContaining("Found: [lib/bc-fips.jar]");
  }

  /**
   * Helper method to invoke {@link FIPSProviderFactory#createFipsProvider()} using a specific {@link ClassLoader}
   * context.
   *
   * <p>
   * This method loads the {@link FIPSProviderFactory} class using the provided ClassLoader and invokes
   * its static createFipsProvider() method via reflection. This allows testing the factory behavior in different
   * classloader contexts (restricted, embedded JAR scenarios, etc.).
   * </p>
   *
   * @param classLoader the {@link ClassLoader} context to use for loading the factory class
   * @return the FIPS {@link Provider} created by the factory
   * @throws ReflectiveOperationException if the factory class cannot be loaded or method invoked
   */
  private Provider createFipsProviderWithClassLoader(
      final ClassLoader classLoader) throws ReflectiveOperationException
  {
    Method createMethod = classLoader
        .loadClass(FIPSProviderFactory.class.getName())
        .getMethod("createFipsProvider");

    return (Provider) createMethod.invoke(null);
  }

  /**
   * Creates a restricted {@link ClassLoader} that excludes FIPS classes to simulate an environment where FIPS libraries
   * are not available. This is useful for testing failure scenarios.
   *
   * @return ClassLoader that throws ClassNotFoundException for FIPS {@link Provider} classes
   */
  private ClassLoader createClassLoaderWithFipsProviderExcluded() {
    ClassLoader parentLoader = this.getClass().getClassLoader();
    return new ExcludeByNameClassLoader(parentLoader)
    {
      @Override
      public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (name.equals(FIPS_PROVIDER_CLASS_NAME)) {
          throw new ClassNotFoundException("FIPS provider not available in restricted environment");
        }
        return name.equals(FIPSProviderFactory.class.getName()) ? findClass(name) : super.loadClass(name);
      }
    };
  }

  /**
   * Creates a restricted ClassLoader that excludes FIPS classes and provides no embedded JAR resources. This simulates
   * an environment where FIPS libraries are completely unavailable both on classpath and as embedded resources.
   *
   * @return ClassLoader that has no access to FIPS resources
   */
  private ClassLoader createClassLoaderWithFipsProviderAndEmbedJardExcluded() {
    ClassLoader parentLoader = this.getClass().getClassLoader();
    return new ExcludeByNameClassLoader(parentLoader)
    {
      @Override
      public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (name.equals(FIPS_PROVIDER_CLASS_NAME)) {
          throw new ClassNotFoundException("FIPS provider not available in restricted environment");
        }
        return name.equals(FIPSProviderFactory.class.getName()) ? findClass(name) : super.loadClass(name);
      }

      @Override
      public InputStream getResourceAsStream(final String name) {
        // Return null for all embedded JAR resources to simulate empty extractEmbeddedJars result
        String normalizedName = name.startsWith("/") ? name.substring(1) : name;
        if (FIPSProviderFactoryTest.isFipsJarResource(normalizedName)) {
          return null;
        }
        return super.getResourceAsStream(name);
      }
    };
  }

  /**
   * Creates a ClassLoader that excludes FIPS classes and provides only partial embedded JAR resources. This simulates a
   * scenario where some FIPS JARs are missing, triggering the incomplete extraction validation.
   *
   * @return ClassLoader that provides only some FIPS JAR resources
   */
  private ClassLoader createClassLoaderWithPartialEmbeddedJars() throws ReflectiveOperationException, IOException {
    ClassLoader parentLoader = this.getClass().getClassLoader();
    byte[] fipsJarBytes = loadFipsJarBytes();

    return new ExcludeByNameClassLoader(parentLoader)
    {
      @Override
      public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (name.equals(FIPS_PROVIDER_CLASS_NAME)) {
          throw new ClassNotFoundException("FIPS provider not available in restricted environment");
        }
        return name.equals(FIPSProviderFactory.class.getName()) ? findClass(name) : super.loadClass(name);
      }

      @Override
      public InputStream getResourceAsStream(final String name) {
        // Only provide one jar, exclude the others to trigger incomplete extraction
        String normalizedName = name.startsWith("/") ? name.substring(1) : name;
        if (EXPECTED_EMBEDDED_FIPS_JARS[0].equals(normalizedName)) {
          return new ByteArrayInputStream(fipsJarBytes);
        }

        // Simulate missing JARs
        if (isFipsJarResource(normalizedName)) {
          return null;
        }

        return super.getResourceAsStream(name);
      }
    };
  }

  /**
   * Creates a ClassLoader that blocks direct classpath access to FIPS classes but provides embedded FIPS JAR resources.
   * This simulates the scenario where FIPS classes are not on the classpath but are available as embedded JAR
   * resources.
   *
   * @return ClassLoader that forces loading from embedded JARs
   */
  private ClassLoader createClassLoaderWithEmbeddedFipsJars() throws ReflectiveOperationException, IOException {
    ClassLoader parentLoader = this.getClass().getClassLoader();
    byte[] fipsJarBytes = loadFipsJarBytes();

    return new ExcludeByNameClassLoader(parentLoader)
    {
      @Override
      public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (name.equals(FIPS_PROVIDER_CLASS_NAME)) {
          throw new ClassNotFoundException("FIPS provider not available in restricted environment");
        }
        return name.equals(FIPSProviderFactory.class.getName()) ? findClass(name) : super.loadClass(name);
      }

      @Override
      public InputStream getResourceAsStream(final String name) {
        // Provide embedded FIPS JAR resources (handle both with and without leading slash)
        String normalizedName = name.startsWith("/") ? name.substring(1) : name;
        if (FIPSProviderFactoryTest.isFipsJarResource(normalizedName)) {
          return new ByteArrayInputStream(fipsJarBytes);
        }
        return super.getResourceAsStream(name);
      }
    };
  }

  /**
   * Abstract ClassLoader that excludes specific classes by name while providing a common implementation for loading in
   * restricted test contexts.
   *
   * <p>
   * This base class handles the common concern of loading the classes from bytecode in an isolated classloader
   * context, while allowing subclasses to define their own class exclusion rules and resource handling strategies.
   * </p>
   *
   * <p>
   * Subclasses must implement {@code loadClass(String)} to define which classes should
   * be excluded and how they should be handled.
   * </p>
   */
  private abstract static class ExcludeByNameClassLoader
      extends ClassLoader
  {
    protected final ClassLoader parentLoader;

    protected ExcludeByNameClassLoader(final ClassLoader parent) {
      super(parent);
      this.parentLoader = parent;
    }

    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
      if (name.equals(FIPSProviderFactory.class.getName())) {
        // Get the factory class bytecode and load it in this restricted classloader
        String resourceName = name.replace('.', '/') + ".class";
        try (var input = parentLoader.getResourceAsStream(resourceName)) {
          if (input == null) {
            throw new ClassNotFoundException(name);
          }
          byte[] bytes = input.readAllBytes();
          return defineClass(name, bytes, 0, bytes.length);
        }
        catch (Exception e) {
          throw new ClassNotFoundException(name, e);
        }
      }
      return super.findClass(name);
    }
  }

  /**
   * Loads the FIPS JAR bytes from the classpath for use in ClassLoader tests.
   *
   * @return byte array containing the FIPS JAR content
   * @throws ReflectiveOperationException if FIPS class cannot be loaded
   * @throws IOException if JAR file cannot be read
   */
  private static byte[] loadFipsJarBytes() throws ReflectiveOperationException, IOException {
    Class<?> fipsClass = Class.forName(FIPS_PROVIDER_CLASS_NAME);
    String fipsJarPath = fipsClass.getProtectionDomain().getCodeSource().getLocation().getPath();
    return readAllBytes(Paths.get(fipsJarPath));
  }

  /**
   * Checks if the given resource name represents an expected FIPS JAR resource. {@link String}
   *
   * @param name - {@link String} with the normalized resource name (without leading slash)
   * @return true if the name represents a FIPS JAR resource
   */
  private static boolean isFipsJarResource(final String name) {
    return Arrays.asList(EXPECTED_EMBEDDED_FIPS_JARS).contains(name);
  }
}
