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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUKeyAssignmentFlagType;
import com.io7m.aurantium.api.AUKeyAssignmentID;
import com.io7m.lanark.core.RDottedName;

import java.math.BigInteger;
import java.util.Objects;
import java.util.SortedSet;
import java.util.stream.Collectors;

// CHECKSTYLE:OFF

public record AE1KeyAssignment(
  @JsonPropertyDescription("The key assignment ID.")
  @JsonProperty(value = "ID", required = true)
  BigInteger id,
  @JsonPropertyDescription("The starting key value.")
  @JsonProperty(value = "KeyValueStart", required = true)
  BigInteger keyValueStart,
  @JsonPropertyDescription("The center key value.")
  @JsonProperty(value = "KeyValueCenter", required = true)
  BigInteger keyValueCenter,
  @JsonPropertyDescription("The end key value.")
  @JsonProperty(value = "KeyValueEnd", required = true)
  BigInteger keyValueEnd,
  @JsonPropertyDescription("The clip ID.")
  @JsonProperty(value = "ClipID", required = true)
  BigInteger clipId,
  @JsonPropertyDescription("The amplitude at the starting key.")
  @JsonProperty(value = "AmplitudeAtKeyStart", required = true)
  double amplitudeAtKeyStart,
  @JsonPropertyDescription("The amplitude at the center key.")
  @JsonProperty(value = "AmplitudeAtKeyCenter", required = true)
  double amplitudeAtKeyCenter,
  @JsonPropertyDescription("The amplitude at the end key.")
  @JsonProperty(value = "AmplitudeAtKeyEnd", required = true)
  double amplitudeAtKeyEnd,
  @JsonPropertyDescription("The starting velocity.")
  @JsonProperty(value = "AtVelocityStart", required = true)
  double atVelocityStart,
  @JsonPropertyDescription("The center velocity.")
  @JsonProperty(value = "AtVelocityCenter", required = true)
  double atVelocityCenter,
  @JsonPropertyDescription("The end velocity.")
  @JsonProperty(value = "AtVelocityEnd", required = true)
  double atVelocityEnd,
  @JsonPropertyDescription("The amplitude at the starting velocity.")
  @JsonProperty(value = "AmplitudeAtVelocityStart", required = true)
  double amplitudeAtVelocityStart,
  @JsonPropertyDescription("The amplitude at the center velocity.")
  @JsonProperty(value = "AmplitudeAtVelocityCenter", required = true)
  double amplitudeAtVelocityCenter,
  @JsonPropertyDescription("The amplitude at the end velocity.")
  @JsonProperty(value = "AmplitudeAtVelocityEnd", required = true)
  double amplitudeAtVelocityEnd,
  @JsonPropertyDescription("The flags.")
  @JsonProperty(value = "Flags", required = true)
  SortedSet<String> flags)
  implements AE1SchemaObjectType
{

  public AUKeyAssignment toKeyAssignment()
  {
    return new AUKeyAssignment(
      new AUKeyAssignmentID(this.id.longValueExact()),
      this.keyValueStart.longValueExact(),
      this.keyValueCenter.longValueExact(),
      this.keyValueEnd.longValueExact(),
      new AUClipID(this.clipId.longValueExact()),
      this.amplitudeAtKeyStart,
      this.amplitudeAtKeyCenter,
      this.amplitudeAtKeyEnd,
      this.atVelocityStart,
      this.atVelocityCenter,
      this.atVelocityEnd,
      this.amplitudeAtVelocityStart,
      this.amplitudeAtVelocityCenter,
      this.amplitudeAtVelocityEnd,
      this.flags.stream()
        .map(AE1KeyAssignment::keyAssignmentFlag)
        .collect(Collectors.toUnmodifiableSet())
    );
  }

  private static AUKeyAssignmentFlagType keyAssignmentFlag(
    final String text)
  {
    for (final var v : AUKeyAssignmentFlagType.AUKeyAssignmentFlagStandard.values()) {
      if (Objects.equals(v.descriptor().value(), text)) {
        return v;
      }
    }
    return new AUKeyAssignmentFlagType.AUKeyAssignmentFlagUnknown(new RDottedName(
      text));
  }
}
