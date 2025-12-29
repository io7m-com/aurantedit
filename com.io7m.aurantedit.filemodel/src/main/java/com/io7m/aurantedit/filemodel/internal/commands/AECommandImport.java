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

import com.io7m.aurantedit.filemodel.AECommandImportParameters;
import com.io7m.aurantedit.filemodel.internal.AECloseables;
import com.io7m.aurantedit.filemodel.internal.AEDatabaseTransactionType;
import com.io7m.aurantedit.filemodel.internal.AEExceptions;
import com.io7m.aurantedit.filemodel.internal.AEFileModel;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1IdentifierPut;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipDescription;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUSectionReadableClipDataType;
import com.io7m.aurantium.api.AUSectionReadableClipDefinitionsType;
import com.io7m.aurantium.api.AUSectionReadableKeyAssignmentsType;
import com.io7m.aurantium.api.AUSectionReadableMetadataType;
import com.io7m.aurantium.parser.api.AUParseRequest;
import com.io7m.aurantium.parser.api.AUParsers;
import org.jooq.DSLContext;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipLoading.clipPut;
import static com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1KeyAssignmentPut.keyAssignmentInsert;
import static com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1Metadata.insertMetadata;

/**
 * Import to a file.
 */

public final class AECommandImport
  extends AECommandAbstract<AECommandImportState, AECommandImportParameters>
{
  private static final double SUBTASK_COUNT = 4.0;
  private final AUParsers parsers;

  /**
   * Import to a file.
   */

  public AECommandImport()
  {
    this.parsers = new AUParsers();
  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AECommandImportState, AECommandImportParameters> provider()
  {
    return new AECommandFactory<>(
      AECommandImport.class.getCanonicalName(),
      AECommandImport::fromState
    );
  }

  private static AECommandImport fromState(
    final AECommandImportState p)
  {
    final var c = new AECommandImport();
    c.setExecuted(true);
    return c;
  }

  private static void copyMetadata(
    final AEFileModel model,
    final Optional<AUSectionReadableMetadataType> metadataOpt,
    final DSLContext context)
    throws IOException
  {
    model.eventProgress(
      "Importing file...",
      4.0 / SUBTASK_COUNT,
      "Copying metadata.",
      0.0
    );

    if (metadataOpt.isPresent()) {
      final var metadataSection =
        metadataOpt.get();
      final var metadata =
        metadataSection.metadata();

      insertMetadata(context, metadata);
      model.setMetadata(metadata);
    }

    model.eventProgress(
      "Importing file...",
      4.0 / SUBTASK_COUNT,
      "Copying metadata.",
      1.0
    );
  }

  private static void copyKeyAssignments(
    final AEFileModel model,
    final AUSectionReadableKeyAssignmentsType keyAssignments,
    final DSLContext context)
    throws IOException
  {
    model.eventProgress(
      "Importing file...",
      3.0 / SUBTASK_COUNT,
      "Copying key assignments.",
      0.0
    );

    final var keyAssignmentList =
      keyAssignments.keyAssignments().assignments();

    for (int index = 0; index < keyAssignmentList.size(); ++index) {
      final var k = keyAssignmentList.get(index);
      model.eventProgress(
        "Importing file...",
        3.0 / SUBTASK_COUNT,
        "Copying key assignments.",
        (double) index / (double) keyAssignmentList.size()
      );
      keyAssignmentInsert(context, k);
      model.eventProgress(
        "Importing file...",
        3.0 / SUBTASK_COUNT,
        "Copying key assignments.",
        (double) index + 1.0 / (double) keyAssignmentList.size()
      );
    }

    model.setKeyAssignments(keyAssignmentList);
    model.eventProgress(
      "Importing file...",
      3.0 / SUBTASK_COUNT,
      "Copying key assignments.",
      1.0
    );
  }

  private static void copyClips(
    final AEFileModel model,
    final AUSectionReadableClipDefinitionsType clipDefs,
    final AUSectionReadableClipDataType clipData,
    final DSLContext context)
    throws IOException
  {
    model.eventProgress(
      "Importing file...",
      2.0 / SUBTASK_COUNT,
      "Copying clips.",
      0.0
    );

    final var clipDeclarations =
      new ArrayList<AUClipDeclaration>();
    final List<AUClipDescription> clipSource =
      clipDefs.clips();

    for (int index = 0; index < clipSource.size(); ++index) {
      final var clip = clipSource.get(index);
      model.eventProgress(
        "Importing file...",
        2.0 / SUBTASK_COUNT,
        "Copying clips.",
        (double) index / (double) clipSource.size()
      );

      try (var dataChannel = clipData.audioDataForClip(clip)) {
        final var dataStream =
          Channels.newInputStream(dataChannel);
        final AUClipDeclaration clipDeclaration =
          new AUClipDeclaration(
            clip.id(),
            clip.name(),
            clip.format(),
            clip.sampleRate(),
            clip.sampleDepth(),
            clip.channels(),
            clip.endianness(),
            clip.hash(),
            clip.size(),
            clip.loopRange()
          );

        clipPut(context, clipDeclaration, dataStream.readAllBytes());
        clipDeclarations.add(clipDeclaration);
      }

      model.eventProgress(
        "Importing file...",
        2.0 / SUBTASK_COUNT,
        "Copying clips.",
        (index + 1.0) / (double) clipSource.size()
      );
    }
    model.setClips(clipDeclarations);

    model.eventProgress(
      "Importing file...",
      2.0 / SUBTASK_COUNT,
      "Copying clips.",
      1.0
    );
  }

  private static void copyIdentifier(
    final AEFileModel model,
    final DSLContext context,
    final AUIdentifier id)
  {
    model.eventProgress(
      "Importing file...",
      1.0 / SUBTASK_COUNT,
      "Copying identifier.",
      0.0
    );

    AEC1IdentifierPut.insertIdentifier(context, id);
    model.setIdentifier(Optional.of(id));

    model.eventProgress(
      "Importing file...",
      1.0 / SUBTASK_COUNT,
      "Copying identifier.",
      1.0
    );
  }

  @Override
  public AECommandImportState state()
  {
    return new AECommandImportState();
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final AECommandImportParameters request)
    throws AUException
  {
    final var context =
      transaction.get(DSLContext.class);

    model.eventProgress(
      "Importing file...",
      0.0,
      "",
      0.0
    );

    try (var resources = AECloseables.create()) {
      final var sampleMap =
        request.sampleMap();
      final var sampleMapChannel =
        resources.add(FileChannel.open(sampleMap));

      final var parseRequest =
        new AUParseRequest(
          sampleMapChannel,
          sampleMap.toUri()
        );

      final var parser =
        resources.add(this.parsers.createParser(parseRequest));
      final var fileReadable =
        resources.add(parser.execute());

      final var id =
        fileReadable.openIdentifier()
          .orElseThrow()
          .identifier();
      final var clipData =
        fileReadable.openClipData()
          .orElseThrow();
      final var clipDefs =
        fileReadable.openClipDefinitions()
          .orElseThrow();
      final var keyAssignments =
        fileReadable.openKeyAssignments()
          .orElseThrow();
      final var metadataOpt =
        fileReadable.openMetadata();

      copyIdentifier(model, context, id);
      copyClips(model, clipDefs, clipData, context);
      copyKeyAssignments(model, keyAssignments, context);
      copyMetadata(model, metadataOpt, context);
    } catch (final Throwable e) {
      throw AEExceptions.wrap(e);
    }

    model.eventProgress(
      "Importing file...",
      1.0,
      "Imported file successfully.",
      1.0
    );
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
    return "Import data.";
  }
}
