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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.io7m.aurantedit.filemodel.internal.Tables.CLIPS;

/**
 * Add clips.
 */

public final class AEC1ClipsAdd
  extends AECommandAbstract<AEC1ClipsAddState, List<Path>>
  implements AEC1Type<AEC1ClipsAddState, List<Path>>
{
  private final ArrayList<AEC1ClipRecorded> savedData;

  /**
   * Add clips.
   */

  public AEC1ClipsAdd()
  {
    this.savedData = new ArrayList<>();
  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AEC1ClipsAddState, List<Path>> provider()
  {
    return new AECommandFactory<>(
      AEC1ClipsAdd.class.getCanonicalName(),
      AEC1ClipsAdd::fromState
    );
  }

  private static AEC1ClipsAdd fromState(
    final AEC1ClipsAddState state)
  {
    final var c = new AEC1ClipsAdd();
    c.savedData.addAll(state.records());
    c.setExecuted(true);
    return c;
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final List<Path> parameters)
    throws AUException
  {
    final var context =
      transaction.get(DSLContext.class);

    var nextClipId =
      model.nextClipId();

    final var max = parameters.size();
    for (var index = 0; index < max; ++index) {
      final var file = parameters.get(index);
      model.setAttribute("ClipFile", file);
      model.eventProgressFormat(
        "Adding clips.",
        (double) index / (double) max,
        0.0,
        "Adding clip '%s'.",
        file
      );

      final var data =
        AEC1ClipLoading.loadAudio(model, nextClipId, file);

      AEC1ClipLoading.clipPut(context, data.declaration(), data.data());

      this.savedData.add(
        new AEC1ClipRecorded(
          data.data(),
          AE1ClipDeclaration.ofDeclaration(data.declaration())
        )
      );

      model.eventProgressFormat(
        "Adding clips.",
        (double) index / (double) max,
        1.0,
        "Adding clip '%s'.",
        file
      );
      nextClipId = nextClipId + 1L;
    }

    model.eventProgressFormat(
      "Added clips.",
      1.0,
      1.0,
      "Added %d clips.",
      Integer.valueOf(max)
    );

    model.setClips(AECommandModelUpdates.clips(context));
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

    var nextClipId =
      model.nextClipId();

    final var max = this.savedData.size();
    for (var index = 0; index < max; ++index) {
      final var data = this.savedData.get(index);
      model.eventProgressFormat(
        "Adding clips.",
        (double) index / (double) max,
        0.0,
        "Adding clip '%s'.",
        data.clip().name()
      );

      AEC1ClipLoading.clipPut(
        context,
        data.clip().toClipDeclaration(),
        data.audioData()
      );

      model.eventProgressFormat(
        "Adding clips.",
        (double) index / (double) max,
        1.0,
        "Adding clip '%s'.",
        data.clip().name()
      );
      nextClipId = nextClipId + 1L;
    }

    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgressFormat(
      "Added clips.",
      1.0,
      1.0,
      "Added %d clips.",
      Integer.valueOf(max)
    );
  }

  @Override
  protected void onUndo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var max = this.savedData.size();
    for (int index = 0; index < max; ++index) {
      final var data = this.savedData.get(index);
      model.eventProgressFormat(
        "Deleting clips.",
        (double) index / (double) max,
        0.0,
        "Deleting clip '%s'.",
        data.clip().name()
      );

      context.deleteFrom(CLIPS)
        .where(CLIPS.CLIP_ID.eq(Long.valueOf(data.clip().id().longValueExact())))
        .execute();

      model.eventProgressFormat(
        "Deleting clips.",
        (double) index / (double) max,
        1.0,
        "Deleting clip '%s'.",
        data.clip().name()
      );
    }

    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgressFormat(
      "Deleted clips.",
      1.0,
      1.0,
      "Deleted %d clips.",
      Integer.valueOf(this.savedData.size())
    );
  }

  @Override
  public AEC1ClipsAddState state()
  {
    return new AEC1ClipsAddState(List.copyOf(this.savedData));
  }

  @Override
  public String describe()
  {
    return "Add clip(s)";
  }

}
