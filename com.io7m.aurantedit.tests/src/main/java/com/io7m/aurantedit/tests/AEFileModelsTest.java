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

package com.io7m.aurantedit.tests;

import com.io7m.aurantedit.filemodel.AECommandExportParameters;
import com.io7m.aurantedit.filemodel.AECommandImportParameters;
import com.io7m.aurantedit.filemodel.AEFileModels;
import com.io7m.aurantium.api.AUAudioFormatType;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUException;
import com.io7m.aurantium.api.AUHashAlgorithm;
import com.io7m.aurantium.api.AUHashValue;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUKeyAssignmentID;
import com.io7m.aurantium.api.AUMetadataValue;
import com.io7m.aurantium.api.AUOctetOrder;
import com.io7m.aurantium.api.AUVersion;
import com.io7m.aurantium.parser.api.AUParseRequest;
import com.io7m.aurantium.validation.api.AUValidationRequest;
import com.io7m.aurantium.vanilla.AU1Parsers;
import com.io7m.aurantium.vanilla.AU1Validators;
import com.io7m.aurantium.vanilla.AU1Writers;
import com.io7m.lanark.core.RDottedName;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.io7m.aurantium.api.AUKeyAssignmentFlagType.AUKeyAssignmentFlagStandard.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class AEFileModelsTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AEFileModelsTest.class);

  private static final AUClipDeclaration SAMPLE_STEREO_48K_U8_FLAC =
    new AUClipDeclaration(
      new AUClipID(0L),
      "sample_stereo_48k_u8.flac",
      AUAudioFormatType.AUAudioFormatStandard.AFFlac,
      48000L,
      8L,
      2L,
      AUOctetOrder.LITTLE_ENDIAN,
      new AUHashValue(
        AUHashAlgorithm.HA_SHA256,
        "6b7dbefe541ab13218b9352915420f8b3a52fc998161f07fab370aaca515d255"
      ),
      48342L,
      Optional.empty()
    );

  private static final AUClipDeclaration SAMPLE_STEREO_48K_U8_WAV =
    new AUClipDeclaration(
      new AUClipID(0L),
      "sample_stereo_48k_u8.wav",
      AUAudioFormatType.AUAudioFormatStandard.AFPCMLinearIntegerUnsigned,
      48000L,
      8L,
      2L,
      AUOctetOrder.LITTLE_ENDIAN,
      new AUHashValue(
        AUHashAlgorithm.HA_SHA256,
        "bfc90d0645ac96a81b0c372e611a6956c1dd8a9e5e5a499d5d2aa099dd1c5eff"
      ),
      216090L,
      Optional.empty()
    );

  private Path directory;
  private Path dbFile;

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory =
      Files.createTempDirectory("aurantedit");
    this.dbFile =
      this.directory.resolve("file.aedb");
  }

  @AfterEach
  public void tearDown()
  {
    try {
      FileUtils.deleteDirectory(this.directory.toFile());
    } catch (final Throwable e) {
      // Don't care.
    }
  }

  @Test
  public void testOpenEmpty()
    throws Exception
  {
    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      assertEquals(
        List.of(),
        file.undoStack().get()
      );
      assertEquals(
        List.of(),
        file.redoStack().get()
      );
      assertEquals(
        Optional.empty(),
        file.identifier().get()
      );
      assertEquals(
        List.of(),
        file.metadata().get()
      );
      assertEquals(
        List.of(),
        file.clips().get()
      );

      file.compact().get();
    }
  }

  @Test
  public void testClipAddWAV()
    throws Exception
  {
    final var clip =
      this.resourceOf("sample_stereo_48k_u8.wav");

    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      file.clipsAdd(List.of(clip)).get();
      assertEquals(
        SAMPLE_STEREO_48K_U8_WAV,
        file.clips().get().getFirst()
      );

      file.undo().get();
      assertEquals(
        List.of(),
        file.clips().get()
      );

      file.redo().get();
      assertEquals(
        SAMPLE_STEREO_48K_U8_WAV,
        file.clips().get().getFirst()
      );

      file.clipDelete(file.clips().get().getFirst().id()).get();
      assertEquals(
        List.of(),
        file.clips().get()
      );

      file.undo().get();
      assertEquals(
        SAMPLE_STEREO_48K_U8_WAV,
        file.clips().get().getFirst()
      );

      file.redo().get();
      assertEquals(
        List.of(),
        file.clips().get()
      );

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testClipAddFLAC()
    throws Exception
  {
    final var clip =
      this.resourceOf("sample_stereo_48k_u8.flac");

    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      file.clipsAdd(List.of(clip)).get();
      assertEquals(
        SAMPLE_STEREO_48K_U8_FLAC,
        file.clips().get().getFirst()
      );

      file.undo().get();
      assertEquals(
        List.of(),
        file.clips().get()
      );

      file.redo().get();
      assertEquals(
        SAMPLE_STEREO_48K_U8_FLAC,
        file.clips().get().getFirst()
      );

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testClipReplace()
    throws Exception
  {
    final var clipWav =
      this.resourceOf("sample_stereo_48k_u8.wav");
    final var clipFlac =
      this.resourceOf("sample_stereo_48k_u8.flac");

    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      file.clipsAdd(List.of(clipWav)).get();

      final AUClipDeclaration clip0 = file.clips().get().getFirst();
      assertEquals(SAMPLE_STEREO_48K_U8_WAV, clip0);

      file.clipReplace(clip0.id(), clipFlac).get();
      assertEquals(SAMPLE_STEREO_48K_U8_FLAC, file.clips().get().getFirst());

      file.undo().get();
      assertEquals(SAMPLE_STEREO_48K_U8_WAV, file.clips().get().getFirst());

      file.redo().get();
      assertEquals(SAMPLE_STEREO_48K_U8_FLAC, file.clips().get().getFirst());

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testClipReplaceNonexistent()
    throws Exception
  {
    final var clip =
      this.resourceOf("sample_stereo_48k_u8.wav");

    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      final var ex =
        assertInstanceOf(
          AUException.class,
          assertThrows(
            ExecutionException.class, () -> {
              file.clipReplace(new AUClipID(0L), clip).get();
            }).getCause()
        );

      assertEquals("error-clip-nonexistent", ex.errorCode());
    }
  }

  @Test
  public void testOpenClipNonexistent()
    throws Exception
  {
    final var clip =
      this.directory.resolve("nonexistent.flac");

    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      final var ex =
        assertInstanceOf(
          AUException.class,
          assertThrows(
            ExecutionException.class, () -> {
              file.clipsAdd(List.of(clip)).get();
            }).getCause()
        );

      assertEquals("error-file-nonexistent", ex.errorCode());
    }
  }

  @Test
  public void testKeyAssignmentAddUpdate()
    throws Exception
  {
    final var clip =
      this.resourceOf("sample_stereo_48k_u8.wav");

    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();
      file.clipsAdd(List.of(clip)).get();

      final var k0 =
        new AUKeyAssignment(
          new AUKeyAssignmentID(1L),
          0L,
          64L,
          128L,
          file.clips().get().get(0).id(),
          0.0,
          0.125,
          0.25,
          0.375,
          0.5,
          0.625,
          0.75,
          0.875,
          1.0,
          Set.of(FlagUnpitched)
        );

      final var k1 =
        new AUKeyAssignment(
          new AUKeyAssignmentID(1L),
          0L,
          32L,
          64L,
          file.clips().get().get(0).id(),
          0.0,
          0.125,
          0.25,
          0.375,
          0.5,
          0.625,
          0.75,
          0.875,
          1.0,
          Set.of()
        );

      file.keyAssignmentPut(k0).get();
      assertEquals(List.of(k0), file.keyAssignments().get());
      file.undo().get();
      assertEquals(List.of(), file.keyAssignments().get());
      file.redo().get();
      assertEquals(List.of(k0), file.keyAssignments().get());

      file.keyAssignmentPut(k1).get();
      assertEquals(List.of(k1), file.keyAssignments().get());
      file.undo().get();
      assertEquals(List.of(k0), file.keyAssignments().get());
      file.redo().get();
      assertEquals(List.of(k1), file.keyAssignments().get());

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testMetadataUpdate()
    throws Exception
  {
    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      final List<AUMetadataValue> m0 = List.of(
        new AUMetadataValue("X", "A"),
        new AUMetadataValue("X", "B"),
        new AUMetadataValue("X", "C"),
        new AUMetadataValue("Y", "A"),
        new AUMetadataValue("Z", "A")
      );

      final List<AUMetadataValue> m1 = List.of(
        new AUMetadataValue("X", "A"),
        new AUMetadataValue("Y", "A"),
        new AUMetadataValue("Z", "A"),
        new AUMetadataValue("Z", "B"),
        new AUMetadataValue("Z", "C")
      );

      file.metadataPut(m0).get();
      assertEquals(m0, file.metadata().get());
      file.undo().get();
      assertEquals(List.of(), file.metadata().get());
      file.redo().get();
      assertEquals(m0, file.metadata().get());

      file.metadataPut(m1).get();
      assertEquals(m1, file.metadata().get());
      file.undo().get();
      assertEquals(m0, file.metadata().get());
      file.redo().get();
      assertEquals(m1, file.metadata().get());

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testMetadataRemove()
    throws Exception
  {
    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      final List<AUMetadataValue> mBefore = List.of(
        new AUMetadataValue("X", "A"),
        new AUMetadataValue("X", "B"),
        new AUMetadataValue("X", "B"),
        new AUMetadataValue("X", "C"),
        new AUMetadataValue("Y", "A"),
        new AUMetadataValue("Z", "A")
      );

      final List<AUMetadataValue> mAfter = List.of(
        new AUMetadataValue("X", "A"),
        new AUMetadataValue("X", "B"),
        new AUMetadataValue("X", "C"),
        new AUMetadataValue("Y", "A"),
        new AUMetadataValue("Z", "A")
      );

      file.metadataPut(mBefore).get();
      assertEquals(mBefore, file.metadata().get());

      file.metadataRemove(new AUMetadataValue("X", "B")).get();
      assertEquals(mAfter, file.metadata().get());

      file.undo().get();
      assertEquals(mBefore, file.metadata().get());

      file.redo().get();
      assertEquals(mAfter, file.metadata().get());

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testMetadataReplace()
    throws Exception
  {
    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      final List<AUMetadataValue> mBefore = List.of(
        new AUMetadataValue("X", "A"),
        new AUMetadataValue("X", "B"),
        new AUMetadataValue("X", "C"),
        new AUMetadataValue("Y", "A"),
        new AUMetadataValue("Z", "A")
      );

      final List<AUMetadataValue> mAfter = List.of(
        new AUMetadataValue("X", "A"),
        new AUMetadataValue("X", "B"),
        new AUMetadataValue("X", "Z"),
        new AUMetadataValue("Y", "A"),
        new AUMetadataValue("Z", "A")
      );

      file.metadataPut(mBefore).get();
      assertEquals(mBefore, file.metadata().get());

      file.metadataReplace(
        new AUMetadataValue("X", "C"),
        new AUMetadataValue("X", "Z")
      ).get();
      assertEquals(mAfter, file.metadata().get());

      file.undo().get();
      assertEquals(mBefore, file.metadata().get());

      file.redo().get();
      assertEquals(mAfter, file.metadata().get());

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testMetadataAdd()
    throws Exception
  {
    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      final List<AUMetadataValue> mBefore = List.of(
        new AUMetadataValue("X", "A"),
        new AUMetadataValue("X", "B"),
        new AUMetadataValue("X", "C"),
        new AUMetadataValue("Y", "A"),
        new AUMetadataValue("Z", "A")
      );

      final List<AUMetadataValue> mAfter = List.of(
        new AUMetadataValue("X", "A"),
        new AUMetadataValue("X", "B"),
        new AUMetadataValue("X", "C"),
        new AUMetadataValue("X", "C"),
        new AUMetadataValue("Y", "A"),
        new AUMetadataValue("Z", "A")
      );

      file.metadataPut(mBefore).get();
      assertEquals(mBefore, file.metadata().get());

      file.metadataAdd(
        new AUMetadataValue("X", "C")
      ).get();
      assertEquals(mAfter, file.metadata().get());

      file.undo().get();
      assertEquals(mBefore, file.metadata().get());

      file.redo().get();
      assertEquals(mAfter, file.metadata().get());

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testIdentifierUpdate()
    throws Exception
  {
    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();

      final AUIdentifier id0 =
        new AUIdentifier(
          new RDottedName("com.io7m.example"),
          new RDottedName("com.io7m.map"),
          new AUVersion(2, 1)
        );

      final AUIdentifier id1 =
        new AUIdentifier(
          new RDottedName("com.io7m.grouch"),
          new RDottedName("com.io7m.bigbird"),
          new AUVersion(3, 2)
        );

      file.identifierPut(id0).get();
      assertEquals(Optional.of(id0), file.identifier().get());
      file.undo().get();
      assertEquals(Optional.empty(), file.identifier().get());
      file.redo().get();
      assertEquals(Optional.of(id0), file.identifier().get());

      file.identifierPut(id1).get();
      assertEquals(Optional.of(id1), file.identifier().get());
      file.undo().get();
      assertEquals(Optional.of(id0), file.identifier().get());
      file.redo().get();
      assertEquals(Optional.of(id1), file.identifier().get());

      file.compact().get();
      assertEquals(0, file.undoStack().get().size());
    }
  }

  @Test
  public void testExport()
    throws Exception
  {
    final var clip =
      this.resourceOf("sample_stereo_48k_u8.wav");

    final AUIdentifier id0 =
      new AUIdentifier(
        new RDottedName("com.io7m.example"),
        new RDottedName("com.io7m.map"),
        new AUVersion(2, 1)
      );

    final var k0 =
      new AUKeyAssignment(
        new AUKeyAssignmentID(1L),
        0L,
        64L,
        128L,
        new AUClipID(0L),
        0.0,
        0.125,
        0.25,
        0.375,
        0.5,
        0.625,
        0.75,
        0.875,
        1.0,
        Set.of(FlagUnpitched)
      );

    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.loading().get();
      file.clipsAdd(List.of(clip)).get();
      file.identifierPut(id0).get();
      file.keyAssignmentPut(k0).get();
      file.export(
        new AECommandExportParameters(
          List.of(new AU1Writers()),
          this.directory.resolve("out.aam"),
          this.directory.resolve("out.aam.tmp"),
          new AUVersion(1, 0)
        )
      ).get();
      validate(this.directory.resolve("out.aam"));
    }
  }

  private static void validate(
    final Path path)
    throws Exception
  {
    final var readers =
      new AU1Parsers();
    final var validators =
      new AU1Validators();

    try (var channel = FileChannel.open(path)) {
      try (var reader = readers.createParser(
        new AUParseRequest(
          channel,
          path.toUri()
        )
      )) {
        final var file = reader.execute();
        final var validator = validators.createValidator(
          new AUValidationRequest(
            file,
            path.toUri()
          )
        );
        final var errors = validator.execute();
        for (final var error : errors) {
          LOG.debug("Error: {}", error);
        }
        assertEquals(0, errors.size());
      }
    }
  }

  @Test
  public void testImport()
    throws Exception
  {
    final var sampleMap =
      this.resourceOf("sample.aam");

    try (var file = AEFileModels.open(this.dbFile, false)) {
      file.importFrom(new AECommandImportParameters(sampleMap)).get();

      final var clips =
        file.clips().get();
      final var keyAssignments =
        file.keyAssignments().get();
      final var metadata =
        file.metadata().get();
      final var identifier =
        file.identifier().get().get();

      assertEquals(12, clips.size());
      assertEquals(12, keyAssignments.size());
      assertEquals(1, metadata.size());
      assertEquals(
        new AUIdentifier(
          new RDottedName("com.io7m.example_group"),
          new RDottedName("com.io7m.example"),
          new AUVersion(1, 0)
        ),
        identifier
      );
    }
  }

  private Path resourceOf(
    final String name)
    throws IOException
  {
    final var path =
      "/com/io7m/aurantedit/tests/%s".formatted(name);
    final var url =
      AEFileModelsTest.class.getResource(path);

    Objects.requireNonNull(url, "URL");
    try (var stream = url.openStream()) {
      final var output = this.directory.resolve(name);
      Files.copy(stream, output, StandardCopyOption.REPLACE_EXISTING);
      return output;
    }
  }
}
