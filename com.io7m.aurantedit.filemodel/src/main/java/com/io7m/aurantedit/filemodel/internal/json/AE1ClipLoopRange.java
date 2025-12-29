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

package com.io7m.aurantedit.filemodel.internal.json;

// CHECKSTYLE:OFF

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.io7m.aurantium.api.AUClipLoopRange;

import java.math.BigInteger;

import static com.io7m.aurantedit.filemodel.internal.json.AE1Scalars.bigInteger;

public record AE1ClipLoopRange(
  @JsonPropertyDescription("The start of the loop range in frames.")
  @JsonProperty(value = "FrameStart", required = true)
  BigInteger frameStart,
  @JsonPropertyDescription("The end of the loop range in frames (inclusive).")
  @JsonProperty(value = "FrameEndInclusive", required = true)
  BigInteger frameEndInclusive)
  implements AE1SchemaObjectType
{
  public static AE1ClipLoopRange of(
    final AUClipLoopRange range)
  {
    return new AE1ClipLoopRange(
      bigInteger(range.frameStart()),
      bigInteger(range.frameEndInclusive())
    );
  }
}
