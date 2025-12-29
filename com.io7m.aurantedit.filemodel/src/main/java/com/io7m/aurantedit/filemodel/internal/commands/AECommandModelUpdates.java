/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.aurantium.api.AUAudioFormatType;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUClipLoopRange;
import com.io7m.aurantium.api.AUHashAlgorithm;
import com.io7m.aurantium.api.AUHashValue;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUKeyAssignmentFlagType;
import com.io7m.aurantium.api.AUKeyAssignmentID;
import com.io7m.aurantium.api.AUMetadataValue;
import com.io7m.aurantium.api.AUOctetOrder;
import com.io7m.aurantium.api.AUVersion;
import com.io7m.lanark.core.RDottedName;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.io7m.aurantedit.filemodel.internal.Tables.CLIPS;
import static com.io7m.aurantedit.filemodel.internal.Tables.IDENTIFIER;
import static com.io7m.aurantedit.filemodel.internal.Tables.KEY_ASSIGNMENTS;
import static com.io7m.aurantedit.filemodel.internal.Tables.METADATA;
import static com.io7m.aurantium.api.AUAudioFormatType.AUAudioFormatStandard;
import static com.io7m.aurantium.api.AUAudioFormatType.AUAudioFormatUnknown;
import static com.io7m.aurantium.api.AUKeyAssignmentFlagType.AUKeyAssignmentFlagStandard;
import static com.io7m.aurantium.api.AUKeyAssignmentFlagType.AUKeyAssignmentFlagUnknown;

/**
 * Functions to perform model updates on database changes.
 */

public final class AECommandModelUpdates
{
  private static final Field<Integer> SIZE_FIELD =
    DSL.field(DSL.name("SIZE"), Integer.TYPE);

  private AECommandModelUpdates()
  {

  }

  /**
   * Get identifier.
   *
   * @param context The context
   *
   * @return The current identifier
   */

  public static Optional<AUIdentifier> identifier(
    final DSLContext context)
  {
    return context.select(
        IDENTIFIER.ID_GROUP,
        IDENTIFIER.ID_NAME,
        IDENTIFIER.ID_VERSION_MAJOR,
        IDENTIFIER.ID_VERSION_MINOR
      )
      .from(IDENTIFIER)
      .limit(1)
      .fetchOptional(AECommandModelUpdates::mapIdentifier);
  }

  private static AUIdentifier mapIdentifier(
    final org.jooq.Record r)
  {
    return new AUIdentifier(
      new RDottedName(r.get(IDENTIFIER.ID_GROUP)),
      new RDottedName(r.get(IDENTIFIER.ID_NAME)),
      new AUVersion(
        r.get(IDENTIFIER.ID_VERSION_MAJOR).intValue(),
        r.get(IDENTIFIER.ID_VERSION_MINOR).intValue()
      )
    );
  }

  /**
   * Get metadata.
   *
   * @param context The context
   *
   * @return The current metadata
   */

  public static List<AUMetadataValue> metadata(
    final DSLContext context)
  {
    return context.select(
        METADATA.METADATA_KEY,
        METADATA.METADATA_VALUE
      ).from(METADATA)
      .orderBy(METADATA.METADATA_KEY, METADATA.METADATA_VALUE)
      .stream()
      .map(AECommandModelUpdates::mapMetadata)
      .toList();
  }

  private static AUMetadataValue mapMetadata(
    final org.jooq.Record r)
  {
    return new AUMetadataValue(
      r.get(METADATA.METADATA_KEY),
      r.get(METADATA.METADATA_VALUE)
    );
  }

  /**
   * Load key assignments.
   *
   * @param context The context
   *
   * @return The key assignments
   */

  public static List<AUKeyAssignment> keyAssignments(
    final DSLContext context)
  {
    return context.select(
        KEY_ASSIGNMENTS.asterisk()
      )
      .from(KEY_ASSIGNMENTS)
      .orderBy(KEY_ASSIGNMENTS.KA_ID)
      .stream()
      .map(AECommandModelUpdates::mapKeyAssignment)
      .toList();
  }

  private static AUKeyAssignment mapKeyAssignment(
    final Record r)
  {
    return new AUKeyAssignment(
      new AUKeyAssignmentID(r.get(KEY_ASSIGNMENTS.KA_ID)),
      r.get(KEY_ASSIGNMENTS.KA_VALUE_START),
      r.get(KEY_ASSIGNMENTS.KA_VALUE_CENTER),
      r.get(KEY_ASSIGNMENTS.KA_VALUE_END),
      new AUClipID(r.get(KEY_ASSIGNMENTS.KA_CLIP_ID)),
      r.get(KEY_ASSIGNMENTS.KA_AMPLITUDE_AT_KEY_START),
      r.get(KEY_ASSIGNMENTS.KA_AMPLITUDE_AT_KEY_CENTER),
      r.get(KEY_ASSIGNMENTS.KA_AMPLITUDE_AT_KEY_END),
      r.get(KEY_ASSIGNMENTS.KA_AT_VELOCITY_START),
      r.get(KEY_ASSIGNMENTS.KA_AT_VELOCITY_CENTER),
      r.get(KEY_ASSIGNMENTS.KA_AT_VELOCITY_END),
      r.get(KEY_ASSIGNMENTS.KA_AMPLITUDE_AT_VELOCITY_START),
      r.get(KEY_ASSIGNMENTS.KA_AMPLITUDE_AT_VELOCITY_CENTER),
      r.get(KEY_ASSIGNMENTS.KA_AMPLITUDE_AT_VELOCITY_END),
      keyAssignmentFlags(r.get(KEY_ASSIGNMENTS.KA_FLAGS))
    );
  }

  private static Set<AUKeyAssignmentFlagType> keyAssignmentFlags(
    final String text)
  {
    if (text.isEmpty()) {
      return Set.of();
    }

    return Stream.of(text.split(","))
      .map(AECommandModelUpdates::keyAssignmentFlag)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static AUKeyAssignmentFlagType keyAssignmentFlag(
    final String text)
  {
    for (final var v : AUKeyAssignmentFlagStandard.values()) {
      if (Objects.equals(v.descriptor().value(), text)) {
        return v;
      }
    }
    return new AUKeyAssignmentFlagUnknown(new RDottedName(text));
  }

  /**
   * Load clips.
   *
   * @param context The context
   *
   * @return The clips
   */

  public static List<AUClipDeclaration> clips(
    final DSLContext context)
  {
    return context.select(
        DSL.octetLength((Field<String>) (Object) CLIPS.CLIP_BLOB).as(SIZE_FIELD),
        CLIPS.CLIP_CHANNELS,
        CLIPS.CLIP_ENDIANNESS,
        CLIPS.CLIP_FORMAT,
        CLIPS.CLIP_HASH_ALGORITHM,
        CLIPS.CLIP_HASH_VALUE,
        CLIPS.CLIP_ID,
        CLIPS.CLIP_LOOP_RANGE_LOWER,
        CLIPS.CLIP_LOOP_RANGE_UPPER,
        CLIPS.CLIP_NAME,
        CLIPS.CLIP_SAMPLE_DEPTH,
        CLIPS.CLIP_SAMPLE_RATE
      )
      .from(CLIPS)
      .orderBy(CLIPS.CLIP_ID)
      .stream()
      .map(AECommandModelUpdates::mapClipRecord)
      .toList();
  }

  private static AUClipDeclaration mapClipRecord(
    final org.jooq.Record r)
  {
    return new AUClipDeclaration(
      new AUClipID(r.get(CLIPS.CLIP_ID).longValue()),
      r.get(CLIPS.CLIP_NAME),
      formatOf(r.get(CLIPS.CLIP_FORMAT)),
      r.get(CLIPS.CLIP_SAMPLE_RATE).longValue(),
      r.get(CLIPS.CLIP_SAMPLE_DEPTH).longValue(),
      r.get(CLIPS.CLIP_CHANNELS).longValue(),
      octetOrderOf(r.get(CLIPS.CLIP_ENDIANNESS)),
      hashValueOf(
        r.get(CLIPS.CLIP_HASH_ALGORITHM),
        r.get(CLIPS.CLIP_HASH_VALUE)
      ),
      r.get(SIZE_FIELD).longValue(),
      optionalLoopRangeOf(
        r.get(CLIPS.CLIP_LOOP_RANGE_LOWER),
        r.get(CLIPS.CLIP_LOOP_RANGE_UPPER)
      )
    );
  }

  private static Optional<AUClipLoopRange> optionalLoopRangeOf(
    final Long lower,
    final Long upper)
  {
    if (lower != null && upper != null) {
      return Optional.of(
        new AUClipLoopRange(lower.longValue(), upper.longValue())
      );
    }
    return Optional.empty();
  }

  private static AUHashValue hashValueOf(
    final String textAlgo,
    final String textValue)
  {
    return new AUHashValue(
      AUHashAlgorithm.parse(textAlgo),
      textValue
    );
  }

  private static AUOctetOrder octetOrderOf(
    final String text)
  {
    return AUOctetOrder.parse(text);
  }

  private static AUAudioFormatType formatOf(
    final String text)
  {
    for (final var std : AUAudioFormatStandard.values()) {
      if (Objects.equals(text, std.descriptor().value())) {
        return std;
      }
    }
    return new AUAudioFormatUnknown(new RDottedName(text));
  }
}
