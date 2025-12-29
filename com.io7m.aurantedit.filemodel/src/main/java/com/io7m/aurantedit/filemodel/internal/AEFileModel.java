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

package com.io7m.aurantedit.filemodel.internal;

import com.io7m.aurantedit.filemodel.AECommandExportParameters;
import com.io7m.aurantedit.filemodel.AECommandImportParameters;
import com.io7m.aurantedit.filemodel.AECommandRecordType;
import com.io7m.aurantedit.filemodel.AEFileModelEventError;
import com.io7m.aurantedit.filemodel.AEFileModelEventProgress;
import com.io7m.aurantedit.filemodel.AEFileModelEventType;
import com.io7m.aurantedit.filemodel.AEFileModelStatusIdle;
import com.io7m.aurantedit.filemodel.AEFileModelStatusLoading;
import com.io7m.aurantedit.filemodel.AEFileModelStatusRunningCommand;
import com.io7m.aurantedit.filemodel.AEFileModelStatusType;
import com.io7m.aurantedit.filemodel.AEFileModelType;
import com.io7m.aurantedit.filemodel.AEProgress;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandCompact;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandExport;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandImport;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandLoad;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandSerialized;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandStateType;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandType;
import com.io7m.aurantedit.filemodel.internal.commands.AECommands;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipReplace;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipReplaceParameters;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipsAdd;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipsDelete;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1IdentifierPut;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1KeyAssignmentPut;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1Metadata;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1MetadataPut;
import com.io7m.aurantedit.filemodel.internal.json.AE1Mappers;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUKeyAssignmentID;
import com.io7m.aurantium.api.AUMetadataValue;
import com.io7m.aurantium.xmedia.AUXMediaConversion;
import com.io7m.darco.api.DDatabaseCreate;
import com.io7m.darco.api.DDatabaseException;
import com.io7m.darco.api.DDatabaseTelemetryNoOp;
import com.io7m.darco.api.DDatabaseUnit;
import com.io7m.darco.api.DDatabaseUpgrade;
import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jattribute.core.AttributeType;
import com.io7m.jattribute.core.Attributes;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.seltzer.api.SStructuredErrorType;
import com.io7m.wendover.core.ByteBufferChannels;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.io7m.aurantedit.filemodel.internal.Tables.CLIPS;
import static com.io7m.aurantedit.filemodel.internal.Tables.REDO;
import static com.io7m.aurantedit.filemodel.internal.Tables.UNDO;
import static java.time.ZoneOffset.UTC;

/**
 * The file model.
 */

public final class AEFileModel
  implements AEFileModelType, AEFileModelViewType, AEFileModelEventsType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AEFileModel.class);

  private static final Attributes ATTRIBUTES =
    Attributes.create(throwable -> {
      LOG.error("Uncaught attribute exception: ", throwable);
    });

  private final AEDatabaseType database;
  private final AttributeType<Optional<AUIdentifier>> identifier;
  private final AttributeType<AEFileModelStatusType> status;
  private final AttributeType<List<AECommandRecordType>> redoStack;
  private final AttributeType<List<AECommandRecordType>> undoStack;
  private final AttributeType<Optional<? extends AECommandType<?, ?>>> redo;
  private final AttributeType<Optional<? extends AECommandType<?, ?>>> undo;
  private final AttributeType<Optional<String>> redoText;
  private final AttributeType<Optional<String>> undoText;
  private final CloseableCollectionType<AUException> resources;
  private final CompletableFuture<Object> loadingLatch;
  private final ConcurrentHashMap<String, String> attributes;
  private final ExecutorService executor;
  private final ReentrantLock commandLock;
  private final SubmissionPublisher<AEFileModelEventType> events;
  private final AttributeType<List<AUMetadataValue>> metadata;
  private final AttributeType<List<AUClipDeclaration>> clips;
  private final AttributeType<List<AUKeyAssignment>> keyAssignments;
  private final AttributeType<Map<AUKeyAssignmentID, AUKeyAssignment>> keyAssignmentMap;
  private final AttributeType<Map<String, List<AUMetadataValue>>> metadataMap;
  private final AtomicBoolean closed;

  private AEFileModel(
    final AEDatabaseType inDatabase)
  {
    this.database =
      Objects.requireNonNull(inDatabase, "database");
    this.resources =
      AECloseables.create();
    this.closed =
      new AtomicBoolean(false);

    this.resources.add(this::finishLoading);
    this.executor =
      this.resources.add(
        Executors.newSingleThreadExecutor(
          Thread.ofVirtual().factory()
        )
      );

    this.resources.add(this.database);
    this.events = this.resources.add(new SubmissionPublisher<>());

    this.identifier =
      ATTRIBUTES.withValue(Optional.empty());
    this.metadata =
      ATTRIBUTES.withValue(List.of());
    this.metadataMap =
      ATTRIBUTES.withValue(Map.of());
    this.clips =
      ATTRIBUTES.withValue(List.of());
    this.keyAssignments =
      ATTRIBUTES.withValue(List.of());
    this.keyAssignmentMap =
      ATTRIBUTES.withValue(Map.of());

    this.resources.add(
      this.keyAssignments.subscribe((_, ka) -> {
        this.onKeyAssignmentsChanged(ka);
      })
    );
    this.resources.add(
      this.metadata.subscribe((_, m) -> {
        this.onMetadataChanged(m);
      })
    );

    this.undo =
      ATTRIBUTES.withValue(Optional.empty());
    this.undoText =
      this.undo.map(o -> o.map(AECommandType::describe));
    this.undoStack =
      ATTRIBUTES.withValue(List.of());
    this.redoStack =
      ATTRIBUTES.withValue(List.of());
    this.redo =
      ATTRIBUTES.withValue(Optional.empty());
    this.redoText =
      this.redo.map(o -> o.map(AECommandType::describe));
    this.resources.add(
      this.undo.subscribe((_, _) -> this.onUndoStateChanged())
    );
    this.resources.add(
      this.redo.subscribe((_, _) -> this.onRedoStateChanged())
    );

    this.status =
      ATTRIBUTES.withValue(new AEFileModelStatusLoading());
    this.commandLock =
      new ReentrantLock();
    this.attributes =
      new ConcurrentHashMap<>();
    this.loadingLatch =
      new CompletableFuture<>();
  }

  /**
   * Import a sample map.
   *
   * @param sampleMap  The sample map
   * @param outputFile The output file
   *
   * @return A file model
   *
   * @throws AUException On errors
   */

  public static AEFileModelType importSampleMap(
    final Path sampleMap,
    final Path outputFile)
    throws AUException
  {
    try {
      final var databases =
        new AEDatabaseFactory();

      final var baseModel =
        new AEFileModel(
          databases.open(
            new AEDatabaseConfiguration(
              Optional.empty(),
              DDatabaseTelemetryNoOp.get(),
              DDatabaseCreate.CREATE_DATABASE,
              DDatabaseUpgrade.UPGRADE_DATABASE,
              outputFile,
              false
            ),
            _ -> {

            }
          )
        );

      baseModel.importFrom(new AECommandImportParameters(sampleMap)).get();
      baseModel.compact().get();
      return baseModel;
    } catch (final Throwable e) {
      throw AEExceptions.wrap(e);
    }
  }

  /**
   * Open a file model.
   *
   * @param file     The file
   * @param readOnly {@code true} if the file should be read-only
   *
   * @return A file model
   *
   * @throws AUException On errors
   */

  public static AEFileModelType open(
    final Path file,
    final boolean readOnly)
    throws AUException
  {
    final var model = openModel(file, readOnly);
    model.load();
    return model;
  }

  private static AEFileModel openModel(
    final Path file,
    final boolean readOnly)
    throws AUException
  {
    try {
      final var databases =
        new AEDatabaseFactory();

      if (readOnly) {
        return new AEFileModel(
          databases.open(
            new AEDatabaseConfiguration(
              Optional.empty(),
              DDatabaseTelemetryNoOp.get(),
              DDatabaseCreate.DO_NOT_CREATE_DATABASE,
              DDatabaseUpgrade.DO_NOT_UPGRADE_DATABASE,
              file,
              readOnly
            ),
            _ -> {

            }
          )
        );
      }

      return new AEFileModel(
        databases.open(
          new AEDatabaseConfiguration(
            Optional.empty(),
            DDatabaseTelemetryNoOp.get(),
            DDatabaseCreate.CREATE_DATABASE,
            DDatabaseUpgrade.UPGRADE_DATABASE,
            file,
            readOnly
          ),
          _ -> {

          }
        )
      );
    } catch (final DDatabaseException e) {
      throw new AUException(
        e.getMessage(),
        e,
        e.errorCode(),
        e.attributes(),
        e.remediatingAction()
      );
    }
  }

  private static long nowMilliseconds()
  {
    return OffsetDateTime.now(UTC)
      .toInstant()
      .toEpochMilli();
  }

  private static void dbUndoMoveToRedo(
    final AEDatabaseTransactionType t,
    final Record oldCommandRec)
  {
    final var context =
      t.get(DSLContext.class);

    final var id =
      oldCommandRec.get(UNDO.UNDO_ID);

    context.deleteFrom(UNDO)
      .where(UNDO.UNDO_ID.eq(id))
      .execute();

    context.insertInto(REDO)
      .set(REDO.REDO_ID, id)
      .set(REDO.REDO_DESCRIPTION, oldCommandRec.get(UNDO.UNDO_DESCRIPTION))
      .set(REDO.REDO_DATA, oldCommandRec.get(UNDO.UNDO_DATA))
      .set(REDO.REDO_TIME, oldCommandRec.get(UNDO.UNDO_TIME))
      .execute();
  }

  private static void dbRedoMoveToUndo(
    final AEDatabaseTransactionType t,
    final Record oldCommandRec)
  {
    final var context =
      t.get(DSLContext.class);

    final var id =
      oldCommandRec.get(REDO.REDO_ID);

    context.deleteFrom(REDO)
      .where(REDO.REDO_ID.eq(id))
      .execute();

    context.insertInto(UNDO)
      .set(UNDO.UNDO_ID, id)
      .set(UNDO.UNDO_DESCRIPTION, oldCommandRec.get(REDO.REDO_DESCRIPTION))
      .set(UNDO.UNDO_DATA, oldCommandRec.get(REDO.REDO_DATA))
      .set(UNDO.UNDO_TIME, oldCommandRec.get(REDO.REDO_TIME))
      .execute();
  }

  private static AECommandType<?, ?> parseUndoCommandFromProperties(
    final Record rec)
    throws IOException
  {
    final var m = AE1Mappers.mapper();
    final AECommandSerialized saved;
    try (var stream = new ByteArrayInputStream(rec.get(UNDO.UNDO_DATA))) {
      saved = m.readValue(stream, AECommandSerialized.class);
    }
    return AECommands.forJSON(saved);
  }

  private static AECommandType<?, ?> parseRedoCommandFromProperties(
    final Record rec)
    throws IOException
  {
    final var m = AE1Mappers.mapper();
    final AECommandSerialized saved;
    try (var stream = new ByteArrayInputStream(rec.get(REDO.REDO_DATA))) {
      saved = m.readValue(stream, AECommandSerialized.class);
    }
    return AECommands.forJSON(saved);
  }

  private static Optional<Record> dbUndoGetTip(
    final AEDatabaseTransactionType t)
  {
    final var context =
      t.get(DSLContext.class);

    return context.select(
        UNDO.UNDO_DATA,
        UNDO.UNDO_TIME,
        UNDO.UNDO_DESCRIPTION,
        UNDO.UNDO_ID
      ).from(UNDO)
      .orderBy(UNDO.UNDO_TIME.desc(), UNDO.UNDO_ID.desc())
      .limit(1)
      .fetchOptional()
      .map(x -> x);
  }

  private static Optional<Record> dbRedoGetTip(
    final AEDatabaseTransactionType t)
  {
    final var context =
      t.get(DSLContext.class);

    return context.select(
        REDO.REDO_DATA,
        REDO.REDO_TIME,
        REDO.REDO_DESCRIPTION,
        REDO.REDO_ID
      ).from(REDO)
      .orderBy(REDO.REDO_TIME.asc(), REDO.REDO_ID.asc())
      .limit(1)
      .fetchOptional()
      .map(x -> x);
  }

  private static AUException errorClipNonexistent(
    final AUClipID id)
  {
    return new AUException(
      "No such clip.",
      "error-clip-nonexistent",
      Map.ofEntries(
        Map.entry("Clip", id.toString())
      ),
      Optional.empty()
    );
  }

  private void onMetadataChanged(
    final List<AUMetadataValue> m)
  {
    final var r = new HashMap<String, List<AUMetadataValue>>();
    for (final var e : m) {
      var values = r.get(e.name());
      if (values == null) {
        values = new ArrayList<>();
      }
      values.add(new AUMetadataValue(e.name(), e.value()));
      r.put(e.name(), values);
    }
    this.metadataMap.set(Map.copyOf(r));
  }

  private void onKeyAssignmentsChanged(
    final List<AUKeyAssignment> ka)
  {
    this.keyAssignmentMap.set(
      ka.stream()
        .collect(Collectors.toMap(AUKeyAssignment::id, k -> k))
    );
  }

  private void load()
  {
    this.runCommand(
      new AECommandLoad(),
      DDatabaseUnit.UNIT
    );
  }

  @Override
  public void close()
    throws AUException
  {
    if (this.closed.compareAndSet(false, true)) {
      LOG.debug("Close");
      this.resources.close();

      try {
        this.executor.awaitTermination(10L, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private <S extends AECommandStateType, P, C extends AECommandType<S, P>>
  CompletableFuture<?>
  runCommand(
    final C command,
    final P parameters)
  {
    final var future = new CompletableFuture<Void>();
    this.executor.execute(() -> {
      try {
        if (command.loading()) {
          this.status.set(new AEFileModelStatusLoading());
        } else {
          this.status.set(new AEFileModelStatusRunningCommand());
        }

        this.executeCommandLocked(command, parameters);

        if (command.loading()) {
          this.finishLoading();
        }

        this.status.set(new AEFileModelStatusIdle());
        future.complete(null);
      } catch (final Throwable e) {
        if (command.loading()) {
          this.loadingLatch.completeExceptionally(e);
        }
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  private void finishLoading()
  {
    LOG.debug("Signalling load completion.");
    this.loadingLatch.complete(new Object());
  }

  private <S extends AECommandStateType, P, C extends AECommandType<S, P>>
  void executeCommandLocked(
    final C command,
    final P parameters)
    throws Exception
  {
    this.commandLock.lock();

    try {
      this.attributes.clear();

      try (var t = this.database.openTransaction()) {
        final var undoable =
          command.execute(this, t, parameters);

        switch (undoable) {
          case COMMAND_UNDOABLE -> {
            final var context = t.get(DSLContext.class);
            final var state =
              new AECommandSerialized(
                OffsetDateTime.now(UTC).withNano(0),
                command.describe(),
                command.getClass().getCanonicalName(),
                command.state()
              );
            final var stateBytes =
              AE1Mappers.mapper()
                .writeValueAsBytes(state);

            context.insertInto(UNDO)
              .set(UNDO.UNDO_DESCRIPTION, command.describe())
              .set(UNDO.UNDO_TIME, Long.valueOf(nowMilliseconds()))
              .set(UNDO.UNDO_DATA, stateBytes)
              .execute();
          }
          case COMMAND_NOT_UNDOABLE -> {

          }
        }

        LOG.trace("{}: Commit", command);
        t.commit();

        if (command.requiresCompaction()) {
          final var context = t.get(DSLContext.class);
          context.execute("VACUUM");
        }

        switch (undoable) {
          case COMMAND_UNDOABLE -> {
            this.undo.set(Optional.of(command));
          }
          case COMMAND_NOT_UNDOABLE -> {

          }
        }
      } catch (final Throwable e) {
        LOG.debug("Exception: ", e);
        throw this.handleThrowable(e);
      }
    } finally {
      this.commandLock.unlock();
    }
  }

  private AUException handleThrowable(
    final Throwable e)
  {
    final var error = this.buildException(e);
    this.event(new AEFileModelEventError(
      error.getMessage(),
      error.errorCode(),
      error.attributes(),
      error.remediatingAction(),
      Optional.of(error)
    ));
    return error;
  }

  private AUException buildException(
    final Throwable e)
  {
    return switch (e) {
      case final SQLiteException x
        when x.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE -> {
        yield new AUException(
          "Object or name already exists.",
          e,
          "error-duplicate",
          this.attributes(),
          Optional.empty()
        );
      }
      case final IntegrityConstraintViolationException x
        when x.getCause() != null -> {
        yield this.buildException(x.getCause());
      }
      case final SStructuredErrorType<?> x -> {
        this.attributes.putAll(x.attributes());
        yield new AUException(
          e.getMessage(),
          e,
          x.errorCode().toString(),
          this.attributes(),
          x.remediatingAction()
        );
      }
      case Throwable _ -> {
        yield new AUException(
          e.getMessage(),
          e,
          "error-exception",
          this.attributes(),
          Optional.empty()
        );
      }
    };
  }

  @Override
  public AttributeReadableType<Optional<String>> undoText()
  {
    return this.undoText;
  }

  @Override
  public AttributeReadableType<List<AECommandRecordType>> undoStack()
  {
    return this.undoStack;
  }

  @Override
  public AttributeReadableType<List<AECommandRecordType>> redoStack()
  {
    return this.redoStack;
  }

  @Override
  public AttributeReadableType<Optional<AUIdentifier>> identifier()
  {
    return this.identifier;
  }

  @Override
  public AttributeReadableType<List<AUClipDeclaration>> clips()
  {
    return this.clips;
  }

  @Override
  public AttributeReadableType<List<AUKeyAssignment>> keyAssignments()
  {
    return this.keyAssignments;
  }

  @Override
  public AttributeReadableType<Map<AUKeyAssignmentID, AUKeyAssignment>> keyAssignmentMap()
  {
    return this.keyAssignmentMap;
  }

  @Override
  public AttributeReadableType<List<AUMetadataValue>> metadata()
  {
    return this.metadata;
  }

  @Override
  public AttributeReadableType<Map<String, List<AUMetadataValue>>> metadataMap()
  {
    return this.metadataMap;
  }

  @Override
  public CompletableFuture<?> undo()
  {
    final var future = new CompletableFuture<Void>();
    this.executor.execute(() -> {
      try {
        this.executeUndo();
        future.complete(null);
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  private void executeUndo()
    throws Exception
  {
    this.commandLock.lock();

    try {
      this.attributes.clear();

      try (var t = this.database.openTransaction()) {
        final var oldCommandRecOpt = dbUndoGetTip(t);
        if (oldCommandRecOpt.isEmpty()) {
          return;
        }

        final var oldCommandRec =
          oldCommandRecOpt.get();
        final var oldCommand =
          parseUndoCommandFromProperties(oldCommandRec);

        oldCommand.undo(this, t);
        dbUndoMoveToRedo(t, oldCommandRec);
        t.commit();

        this.redo.set(Optional.of(oldCommand));

        final var newCommandRecOpt = dbUndoGetTip(t);
        if (newCommandRecOpt.isPresent()) {
          this.undo.set(Optional.of(
            parseUndoCommandFromProperties(newCommandRecOpt.get()))
          );
        } else {
          this.undo.set(Optional.empty());
        }
      } catch (final Throwable e) {
        throw this.handleThrowable(e);
      }
    } finally {
      this.commandLock.unlock();
    }
  }

  @Override
  public CompletableFuture<?> redo()
  {
    final var future = new CompletableFuture<Void>();
    this.executor.execute(() -> {
      try {
        this.executeRedo();
        future.complete(null);
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public AttributeReadableType<Optional<String>> redoText()
  {
    return this.redoText;
  }

  @Override
  public CompletableFuture<?> loading()
  {
    return this.loadingLatch;
  }

  @Override
  public CompletableFuture<?> clipsAdd(
    final List<Path> files)
  {
    return this.runCommand(
      new AEC1ClipsAdd(),
      List.copyOf(files)
    );
  }

  @Override
  public CompletableFuture<?> keyAssignmentPut(
    final AUKeyAssignment keyAssignment)
  {
    return this.runCommand(
      new AEC1KeyAssignmentPut(),
      keyAssignment
    );
  }

  @Override
  public CompletableFuture<?> metadataPut(
    final List<AUMetadataValue> newMetadata)
  {
    return this.runCommand(
      new AEC1MetadataPut(),
      _ -> newMetadata
    );
  }

  @Override
  public CompletableFuture<?> identifierPut(
    final AUIdentifier newIdentifier)
  {
    return this.runCommand(
      new AEC1IdentifierPut(),
      newIdentifier
    );
  }

  @Override
  public CompletableFuture<?> compact()
  {
    return this.runCommand(
      new AECommandCompact(),
      DDatabaseUnit.UNIT
    );
  }

  @Override
  public CompletableFuture<?> export(
    final AECommandExportParameters parameters)
  {
    return this.runCommand(
      new AECommandExport(),
      parameters
    );
  }

  @Override
  public CompletableFuture<?> importFrom(
    final AECommandImportParameters parameters)
  {
    return this.runCommand(
      new AECommandImport(),
      parameters
    );
  }

  @Override
  public SeekableByteChannel clipAudioData(
    final AUClipID id)
    throws AUException
  {
    try (var transaction = this.database.openTransaction()) {
      final var context =
        transaction.get(DSLContext.class);

      final var blob =
        context.select(CLIPS.CLIP_BLOB)
          .from(CLIPS)
          .where(CLIPS.CLIP_ID.eq(Long.valueOf(id.value())))
          .fetchOne(CLIPS.CLIP_BLOB);

      if (blob == null) {
        throw errorClipNonexistent(id);
      }
      return ByteBufferChannels.ofByteBuffer(ByteBuffer.wrap(blob));
    } catch (final DDatabaseException e) {
      throw this.handleThrowable(e);
    }
  }

  @Override
  public AudioInputStream clipAudioStream(
    final AUClipID id)
    throws AUException
  {
    final var clipDeclaration =
      this.clips.get()
        .stream()
        .filter(c -> Objects.equals(c.id(), id))
        .findFirst()
        .orElseThrow(() -> errorClipNonexistent(id));

    final var data = this.clipAudioData(id);
    return AUXMediaConversion.createAudioStreamOf(clipDeclaration, data);
  }

  @Override
  public CompletableFuture<?> clipDelete(
    final AUClipID clipId)
  {
    return this.runCommand(
      new AEC1ClipsDelete(),
      List.of(clipId)
    );
  }

  @Override
  public CompletableFuture<?> clipReplace(
    final AUClipID clipId,
    final Path file)
  {
    return this.runCommand(
      new AEC1ClipReplace(),
      new AEC1ClipReplaceParameters(clipId, file)
    );
  }

  @Override
  public CompletableFuture<?> keyAssignmentUpdate(
    final AUKeyAssignment k)
  {
    return null;
  }

  @Override
  public CompletableFuture<?> metadataAdd(
    final AUMetadataValue newMetadata)
  {
    return this.runCommand(
      new AEC1MetadataPut(),
      m -> AEC1Metadata.metadataAdd(m, newMetadata)
    );
  }

  @Override
  public CompletableFuture<?> metadataRemove(
    final AUMetadataValue oldMetadata)
  {
    return this.runCommand(
      new AEC1MetadataPut(),
      m -> AEC1Metadata.metadataRemove(m, oldMetadata)
    );
  }

  @Override
  public CompletableFuture<?> metadataReplace(
    final AUMetadataValue metadataReplace,
    final AUMetadataValue metadataReplaceWith)
  {
    return this.runCommand(
      new AEC1MetadataPut(),
      m -> AEC1Metadata.metadataReplace(
        m,
        metadataReplace,
        metadataReplaceWith
      )
    );
  }

  @Override
  public CompletableFuture<?> keyAssignmentAdd(
    final AUClipDeclaration clip,
    final long keyCenter)
  {
    return null;
  }

  private void executeRedo()
    throws Exception
  {
    this.commandLock.lock();

    try {
      this.attributes.clear();

      try (var t = this.database.openTransaction()) {
        final var oldCommandRecOpt = dbRedoGetTip(t);
        if (oldCommandRecOpt.isEmpty()) {
          return;
        }

        final var oldCommandRec =
          oldCommandRecOpt.get();
        final var oldCommand =
          parseRedoCommandFromProperties(oldCommandRec);

        oldCommand.redo(this, t);
        dbRedoMoveToUndo(t, oldCommandRec);
        t.commit();

        this.undo.set(Optional.of(oldCommand));

        final var newCommandRecOpt = dbRedoGetTip(t);
        if (newCommandRecOpt.isPresent()) {
          this.redo.set(Optional.of(
            parseRedoCommandFromProperties(newCommandRecOpt.get()))
          );
        } else {
          this.redo.set(Optional.empty());
        }
      } catch (final Throwable e) {
        throw this.handleThrowable(e);
      }
    } finally {
      this.commandLock.unlock();
    }
  }

  @Override
  public Map<String, String> attributes()
  {
    return Map.copyOf(this.attributes);
  }

  /**
   * Clear the undo/redo stacks.
   */

  public void clearUndo()
  {
    this.undo.set(Optional.empty());
    this.undoStack.set(List.of());
    this.redo.set(Optional.empty());
    this.redoStack.set(List.of());
  }

  void event(
    final AEFileModelEventType event)
  {
    this.events.submit(event);
  }

  /**
   * Load the undo stack.
   *
   * @param transaction The database transaction
   */

  public void loadUndo(
    final AEDatabaseTransactionType transaction)
  {
    final var rec = dbUndoGetTip(transaction);
    if (rec.isPresent()) {
      try {
        this.undo.set(Optional.of(parseUndoCommandFromProperties(rec.get())));
      } catch (final IOException e) {
        LOG.debug("Unable to parse stored undo command: ", e);
      }
    }
  }

  /**
   * Load the redo stack.
   *
   * @param transaction The database transaction
   */

  public void loadRedo(
    final AEDatabaseTransactionType transaction)
  {
    final var rec = dbRedoGetTip(transaction);
    if (rec.isPresent()) {
      try {
        this.redo.set(Optional.of(parseRedoCommandFromProperties(rec.get())));
      } catch (final IOException e) {
        LOG.debug("Unable to parse stored redo command: ", e);
      }
    }
  }

  private void onRedoStateChanged()
  {
    this.executor.execute(() -> {
      try (var t = this.database.openTransaction()) {
        final var context = t.get(DSLContext.class);

        this.redoStack.set(
          context.select(
              REDO.REDO_TIME,
              REDO.REDO_DESCRIPTION
            ).from(REDO)
            .orderBy(REDO.REDO_TIME.desc())
            .stream()
            .map(r -> {
              final var instant =
                Instant.ofEpochMilli(r.get(REDO.REDO_TIME).longValue());
              final var time =
                OffsetDateTime.ofInstant(instant, UTC);
              final var description =
                r.get(REDO.REDO_DESCRIPTION);
              return (AECommandRecordType) new AECommandRecord(
                time,
                description);
            })
            .toList()
        );
      } catch (final Throwable e) {
        LOG.debug("Error reading redo stack: ", e);
      }
    });
  }

  private void onUndoStateChanged()
  {
    this.executor.execute(() -> {
      try (var t = this.database.openTransaction()) {
        final var context = t.get(DSLContext.class);

        this.undoStack.set(
          context.select(
              UNDO.UNDO_TIME,
              UNDO.UNDO_DESCRIPTION
            ).from(UNDO)
            .orderBy(UNDO.UNDO_TIME.desc())
            .stream()
            .map(r -> {
              final var instant =
                Instant.ofEpochMilli(r.get(UNDO.UNDO_TIME).longValue());
              final var time =
                OffsetDateTime.ofInstant(instant, UTC);
              final var description =
                r.get(UNDO.UNDO_DESCRIPTION);
              return (AECommandRecordType) new AECommandRecord(
                time,
                description);
            })
            .toList()
        );
      } catch (final Throwable e) {
        LOG.debug("Error reading undo stack: ", e);
      }
    });
  }

  @Override
  public void setStatus(
    final AEFileModelStatusType newStatus)
  {
    this.status.set(newStatus);
  }

  @Override
  public SubmissionPublisher<AEFileModelEventType> events()
  {
    return this.events;
  }

  @Override
  public AttributeReadableType<AEFileModelStatusType> status()
  {
    return this.status;
  }

  @Override
  public void setIdentifier(
    final Optional<AUIdentifier> newIdentifier)
  {
    this.identifier.set(newIdentifier);
  }

  @Override
  public void setMetadata(
    final List<AUMetadataValue> newMetadata)
  {
    this.metadata.set(newMetadata);
  }

  @Override
  public void setClips(
    final List<AUClipDeclaration> newClips)
  {
    this.clips.set(newClips);
  }

  @Override
  public void setAttribute(
    final String name,
    final Object value)
  {
    this.attributes.put(name, value.toString());
  }

  @Override
  public void setKeyAssignments(
    final List<AUKeyAssignment> newKeyAssignments)
  {
    this.keyAssignments.set(List.copyOf(newKeyAssignments));
  }

  @Override
  public void eventProgress(
    final String task,
    final double taskProgress,
    final String subTask,
    final double subTaskProgress)
  {
    this.event(
      new AEFileModelEventProgress(
        new AEProgress(task, taskProgress, subTask, subTaskProgress)
      )
    );
  }

  /**
   * Find the next clip ID.
   *
   * @return The clip ID
   */

  public long nextClipId()
  {
    final var clipIdOpt =
      this.clips.get()
        .stream()
        .map(AUClipDeclaration::id)
        .map(x -> Long.valueOf(x.value()))
        .max(Long::compareUnsigned);

    return clipIdOpt.map(x -> Long.valueOf(x.longValue() + 1L))
      .orElse(Long.valueOf(0L))
      .longValue();
  }

  @Override
  public boolean isClosed()
  {
    return this.closed.get();
  }

  private record AECommandRecord(
    OffsetDateTime time,
    String description)
    implements AECommandRecordType
  {

  }
}
