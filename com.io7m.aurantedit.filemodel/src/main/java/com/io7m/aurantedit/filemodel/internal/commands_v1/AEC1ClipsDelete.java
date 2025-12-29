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
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUException;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.io7m.aurantedit.filemodel.internal.Tables.CLIPS;

/**
 * Delete clips.
 */

public final class AEC1ClipsDelete
  extends AECommandAbstract<AEC1ClipsDeleteState, List<AUClipID>>
  implements AEC1Type<AEC1ClipsDeleteState, List<AUClipID>>
{
  private final ArrayList<AEC1ClipRecorded> savedData;

  /**
   * Delete clips.
   */

  public AEC1ClipsDelete()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AEC1ClipsDeleteState, List<AUClipID>> provider()
  {
    return new AECommandFactory<>(
      AEC1ClipsDelete.class.getCanonicalName(),
      AEC1ClipsDelete::fromState
    );
  }

  private static AEC1ClipsDelete fromState(
    final AEC1ClipsDeleteState state)
  {
    final var c = new AEC1ClipsDelete();
    c.savedData.addAll(state.records());
    c.setExecuted(true);
    return c;
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final List<AUClipID> parameters)
    throws AUException
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventProgressFormat(
      "Deleting clips.",
      0.0,
      0.0,
      "Deleting clips."
    );

    final var clipIdSet =
      Set.copyOf(parameters);

    final var clipsToDelete =
      model.clips()
        .get()
        .stream()
        .filter(c -> clipIdSet.contains(c.id()))
        .collect(Collectors.toUnmodifiableSet());

    var index = 0;
    for (final var clip : clipsToDelete) {
      model.eventProgressFormat(
        "Deleting clips.",
        (double) index / (double) clipsToDelete.size(),
        0.0,
        "Deleting clip '%s'.",
        clip.name()
      );

      final var data =
        context.select(CLIPS.CLIP_BLOB)
          .from(CLIPS)
          .where(CLIPS.CLIP_ID.eq(clip.id().value()))
          .fetchOne(CLIPS.CLIP_BLOB);

      this.savedData.add(
        new AEC1ClipRecorded(
          data,
          AE1ClipDeclaration.ofDeclaration(clip)
        )
      );

      context.deleteFrom(CLIPS)
        .where(CLIPS.CLIP_ID.eq(clip.id().value()))
        .execute();

      model.eventProgressFormat(
        "Deleting clips.",
        (double) index / (double) clipsToDelete.size(),
        1.0,
        "Deleting clip '%s'.",
        clip.name()
      );
      ++index;
    }

    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgressFormat(
      "Deleting clips.",
      1.0,
      1.0,
      "Deleted clips."
    );
    return AECommandUndoable.COMMAND_UNDOABLE;
  }

  @Override
  protected void onRedo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
    throws AUException
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventProgressFormat(
      "Deleting clips.",
      0.0,
      0.0,
      "Deleting clips."
    );

    var index = 0;
    for (final var data : this.savedData) {
      final var clip = data.clip();
      model.eventProgressFormat(
        "Deleting clips.",
        (double) index / (double) savedData.size(),
        0.0,
        "Deleting clip '%s'.",
        clip.name()
      );

      context.deleteFrom(CLIPS)
        .where(CLIPS.CLIP_ID.eq(clip.id().longValueExact()))
        .execute();

      model.eventProgressFormat(
        "Deleting clips.",
        (double) index / (double) savedData.size(),
        1.0,
        "Deleting clip '%s'.",
        clip.name()
      );
      ++index;
    }

    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgressFormat(
      "Deleting clips.",
      1.0,
      1.0,
      "Deleted clips."
    );
  }

  @Override
  protected void onUndo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventProgressFormat(
      "Adding clips.",
      0.0,
      0.0,
      "Adding clips."
    );

    var index = 0;
    for (final var data : this.savedData) {
      final var clip = data.clip();
      model.eventProgressFormat(
        "Adding clips.",
        (double) index / (double) this.savedData.size(),
        0.0,
        "Adding clip '%s'.",
        clip.name()
      );

      AEC1ClipLoading.clipPut(
        context,
        clip.toClipDeclaration(),
        data.audioData()
      );

      model.eventProgressFormat(
        "Adding clips.",
        (double) index / (double) this.savedData.size(),
        1.0,
        "Adding clip '%s'.",
        clip.name()
      );
      ++index;
    }

    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgressFormat(
      "Adding clips.",
      1.0,
      1.0,
      "Adding clips."
    );
  }

  @Override
  public AEC1ClipsDeleteState state()
  {
    return new AEC1ClipsDeleteState(List.copyOf(this.savedData));
  }

  @Override
  public String describe()
  {
    return "Remove clip(s)";
  }

}
