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

package com.io7m.aurantedit.cmdline.internal;

import com.io7m.seltzer.api.SStructuredErrorType;
import com.io7m.seltzer.slf4j.SSLogging;
import org.slf4j.Logger;
import org.slf4j.event.Level;

final class AELogging
{
  private AELogging()
  {

  }

  static void logException(
    final Logger logger,
    final Exception e)
  {
    if (e instanceof final SStructuredErrorType<?> x) {
      SSLogging.logMDC(logger, Level.ERROR, x);
    } else {
      logger.error("{}: ", e.getMessage(), e);
    }
  }
}
