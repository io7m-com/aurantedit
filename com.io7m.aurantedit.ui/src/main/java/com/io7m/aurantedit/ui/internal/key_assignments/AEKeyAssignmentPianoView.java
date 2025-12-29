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
import com.io7m.aurantium.api.AUKeyAssignment;
import com.io7m.aurantium.api.AUKeyAssignmentID;
import com.io7m.ivoirax.core.IvHorizontalPiano;
import com.io7m.ivoirax.core.IvKeyEnter;
import com.io7m.ivoirax.core.IvKeyEventType;
import com.io7m.ivoirax.core.IvKeyExit;
import com.io7m.ivoirax.core.IvKeyPressed;
import com.io7m.ivoirax.core.IvKeyReleased;
import com.io7m.jaffirm.core.Preconditions;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * The piano view for key assignments.
 */

public final class AEKeyAssignmentPianoView implements AEViewType
{
  private final AEControllerType controller;
  private final HashMap<AUKeyAssignmentID, KeyAssignmentNode> assignmentNodes;
  private Consumer<AUKeyAssignmentID> keyAssignmentClickHandler;

  @FXML private IvHorizontalPiano piano;
  @FXML private Label keyOverText;
  @FXML private Label keyAssignmentOverText;
  @FXML private Pane keyAssignmentNodeArea;

  /**
   * The piano view for key assignments.
   *
   * @param inController The controller
   */

  public AEKeyAssignmentPianoView(
    final AEControllerType inController)
  {
    this.controller =
      Objects.requireNonNull(inController, "controller");
    this.assignmentNodes =
      new HashMap<>();
    this.keyAssignmentClickHandler =
      (final AUKeyAssignmentID k) -> {
        // Nothing yet.
      };
  }

  /**
   * Set the click handler for key assignments.
   *
   * @param handler The handler
   */

  public void setKeyAssignmentClickHandler(
    final Consumer<AUKeyAssignmentID> handler)
  {
    this.keyAssignmentClickHandler =
      Objects.requireNonNull(handler, "handler");
  }

  /**
   * @return The current click handler for key assignments
   */

  public Consumer<AUKeyAssignmentID> keyAssignmentClickHandler()
  {
    return this.keyAssignmentClickHandler;
  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.piano.naturalKeyWidthProperty()
      .set(32.0);
    this.piano.setOnKeyEventHandler(
      this::onPianoKeyboardEvent);

    this.keyAssignmentNodeArea.heightProperty()
      .subscribe((ignored0, ignored1) -> {
        this.onKeyAssignmentNodeAreaHeightChanged();
      });

    this.controller.keyAssignments()
      .addListener(
        (ListChangeListener<? super AUKeyAssignment>)
          this::onKeyAssignmentsChanged
      );

    this.controller.keyAssignments()
      .forEach(this::createKeyAssignmentNode);
  }

  private void onKeyAssignmentNodeAreaHeightChanged()
  {
    for (final var assignment : this.controller.keyAssignments()) {
      this.updateAssignmentNodeFor(assignment);
    }
  }

  private void updateAssignmentNodeFor(
    final AUKeyAssignment assignment)
  {
    final var node =
      this.assignmentNodes.get(assignment.id());

    if (node != null) {
      node.updateFor(assignment);
    }
  }

  private void createKeyAssignmentNode(
    final AUKeyAssignment keyAssignment)
  {
    final var node =
      new KeyAssignmentNode(this, keyAssignment);

    this.assignmentNodes
      .put(keyAssignment.id(), node);
    this.keyAssignmentNodeArea.getChildren()
      .addAll(node.nodes());
  }

  private void onKeyAssignmentsChanged(
    final ListChangeListener.Change<? extends AUKeyAssignment> c)
  {
    while (c.next()) {
      if (c.wasReplaced()) {
        for (final var a : c.getAddedSubList()) {
          this.updateAssignmentNodeFor(a);
        }
        continue;
      } else {
        if (c.wasAdded()) {
          for (final var a : c.getAddedSubList()) {
            this.createKeyAssignmentNode(a);
          }
          continue;
        }
        if (c.wasRemoved()) {
          for (final var a : c.getRemoved()) {
            this.removeKeyAssignmentNode(a);
          }
          continue;
        }
      }
    }
  }

  private void removeKeyAssignmentNode(
    final AUKeyAssignment assignment)
  {
    final var node =
      this.assignmentNodes.get(assignment.id());
    this.keyAssignmentNodeArea.getChildren()
      .removeAll(node.nodes());

    this.assignmentNodes.remove(assignment.id());
  }

  private void onPianoKeyboardEvent(
    final IvKeyEventType event)
  {
    switch (event) {
      case final IvKeyPressed pressed -> {
        this.keyOverText.setText(Integer.toUnsignedString(pressed.index()));
      }
      case final IvKeyReleased released -> {
        this.keyOverText.setText(Integer.toUnsignedString(released.index()));
      }
      case final IvKeyEnter enter -> {
        this.keyOverText.setText(Integer.toUnsignedString(enter.index()));
      }
      case final IvKeyExit exit -> {
        this.keyOverText.setText("");
      }
    }
  }

  private static final class KeyAssignmentNode
  {
    private final AUKeyAssignmentID id;
    private final Rectangle rectangleBase;
    private final Line centerKeyLine;
    private final AEKeyAssignmentPianoView piano;
    private final HashSet<Node> nodes;
    private final Line centerVelocityLine;

    private final Paint centerLineKey =
      Color.color(1.0, 0.0, 0.0, 0.4);
    private final Paint centerLineVelocity =
      Color.color(0.0, 1.0, 0.0, 0.4);
    private final Paint baseNormal =
      Color.color(1.0, 1.0, 1.0, 0.1);
    private final Paint baseOver =
      Color.color(1.0, 1.0, 1.0, 0.2);

    KeyAssignmentNode(
      final AEKeyAssignmentPianoView inPiano,
      final AUKeyAssignment assignment)
    {
      Objects.requireNonNull(assignment, "assignment");
      this.piano =
        Objects.requireNonNull(inPiano, "piano");

      this.id = assignment.id();
      this.nodes = new HashSet<Node>();

      this.rectangleBase = new Rectangle();
      this.rectangleBase.setBlendMode(BlendMode.ADD);
      this.rectangleBase.setFill(this.baseNormal);
      this.rectangleBase.setStroke(Color.WHITE);
      this.rectangleBase.setOnMouseEntered(event -> {
        this.rectangleBase.setFill(this.baseOver);
        this.piano.keyAssignmentOverText.setText(this.id.toString());
      });
      this.rectangleBase.setOnMouseExited(event -> {
        this.rectangleBase.setFill(this.baseNormal);
        this.piano.keyAssignmentOverText.setText("");
      });
      this.rectangleBase.setOnMouseClicked(event -> {
        this.piano.keyAssignmentClickHandler().accept(this.id);
      });
      this.nodes.add(this.rectangleBase);

      this.centerKeyLine = new Line();
      this.centerKeyLine.setBlendMode(BlendMode.ADD);
      this.centerKeyLine.setStroke(this.centerLineKey);
      this.centerKeyLine.setMouseTransparent(true);
      this.nodes.add(this.centerKeyLine);

      this.centerVelocityLine = new Line();
      this.centerVelocityLine.setBlendMode(BlendMode.ADD);
      this.centerVelocityLine.setStroke(this.centerLineVelocity);
      this.centerVelocityLine.setMouseTransparent(true);
      this.nodes.add(this.centerVelocityLine);

      this.updateFor(assignment);
    }

    void updateFor(
      final AUKeyAssignment assignment)
    {
      Preconditions.checkPreconditionV(
        Objects.equals(this.id, assignment.id()),
        "Assignment ID must match."
      );

      final var maxHeight =
        this.piano.keyAssignmentNodeArea.getHeight();

      final var xStart =
        this.piano.piano.xPositionOf(
          Math.toIntExact(assignment.keyValueStart())
        );
      final var xCenter =
        this.piano.piano.xPositionOf(
          Math.toIntExact(assignment.keyValueCenter())
        );
      final var xEnd =
        this.piano.piano.xPositionOf(
          Math.toIntExact(assignment.keyValueEnd()) + 1
        );

      /*
       * JavaFX's coordinate system has the origin at the top left, but we're
       * arranging key assignments such that velocity is on the Y axis, with
       * the lowest velocity 0.0 being at the bottom left.
       *
       * The atVelocityStart() value will be less than or equal to the
       * atVelocityEnd() value. We need to transform both
       * values such that atVelocityEnd() appears at the top of the
       * view, and atVelocityStart() appears below it.
       */

      final var yTop =
        maxHeight - (assignment.atVelocityEnd() * maxHeight);
      final var yCenter =
        maxHeight - (assignment.atVelocityCenter() * maxHeight);
      final var yBottom =
        maxHeight - (assignment.atVelocityStart() * maxHeight);

      this.rectangleBase.setLayoutX(xStart);
      this.rectangleBase.setLayoutY(yTop);
      this.rectangleBase.setWidth(xEnd - xStart);
      this.rectangleBase.setHeight(yBottom - yTop);

      this.centerKeyLine.setStartY(yTop);
      this.centerKeyLine.setEndY(yBottom);
      this.centerKeyLine.setStartX(xCenter);
      this.centerKeyLine.setEndX(xCenter);

      this.centerVelocityLine.setStartY(yCenter);
      this.centerVelocityLine.setEndY(yCenter);
      this.centerVelocityLine.setStartX(xStart);
      this.centerVelocityLine.setEndX(xEnd);
    }

    public Collection<Node> nodes()
    {
      return this.nodes;
    }
  }
}
