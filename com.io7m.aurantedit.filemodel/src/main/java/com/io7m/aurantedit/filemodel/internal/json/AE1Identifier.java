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
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUVersion;
import com.io7m.lanark.core.RDottedName;

public record AE1Identifier(
  @JsonPropertyDescription("The group.")
  @JsonProperty(value = "Group", required = true)
  RDottedName group,
  @JsonPropertyDescription("The name.")
  @JsonProperty(value = "Name", required = true)
  RDottedName name,
  @JsonPropertyDescription("The major version.")
  @JsonProperty(value = "VersionMajor", required = true)
  int versionMajor,
  @JsonPropertyDescription("The minor version.")
  @JsonProperty(value = "VersionMinor", required = true)
  int versionMinor)
  implements AE1SchemaObjectType
{
  public AUIdentifier toIdentifier()
  {
    return new AUIdentifier(
      this.group,
      this.name,
      new AUVersion(this.versionMajor, this.versionMinor)
    );
  }
}
