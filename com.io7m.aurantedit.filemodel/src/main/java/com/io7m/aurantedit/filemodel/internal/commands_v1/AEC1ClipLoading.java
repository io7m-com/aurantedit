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

import com.io7m.aurantedit.filemodel.internal.AECloseables;
import com.io7m.aurantedit.filemodel.internal.AEExceptions;
import com.io7m.aurantedit.filemodel.internal.AEFileModel;
import com.io7m.aurantium.api.AUAudioFormatType;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUHashAlgorithm;
import com.io7m.aurantium.api.AUHashValue;
import com.io7m.aurantium.api.AUOctetOrder;
import org.jooq.DSLContext;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.aurantedit.filemodel.internal.Tables.CLIPS;

/**
 * Functions to load clips.
 */

public final class AEC1ClipLoading
{
  private AEC1ClipLoading()
  {

  }

  /**
   * Load audio from the given file.
   *
   * @param model  The model
   * @param clipId The clip ID
   * @param file   The file
   *
   * @return The loaded audio
   *
   * @throws AUException On errors
   */

  public static AEC1AudioBytesAndHash loadAudio(
    final AEFileModel model,
    final long clipId,
    final Path file)
    throws AUException
  {
    if (isFlac(file)) {
      try (var resources = AECloseables.create()) {
        final var fileStream =
          resources.add(Files.newInputStream(file));
        final var digest =
          MessageDigest.getInstance("SHA-256");
        final var digestStream =
          resources.add(new DigestInputStream(fileStream, digest));
        final var audioStream =
          resources.add(AudioSystem.getAudioInputStream(file.toFile()));

        final var data =
          digestStream.readAllBytes();

        final var baseFormat =
          audioStream.getFormat();
        final var endianness =
          endiannessOf(baseFormat.isBigEndian());
        final var channels =
          baseFormat.getChannels();
        final var hashValue =
          new AUHashValue(
            AUHashAlgorithm.HA_SHA256,
            HexFormat.of().formatHex(digest.digest())
          );
        final var sampleRate =
          (long) baseFormat.getSampleRate();
        final var sampleDepth =
          (long) baseFormat.getSampleSizeInBits();
        final var format =
          formatOf(model, baseFormat.getEncoding());

        return new AEC1AudioBytesAndHash(
          data,
          new AUClipDeclaration(
            new AUClipID(clipId),
            file.getFileName().toString(),
            format,
            sampleRate,
            sampleDepth,
            channels,
            endianness,
            hashValue,
            (long) data.length,
            Optional.empty()
          )
        );
      } catch (final Exception e) {
        throw AEExceptions.wrap(e);
      }
    }

    try (var resources = AECloseables.create()) {
      final var audioStream =
        resources.add(AudioSystem.getAudioInputStream(file.toFile()));
      final var digest =
        MessageDigest.getInstance("SHA-256");
      final var digestStream =
        resources.add(new DigestInputStream(audioStream, digest));
      final var data =
        digestStream.readAllBytes();

      final var baseFormat =
        audioStream.getFormat();
      final var endianness =
        endiannessOf(baseFormat.isBigEndian());
      final var channels =
        baseFormat.getChannels();
      final var hashValue =
        new AUHashValue(
          AUHashAlgorithm.HA_SHA256,
          HexFormat.of().formatHex(digest.digest())
        );
      final var sampleRate =
        (long) baseFormat.getSampleRate();
      final var sampleDepth =
        (long) baseFormat.getSampleSizeInBits();

      final var format =
        formatOf(model, baseFormat.getEncoding());

      return new AEC1AudioBytesAndHash(
        data,
        new AUClipDeclaration(
          new AUClipID(clipId),
          file.getFileName().toString(),
          format,
          sampleRate,
          sampleDepth,
          channels,
          endianness,
          hashValue,
          (long) data.length,
          Optional.empty()
        )
      );
    } catch (final Exception e) {
      throw AEExceptions.wrap(e);
    }
  }

  private static boolean isFlac(
    final Path file)
    throws AUException
  {
    try (var resources = AECloseables.create()) {
      final var audioStream =
        resources.add(AudioSystem.getAudioInputStream(file.toFile()));
      return Objects.equals(
        audioStream.getFormat().getEncoding(),
        new AudioFormat.Encoding("FLAC")
      );
    } catch (final Exception e) {
      throw AEExceptions.wrap(e);
    }
  }

  private static AUAudioFormatType formatOf(
    final AEFileModel model,
    final AudioFormat.Encoding encoding)
    throws AUException
  {
    if (Objects.equals(encoding, AudioFormat.Encoding.PCM_FLOAT)) {
      return AUAudioFormatType.AUAudioFormatStandard.AFPCMLinearFloat;
    }
    if (Objects.equals(encoding, AudioFormat.Encoding.PCM_SIGNED)) {
      return AUAudioFormatType.AUAudioFormatStandard.AFPCMLinearIntegerSigned;
    }
    if (Objects.equals(encoding, AudioFormat.Encoding.PCM_UNSIGNED)) {
      return AUAudioFormatType.AUAudioFormatStandard.AFPCMLinearIntegerUnsigned;
    }
    if (Objects.equals(encoding, new AudioFormat.Encoding("FLAC"))) {
      return AUAudioFormatType.AUAudioFormatStandard.AFFlac;
    }

    throw new AUException(
      "Unsupported audio encoding.",
      "error-audio-format-unsupported",
      model.attributes(),
      Optional.empty()
    );
  }

  private static AUOctetOrder endiannessOf(
    final boolean bigEndian)
  {
    if (bigEndian) {
      return AUOctetOrder.BIG_ENDIAN;
    } else {
      return AUOctetOrder.LITTLE_ENDIAN;
    }
  }

  /**
   * Insert a clip into the database.
   *
   * @param context The context
   * @param clip    The clip
   * @param data    The audio data
   */

  public static void clipPut(
    final DSLContext context,
    final AUClipDeclaration clip,
    final byte[] data)
  {
    final var hash =
      clip.hash();
    final var hashAlgorithm =
      hash.algorithm();
    final var endianness =
      clip.endianness();
    final var format =
      clip.format();

    context.insertInto(CLIPS)
      .set(CLIPS.CLIP_ID, Long.valueOf(clip.id().value()))
      .set(CLIPS.CLIP_BLOB, data)
      .set(CLIPS.CLIP_CHANNELS, Long.valueOf(clip.channels()))
      .set(CLIPS.CLIP_ENDIANNESS, endianness.descriptor().value())
      .set(CLIPS.CLIP_FORMAT, format.descriptor().value())
      .set(CLIPS.CLIP_HASH_ALGORITHM, hashAlgorithm.descriptor().value())
      .set(CLIPS.CLIP_HASH_VALUE, hash.value())
      .set(CLIPS.CLIP_NAME, clip.name())
      .set(CLIPS.CLIP_SAMPLE_DEPTH, Long.valueOf(clip.sampleDepth()))
      .set(CLIPS.CLIP_SAMPLE_RATE, Long.valueOf(clip.sampleRate()))
      .onConflict(CLIPS.CLIP_ID)
      .doUpdate()
      .set(CLIPS.CLIP_BLOB, data)
      .set(CLIPS.CLIP_CHANNELS, Long.valueOf(clip.channels()))
      .set(CLIPS.CLIP_ENDIANNESS, endianness.descriptor().value())
      .set(CLIPS.CLIP_FORMAT, format.descriptor().value())
      .set(CLIPS.CLIP_HASH_ALGORITHM, hashAlgorithm.descriptor().value())
      .set(CLIPS.CLIP_HASH_VALUE, hash.value())
      .set(CLIPS.CLIP_NAME, clip.name())
      .set(CLIPS.CLIP_SAMPLE_DEPTH, Long.valueOf(clip.sampleDepth()))
      .set(CLIPS.CLIP_SAMPLE_RATE, Long.valueOf(clip.sampleRate()))
      .execute();
  }

  record AEC1AudioBytesAndHash(
    byte[] data,
    AUClipDeclaration declaration)
  {

  }
}
