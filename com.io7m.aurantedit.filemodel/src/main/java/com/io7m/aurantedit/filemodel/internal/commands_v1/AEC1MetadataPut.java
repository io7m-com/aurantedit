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
import com.io7m.aurantedit.filemodel.internal.json.AE1MetadataValue;
import org.jooq.DSLContext;

import java.util.List;

/**
 * Add/update metadata.
 */

public final class AEC1MetadataPut
  extends AECommandAbstract<AEC1MetadataPutState, AEC1MetadataModificationType>
  implements AEC1Type<AEC1MetadataPutState, AEC1MetadataModificationType>
{
  private AEC1MetadataPutState savedData;

  /**
   * Add/update metadata.
   */

  public AEC1MetadataPut()
  {
    this.savedData = null;
  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AEC1MetadataPutState, AEC1MetadataModificationType> provider()
  {
    return new AECommandFactory<>(
      AEC1MetadataPut.class.getCanonicalName(),
      AEC1MetadataPut::fromState
    );
  }

  private static AEC1MetadataPut fromState(
    final AEC1MetadataPutState p)
  {
    final var c = new AEC1MetadataPut();
    c.savedData = p;
    c.setExecuted(true);
    return c;
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final AEC1MetadataModificationType m)
  {
    final var context =
      transaction.get(DSLContext.class);

    final var oldMetadata =
      List.copyOf(model.metadata().get());
    final var newMetadata =
      List.copyOf(m.modify(oldMetadata));

    AEC1Metadata.insertMetadata(context, newMetadata);

    this.savedData =
      new AEC1MetadataPutState(
        AE1MetadataValue.ofMetadataList(oldMetadata),
        AE1MetadataValue.ofMetadataList(newMetadata)
      );
    model.setMetadata(AECommandModelUpdates.metadata(context));
    return AECommandUndoable.COMMAND_UNDOABLE;
  }

  @Override
  protected void onRedo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context = transaction.get(DSLContext.class);
    AEC1Metadata.insertMetadata(
      context,
      AE1MetadataValue.toMetadataList(this.savedData.metadataAfter())
    );
    model.setMetadata(AECommandModelUpdates.metadata(context));
  }

  @Override
  protected void onUndo(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction)
  {
    final var context = transaction.get(DSLContext.class);
    AEC1Metadata.insertMetadata(
      context,
      AE1MetadataValue.toMetadataList(this.savedData.metadataBefore())
    );
    model.setMetadata(AECommandModelUpdates.metadata(context));
  }

  @Override
  public AEC1MetadataPutState state()
  {
    return this.savedData;
  }

  @Override
  public String describe()
  {
    return "Update metadata";
  }
}
