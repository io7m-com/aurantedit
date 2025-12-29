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

import com.io7m.aurantium.api.AUVersion;
import com.io7m.quarrel.core.QException;
import com.io7m.quarrel.core.QValueConverterType;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A converter.
 */

public final class AUVersionConverter
  implements QValueConverterType<AUVersion>
{
  /**
   * A converter.
   */

  public AUVersionConverter()
  {

  }

  @Override
  public AUVersion convertFromString(
    final String text)
    throws QException
  {
    try {
      return AUVersion.parse(text);
    } catch (final ParseException e) {
      throw new QException(
        e.getMessage(),
        e,
        "error-parse",
        Map.of(),
        Optional.empty(),
        List.of()
      );
    }
  }

  @Override
  public String convertToString(
    final AUVersion version)
  {
    return version.toString();
  }

  @Override
  public AUVersion exampleValue()
  {
    return new AUVersion(1, 0);
  }

  @Override
  public String syntax()
  {
    return "[0-9]+\\.[0-9]+";
  }

  @Override
  public Class<AUVersion> convertedClass()
  {
    return AUVersion.class;
  }
}
