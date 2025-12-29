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

import com.io7m.aurantedit.filemodel.AEFileModelStatusLoading;
import com.io7m.aurantedit.filemodel.internal.AEDatabaseTransactionType;
import com.io7m.aurantedit.filemodel.internal.AEFileModel;
import com.io7m.darco.api.DDatabaseUnit;
import org.jooq.DSLContext;

/**
 * Load everything.
 */

public final class AECommandLoad
  extends AECommandAbstract<AECommandLoadState, DDatabaseUnit>
{
  /**
   * Load everything.
   */

  public AECommandLoad()
  {

  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AECommandLoadState, DDatabaseUnit> provider()
  {
    return new AECommandFactory<>(
      AECommandLoad.class.getCanonicalName(),
      AECommandLoad::fromState
    );
  }

  private static AECommandLoad fromState(
    final AECommandLoadState p)
  {
    final var c = new AECommandLoad();
    c.setExecuted(true);
    return c;
  }

  @Override
  public boolean loading()
  {
    return true;
  }

  @Override
  public AECommandLoadState state()
  {
    return new AECommandLoadState();
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final DDatabaseUnit request)
  {
    model.setStatus(new AEFileModelStatusLoading());

    final var tasks = 7.0;

    final var context = transaction.get(DSLContext.class);
    model.eventProgress(
      "Loading file.",
      1.0 / tasks,
      "Loading identifier…",
      0.0);
    model.setIdentifier(AECommandModelUpdates.identifier(context));
    model.eventProgress(
      "Loading file.",
      1.0 / tasks,
      "Loading identifier…",
      1.0);

    model.eventProgress("Loading file.", 2.0 / tasks, "Loading metadata…", 0.0);
    model.setMetadata(AECommandModelUpdates.metadata(context));
    model.eventProgress("Loading file.", 2.0 / tasks, "Loading metadata…", 1.0);

    model.eventProgress("Loading file.", 3.0 / tasks, "Loading clips…", 0.0);
    model.setClips(AECommandModelUpdates.clips(context));
    model.eventProgress("Loading file.", 3.0 / tasks, "Loading clips…", 1.0);

    model.eventProgress(
      "Loading file.",
      4.0 / tasks,
      "Loading key assignments…",
      0.0);
    model.setKeyAssignments(AECommandModelUpdates.keyAssignments(context));
    model.eventProgress(
      "Loading file.",
      4.0 / tasks,
      "Loading key assignments…",
      1.0);

    model.eventProgress(
      "Loading file.",
      5.0 / tasks,
      "Loading undo stack…",
      0.0);
    model.loadUndo(transaction);
    model.eventProgress(
      "Loading file.",
      5.0 / tasks,
      "Loading undo stack…",
      1.0);

    model.eventProgress(
      "Loading file.",
      6.0 / tasks,
      "Loading redo stack…",
      0.0);
    model.loadRedo(transaction);
    model.eventProgress(
      "Loading file.",
      6.0 / tasks,
      "Loading redo stack…",
      1.0);

    model.eventProgress("Loading file.", 7.0 / tasks, "Loaded file.", 1.0);
    return AECommandUndoable.COMMAND_NOT_UNDOABLE;
  }

  @Override
  protected void onUndo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void onRedo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String describe()
  {
    return "Load data.";
  }
}
