/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.diagnostics;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Map;

/**
 * Pure-Java replacement for the jemalloc-jni native library. Calls jemalloc's {@code mallctl()} directly via the
 * Foreign Function &amp; Memory (FFM) API.
 *
 * <p>
 * Requires jemalloc to be {@code LD_PRELOAD}'d into the JVM process so that the {@code mallctl} symbol is
 * available in the process's loaded libraries. No native compilation, JNI, or architecture-specific shared library
 * artifacts are needed.
 */
public class JemallocProfileDumper
{

  // Platform-native layouts from the Linker, used for C types whose size varies by platform.
  private static final Map<String, MemoryLayout> CANONICAL_LAYOUTS = Linker.nativeLinker().canonicalLayouts();

  private static final ValueLayout SIZE_T = (ValueLayout) CANONICAL_LAYOUTS.get("size_t");

  private static final ValueLayout C_INT = (ValueLayout) CANONICAL_LAYOUTS.get("int");

  private static final ValueLayout.OfBoolean C_BOOL = (ValueLayout.OfBoolean) CANONICAL_LAYOUTS.get("bool");

  // mallctl signature: int mallctl(const char *name, void *oldp, size_t *oldlenp, void *newp, size_t newlen)
  private static final FunctionDescriptor MALLCTL_DESCRIPTOR = FunctionDescriptor.of(
      C_INT, // return: int (errno-style error code)
      ValueLayout.ADDRESS, // name: const char*
      ValueLayout.ADDRESS, // oldp: void*
      ValueLayout.ADDRESS, // oldlenp: size_t*
      ValueLayout.ADDRESS, // newp: void*
      SIZE_T // newlen: size_t
  );

  private static final MethodHandle MALLCTL;

  static {
    // jemalloc uses a large static TLS block and cannot be dlopen'd at runtime — it must be LD_PRELOAD'd.
    // System.loadLibrary registers the already-loaded library with the classloader so that loaderLookup() can
    // find its symbols. If jemalloc isn't preloaded, this throws UnsatisfiedLinkError which is caught by
    // JemallocHeapProfileTask.
    System.loadLibrary("jemalloc");

    Linker linker = Linker.nativeLinker();
    SymbolLookup lookup = SymbolLookup.loaderLookup();

    MemorySegment mallctlAddr = lookup.find("mallctl")
        .orElseThrow(() -> new UnsatisfiedLinkError(
            "mallctl symbol not found in loaded jemalloc library"));

    MALLCTL = linker.downcallHandle(mallctlAddr, MALLCTL_DESCRIPTOR);
  }

  private JemallocProfileDumper() {
    // utility class
  }

  /**
   * Dump the current jemalloc heap profile to the specified file. Profiling must already be enabled
   * ({@code prof:true} in {@code MALLOC_CONF}). If profiling is enabled but not active, calling this method will
   * activate it.
   *
   * @param filename the file to write the profile to
   * @return whether or not profiling was active prior to this call
   * @throws NullPointerException if filename is null
   * @throws IllegalArgumentException if filename is empty
   * @throws IllegalStateException if profiling is not enabled or permissions are insufficient
   * @throws RuntimeException if the dump fails for other reasons
   */
  public static synchronized boolean dumpProfile(String filename) {
    if (filename == null) {
      throw new NullPointerException("Filename must not be null");
    }
    if (filename.isEmpty()) {
      throw new IllegalArgumentException("Filename must not be empty");
    }

    try (Arena arena = Arena.ofConfined()) {
      boolean wasActive = activateProfiling(arena);
      dumpToFile(arena, filename);
      return wasActive;
    }
  }

  /**
   * Activates profiling by setting {@code prof.active} to true.
   *
   * @return whether profiling was already active before this call
   */
  private static boolean activateProfiling(Arena arena) {
    MemorySegment oldValue = arena.allocate(C_BOOL);
    MemorySegment oldSize = arena.allocate(SIZE_T);
    SIZE_T.varHandle().set(oldSize, 0L, C_BOOL.byteSize()); // sizeof(bool)

    MemorySegment newValue = arena.allocate(C_BOOL);
    newValue.set(C_BOOL, 0, true); // activate

    MemorySegment name = arena.allocateFrom("prof.active");

    int ret = invokeMallctl(name, oldValue, oldSize, newValue, C_BOOL.byteSize());

    if (ret != 0) {
      handleMallctlError("Failed to activate jemalloc profiling", ret);
    }

    return (boolean) oldValue.get(C_BOOL, 0);
  }

  /**
   * Dumps the heap profile to the specified file via {@code mallctl("prof.dump", ...)}.
   */
  private static void dumpToFile(Arena arena, String filename) {
    MemorySegment name = arena.allocateFrom("prof.dump");

    // mallctl("prof.dump") expects newp to be a char** (pointer to a char pointer)
    MemorySegment filenameStr = arena.allocateFrom(filename); // null-terminated C string
    MemorySegment filenamePtr = arena.allocate(ValueLayout.ADDRESS); // char**
    filenamePtr.set(ValueLayout.ADDRESS, 0, filenameStr);

    long pointerSize = ValueLayout.ADDRESS.byteSize();
    int ret = invokeMallctl(name, MemorySegment.NULL, MemorySegment.NULL, filenamePtr, pointerSize);

    if (ret != 0) {
      handleMallctlError("Failed to dump jemalloc profile", ret);
    }
  }

  private static int invokeMallctl(
      MemorySegment name,
      MemorySegment oldp,
      MemorySegment oldlenp,
      MemorySegment newp,
      long newlen)
  {
    try {
      return (int) MALLCTL.invoke(name, oldp, oldlenp, newp, newlen);
    }
    catch (Throwable t) {
      throw new RuntimeException("Failed to invoke mallctl", t);
    }
  }

  private static void handleMallctlError(String operation, int errno) {
    // errno values from <errno.h> on Linux (same on x86_64 and aarch64)
    String detail = switch (errno) {
      case 22 /* EINVAL */, 2 /* ENOENT */ -> "Invalid mallctl argument. Is profiling enabled?";
      case 1 /* EPERM */ -> "Permission denied";
      case 11 /* EAGAIN */ -> "A memory allocation failure occurred";
      default -> "Unknown error (errno=" + errno + ")";
    };

    boolean isStateError = (errno == 22 || errno == 2 || errno == 1);
    String message = operation + ": " + detail;

    if (isStateError) {
      throw new IllegalStateException(message);
    }
    else {
      throw new RuntimeException(message);
    }
  }
}
