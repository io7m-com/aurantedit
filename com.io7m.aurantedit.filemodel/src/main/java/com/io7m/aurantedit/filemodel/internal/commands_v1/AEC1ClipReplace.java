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
import com.io7m.aurantedit.filemodel.internal.json.AE1ClipDeclaration;
import com.io7m.aurantium.api.AUException;
import org.jooq.DSLContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.aurantedit.filemodel.internal.Tables.CLIPS;

/**
 * Replace clips.
 */

public final class AEC1ClipReplace
  extends AECommandAbstract<AEC1ClipReplaceState, AEC1ClipReplaceParameters>
  implements AEC1Type<AEC1ClipReplaceState, AEC1ClipReplaceParameters>
{
  private AEC1ClipReplaceState savedData;

  /**
   * Replace clips.
   */

  public AEC1ClipReplace()
  {
    this.savedData = null;
  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AEC1ClipReplaceState, AEC1ClipReplaceParameters> provider()
  {
    return new AECommandFactory<>(
      AEC1ClipReplace.class.getCanonicalName(),
      AEC1ClipReplace::fromState
    );
  }

  private static AEC1ClipReplace fromState(
    final AEC1ClipReplaceState state)
  {
    final var c = new AEC1ClipReplace();
    c.savedData = state;
    c.setExecuted(true);
    return c;
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final AEC1ClipReplaceParameters parameters)
    throws AUException
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventProgress(
      "Replacing clip.",
      0.0,
      "Replacing clip.",
      0.0
    );

    final var existingClip =
      model.clips()
        .get()
        .stream()
        .filter(c -> Objects.equals(c.id(), parameters.clipID()))
        .findFirst()
        .orElseThrow(() -> errorClipNonexistent(parameters));

    final var existingData =
      context.select(CLIPS.CLIP_BLOB)
        .from(CLIPS)
        .where(CLIPS.CLIP_ID.eq(Long.valueOf(parameters.clipID().value())))
        .fetchOne(CLIPS.CLIP_BLOB);

    final var newData =
      AEC1ClipLoading.loadAudio(
        model,
        parameters.clipID().value(),
        parameters.file()
      );

    this.savedData =
      new AEC1ClipReplaceState(
        new AEC1ClipRecorded(
          existingData,
          AE1ClipDeclaration.ofDeclaration(existingClip)
        ),
        new AEC1ClipRecorded(
          newData.data(),
          AE1ClipDeclaration.ofDeclaration(newData.declaration())
        )
      );

    AEC1ClipLoading.clipPut(context, newData.declaration(), newData.data());
    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgress(
      "Replacing clip.",
      1.0,
      "Replacing clip.",
      1.0
    );
    return AECommandUndoable.COMMAND_UNDOABLE;
  }

  private static AUException errorClipNonexistent(
    final AEC1ClipReplaceParameters parameters)
  {
    return new AUException(
      "Clip does not exist.",
      "error-clip-nonexistent",
      Map.ofEntries(
        Map.entry("Clip", parameters.clipID().toString())
      ),
      Optional.empty()
    );
  }

  @Override
  protected void onRedo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
    throws AUException
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventProgress(
      "Replacing clip.",
      0.0,
      "Replacing clip.",
      0.0
    );

    final AEC1ClipRecorded clipRecord =
      this.savedData.replacedWith();

    AEC1ClipLoading.clipPut(
      context,
      clipRecord.clip().toClipDeclaration(),
      clipRecord.audioData()
    );
    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgress(
      "Replacing clip.",
      1.0,
      "Replacing clip.",
      1.0
    );
  }

  @Override
  protected void onUndo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventProgress(
      "Replacing clip.",
      0.0,
      "Replacing clip.",
      0.0
    );

    final AEC1ClipRecorded clipRecord =
      this.savedData.replaced();

    AEC1ClipLoading.clipPut(
      context,
      clipRecord.clip().toClipDeclaration(),
      clipRecord.audioData()
    );
    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgress(
      "Replacing clip.",
      1.0,
      "Replacing clip.",
      1.0
    );
  }

  @Override
  public AEC1ClipReplaceState state()
  {
    return this.savedData;
  }

  @Override
  public String describe()
  {
    return "Replace clip(s)";
  }

}
