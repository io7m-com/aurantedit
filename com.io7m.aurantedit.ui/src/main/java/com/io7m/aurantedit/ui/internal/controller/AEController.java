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

package com.io7m.aurantedit.ui.internal.controller;

import com.io7m.aurantedit.filemodel.AECommandImportParameters;
import com.io7m.aurantedit.filemodel.AEFileModelEventType;
import com.io7m.aurantedit.filemodel.AEFileModelType;
import com.io7m.aurantedit.filemodel.AEFileModels;
import com.io7m.aurantedit.ui.internal.AEImport;
import com.io7m.aurantedit.ui.internal.AEPerpetualSubscriber;
import com.io7m.aurantedit.ui.internal.AEStringsType;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUMetadataValue;
import com.io7m.aurantium.api.AUVersion;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.jmulticlose.core.ClosingResourceFailedException;
import com.io7m.jsamplebuffer.api.SampleBufferType;
import com.io7m.jsamplebuffer.vanilla.SampleBufferFloat;
import com.io7m.jsamplebuffer.xmedia.SXMSampleBuffers;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * The controller implementation.
 */

public final class AEController implements AEControllerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AEController.class);

  private final AEStringsType strings;
  private final ObservableList<AUMetadataValue> metadata;
  private final ObservableList<AUClipDeclaration> clips;
  private final ObservableList<AUKeyAssignment> keyAssignments;
  private final SimpleObjectProperty<Optional<AEFileModelType>> fileModel;
  private final SimpleObjectProperty<Optional<AUIdentifier>> identifier;
  private final SimpleObjectProperty<Optional<Path>> filePath;
  private final SimpleObjectProperty<Optional<String>> redoState;
  private final SimpleObjectProperty<Optional<String>> undoState;
  private final SortedList<AUMetadataValue> metadataSorted;
  private final SortedList<AUClipDeclaration> clipsSorted;
  private final SortedList<AUKeyAssignment> keyAssignmentsSorted;
  private final SubmissionPublisher<AEFileModelEventType> events;
  private final FilteredList<AUMetadataValue> metadataFiltered;
  private final FilteredList<AUClipDeclaration> clipsFiltered;
  private CloseableCollectionType<ClosingResourceFailedException> fileModelSubscriptions;

  private AEController(
    final AEStringsType inStrings)
  {
    this.strings =
      Objects.requireNonNull(inStrings, "Strings");

    this.keyAssignments =
      FXCollections.observableArrayList();
    this.keyAssignmentsSorted =
      new SortedList<>(this.keyAssignments);

    this.clips =
      FXCollections.observableArrayList();
    this.clipsFiltered =
      new FilteredList<>(this.clips);
    this.clipsSorted =
      new SortedList<>(this.clipsFiltered);

    this.metadata =
      FXCollections.observableArrayList();
    this.metadataFiltered =
      new FilteredList<>(this.metadata);
    this.metadataSorted =
      new SortedList<>(this.metadataFiltered);

    this.filePath =
      new SimpleObjectProperty<>(Optional.empty());
    this.undoState =
      new SimpleObjectProperty<>(Optional.empty());
    this.redoState =
      new SimpleObjectProperty<>(Optional.empty());
    this.identifier =
      new SimpleObjectProperty<>(Optional.empty());

    this.events =
      new SubmissionPublisher<>();

    this.fileModel =
      new SimpleObjectProperty<>(Optional.empty());
    this.fileModelSubscriptions =
      CloseableCollection.create();
  }

  /**
   * Create a new controller.
   *
   * @param strings The string resources
   *
   * @return A new controller
   */

  public static AEControllerType empty(
    final AEStringsType strings)
  {
    return new AEController(strings);
  }

  private void closeFileModel()
  {
    final var fileModelNowOpt = this.fileModel.get();
    if (fileModelNowOpt.isPresent()) {
      LOG.debug("Closing file model.");
      final var fileModelClosing = fileModelNowOpt.get();
      try {
        fileModelClosing.close();
      } catch (final AUException e) {
        LOG.debug("Error closing file model: ", e);
      } finally {
        this.fileModel.set(Optional.empty());
      }
    }

    final var subs = this.fileModelSubscriptions;
    if (subs != null) {
      LOG.debug("Closing file model subscriptions.");
      try {
        subs.close();
      } catch (final ClosingResourceFailedException e) {
        LOG.debug("Failed to close subscriptions: ", e);
      } finally {
        this.fileModelSubscriptions = null;
      }
    }

    this.fileModelSubscriptions = CloseableCollection.create();
  }

  @Override
  public AEFileModelType fileModel()
  {
    return this.fileModel.get().orElseThrow();
  }

  @Override
  public SortedList<AUKeyAssignment> keyAssignments()
  {
    return this.keyAssignmentsSorted;
  }

  @Override
  public SortedList<AUClipDeclaration> clips()
  {
    return this.clipsSorted;
  }

  @Override
  public void keyAssignmentCreate(
    final AUClipDeclaration clip,
    final long keyCenter)
  {
    this.fileModel()
      .keyAssignmentAdd(clip, keyCenter);
  }

  @Override
  public ObservableValue<Optional<AUIdentifier>> identifier()
  {
    return this.identifier;
  }

  @Override
  public void closeFile()
  {
    this.closeFileModel();
  }

  @Override
  public void undo()
  {
    this.fileModel().undo();
  }

  @Override
  public void redo()
  {
    this.fileModel().redo();
  }

  @Override
  public ObservableValue<Optional<String>> undoState()
  {
    return this.undoState;
  }

  @Override
  public ObservableValue<Optional<String>> redoState()
  {
    return this.redoState;
  }

  @Override
  public SampleBufferType clipBuffer(
    final AUClipID id)
  {
    final var model = this.fileModel();
    try (var stream = model.clipAudioStream(id)) {
      return SXMSampleBuffers.readSampleBufferFromStream(
        stream,
        SampleBufferFloat::createWithHeapBuffer
      );
    } catch (final Exception e) {
      return SampleBufferFloat.createWithByteBuffer(
        1,
        0L,
        48000.0,
        value -> ByteBuffer.allocate(Math.toIntExact(value))
      );
    }
  }

  @Override
  public ObservableValue<Optional<Path>> file()
  {
    return this.filePath;
  }

  @Override
  public SortedList<AUMetadataValue> metadata()
  {
    return this.metadataSorted;
  }

  @Override
  public void open(
    final Path file,
    final boolean readOnly)
    throws AUException
  {
    LOG.debug("Open {}.", file);
    this.closeFileModel();

    LOG.debug("Opening new file model for {}.", file);
    this.setNewFileModel(file, AEFileModels.open(file, readOnly));
  }

  private void setNewFileModel(
    final Path file,
    final AEFileModelType newModel)
  {
    LOG.debug("Creating new file model subscriptions.");
    this.fileModelSubscriptions.add(
      newModel.identifier()
        .subscribe((_, newId) -> {
          Platform.runLater(() -> {
            this.identifier.set(newId);
          });
        })
    );
    this.fileModelSubscriptions.add(
      newModel.clips()
        .subscribe((_, newClips) -> {
          Platform.runLater(() -> {
            this.clips.setAll(newClips);
          });
        })
    );
    this.fileModelSubscriptions.add(
      newModel.keyAssignments()
        .subscribe((_, newKeys) -> {
          Platform.runLater(() -> {
            this.keyAssignments.setAll(newKeys);
          });
        })
    );
    this.fileModelSubscriptions.add(
      newModel.metadata()
        .subscribe((_, metas) -> {
          Platform.runLater(() -> {
            this.metadata.setAll(
              metas
            );
          });
        })
    );

    newModel.events()
      .subscribe(new AEPerpetualSubscriber<>(this.events::submit));

    this.filePath.set(
      Optional.of(file));
    this.fileModel.set(
      Optional.of(newModel));
  }

  @Override
  public void clipAdd(
    final Path file)
  {
    this.fileModel()
      .clipsAdd(List.of(file));
  }

  @Override
  public void clipDelete(
    final AUClipID clipId)
  {
    this.fileModel()
      .clipDelete(clipId);
  }

  @Override
  public void clipReplace(
    final AUClipID clipId,
    final Path file)
  {
    this.fileModel()
      .clipReplace(clipId, file);
  }

  @Override
  public Flow.Publisher<AEFileModelEventType> events()
  {
    return this.events;
  }

  @Override
  public void keyAssignmentUpdate(
    final AUKeyAssignment k)
  {
    this.fileModel()
      .keyAssignmentUpdate(k);
  }

  @Override
  public void setMetadataFilter(
    final String newValue)
  {
    Platform.runLater(() -> {
      if (newValue.isEmpty()) {
        this.metadataFiltered.setPredicate(_ -> true);
      } else {
        this.metadataFiltered.setPredicate(meta -> {
          final var upper =
            newValue.trim().toUpperCase(Locale.ROOT);
          final var nameUpper =
            meta.name().toUpperCase(Locale.ROOT);
          final var valueUpper =
            meta.value().toUpperCase(Locale.ROOT);
          return nameUpper.contains(upper) || valueUpper.contains(upper);
        });
      }
    });
  }

  @Override
  public void setClipFilter(
    final String newValue)
  {
    Platform.runLater(() -> {
      if (newValue.isEmpty()) {
        this.clipsFiltered.setPredicate(_ -> true);
      } else {
        this.clipsFiltered.setPredicate(meta -> {
          final var upper =
            newValue.trim().toUpperCase(Locale.ROOT);
          final var nameUpper =
            meta.name().toUpperCase(Locale.ROOT);
          return nameUpper.contains(upper);
        });
      }
    });
  }

  @Override
  public void create(
    final Path file)
    throws AUException, IOException
  {
    LOG.debug("Create {}.", file);
    this.closeFileModel();
    Files.delete(file);

    LOG.debug("Opening new file model for {}.", file);
    this.setNewFileModel(file, AEFileModels.open(file, false));
  }

  @Override
  public void metadataAdd(
    final AUMetadataValue meta)
  {
    this.fileModel().metadataAdd(meta);
  }

  @Override
  public void metadataRemove(
    final AUMetadataValue meta)
  {
    this.fileModel().metadataRemove(meta);
  }

  @Override
  public void metadataReplace(
    final AUMetadataValue metaReplace,
    final AUMetadataValue metaReplaceWith)
  {
    this.fileModel().metadataReplace(metaReplace, metaReplaceWith);
  }

  @Override
  public void setVersion(
    final AUVersion version)
  {
    final var identifierOpt = this.identifier.get();
    if (identifierOpt.isPresent()) {
      final var idOld =
        identifierOpt.get();
      final var idNew =
        new AUIdentifier(idOld.group(), idOld.name(), version);
      this.fileModel().identifierPut(idNew);
    }
  }

  @Override
  public void importNow(
    final AEImport importR)
    throws AUException, IOException
  {
    LOG.debug("Import {}.", importR.sampleMapFile());
    this.closeFileModel();
    Files.delete(importR.outputFile());

    LOG.debug("Creating file model for {}.", importR.outputFile());
    final var newModel = AEFileModels.open(importR.outputFile(), false);

    this.setNewFileModel(importR.outputFile(), newModel);

    newModel.importFrom(
      new AECommandImportParameters(importR.sampleMapFile())
    );
  }
}
