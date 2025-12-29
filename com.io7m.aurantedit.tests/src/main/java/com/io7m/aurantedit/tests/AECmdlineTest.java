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

import com.io7m.aurantedit.cmdline.AECMain;
import com.io7m.aurantedit.filemodel.AEFileModels;
import com.io7m.aurantium.api.AUAudioFormatType;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUClipID;
import com.io7m.aurantium.api.AUHashAlgorithm;
import com.io7m.aurantium.api.AUHashValue;
import com.io7m.aurantium.api.AUIdentifier;
import com.io7m.aurantium.api.AUOctetOrder;
import com.io7m.aurantium.api.AUVersion;
import com.io7m.lanark.core.RDottedName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class AECmdlineTest
{
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

  @BeforeEach
  public void setup()
    throws IOException
  {
    this.directory =
      Files.createTempDirectory("aurantedit");
  }

  @Test
  public void testExport()
    throws Exception
  {
    final var clip =
      this.resourceOf("sample_stereo_48k_u8.wav");
    final var file =
      this.directory.resolve("test.auedit");
    final var output =
      this.directory.resolve("out.aam");

    try (var model = AEFileModels.open(file, false)) {
      model.identifierPut(new AUIdentifier(
        new RDottedName("com.io7m.example"),
        new RDottedName("com.io7m.example"),
        new AUVersion(1, 2)
      ));
      model.clipsAdd(List.of(clip)).get();
    }

    final var app =
      new AECMain(new String[]{
        "export",
        "--verbose",
        "trace",
        "--input-aurantedit",
        file.toAbsolutePath().toString(),
        "--output-sample-map",
        output.toAbsolutePath().toString()
      });

    app.run();
    assertEquals(0, app.exitCode());
    AEValidation.validate(output);
  }

  @Test
  public void testImport()
    throws Exception
  {
    final var input =
      this.resourceOf("sample.aam");
    final var output =
      this.directory.resolve("test.auedit");

    final var app =
      new AECMain(new String[]{
        "import",
        "--verbose",
        "trace",
        "--input-sample-map",
        input.toAbsolutePath().toString(),
        "--output-aurantedit",
        output.toAbsolutePath().toString()
      });

    app.run();
    assertEquals(0, app.exitCode());
  }

  @Test
  public void testStatistics()
    throws Exception
  {
    final var input =
      this.resourceOf("sample.aam");
    final var output =
      this.directory.resolve("test.auedit");

    {
      final var app =
        new AECMain(new String[]{
          "import",
          "--verbose",
          "trace",
          "--input-sample-map",
          input.toAbsolutePath().toString(),
          "--output-aurantedit",
          output.toAbsolutePath().toString()
        });

      app.run();
      assertEquals(0, app.exitCode());
    }

    {
      final var app =
        new AECMain(new String[]{
          "statistics",
          "--verbose",
          "trace",
          "--input-aurantedit",
          output.toAbsolutePath().toString()
        });

      app.run();
      assertEquals(0, app.exitCode());
    }
  }

  @Test
  public void testExportVersionUnparseable()
    throws Exception
  {
    final var clip =
      this.resourceOf("sample_stereo_48k_u8.wav");
    final var file =
      this.directory.resolve("test.auedit");
    final var output =
      this.directory.resolve("out.aam");

    try (var model = AEFileModels.open(file, false)) {
      model.identifierPut(new AUIdentifier(
        new RDottedName("com.io7m.example"),
        new RDottedName("com.io7m.example"),
        new AUVersion(1, 2)
      ));
      model.clipsAdd(List.of(clip)).get();
    }

    final var app =
      new AECMain(new String[]{
        "export",
        "--verbose",
        "trace",
        "--input-aurantedit",
        file.toAbsolutePath().toString(),
        "--output-sample-map",
        output.toAbsolutePath().toString(),
        "--output-format-version",
        "x.y"
      });

    app.run();
    assertEquals(1, app.exitCode());
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
