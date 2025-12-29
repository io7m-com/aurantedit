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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.io7m.aurantium.api.AUMetadataValue;

import java.util.List;
import java.util.Objects;

// CHECKSTYLE:OFF

public record AE1MetadataValue(
  @JsonPropertyDescription("The metadata name.")
  @JsonProperty("Name")
  String name,
  @JsonPropertyDescription("The metadata value.")
  @JsonProperty("Value")
  String value)
  implements AE1SchemaObjectType
{
  public AE1MetadataValue
  {
    Objects.requireNonNull(name, "Name");
    Objects.requireNonNull(value, "Value");
  }

  public AUMetadataValue toMetadata()
  {
    return new AUMetadataValue(
      this.name,
      this.value
    );
  }

  public static List<AUMetadataValue> toMetadataList(
    final List<AE1MetadataValue> xs)
  {
    return xs.stream()
      .map(AE1MetadataValue::toMetadata)
      .toList();
  }

  public static AE1MetadataValue ofMetadata(
    final AUMetadataValue metadata)
  {
    return new AE1MetadataValue(
      metadata.name(),
      metadata.value()
    );
  }

  public static List<AE1MetadataValue> ofMetadataList(
    final List<AUMetadataValue> xs)
  {
    return xs.stream()
      .map(AE1MetadataValue::ofMetadata)
      .toList();
  }
}
