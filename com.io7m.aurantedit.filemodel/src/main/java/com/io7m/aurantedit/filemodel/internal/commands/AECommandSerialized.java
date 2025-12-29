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
import com.io7m.aurantedit.filemodel.AECommandRecordType;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A serialized command and state.
 *
 * @param time        The time
 * @param description The description
 * @param commandName The command name
 * @param state       The command state
 */

@JsonSerialize
@JsonDeserialize
public record AECommandSerialized(
  @JsonProperty(value = "Time", required = true)
  OffsetDateTime time,
  @JsonProperty(value = "Description", required = true)
  String description,
  @JsonProperty(value = "Command", required = true)
  String commandName,
  @JsonProperty(value = "State", required = true)
  AECommandStateType state)
  implements AECommandRecordType
{
  /**
   * A serialized command and state.
   *
   * @param time        The time
   * @param description The description
   * @param commandName The command name
   * @param state       The command state
   */

  public AECommandSerialized
  {
    Objects.requireNonNull(time, "Time");
    Objects.requireNonNull(description, "Description");
    Objects.requireNonNull(commandName, "CommandName");
    Objects.requireNonNull(state, "State");
  }
}
