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


package com.io7m.aurantedit.ui.internal.key_assignments;

import com.io7m.aurantedit.ui.internal.AEUnsignedIntValueFactory;
import com.io7m.aurantedit.ui.internal.AEViewType;
import com.io7m.aurantedit.ui.internal.controller.AEControllerType;
import com.io7m.aurantium.api.AUClipDeclaration;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Spinner;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A key assignment addition view.
 */

public final class AEKeyAssignmentAddView implements AEViewType
{
  private final Stage stage;
  private final AEControllerType controller;

  @FXML private Button cancel;
  @FXML private Button create;
  @FXML private ChoiceBox<AUClipDeclaration> clipChoice;
  @FXML private Spinner<Long> keyCenter;

  /**
   * A key assignment addition  view.
   *
   * @param inStage      The stage
   * @param inController The controller
   */

  public AEKeyAssignmentAddView(
    final Stage inStage,
    final AEControllerType inController)
  {
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.controller =
      Objects.requireNonNull(inController, "controller");
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    final var valueFactory = new AEUnsignedIntValueFactory();
    this.keyCenter.setValueFactory(valueFactory);
    this.keyCenter.getValueFactory().setValue(Long.valueOf(30L));
    this.keyCenter.getEditor().textProperty().addListener(valueFactory);

    this.clipChoice.setItems(this.controller.clips());
    this.clipChoice.setConverter(new AEKeyAssignmentClipConverter());

    this.clipChoice.getSelectionModel()
      .selectedItemProperty()
      .addListener((observable, oldValue, newValue) -> this.validate());
  }

  private void validate()
  {
    this.create.setDisable(true);

    if (this.clipChoice.getSelectionModel().getSelectedItem() == null) {
      return;
    }

    this.create.setDisable(false);
  }

  @FXML
  private void onCancelSelected()
  {
    this.stage.close();
  }

  @FXML
  private void onCreateSelected()
  {
    this.controller.keyAssignmentCreate(
      this.clipChoice.getSelectionModel()
        .getSelectedItem(),
      this.keyCenter.getValueFactory()
        .getValue()
    );
    this.stage.close();
  }
}
