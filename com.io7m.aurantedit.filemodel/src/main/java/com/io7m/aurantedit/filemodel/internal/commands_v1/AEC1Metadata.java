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

import com.io7m.aurantedit.filemodel.internal.tables.Metadata;
import com.io7m.aurantium.api.AUMetadataValue;
import org.jooq.DSLContext;
import org.jooq.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Functions over metadata.
 */

public final class AEC1Metadata
{
  private AEC1Metadata()
  {

  }

  /**
   * Insert metadata into the database.
   *
   * @param context The context
   * @param m       The metadata
   */

  public static void insertMetadata(
    final DSLContext context,
    final List<AUMetadataValue> m)
  {
    context.deleteFrom(Metadata.METADATA)
      .execute();

    final var inserts =
      new ArrayList<Query>(m.size());

    for (final var entry : m) {
      inserts.add(
        context.insertInto(Metadata.METADATA)
          .set(Metadata.METADATA.METADATA_KEY, entry.name())
          .set(Metadata.METADATA.METADATA_VALUE, entry.value())
      );
    }

    context.batch(inserts).execute();
  }

  /**
   * Add a metadata value.
   *
   * @param metadata      The metadata
   * @param metadataValue The value
   *
   * @return The new list
   */

  public static List<AUMetadataValue> metadataAdd(
    final List<AUMetadataValue> metadata,
    final AUMetadataValue metadataValue)
  {
    return Stream.concat(
        metadata.stream(),
        Stream.of(metadataValue)
      )
      .sorted()
      .toList();
  }

  /**
   * Remove the first matching metadata value.
   *
   * @param metadata      The metadata
   * @param metadataValue The value
   *
   * @return The new list
   */

  public static List<AUMetadataValue> metadataRemove(
    final List<AUMetadataValue> metadata,
    final AUMetadataValue metadataValue)
  {
    /*
     * A stateful filter that removes at most one metadata value.
     */
    final var found = new AtomicBoolean(false);
    return metadata.stream()
      .filter(v -> {
        if (Objects.equals(v, metadataValue)) {
          return !found.compareAndSet(false, true);
        }
        return true;
      })
      .sorted()
      .toList();
  }

  /**
   * Replace the first matching metadata value.
   *
   * @param metadata            The metadata
   * @param metadataReplace     The value to replace
   * @param metadataReplaceWith The replacement value
   *
   * @return The new list
   */

  public static List<AUMetadataValue> metadataReplace(
    final List<AUMetadataValue> metadata,
    final AUMetadataValue metadataReplace,
    final AUMetadataValue metadataReplaceWith)
  {
    /*
     * A stateful filter that replaces at most one metadata value.
     */
    final var found = new AtomicBoolean(false);
    return metadata.stream()
      .map(v -> {
        if (Objects.equals(v, metadataReplace)) {
          if (found.compareAndSet(false, true)) {
            return metadataReplaceWith;
          }
        }
        return v;
      })
      .sorted()
      .toList();
  }
}
