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

import com.io7m.aurantedit.filemodel.AECommandExportParameters;
import com.io7m.aurantedit.filemodel.AEFileModels;
import com.io7m.aurantium.api.AUVersion;
import com.io7m.aurantium.writer.api.AUWriterFactoryType;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.ext.logback.QLogback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

import static com.io7m.quarrel.core.QStringType.QConstant;

/**
 * The export command.
 */

public final class AECmdExport implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AECmdExport.class);

  private static final QParameterNamed1<Path> INPUT_AURANTEDIT =
    new QParameterNamed1<>(
      "--input-aurantedit",
      List.of(),
      new QConstant("The input aurantedit file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> OUTPUT_MAP =
    new QParameterNamed1<>(
      "--output-sample-map",
      List.of(),
      new QConstant("The output aurantium sample map file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<AUVersion> FORMAT_VERSION =
    new QParameterNamed1<>(
      "--output-format-version",
      List.of(),
      new QConstant("The output aurantium sample map file format version."),
      Optional.of(new AUVersion(1, 0)),
      AUVersion.class
    );

  private final QCommandMetadata metadata;

  /**
   * The export command.
   */

  public AECmdExport()
  {
    this.metadata =
      new QCommandMetadata(
        "export",
        new QConstant("Export an aurantedit file to a sample map."),
        Optional.empty()
      );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(List.of(
      INPUT_AURANTEDIT,
      OUTPUT_MAP,
      FORMAT_VERSION
    ));
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
  {
    QLogback.configure(context);

    try {
      final var inputFile =
        context.parameterValue(INPUT_AURANTEDIT);
      final var outputFile =
        context.parameterValue(OUTPUT_MAP);
      final var outputFileTemp =
        Paths.get(outputFile + ".tmp");
      final var formatVersion =
        context.parameterValue(FORMAT_VERSION);

      final var writers =
        ServiceLoader.load(AUWriterFactoryType.class)
          .stream()
          .map(ServiceLoader.Provider::get)
          .toList();

      try (var fileModel = AEFileModels.open(inputFile, true)) {
        fileModel.loading().get();
        fileModel.export(
          new AECommandExportParameters(
            writers,
            outputFile,
            outputFileTemp,
            formatVersion
          )
        ).get();
      }

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
