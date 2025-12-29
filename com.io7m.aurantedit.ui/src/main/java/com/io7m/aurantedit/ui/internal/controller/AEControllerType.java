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

import com.io7m.aurantedit.filemodel.AEFileModelEventType;
import com.io7m.aurantedit.filemodel.AEFileModelType;
import com.io7m.aurantedit.ui.internal.AEImport;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUMetadataValue;
import com.io7m.aurantium.api.AUVersion;
import com.io7m.jsamplebuffer.api.SampleBufferType;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.SortedList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Flow;

/**
 * The main application controller.
 *
 * <p>This interface defines the operations that coordinate file management,
 * clip management, key assignments, metadata, undo/redo state, and import
 * functionality. Implementations are expected to expose observable state
 * suitable for UI binding and to publish model-level events.</p>
 */

public interface AEControllerType
{
  /**
   * @return The current file model backing the application state
   */

  AEFileModelType fileModel();

  /**
   * @return A sorted, observable view of all key assignments
   */

  SortedList<AUKeyAssignment> keyAssignments();

  /**
   * @return A sorted, observable view of all clip declarations
   */

  SortedList<AUClipDeclaration> clips();

  /**
   * Create a new key assignment for the given clip.
   *
   * @param clip      The clip to associate with the key
   * @param keyCenter The key center value
   */

  void keyAssignmentCreate(
    AUClipDeclaration clip,
    long keyCenter);

  /**
   * @return An observable identifier for the currently open file, if any
   */

  ObservableValue<Optional<AUIdentifier>> identifier();

  /**
   * Close the currently open file.
   */

  void closeFile();

  /**
   * Undo the most recent operation, if possible.
   */

  void undo();

  /**
   * Redo the most recently undone operation, if possible.
   */

  void redo();

  /**
   * @return An observable description of the current undo state, if available
   */

  ObservableValue<Optional<String>> undoState();

  /**
   * @return An observable description of the current redo state, if available
   */

  ObservableValue<Optional<String>> redoState();

  /**
   * Obtain the audio buffer for a given clip.
   *
   * @param id The clip identifier
   *
   * @return A sample buffer for the clip
   */

  SampleBufferType clipBuffer(
    AUClipID id);

  /**
   * @return An observable path to the currently open file, if any
   */

  ObservableValue<Optional<Path>> file();

  /**
   * @return A sorted, observable view of all metadata entries
   */

  SortedList<AUMetadataValue> metadata();

  /**
   * Open an existing file.
   *
   * @param file     The file path
   * @param readOnly Whether the file should be opened read-only
   *
   * @throws AUException On application-level errors
   */

  void open(
    Path file,
    boolean readOnly)
    throws AUException;

  /**
   * Add a new clip from the given file.
   *
   * @param file The source file for the clip
   */

  void clipAdd(
    Path file);

  /**
   * Delete an existing clip.
   *
   * @param clipId The identifier of the clip to delete
   */

  void clipDelete(
    AUClipID clipId);

  /**
   * Replace the contents of an existing clip.
   *
   * @param clipId The identifier of the clip to replace
   * @param file   The new source file
   */

  void clipReplace(
    AUClipID clipId,
    Path file);

  /**
   * @return A publisher that emits file model events as they occur
   */

  Flow.Publisher<AEFileModelEventType> events();

  /**
   * Update an existing key assignment.
   *
   * @param k The updated key assignment
   */

  void keyAssignmentUpdate(
    AUKeyAssignment k);

  /**
   * Set the filter string used to constrain visible metadata.
   *
   * @param newValue The new filter value
   */

  void setMetadataFilter(
    String newValue);

  /**
   * Set the filter string used to constrain visible clips.
   *
   * @param newValue The new filter value
   */

  void setClipFilter(
    String newValue);

  /**
   * Create a new file.
   *
   * @param file The path of the file to create
   *
   * @throws AUException On application-level errors
   * @throws IOException On I/O errors
   */

  void create(Path file)
    throws AUException, IOException;

  /**
   * Add a metadata entry.
   *
   * @param meta The metadata to add
   */

  void metadataAdd(
    AUMetadataValue meta);

  /**
   * Remove a metadata entry.
   *
   * @param meta The metadata to remove
   */

  void metadataRemove(
    AUMetadataValue meta);

  /**
   * Replace an existing metadata entry.
   *
   * @param metaReplace     The metadata to be replaced
   * @param metaReplaceWith The metadata to use as a replacement
   */

  void metadataReplace(
    AUMetadataValue metaReplace,
    AUMetadataValue metaReplaceWith);

  /**
   * Set the sample map version.
   *
   * @param version The version to set
   */

  void setVersion(
    AUVersion version);

  /**
   * Execute an import operation.
   *
   * @param importR The import request
   *
   * @throws AUException On application-level errors
   * @throws IOException On I/O errors
   */

  void importNow(
    AEImport importR)
    throws AUException, IOException;
}
