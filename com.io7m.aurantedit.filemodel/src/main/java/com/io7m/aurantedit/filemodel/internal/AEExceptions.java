/*
 * Copyright © 2025 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.aurantedit.filemodel.internal;

import com.io7m.aurantium.api.AUException;
import com.io7m.seltzer.api.SStructuredErrorExceptionType;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Functions to wrap exceptions.
 */

public final class AEExceptions
{
  private AEExceptions()
  {

  }

  /**
   * Wrap an exception.
   *
   * @param e The source
   *
   * @return The wrapped exception
   */

  public static AUException wrap(
    final Throwable e)
  {
    return switch (e) {
      case final AUException x -> {
        yield x;
      }
      case final SStructuredErrorExceptionType<?> x -> {
        yield new AUException(
          x.getMessage(),
          e,
          x.errorCode().toString(),
          x.attributes(),
          x.remediatingAction()
        );
      }
      case final FileNotFoundException x -> {
        yield new AUException(
          "File does not exist.",
          e,
          "error-file-nonexistent",
          Map.ofEntries(
            Map.entry("File", x.getMessage())
          ),
          Optional.empty()
        );
      }
      case final NoSuchFileException x -> {
        yield new AUException(
          "File does not exist.",
          e,
          "error-file-nonexistent",
          Map.ofEntries(
            Map.entry("File", x.getMessage())
          ),
          Optional.empty()
        );
      }
      case final ExecutionException x -> {
        yield wrap(x.getCause());
      }
      case Throwable _ -> {
        yield new AUException(
          e.getMessage(),
          e,
          "error-exception",
          Map.of(),
          Optional.empty()
        );
      }
    };
  }
}
