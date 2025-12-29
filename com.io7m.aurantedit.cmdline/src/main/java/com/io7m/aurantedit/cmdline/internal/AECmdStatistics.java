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

import com.io7m.aurantedit.filemodel.AEFileModels;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUMetadataValue;
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
import java.util.List;
import java.util.Optional;

import static com.io7m.quarrel.core.QStringType.QConstant;

/**
 * The stats command.
 */

public final class AECmdStatistics implements QCommandType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AECmdStatistics.class);

  private static final QParameterNamed1<Path> INPUT_AURANTEDIT =
    new QParameterNamed1<>(
      "--input-aurantedit",
      List.of(),
      new QConstant("The input aurantedit file."),
      Optional.empty(),
      Path.class
    );

  private final QCommandMetadata metadata;

  /**
   * The import command.
   */

  public AECmdStatistics()
  {
    this.metadata =
      new QCommandMetadata(
        "statistics",
        new QConstant("Show aurantedit file statistics."),
        Optional.empty()
      );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(List.of(
      INPUT_AURANTEDIT
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
        context.parameterValue(INPUT_AURANTEDIT);

      try (var fileModel = AEFileModels.open(inputFile, true)) {
        fileModel.loading().get();

        final List<AUClipDeclaration> clips =
          fileModel.clips().get();
        final List<AUKeyAssignment> keyAssignments =
          fileModel.keyAssignments().get();
        final List<AUMetadataValue> meta =
          fileModel.metadata().get();
        final var undoStack =
          fileModel.undoStack().get();
        final var redoStack =
          fileModel.redoStack().get();

        System.out.printf(
          "Clips: %s%n",
          Integer.toUnsignedString(clips.size())
        );
        System.out.printf(
          "Key Assignments: %s%n",
          Integer.toUnsignedString(keyAssignments.size())
        );
        System.out.printf(
          "Metadata: %s%n",
          Integer.toUnsignedString(meta.size())
        );

        {
          var sizeOctets = 0L;
          for (final var clip : clips) {
            sizeOctets = sizeOctets + clip.size();
          }

          System.out.printf(
            "Audio data: %s MB (%s B)%n",
            Double.valueOf((double) sizeOctets / (double) 1_000_000.0),
            Long.toUnsignedString(sizeOctets)
          );
        }

        System.out.printf(
          "Undo stack: %s entries%n",
          Integer.toUnsignedString(undoStack.size())
        );
        System.out.printf(
          "Redo stack: %s entries%n",
          Integer.toUnsignedString(redoStack.size())
        );
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
