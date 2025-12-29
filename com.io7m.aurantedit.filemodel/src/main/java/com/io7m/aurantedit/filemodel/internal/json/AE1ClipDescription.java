/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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
import com.io7m.aurantium.api.AUAudioFormatType;
import com.io7m.aurantium.api.AUClipDescription;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUClipLoopRange;
import com.io7m.aurantium.api.AUHashAlgorithm;
import com.io7m.aurantium.api.AUHashValue;
import com.io7m.aurantium.api.AUOctetOrder;
import com.io7m.lanark.core.RDottedName;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

public record AE1ClipDescription(
  @JsonPropertyDescription("The clip ID.")
  @JsonProperty(value = "ID", required = true)
  BigInteger id,
  @JsonPropertyDescription("The clip name.")
  @JsonProperty(value = "Name", required = true)
  String name,
  @JsonPropertyDescription("The clip format.")
  @JsonProperty(value = "Format", required = true)
  RDottedName format,
  @JsonPropertyDescription("The clip sample rate.")
  @JsonProperty(value = "SampleRate", required = true)
  BigInteger sampleRate,
  @JsonPropertyDescription("The clip sample depth.")
  @JsonProperty(value = "SampleDepth", required = true)
  BigInteger sampleDepth,
  @JsonPropertyDescription("The clip channel count.")
  @JsonProperty(value = "Channels", required = true)
  BigInteger channels,
  @JsonPropertyDescription("The clip endianness.")
  @JsonProperty(value = "Endianness", required = true)
  RDottedName endianness,
  @JsonPropertyDescription("The clip hash value.")
  @JsonProperty(value = "Hash", required = true)
  AE1HashValue hash,
  @JsonPropertyDescription("The clip audio data offset.")
  @JsonProperty(value = "Offset", required = true)
  BigInteger offset,
  @JsonPropertyDescription("The clip audio data size.")
  @JsonProperty(value = "Size", required = true)
  BigInteger size,
  @JsonPropertyDescription("The clip loop range.")
  @JsonProperty(value = "LoopRange", required = false)
  Optional<AE1ClipLoopRange> loopRange)
  implements AE1SchemaObjectType
{

  public AUClipDescription toClipDescription()
  {
    return new AUClipDescription(
      new AUClipID(this.id.longValueExact()),
      this.name,
      formatOf(this.format),
      this.sampleRate.longValueExact(),
      this.sampleDepth.longValueExact(),
      this.channels.longValueExact(),
      octetOrderOf(this.endianness),
      hashValueOf(this.hash),
      this.offset.longValueExact(),
      this.size.longValueExact(),
      this.loopRange().map(AE1ClipDescription::loopRangeOf)
    );
  }

  private static AUClipLoopRange loopRangeOf(
    final AE1ClipLoopRange x)
  {
    return new AUClipLoopRange(
      x.frameStart().longValueExact(),
      x.frameEndInclusive().longValueExact()
    );
  }

  private static AUAudioFormatType formatOf(
    final RDottedName formatName)
  {
    for (final var std : AUAudioFormatType.AUAudioFormatStandard.values()) {
      if (Objects.equals(formatName, std.descriptor())) {
        return std;
      }
    }
    return new AUAudioFormatType.AUAudioFormatUnknown(formatName);
  }

  private static AUOctetOrder octetOrderOf(
    final RDottedName endianness)
  {
    return AUOctetOrder.parse(endianness.value());
  }

  private static AUHashValue hashValueOf(
    final AE1HashValue v)
  {
    return new AUHashValue(
      AUHashAlgorithm.parse(v.algorithm().value()),
      v.value().value()
    );
  }
}
