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

package com.io7m.aurantedit.cmdline.internal;

import com.io7m.aurantedit.filemodel.AECommandImportParameters;
import com.io7m.aurantedit.filemodel.AEFileModels;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.ext.logback.QLogback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import static com.io7m.quarrel.core.QStringType.QConstant;

/**
 * The import command.
 */

public final class AECmdImport implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AECmdImport.class);

  private static final QParameterNamed1<Path> INPUT_MAP =
    new QParameterNamed1<>(
      "--input-sample-map",
      List.of(),
      new QConstant("The input sample map file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> OUTPUT_AURANTEDIT =
    new QParameterNamed1<>(
      "--output-aurantedit",
      List.of(),
      new QConstant("The output aurantedit file."),
      Optional.empty(),
      Path.class
    );

  private final QCommandMetadata metadata;

  /**
   * The import command.
   */

  public AECmdImport()
  {
    this.metadata =
      new QCommandMetadata(
        "import",
        new QConstant("Import a sample map to an aurantedit file."),
        Optional.empty()
      );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(List.of(
      INPUT_MAP,
      OUTPUT_AURANTEDIT
    ));
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    QLogback.configure(context);

    try {
      final var inputFile =
        context.parameterValue(INPUT_MAP);
      final var outputFile =
        context.parameterValue(OUTPUT_AURANTEDIT);
      final var outputFileTemp =
        Paths.get(outputFile + ".tmp");

      Files.deleteIfExists(outputFileTemp);

      try (var fileModel = AEFileModels.open(outputFileTemp, false)) {
        fileModel.loading().get();
        fileModel.importFrom(new AECommandImportParameters(inputFile)).get();
      }

      Files.move(
        outputFileTemp,
        outputFile,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE
      );

      return QCommandStatus.SUCCESS;
    } catch (final Exception e) {
      AELogging.logException(LOG, e);
      return QCommandStatus.FAILURE;
    }
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
