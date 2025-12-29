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

import com.io7m.aurantedit.filemodel.AECommandExportParameters;
import com.io7m.aurantedit.filemodel.internal.AECloseables;
import com.io7m.aurantedit.filemodel.internal.AEDatabaseTransactionType;
import com.io7m.aurantedit.filemodel.internal.AEExceptions;
import com.io7m.aurantedit.filemodel.internal.AEFileModel;
import com.io7m.aurantium.api.AUClipDeclarations;
import com.io7m.aurantium.api.AUClipDescription;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUFileWritableType;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUKeyAssignments;
import com.io7m.aurantium.api.AUWritableClipsType;
import com.io7m.aurantium.writer.api.AUWriteRequest;
import com.io7m.aurantium.writer.api.AUWriterFactoryType;
import org.jooq.DSLContext;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

import static com.io7m.aurantedit.filemodel.internal.Tables.CLIPS;

/**
 * Export to a file.
 */

public final class AECommandExport
  extends AECommandAbstract<AECommandExportState, AECommandExportParameters>
{
  private static final OpenOption[] OPEN_OPTIONS = {
    StandardOpenOption.WRITE,
    StandardOpenOption.TRUNCATE_EXISTING,
    StandardOpenOption.CREATE,
  };

  /**
   * Export to a file.
   */

  public AECommandExport()
  {

  }

  /**
   * @return A command factory
   */

  public static AECommandFactoryType<AECommandExportState, AECommandExportParameters> provider()
  {
    return new AECommandFactory<>(
      AECommandExport.class.getCanonicalName(),
      AECommandExport::fromState
    );
  }

  private static AECommandExport fromState(
    final AECommandExportState p)
  {
    final var c = new AECommandExport();
    c.setExecuted(true);
    return c;
  }

  private static SortedMap<AUClipID, AUClipDescription> writeClipDataAll(
    final AEFileModel model,
    final AUFileWritableType writable,
    final DSLContext context)
    throws IOException
  {
    final SortedMap<AUClipID, AUClipDescription> clipDefinitions;
    try (var section = writable.createSectionClipData()) {
      final var clipsWritable =
        section.createClips(new AUClipDeclarations(model.clips().get()));
      clipDefinitions =
        clipsWritable.clipDescriptions();

      model.eventProgress(
        "Exporting file.",
        0.5,
        "Writing clip data.",
        0.0
      );

      int index = 0;
      final var entries = clipDefinitions.entrySet();
      for (final var entry : entries) {
        model.eventProgress(
          "Exporting file.",
          0.5,
          "Writing clip data.",
          (double) index / (double) entries.size()
        );
        writeClipData(context, clipsWritable, entry.getValue());
        ++index;
      }
    }
    return clipDefinitions;
  }

  private static void writeIdentifier(
    final AEFileModel model,
    final AUFileWritableType writable,
    final AUIdentifier identifier)
    throws IOException
  {
    try (var section = writable.createSectionIdentifier()) {
      model.eventProgress(
        "Exporting file.",
        0.25,
        "Writing identifier.",
        0.0
      );
      section.setIdentifier(identifier);
      model.eventProgress(
        "Exporting file.",
        0.25,
        "Writing identifier.",
        1.0
      );
    }
  }

  private static void writeClipData(
    final DSLContext context,
    final AUWritableClipsType clipsWritable,
    final AUClipDescription value)
    throws IOException
  {
    try (var channel = clipsWritable.writeAudioDataForClip(value.id())) {
      try (var output = Channels.newOutputStream(channel)) {
        output.write(
          context.select(CLIPS.CLIP_BLOB)
            .from(CLIPS)
            .where(CLIPS.CLIP_ID.eq(value.id().value()))
            .fetchOne(CLIPS.CLIP_BLOB)
        );
        output.flush();
      }
    }
  }

  private static AUException missingRequiredData(
    final String type)
  {
    return new AUException(
      "Missing data required for export.",
      "error-missing-data",
      Map.ofEntries(
        Map.entry("Data", type)
      ),
      Optional.empty()
    );
  }

  private static AUWriterFactoryType findWriters(
    final AECommandExportParameters request)
    throws AUException
  {
    final var version = request.formatVersion();
    for (final var factory : request.writers()) {
      if (factory.supportedMajorVersion() == version.major()) {
        if (factory.highestMinorVersion() >= version.minor()) {
          return factory;
        }
      }
    }

    throw new AUException(
      "None of the provided writers supports the requested format version.",
      "error-format-unsupported",
      Map.ofEntries(
        Map.entry("Version", version.toString())
      ),
      Optional.empty()
    );
  }

  private static void writeEnd(
    final AUFileWritableType writable)
    throws IOException
  {
    try (var _ = writable.createSectionEnd()) {
      // Nothing required.
    }
  }

  private static void writeMetadata(
    final AEFileModel model,
    final AUFileWritableType writable)
    throws IOException
  {
    try (var section = writable.createSectionMetadata()) {
      model.eventProgress(
        "Exporting file.",
        0.8,
        "Writing metadata.",
        0.0
      );
      section.setMetadata(model.metadata().get());
      model.eventProgress(
        "Exporting file.",
        0.8,
        "Writing metadata.",
        1.0
      );
    }
  }

  private static void writeKeyAssignments(
    final AEFileModel model,
    final AUFileWritableType writable)
    throws IOException
  {
    try (var section = writable.createSectionKeyAssignments()) {
      model.eventProgress(
        "Exporting file.",
        0.7,
        "Writing key assignments.",
        0.0
      );
      section.setKeyAssignments(
        new AUKeyAssignments(model.keyAssignments().get())
      );
      model.eventProgress(
        "Exporting file.",
        0.7,
        "Writing key assignments.",
        1.0
      );
    }
  }

  private static void writeClipDefinitions(
    final AEFileModel model,
    final AUFileWritableType writable,
    final SortedMap<AUClipID, AUClipDescription> clipDefinitions)
    throws IOException
  {
    try (var section = writable.createSectionClipDefinitions()) {
      model.eventProgress(
        "Exporting file.",
        0.6,
        "Writing clip definitions.",
        0.0
      );
      section.writeClipDescriptions(clipDefinitions);
      model.eventProgress(
        "Exporting file.",
        0.6,
        "Writing clip definitions.",
        1.0
      );
    }
  }

  @Override
  public AECommandExportState state()
  {
    return new AECommandExportState();
  }

  @Override
  protected AECommandUndoable onExecute(
    final AEFileModel model,
    final AEDatabaseTransactionType transaction,
    final AECommandExportParameters request)
    throws AUException
  {
    final var context =
      transaction.get(DSLContext.class);

    final var identifier =
      model.identifier()
        .get()
        .orElseThrow(() -> missingRequiredData("Identifier"));

    final var writers =
      findWriters(request);
    final var fileTemp =
      request.fileTemp();
    final var file =
      request.file();
    final var version =
      request.formatVersion();

    model.eventProgress(
      "Exporting file.",
      0.0,
      "Exporting file.",
      0.0
    );

    try (var resources = AECloseables.create()) {
      final var out =
        resources.add(FileChannel.open(fileTemp, OPEN_OPTIONS));
      final var writeRequest =
        new AUWriteRequest(out, fileTemp.toUri(), version);
      final var writer =
        resources.add(writers.createWriter(writeRequest));
      final var writable =
        resources.add(writer.execute());

      writeIdentifier(model, writable, identifier);

      final SortedMap<AUClipID, AUClipDescription> clipDefinitions =
        writeClipDataAll(model, writable, context);

      writeClipDefinitions(model, writable, clipDefinitions);
      writeKeyAssignments(model, writable);
      writeMetadata(model, writable);
      writeEnd(writable);

      model.eventProgress(
        "Exporting file.",
        1.0,
        "Finishing writes.",
        1.0
      );
    } catch (final Exception e) {
      throw AEExceptions.wrap(e);
    }

    try {
      Files.move(
        fileTemp,
        file,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
      );
    } catch (final IOException e) {
      throw AEExceptions.wrap(e);
    }

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
    return "Export data.";
  }
}
