/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.aurantedit.ui.internal;

import com.io7m.jwheatsheaf.api.JWFileChooserAction;
import com.io7m.jwheatsheaf.api.JWFileChooserConfiguration;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * A file import view.
 */

public final class AEImportView implements AEViewType
{
  private final Stage stage;
  private Optional<AEImport> result;
  private final AEFileChoosersType fileChoosers;

  @FXML private Button cancel;
  @FXML private Button create;
  @FXML private TextField sampleMapField;
  @FXML private TextField outputFileField;

  /**
   * A file creation view.
   *
   * @param services The service directory
   * @param inStage  The stage
   */

  public AEImportView(
    final RPServiceDirectoryType services,
    final Stage inStage)
  {
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.fileChoosers =
      services.requireService(AEFileChoosersType.class);
    this.result =
      Optional.empty();
  }

  /**
   * @return The result
   */

  public Optional<AEImport> result()
  {
    return this.result;
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.sampleMapField.textProperty()
      .addListener((observable) -> this.validate());
    this.outputFileField.textProperty()
      .addListener((observable) -> this.validate());

    Platform.runLater(() -> {
      this.cancel.requestFocus();
    });
  }

  private void validate()
  {
    try {
      final var sampleMapFileText =
        this.sampleMapField.getText().trim();
      final var outputFileText =
        this.outputFileField.getText().trim();

      if (sampleMapFileText.isBlank()) {
        throw new IllegalArgumentException();
      }
      if (outputFileText.isBlank()) {
        throw new IllegalArgumentException();
      }

      this.create.setDisable(false);
    } catch (final IllegalArgumentException e) {
      this.create.setDisable(true);
    }
  }

  @FXML
  private void onCreateSelected()
  {
    this.result = Optional.of(
      new AEImport(
        Paths.get(this.sampleMapField.getText()),
        Paths.get(this.outputFileField.getText())
      )
    );
    this.stage.close();
  }

  @FXML
  private void onCancelSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onSampleMapSetSelected()
  {
    final var fileChooser =
      this.fileChoosers.create(
        JWFileChooserConfiguration.builder()
          .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
          .addFileFilters(this.fileChoosers.aurantiumFilter())
          .setFileFilterDefault(this.fileChoosers.aurantiumFilter())
          .build()
      );

    final var newResult = fileChooser.showAndWait();
    if (newResult.isEmpty()) {
      return;
    }

    this.sampleMapField.setText(
      newResult.get(0).toAbsolutePath().toString()
    );
  }

  @FXML
  private void onOutputFileSetSelected()
  {
    final var fileChooser =
      this.fileChoosers.create(
        JWFileChooserConfiguration.builder()
          .setConfirmFileSelection(true)
          .setAction(JWFileChooserAction.CREATE)
          .addFileFilters(this.fileChoosers.aueditFilter())
          .setFileFilterDefault(this.fileChoosers.aueditFilter())
          .build()
      );

    final var newResult = fileChooser.showAndWait();
    if (newResult.isEmpty()) {
      return;
    }

    this.outputFileField.setText(
      newResult.get(0).toAbsolutePath().toString()
    );
  }
}
