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

import com.io7m.aurantedit.ui.internal.AEViewType;
import com.io7m.aurantedit.ui.internal.controller.AEControllerType;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUKeyAssignmentFlagType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import static java.lang.Boolean.FALSE;

/**
 * The key assignment tab.
 */

public final class AEKeyAssignmentTabView implements AEViewType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AEKeyAssignmentTabView.class);

  private final AEControllerType controller;
  private final AEKeyAssignmentAddDialogs keyAssignmentAddDialogs;

  @FXML private AEKeyAssignmentPianoView pianoViewController;

  @FXML private Accordion keyAssignmentControls;
  @FXML private ChoiceBox<AUKeyAssignment> keyAssignmentChoice;

  @FXML private Slider keyStart;
  @FXML private Slider keyCenter;
  @FXML private Slider keyEnd;
  @FXML private TextField keyStartField;
  @FXML private TextField keyCenterField;
  @FXML private TextField keyEndField;

  @FXML private Slider velocityStart;
  @FXML private Slider velocityCenter;
  @FXML private Slider velocityEnd;
  @FXML private TextField velocityStartField;
  @FXML private TextField velocityCenterField;
  @FXML private TextField velocityEndField;

  @FXML private Slider ampAtVelocityStart;
  @FXML private Slider ampAtVelocityCenter;
  @FXML private Slider ampAtVelocityEnd;
  @FXML private TextField ampAtVelocityStartField;
  @FXML private TextField ampAtVelocityCenterField;
  @FXML private TextField ampAtVelocityEndField;

  @FXML private Slider ampAtKeyStart;
  @FXML private Slider ampAtKeyCenter;
  @FXML private Slider ampAtKeyEnd;
  @FXML private TextField ampAtKeyStartField;
  @FXML private TextField ampAtKeyCenterField;
  @FXML private TextField ampAtKeyEndField;

  @FXML private CheckBox unpitched;
  @FXML private Button keyAssignmentRemove;
  @FXML private Button keyAssignmentAdd;
  @FXML private Pane keyAssignmentNodeArea;

  @FXML private ChoiceBox<AUClipDeclaration> clipChoice;

  /**
   * The key assignment tab.
   *
   * @param services     The application services
   * @param inController The controller
   */

  public AEKeyAssignmentTabView(
    final RPServiceDirectoryType services,
    final AEControllerType inController)
  {
    this.controller =
      Objects.requireNonNull(inController, "controller");
    this.keyAssignmentAddDialogs =
      services.requireService(AEKeyAssignmentAddDialogs.class);
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    /*
     * When the user clicks a key assignment in the piano view, it should
     * have the same effect as if they'd chosen it from the drop-down menu.
     */

    this.pianoViewController.setKeyAssignmentClickHandler(assignmentID -> {
      this.keyAssignmentChoice.getSelectionModel()
        .select(
          this.controller.keyAssignments()
            .stream()
            .filter(x -> Objects.equals(x.id(), assignmentID))
            .findFirst()
            .orElse(null)
        );
    });

    this.clipChoice.converterProperty()
      .set(new AEKeyAssignmentClipConverter());

    this.clipChoice.setItems(this.controller.clips());

    this.keyAssignmentChoice.converterProperty()
      .set(new AEKeyAssignmentConverter());

    this.keyAssignmentChoice.getSelectionModel()
      .selectedItemProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.keyAssignmentControls
          .setDisable(newValue == null);
        this.keyAssignmentRemove
          .setDisable(newValue == null);

        if (newValue == null) {
          return;
        }

        this.clipChoice.getSelectionModel()
          .select(
            this.controller.fileModel()
              .clips()
              .get()
              .stream()
              .filter(c -> Objects.equals(c.id(), newValue.clipId()))
              .findFirst()
              .orElse(null)
          );

        this.keyStart
          .setValue(newValue.keyValueStart());
        this.keyCenter
          .setValue(newValue.keyValueCenter());
        this.keyEnd
          .setValue(newValue.keyValueEnd());

        this.velocityStart
          .setValue(newValue.atVelocityStart());
        this.velocityCenter
          .setValue(newValue.atVelocityCenter());
        this.velocityEnd
          .setValue(newValue.atVelocityEnd());

        this.ampAtKeyCenter
          .setValue(newValue.amplitudeAtKeyCenter());
        this.ampAtKeyStart
          .setValue(newValue.amplitudeAtKeyStart());
        this.ampAtKeyEnd
          .setValue(newValue.amplitudeAtKeyEnd());

        this.ampAtVelocityCenter
          .setValue(newValue.amplitudeAtVelocityCenter());
        this.ampAtVelocityStart
          .setValue(newValue.amplitudeAtVelocityStart());
        this.ampAtVelocityEnd
          .setValue(newValue.amplitudeAtVelocityEnd());
      });

    this.keyStart.valueChangingProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.clampKeySliders();
        this.onKeyAssignmentModified(coerceBoolean(newValue));
      });

    this.keyEnd.valueChangingProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.clampKeySliders();
        this.onKeyAssignmentModified(coerceBoolean(newValue));
      });

    this.keyCenter.valueChangingProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.clampKeySliders();
        this.onKeyAssignmentModified(coerceBoolean(newValue));
      });

    this.velocityStart.valueChangingProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.clampVelocitySliders();
        this.onKeyAssignmentModified(coerceBoolean(newValue));
      });

    this.velocityEnd.valueChangingProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.clampVelocitySliders();
        this.onKeyAssignmentModified(coerceBoolean(newValue));
      });

    this.velocityCenter.valueChangingProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.clampVelocitySliders();
        this.onKeyAssignmentModified(coerceBoolean(newValue));
      });

    this.keyCenterField.textProperty()
      .bind(this.keyCenter.valueProperty().map(AEKeyAssignmentTabView::formatLong));
    this.keyStartField.textProperty()
      .bind(this.keyStart.valueProperty().map(AEKeyAssignmentTabView::formatLong));
    this.keyEndField.textProperty()
      .bind(this.keyEnd.valueProperty().map(AEKeyAssignmentTabView::formatLong));

    this.velocityCenterField.textProperty()
      .bind(this.velocityCenter.valueProperty().map(AEKeyAssignmentTabView::formatReal));
    this.velocityStartField.textProperty()
      .bind(this.velocityStart.valueProperty().map(AEKeyAssignmentTabView::formatReal));
    this.velocityEndField.textProperty()
      .bind(this.velocityEnd.valueProperty().map(AEKeyAssignmentTabView::formatReal));

    this.ampAtKeyCenterField.textProperty()
      .bind(this.ampAtKeyCenter.valueProperty().map(AEKeyAssignmentTabView::formatReal));
    this.ampAtKeyStartField.textProperty()
      .bind(this.ampAtKeyStart.valueProperty().map(AEKeyAssignmentTabView::formatReal));
    this.ampAtKeyEndField.textProperty()
      .bind(this.ampAtKeyEnd.valueProperty().map(AEKeyAssignmentTabView::formatReal));

    this.ampAtVelocityCenterField.textProperty()
      .bind(this.ampAtVelocityCenter.valueProperty().map(AEKeyAssignmentTabView::formatReal));
    this.ampAtVelocityStartField.textProperty()
      .bind(this.ampAtVelocityStart.valueProperty().map(AEKeyAssignmentTabView::formatReal));
    this.ampAtVelocityEndField.textProperty()
      .bind(this.ampAtVelocityEnd.valueProperty().map(AEKeyAssignmentTabView::formatReal));

    this.keyAssignmentChoice.setItems(
      this.controller.keyAssignments()
    );

    this.controller.keyAssignments()
      .addListener(
        (ListChangeListener<? super AUKeyAssignment>)
          this::onKeyAssignmentListChanged
      );
  }

  private void onKeyAssignmentListChanged(
    final ListChangeListener.Change<? extends AUKeyAssignment> c)
  {
    /*
     * If a key assignment is currently selected, and the current list of
     * key assignments has changed in some way, then we may need to update
     * the views. We only need to update anything if the currently selected
     * key assignment is one of the ones that changed.
     */

    final var current = this.currentlySelectedKeyAssignment();
    if (current == null) {
      return;
    }

    while (c.next()) {

      /*
       * If the selected key assignment was replaced, then simply re-selecting
       * it will automatically update all of the dependent views.
       */

      if (c.wasReplaced()) {
        for (final var a : c.getAddedSubList()) {
          if (Objects.equals(current.id(), a.id())) {
            this.keyAssignmentChoice.getSelectionModel()
              .select(a);
          }
        }
        continue;
      } else {
        if (c.wasAdded()) {
          continue;
        }

        /*
         * If the selected key assignment was removed, then we need to select
         * nothing.
         */

        if (c.wasRemoved()) {
          for (final var a : c.getRemoved()) {
            if (Objects.equals(current.id(), a.id())) {
              this.keyAssignmentChoice.getSelectionModel()
                .select(null);
            }
          }
          continue;
        }
      }
    }
  }

  private AUKeyAssignment currentlySelectedKeyAssignment()
  {
    return this.keyAssignmentChoice.getSelectionModel().getSelectedItem();
  }

  private static String formatLong(
    final Number number)
  {
    return Long.toUnsignedString(number.longValue());
  }

  private static Boolean coerceBoolean(
    final Boolean x)
  {
    return x == null ? FALSE : x;
  }

  private void onKeyAssignmentModified(
    final boolean changing)
  {
    if (!changing) {
      this.updateKeyAssignment();
    }
  }

  private void updateKeyAssignment()
  {
    LOG.debug("Updating key assignment.");

    final var existing =
      this.currentlySelectedKeyAssignment();

    final var selectedClip =
      this.clipChoice.getSelectionModel()
        .getSelectedItem();

    final var updated =
      new AUKeyAssignment(
        existing.id(),
        (long) this.keyStart.getValue(),
        (long) this.keyCenter.getValue(),
        (long) this.keyEnd.getValue(),
        selectedClip.id(),
        this.ampAtKeyStart.getValue(),
        this.ampAtKeyCenter.getValue(),
        this.ampAtKeyEnd.getValue(),
        this.velocityStart.getValue(),
        this.velocityCenter.getValue(),
        this.velocityEnd.getValue(),
        this.ampAtVelocityStart.getValue(),
        this.ampAtVelocityCenter.getValue(),
        this.ampAtVelocityEnd.getValue(),
        this.flagsNow()
      );

    this.controller.keyAssignmentUpdate(updated);
  }

  private Set<AUKeyAssignmentFlagType> flagsNow()
  {
    return Set.of();
  }

  /**
   * In Aurantium key assignments, velocity start/center/end values must
   * satisfy {@code start <= center <= end}. We sort the values of the sliders
   * and then clamp them such that they will snap to valid values and always
   * satisfy the above property.
   */

  private void clampVelocitySliders()
  {
    final var velocities = new double[3];
    velocities[0] = this.velocityStart.getValue();
    velocities[1] = this.velocityCenter.getValue();
    velocities[2] = this.velocityEnd.getValue();

    Arrays.sort(velocities);

    this.velocityStart
      .setValue(Math.min(velocities[0], velocities[1]));
    this.velocityEnd
      .setValue(Math.max(velocities[1], velocities[2]));
    this.velocityCenter
      .setValue(
        Math.clamp(
          this.velocityCenter.getValue(),
          this.velocityStart.getValue(),
          this.velocityEnd.getValue())
      );
  }

  /**
   * In Aurantium key assignments, key start/center/end values must
   * satisfy {@code start <= center <= end}. We sort the values of the sliders
   * and then clamp them such that they will snap to valid values and always
   * satisfy the above property.
   */

  private void clampKeySliders()
  {
    final var keys = new long[3];
    keys[0] = (long) this.keyStart.getValue();
    keys[1] = (long) this.keyCenter.getValue();
    keys[2] = (long) this.keyEnd.getValue();

    Arrays.sort(keys);

    this.keyStart
      .setValue(Math.min(keys[0], keys[1]));
    this.keyEnd
      .setValue(Math.max(keys[1], keys[2]));
    this.keyCenter
      .setValue(
        Math.clamp(
          this.keyCenter.getValue(),
          this.keyStart.getValue(),
          this.keyEnd.getValue())
      );
  }

  private static String formatReal(
    final Number value)
  {
    return String.format("%.3f", value.doubleValue());
  }


  @FXML
  private void onKeyAssignmentAddSelected()
    throws IOException
  {
    final var result =
      this.keyAssignmentAddDialogs.createDialog(this.controller);

    result.stage().showAndWait();
  }

  @FXML
  private void onKeyAssignmentRemoveSelected()
  {

  }
}
