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

package com.io7m.aurantedit.filemodel.internal;

import com.io7m.aurantedit.filemodel.AEFileModelStatusType;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUMetadataValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * File model view functions.
 */

public interface AEFileModelViewType
{
  /**
   * @return The current attributes
   */

  Map<String, String> attributes();

  /**
   * Set the file model status.
   *
   * @param newStatus The new status
   */

  void setStatus(
    AEFileModelStatusType newStatus);

  /**
   * Set the new identifier.
   *
   * @param identifier The identifier
   */

  void setIdentifier(
    Optional<AUIdentifier> identifier);

  /**
   * Set the new metadata.
   *
   * @param metadata The metadata
   */

  void setMetadata(
    List<AUMetadataValue> metadata);

  /**
   * Set the new clips.
   *
   * @param clips The clips
   */

  void setClips(
    List<AUClipDeclaration> clips);

  /**
   * Set an error attribute.
   *
   * @param name  The name
   * @param value The value
   */

  void setAttribute(
    String name,
    Object value);

  /**
   * Set the key assignments.
   *
   * @param keyAssignments The key assignments
   */

  void setKeyAssignments(
    List<AUKeyAssignment> keyAssignments);
}
