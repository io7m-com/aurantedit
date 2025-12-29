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

package com.io7m.aurantedit.filemodel;

import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUKeyAssignmentID;
import com.io7m.aurantium.api.AUMetadataValue;
import com.io7m.jattribute.core.AttributeReadableType;
import com.io7m.jmulticlose.core.CloseableType;

import javax.sound.sampled.AudioInputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * The type of editable file models.
 */

public interface AEFileModelType
  extends CloseableType
{
  @Override
  void close()
    throws AUException;

  /**
   * @return The file model events
   */

  Flow.Publisher<AEFileModelEventType> events();

  /**
   * @return An observable status value for the file model
   */

  AttributeReadableType<AEFileModelStatusType> status();

  /**
   * @return Text describing the top of the undo stack, if any
   */

  AttributeReadableType<Optional<String>> undoText();

  /**
   * @return The undo stack
   */

  AttributeReadableType<List<AECommandRecordType>> undoStack();

  /**
   * @return The redo stack
   */

  AttributeReadableType<List<AECommandRecordType>> redoStack();

  /**
   * @return The identifier
   */

  AttributeReadableType<Optional<AUIdentifier>> identifier();

  /**
   * @return The clips
   */

  AttributeReadableType<List<AUClipDeclaration>> clips();

  /**
   * @return The key assignments
   */

  AttributeReadableType<List<AUKeyAssignment>> keyAssignments();

  /**
   * @return The key assignment map
   */

  AttributeReadableType<Map<AUKeyAssignmentID, AUKeyAssignment>> keyAssignmentMap();

  /**
   * @return The metadata
   */

  AttributeReadableType<List<AUMetadataValue>> metadata();

  /**
   * @return The metadata map
   */

  AttributeReadableType<Map<String, List<AUMetadataValue>>> metadataMap();

  /**
   * Undo the operation on the top of the undo stack
   *
   * @return The operation in progress
   */

  CompletableFuture<?> undo();

  /**
   * Redo the operation on the top of the redo stack
   *
   * @return The operation in progress
   */

  CompletableFuture<?> redo();

  /**
   * @return Text describing the top of the redo stack, if any
   */

  AttributeReadableType<Optional<String>> redoText();

  /**
   * @return A future that represents the file model's loading process
   */

  CompletableFuture<?> loading();

  /**
   * Add the given clips.
   *
   * @param files The clips
   *
   * @return The operation in progress
   */

  CompletableFuture<?> clipsAdd(List<Path> files);

  /**
   * Add/update the given key assignment.
   *
   * @param keyAssignment The key assignment
   *
   * @return The operation in progress
   */

  CompletableFuture<?> keyAssignmentPut(AUKeyAssignment keyAssignment);

  /**
   * Add/update metadata.
   *
   * @param metadata The metadata
   *
   * @return The operation in progress
   */

  CompletableFuture<?> metadataPut(List<AUMetadataValue> metadata);

  /**
   * Add/update identifier.
   *
   * @param identifier The identifier
   *
   * @return The operation in progress
   */

  CompletableFuture<?> identifierPut(AUIdentifier identifier);

  /**
   * Compact the database.
   *
   * @return The operation in progress
   */

  CompletableFuture<?> compact();

  /**
   * Export to a file.
   *
   * @param parameters The parameters
   *
   * @return The operation in progress
   */

  CompletableFuture<?> export(AECommandExportParameters parameters);

  /**
   * Import from a file.
   *
   * @param parameters The parameters
   *
   * @return The operation in progress
   */

  CompletableFuture<?> importFrom(AECommandImportParameters parameters);

  /**
   * @param id The clip ID
   *
   * @return The audio data for the given clip
   *
   * @throws AUException On errors
   */

  SeekableByteChannel clipAudioData(
    AUClipID id)
    throws AUException;

  /**
   * @param id The clip ID
   *
   * @return The audio stream for the given clip
   *
   * @throws AUException On errors
   */

  AudioInputStream clipAudioStream(
    AUClipID id)
    throws AUException;

  /**
   * Delete a clip asynchronously.
   *
   * @param clipId The identifier of the clip to delete
   *
   * @return The operation in progress
   */

  CompletableFuture<?> clipDelete(
    AUClipID clipId);

  /**
   * Replace an existing clip asynchronously.
   *
   * @param clipId The identifier of the clip to replace
   * @param file   The new source file
   *
   * @return The operation in progress
   */

  CompletableFuture<?> clipReplace(
    AUClipID clipId,
    Path file);

  /**
   * Update an existing key assignment asynchronously.
   *
   * @param k The updated key assignment
   *
   * @return The operation in progress
   */

  CompletableFuture<?> keyAssignmentUpdate(
    AUKeyAssignment k);

  /**
   * Add a metadata entry asynchronously.
   *
   * @param metadata The metadata value
   *
   * @return The operation in progress
   */

  CompletableFuture<?> metadataAdd(
    AUMetadataValue metadata);

  /**
   * Remove a metadata entry asynchronously.
   *
   * @param metadata The metadata value
   *
   * @return The operation in progress
   */

  CompletableFuture<?> metadataRemove(
    AUMetadataValue metadata);

  /**
   * Replace an existing metadata entry asynchronously.
   *
   * @param metadataReplace     The metadata value that will be replaced
   * @param metadataReplaceWith The replacement value
   *
   * @return The operation in progress
   */

  CompletableFuture<?> metadataReplace(
    AUMetadataValue metadataReplace,
    AUMetadataValue metadataReplaceWith);

  /**
   * Add a new key assignment asynchronously.
   *
   * @param clip      The clip to associate with the key
   * @param keyCenter The key center value
   *
   * @return The operation in progress
   */

  CompletableFuture<?> keyAssignmentAdd(
    AUClipDeclaration clip,
    long keyCenter);
}
