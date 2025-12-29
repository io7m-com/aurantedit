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
import com.io7m.aurantedit.filemodel.internal.json.AE1Identifier;
import com.io7m.aurantium.api.AUIdentifier;
import org.jooq.DSLContext;

import java.util.Optional;

import static com.io7m.aurantedit.filemodel.internal.tables.Identifier.IDENTIFIER;

/**
 * Add/update identifier.
 */

public final class AEC1IdentifierPut
  extends AECommandAbstract<AEC1IdentifierPutState, AUIdentifier>
  implements AEC1Type<AEC1IdentifierPutState, AUIdentifier>
{
  private AEC1IdentifierPutState savedData;

  /**
   * Add/update identifier.
   */

  public AEC1IdentifierPut()
  {
    this.savedData = null;
  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AEC1IdentifierPutState, AUIdentifier> provider()
  {
    return new AECommandFactory<>(
      AEC1IdentifierPut.class.getCanonicalName(),
      AEC1IdentifierPut::fromState
    );
  }

  private static AEC1IdentifierPut fromState(
    final AEC1IdentifierPutState p)
  {
    final var c = new AEC1IdentifierPut();
    c.savedData = p;
    c.setExecuted(true);
    return c;
  }

  private static AE1Identifier identifierOf(
    final AUIdentifier i)
  {
    if (i == null) {
      return null;
    }

    return new AE1Identifier(
      i.group(),
      i.name(),
      i.version().major(),
      i.version().minor()
    );
  }

  /**
   * Insert an identifier into the database.
   *
   * @param context    The context
   * @param identifier The identifier
   */

  public static void insertIdentifier(
    final DSLContext context,
    final AUIdentifier identifier)
  {
    context.deleteFrom(IDENTIFIER).execute();

    if (identifier == null) {
      return;
    }

    context.insertInto(IDENTIFIER)
      .set(IDENTIFIER.ID_GROUP, identifier.group().value())
      .set(IDENTIFIER.ID_NAME, identifier.name().value())
      .set(IDENTIFIER.ID_VERSION_MAJOR, (long) identifier.version().major())
      .set(IDENTIFIER.ID_VERSION_MINOR, (long) identifier.version().minor())
      .execute();
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final AUIdentifier m)
  {
    final var context =
      transaction.get(DSLContext.class);

    insertIdentifier(context, m);

    this.savedData = new AEC1IdentifierPutState(
      identifierOf(model.identifier().get().orElse(null)),
      identifierOf(m)
    );

    model.setIdentifier(AECommandModelUpdates.identifier(context));
    return AECommandUndoable.COMMAND_UNDOABLE;
  }

  @Override
  protected void onRedo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context = transaction.get(DSLContext.class);

    final var id =
      Optional.ofNullable(this.savedData.identifierAfter())
        .map(AE1Identifier::toIdentifier)
        .orElse(null);

    insertIdentifier(context, id);
    model.setIdentifier(AECommandModelUpdates.identifier(context));
  }

  @Override
  protected void onUndo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context = transaction.get(DSLContext.class);

    final var id =
      Optional.ofNullable(this.savedData.identifierBefore())
        .map(AE1Identifier::toIdentifier)
        .orElse(null);

    insertIdentifier(context, id);
    model.setIdentifier(AECommandModelUpdates.identifier(context));
  }

  @Override
  public AEC1IdentifierPutState state()
  {
    return this.savedData;
  }

  @Override
  public String describe()
  {
    return "Update metadata";
  }
}
