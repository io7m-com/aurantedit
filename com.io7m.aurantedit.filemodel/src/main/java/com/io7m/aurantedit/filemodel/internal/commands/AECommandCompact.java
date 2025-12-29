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

import com.io7m.aurantedit.filemodel.internal.AEDatabaseTransactionType;
import com.io7m.aurantedit.filemodel.internal.AEFileModel;
import com.io7m.darco.api.DDatabaseException;
import com.io7m.darco.api.DDatabaseUnit;
import org.jooq.DSLContext;

import static com.io7m.aurantedit.filemodel.internal.Tables.REDO;
import static com.io7m.aurantedit.filemodel.internal.Tables.UNDO;

/**
 * Compact everything.
 */

public final class AECommandCompact
  extends AECommandAbstract<AECommandCompactState, DDatabaseUnit>
{
  /**
   * Compact everything.
   */

  public AECommandCompact()
  {

  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AECommandCompactState, DDatabaseUnit> provider()
  {
    return new AECommandFactory<>(
      AECommandCompact.class.getCanonicalName(),
      AECommandCompact::fromState
    );
  }

  private static AECommandCompact fromState(
    final AECommandCompactState p)
  {
    final var c = new AECommandCompact();
    c.setExecuted(true);
    return c;
  }

  @Override
  public boolean loading()
  {
    return false;
  }

  @Override
  public AECommandCompactState state()
  {
    return new AECommandCompactState();
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final DDatabaseUnit request)
    throws DDatabaseException
  {
    final var context =
      transaction.get(DSLContext.class);

    context.truncate(UNDO)
      .execute();
    context.truncate(REDO)
      .execute();

    transaction.commit();

    model.clearUndo();
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
    return "Compact data.";
  }
}
