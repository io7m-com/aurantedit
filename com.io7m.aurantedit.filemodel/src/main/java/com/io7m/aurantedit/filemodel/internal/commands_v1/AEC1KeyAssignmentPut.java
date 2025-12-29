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

import com.io7m.aurantedit.filemodel.internal.AEDatabaseTransactionType;
import com.io7m.aurantedit.filemodel.internal.AEFileModel;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandAbstract;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandFactory;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandFactoryType;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandModelUpdates;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandUndoable;
import com.io7m.aurantedit.filemodel.internal.json.AE1KeyAssignment;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUKeyAssignmentFlagType;
import org.jooq.DSLContext;

import java.math.BigInteger;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.io7m.aurantedit.filemodel.internal.Tables.KEY_ASSIGNMENTS;

/**
 * Add/update key assignments.
 */

public final class AEC1KeyAssignmentPut
  extends AECommandAbstract<AEC1KeyAssignmentPutState, AUKeyAssignment>
  implements AEC1Type<AEC1KeyAssignmentPutState, AUKeyAssignment>
{
  private AEC1KeyAssignmentPutState savedData;

  /**
   * Add/update key assignments.
   */

  public AEC1KeyAssignmentPut()
  {
    this.savedData = null;
  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AEC1KeyAssignmentPutState, AUKeyAssignment> provider()
  {
    return new AECommandFactory<>(
      AEC1KeyAssignmentPut.class.getCanonicalName(),
      AEC1KeyAssignmentPut::fromState
    );
  }

  private static AEC1KeyAssignmentPut fromState(
    final AEC1KeyAssignmentPutState p)
  {
    final var c = new AEC1KeyAssignmentPut();
    c.savedData = p;
    c.setExecuted(true);
    return c;
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final AUKeyAssignment k)
    throws AUException
  {
    final var context =
      transaction.get(DSLContext.class);

    final var existing =
      model.keyAssignmentMap()
        .get()
        .get(k.id());

    keyAssignmentInsert(context, k);

    this.savedData =
      new AEC1KeyAssignmentPutState(
        ofKeyAssignment(existing),
        ofKeyAssignment(k)
      );

    model.setKeyAssignments(AECommandModelUpdates.keyAssignments(context));
    return AECommandUndoable.COMMAND_UNDOABLE;
  }

  private static void keyAssignmentDelete(
    final DSLContext context,
    final AUKeyAssignment keyAssignment)
  {
    final var kt = KEY_ASSIGNMENTS;
    context.deleteFrom(kt)
      .where(kt.KA_ID.eq(keyAssignment.id().value()))
      .execute();
  }

  /**
   * Inset a key assignment into the database.
   *
   * @param context The context
   * @param k       The key assignment
   */

  public static void keyAssignmentInsert(
    final DSLContext context,
    final AUKeyAssignment k)
  {
    final var kt = KEY_ASSIGNMENTS;
    context.insertInto(kt)
      .set(kt.KA_ID, k.id().value())
      .set(kt.KA_CLIP_ID, k.clipId().value())
      .set(kt.KA_AMPLITUDE_AT_KEY_CENTER, (float) k.amplitudeAtKeyCenter())
      .set(kt.KA_AMPLITUDE_AT_KEY_END, (float) k.amplitudeAtKeyEnd())
      .set(kt.KA_AMPLITUDE_AT_KEY_START, (float) k.amplitudeAtKeyStart())
      .set(
        kt.KA_AMPLITUDE_AT_VELOCITY_CENTER,
        (float) k.amplitudeAtVelocityCenter())
      .set(kt.KA_AMPLITUDE_AT_VELOCITY_END, (float) k.amplitudeAtVelocityEnd())
      .set(
        kt.KA_AMPLITUDE_AT_VELOCITY_START,
        (float) k.amplitudeAtVelocityStart())
      .set(kt.KA_AT_VELOCITY_CENTER, (float) k.atVelocityCenter())
      .set(kt.KA_AT_VELOCITY_END, (float) k.atVelocityEnd())
      .set(kt.KA_AT_VELOCITY_START, (float) k.atVelocityStart())
      .set(kt.KA_VALUE_CENTER, k.keyValueCenter())
      .set(kt.KA_VALUE_END, k.keyValueEnd())
      .set(kt.KA_VALUE_START, k.keyValueStart())
      .set(kt.KA_FLAGS, flagsOf(k.flags()))
      .onConflict(kt.KA_ID)
      .doUpdate()
      .set(kt.KA_CLIP_ID, k.clipId().value())
      .set(kt.KA_AMPLITUDE_AT_KEY_CENTER, (float) k.amplitudeAtKeyCenter())
      .set(kt.KA_AMPLITUDE_AT_KEY_END, (float) k.amplitudeAtKeyEnd())
      .set(kt.KA_AMPLITUDE_AT_KEY_START, (float) k.amplitudeAtKeyStart())
      .set(
        kt.KA_AMPLITUDE_AT_VELOCITY_CENTER,
        (float) k.amplitudeAtVelocityCenter())
      .set(kt.KA_AMPLITUDE_AT_VELOCITY_END, (float) k.amplitudeAtVelocityEnd())
      .set(
        kt.KA_AMPLITUDE_AT_VELOCITY_START,
        (float) k.amplitudeAtVelocityStart())
      .set(kt.KA_AT_VELOCITY_CENTER, (float) k.atVelocityCenter())
      .set(kt.KA_AT_VELOCITY_END, (float) k.atVelocityEnd())
      .set(kt.KA_AT_VELOCITY_START, (float) k.atVelocityStart())
      .set(kt.KA_VALUE_CENTER, k.keyValueCenter())
      .set(kt.KA_VALUE_END, k.keyValueEnd())
      .set(kt.KA_VALUE_START, k.keyValueStart())
      .set(kt.KA_FLAGS, flagsOf(k.flags()))
      .execute();
  }

  private static String flagsOf(
    final Set<AUKeyAssignmentFlagType> flags)
  {
    return flags.stream()
      .map(f -> f.descriptor().value())
      .sorted()
      .collect(Collectors.joining(","));
  }

  @Override
  protected void onRedo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var existing =
      this.savedData;
    final var updatedAssignment =
      existing.keyAssignmentAdded();

    keyAssignmentInsert(context, updatedAssignment.toKeyAssignment());
    model.setKeyAssignments(AECommandModelUpdates.keyAssignments(context));
  }

  @Override
  protected void onUndo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var existing =
      this.savedData;
    final var existingAssignment =
      existing.keyAssignmentExisting();
    final var updatedAssignment =
      existing.keyAssignmentAdded();

    if (existingAssignment != null) {
      keyAssignmentInsert(context, existingAssignment.toKeyAssignment());
    } else {
      keyAssignmentDelete(context, updatedAssignment.toKeyAssignment());
    }

    model.setKeyAssignments(AECommandModelUpdates.keyAssignments(context));
  }

  @Override
  public AEC1KeyAssignmentPutState state()
  {
    return this.savedData;
  }

  private static AE1KeyAssignment ofKeyAssignment(
    final AUKeyAssignment k)
  {
    if (k == null) {
      return null;
    }

    return new AE1KeyAssignment(
      bigInteger(k.id().value()),
      bigInteger(k.keyValueStart()),
      bigInteger(k.keyValueCenter()),
      bigInteger(k.keyValueEnd()),
      bigInteger(k.clipId().value()),
      k.amplitudeAtKeyStart(),
      k.amplitudeAtKeyCenter(),
      k.amplitudeAtKeyEnd(),
      k.atVelocityStart(),
      k.atVelocityCenter(),
      k.atVelocityEnd(),
      k.amplitudeAtVelocityStart(),
      k.amplitudeAtVelocityCenter(),
      k.amplitudeAtVelocityEnd(),
      k.flags()
        .stream()
        .map(x -> x.descriptor().value())
        .collect(Collectors.toCollection(TreeSet::new))
    );
  }

  private static BigInteger bigInteger(
    final long value)
  {
    return new BigInteger(Long.toUnsignedString(value));
  }

  @Override
  public String describe()
  {
    return "Update key assignment";
  }
}
