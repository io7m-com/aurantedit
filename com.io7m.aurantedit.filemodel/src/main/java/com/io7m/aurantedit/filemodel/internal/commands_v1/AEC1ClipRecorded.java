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

package com.io7m.aurantedit.filemodel.internal.commands_v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.io7m.aurantedit.filemodel.internal.json.AE1ClipDeclaration;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * Saved data for the clips commands.
 *
 * @param audioData The audio data
 * @param clip      The clip
 */

@JsonDeserialize
@JsonSerialize
public record AEC1ClipRecorded(
  @JsonProperty(value = "AudioData", required = true)
  byte[] audioData,
  @JsonProperty(value = "Clip", required = true)
  AE1ClipDeclaration clip)
{
  /**
   * Saved data for the clips commands.
   *
   * @param audioData The audio data
   * @param clip      The clip
   */

  public AEC1ClipRecorded
  {
    Objects.requireNonNull(audioData, "AudioData");
    Objects.requireNonNull(clip, "Clip");
  }
}
