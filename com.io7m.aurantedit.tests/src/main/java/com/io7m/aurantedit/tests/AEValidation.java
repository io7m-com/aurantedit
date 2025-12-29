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

package com.io7m.aurantedit.tests;

import com.io7m.aurantium.parser.api.AUParseRequest;
import com.io7m.aurantium.validation.api.AUValidationRequest;
import com.io7m.aurantium.vanilla.AU1Parsers;
import com.io7m.aurantium.vanilla.AU1Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class AEValidation
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AEValidation.class);

  private AEValidation()
  {

  }

  static void validate(
    final Path path)
    throws Exception
  {
    final var readers =
      new AU1Parsers();
    final var validators =
      new AU1Validators();

    try (var channel = FileChannel.open(path)) {
      try (var reader = readers.createParser(
        new AUParseRequest(
          channel,
          path.toUri()
        )
      )) {
        final var file = reader.execute();
        final var validator = validators.createValidator(
          new AUValidationRequest(
            file,
            path.toUri()
          )
        );
        final var errors = validator.execute();
        for (final var error : errors) {
          LOG.debug("Error: {}", error);
        }
        assertEquals(0, errors.size());
      }
    }
  }
}
