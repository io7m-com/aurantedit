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

package com.io7m.aurantedit.filemodel.internal.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipReplaceState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipsAddState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipsDeleteState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1IdentifierPutState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1KeyAssignmentPutState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1MetadataPutState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1StateType;

/**
 * The type of command state.
 */

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "@Type"
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AEC1ClipReplaceState.class, name = "AEC1ClipReplaceState"),
  @JsonSubTypes.Type(value = AEC1ClipsAddState.class, name = "AEC1ClipsAddState"),
  @JsonSubTypes.Type(value = AEC1ClipsDeleteState.class, name = "AEC1ClipsDeleteState"),
  @JsonSubTypes.Type(value = AEC1IdentifierPutState .class, name = "AEC1IdentifierPutState"),
  @JsonSubTypes.Type(value = AEC1KeyAssignmentPutState.class, name = "AEC1KeyAssignmentPutState"),
  @JsonSubTypes.Type(value = AEC1MetadataPutState.class, name = "AEC1MetadataPutState"),
})
public sealed interface AECommandStateType
  permits AECommandCompactState,
  AECommandExportState,
  AECommandImportState,
  AECommandLoadState,
  AEC1StateType
{
  /**
   * @return The name of the command
   */

  @JsonProperty(value = "@Type", required = true)
  String type();
}
