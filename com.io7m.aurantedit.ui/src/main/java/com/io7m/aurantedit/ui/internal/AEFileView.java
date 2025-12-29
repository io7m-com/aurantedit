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

import com.io7m.aurantedit.filemodel.AEFileModelEventError;
import com.io7m.aurantedit.filemodel.AEFileModelEventProgress;
import com.io7m.aurantedit.filemodel.AEFileModelEventType;
import com.io7m.aurantedit.filemodel.AEProgress;
import com.io7m.aurantedit.ui.AEApplication;
import com.io7m.aurantedit.ui.internal.AEApplicationEventType.ExitRequested;
import com.io7m.aurantedit.ui.internal.controller.AEController;
import com.io7m.aurantedit.ui.internal.controller.AEControllerType;
import com.io7m.aurantedit.ui.internal.database.AEDatabaseType;
import com.io7m.aurantedit.ui.internal.database.AERecentFileAddType;
import com.io7m.aurantedit.ui.internal.key_assignments.AEKeyAssignmentPianoView;
import com.io7m.aurantedit.ui.internal.key_assignments.AEKeyAssignmentTabView;
import com.io7m.aurantium.api.AUClipDeclaration;
import com.io7m.aurantium.api.AUMetadataValue;
import com.io7m.brackish.core.WaveView;
import com.io7m.darco.api.DDatabaseException;
import com.io7m.jwheatsheaf.api.JWFileChooserAction;
import com.io7m.jwheatsheaf.api.JWFileChooserConfiguration;
import com.io7m.miscue.fx.seltzer.MSErrorDialogs;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import com.io7m.seltzer.api.SStructuredErrorType;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.io7m.aurantedit.ui.internal.AEStringConstants.AE_REDO;
import static com.io7m.aurantedit.ui.internal.AEStringConstants.AE_REDO_NAMED;
import static com.io7m.aurantedit.ui.internal.AEStringConstants.AE_TITLE;
import static com.io7m.aurantedit.ui.internal.AEStringConstants.AE_UNDO;
import static com.io7m.aurantedit.ui.internal.AEStringConstants.AE_UNDO_NAMED;
import static com.io7m.aurantedit.ui.internal.AEUIThread.onUIThread;
import static com.io7m.aurantedit.ui.internal.AEUnit.UNIT;

/**
 * A file view.
 */

public final class AEFileView implements AEViewType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AEApplication.class);

  private final AEFileChoosersType choosers;
  private final AEStringsType strings;
  private final AEWaveModel waveModel;
  private final Stage stage;
  private final AEVersionSetDialogs versionSet;
  private final ChangeListener<Optional<String>> undoListener;
  private final ChangeListener<Optional<String>> redoListener;
  private final RPServiceDirectoryType services;
  private final AEControllerType controller;
  private final AECreateDialogs createDialogs;
  private final AENameSetDialogs nameSet;
  private final AEMetadataEditDialogs metadataEditDialogs;
  private final AEDatabaseType database;
  private final AEFileWindowTrackerType windowTracker;
  private final AEPerpetualSubscriber<AEApplicationEventType> appEventSubscriber;
  private final AEApplicationEventsType appEvents;
  private final AEImportDialogs importDialogs;

  @FXML private MenuItem undo;
  @FXML private MenuItem redo;

  @FXML private Button error;

  @FXML private TextField status;
  @FXML private ProgressBar progressMajor;
  @FXML private ProgressBar progressMinor;

  /*
   * Metadata tab.
   */

  @FXML private TextField metaGroup;
  @FXML private TextField metaName;
  @FXML private TextField metaVersionMajor;
  @FXML private TextField metaVersionMinor;
  @FXML private TextField metaSearch;
  @FXML private Button metaAdd;
  @FXML private Button metaRemove;
  @FXML private TableView<AUMetadataValue> metaTable;
  @FXML private TableColumn<AUMetadataValue, String> metaTableName;
  @FXML private TableColumn<AUMetadataValue, String> metaTableValue;

  /*
   * Clips tab.
   */

  @FXML private Button clipAdd;
  @FXML private Button clipRemove;
  @FXML private Button clipReplace;
  @FXML private TextField clipSearch;
  @FXML private TextField fieldClipName;
  @FXML private TextField fieldClipID;
  @FXML private TextField fieldClipFormat;
  @FXML private TextField fieldClipEndianness;
  @FXML private TextField fieldClipHash;
  @FXML private TextField fieldClipSampleRate;
  @FXML private TextField fieldClipSampleDepth;
  @FXML private TextField fieldClipChannels;
  @FXML private TextField fieldClipSize;
  @FXML private AnchorPane waveCanvasHolder;

  /*
   * Menu and tabs.
   */

  @FXML private TabPane fileTabs;
  @FXML private Tab fileTabMetadata;
  @FXML private Tab fileTabClips;
  @FXML private Tab fileTabKeyAssignments;
  @FXML private MenuItem itemNew;
  @FXML private MenuItem itemOpen;
  @FXML private MenuItem itemClose;
  @FXML private MenuItem itemImport;
  @FXML private MenuItem itemExit;

  @FXML private TableView<AUClipDeclaration> clipTable;
  @FXML private TableColumn<AUClipDeclaration, String> clipTableID;
  @FXML private TableColumn<AUClipDeclaration, String> clipTableReferences;
  @FXML private TableColumn<AUClipDeclaration, String> clipTableName;

  private Set<TextField> fieldsClip;
  private WaveView waveView;
  private SStructuredErrorType<String> errorLast;

  /**
   * A file view.
   *
   * @param inServices   The service directory
   * @param inStage      The stage
   * @param inController The controller
   */

  public AEFileView(
    final RPServiceDirectoryType inServices,
    final Stage inStage,
    final AEControllerType inController)
  {
    this.services =
      Objects.requireNonNull(inServices, "services");
    this.stage =
      Objects.requireNonNull(inStage, "stage");
    this.database =
      inServices.requireService(AEDatabaseType.class);
    this.choosers =
      inServices.requireService(AEFileChoosersType.class);
    this.strings =
      inServices.requireService(AEStringsType.class);
    this.versionSet =
      inServices.requireService(AEVersionSetDialogs.class);
    this.nameSet =
      inServices.requireService(AENameSetDialogs.class);
    this.createDialogs =
      inServices.requireService(AECreateDialogs.class);
    this.metadataEditDialogs =
      inServices.requireService(AEMetadataEditDialogs.class);
    this.windowTracker =
      inServices.requireService(AEFileWindowTrackerType.class);
    this.appEvents =
      inServices.requireService(AEApplicationEventsType.class);
    this.importDialogs =
      inServices.requireService(AEImportDialogs.class);

    this.controller =
      Objects.requireNonNull(inController, "inController");
    this.waveModel =
      new AEWaveModel();

    this.undoListener =
      (observable, oldValue, newValue) -> {
        this.undo.setDisable(newValue.isEmpty());

        if (newValue.isPresent()) {
          this.undo.setText(this.strings.format(AE_UNDO_NAMED, newValue.get()));
        } else {
          this.undo.setText(this.strings.format(AE_UNDO));
        }
      };

    this.redoListener =
      (observable, oldValue, newValue) -> {
        this.redo.setDisable(newValue.isEmpty());

        if (newValue.isPresent()) {
          this.redo.setText(this.strings.format(AE_REDO_NAMED, newValue.get()));
        } else {
          this.redo.setText(this.strings.format(AE_REDO));
        }
      };

    this.stage.addEventFilter(
      WindowEvent.WINDOW_CLOSE_REQUEST,
      event -> {
        LOG.debug("Window close requested.");
        event.consume();
      });

    this.windowTracker.register(this.stage);

    this.appEventSubscriber =
      new AEPerpetualSubscriber<>(this::onApplicationEvent);
    this.appEvents.events()
      .subscribe(this.appEventSubscriber);
  }

  /**
   * Open a new file view for the given stage.
   *
   * @param services The service directory
   * @param stage    The stage
   *
   * @return A view and stage
   *
   * @throws Exception On errors
   */

  public static AEViewAndStage<AEFileView> openForStage(
    final RPServiceDirectoryType services,
    final Stage stage)
    throws Exception
  {
    final var strings =
      services.requireService(AEStringsType.class);

    final var xml =
      AEFileView.class.getResource(
        "/com/io7m/aurantedit/ui/internal/main.fxml"
      );
    final var resources =
      strings.resources();
    final var loader =
      new FXMLLoader(xml, resources);

    final var controller =
      AEController.empty(strings);

    final AEControllerFactoryType<AEViewType> controllers =
      AEControllerFactoryMapped.create(
        Map.entry(
          AEFileView.class,
          () -> new AEFileView(services, stage, controller)
        ),
        Map.entry(
          AEKeyAssignmentTabView.class,
          () -> new AEKeyAssignmentTabView(services, controller)
        ),
        Map.entry(
          AEKeyAssignmentPianoView.class,
          () -> new AEKeyAssignmentPianoView(controller)
        )
      );

    loader.setControllerFactory(param -> {
      return controllers.call((Class<? extends AEViewType>) param);
    });

    final var pane = loader.<Pane>load();
    AECSS.setCSS(pane);
    stage.setScene(new Scene(pane));
    stage.setTitle(strings.format(AE_TITLE));
    return new AEViewAndStage<>(loader.getController(), stage);
  }

  private void onApplicationEvent(
    final AEApplicationEventType event)
  {
    LOG.debug("onApplicationEvent: {}", event);

    switch (event) {
      case final ExitRequested ignored -> {
        this.tryExit();
      }
    }
  }

  private void setWindowTitle()
  {

  }

  @Override
  public void initialize(
    final URL url,
    final ResourceBundle resourceBundle)
  {
    this.fieldsClip = Set.of(
      this.fieldClipChannels,
      this.fieldClipEndianness,
      this.fieldClipFormat,
      this.fieldClipHash,
      this.fieldClipID,
      this.fieldClipName,
      this.fieldClipSampleDepth,
      this.fieldClipSampleRate,
      this.fieldClipSize
    );

    this.status.setText(AEVersionStrings.VERSION);

    this.fileTabs.setDisable(true);
    this.fileTabs.setVisible(false);

    this.itemClose.setDisable(true);

    this.initializeMetaTab();
    this.initializeClipsTab();

    this.controller.file()
      .addListener((observable, oldValue, newValue) -> {
        if (newValue.isPresent()) {
          this.fileTabs.setDisable(false);
          this.fileTabs.setVisible(true);
          this.itemClose.setDisable(false);
          this.setWindowTitle();
        } else {
          this.fileTabs.setDisable(true);
          this.fileTabs.setVisible(false);
          this.itemClose.setDisable(true);
          this.setWindowTitle();
        }
      });

    this.controller.undoState()
      .addListener(this.undoListener);
    this.controller.redoState()
      .addListener(this.redoListener);

    this.controller.events()
      .subscribe(new AEPerpetualSubscriber<>(this::onFileModelEvent));

    this.error.setDisable(true);
    this.error.setVisible(false);
  }

  /**
   * Configure the metadata tab.
   */

  private void initializeMetaTab()
  {
    this.controller.identifier()
      .addListener((_, _, newIdOpt) -> {
        if (newIdOpt.isPresent()) {
          final var newId = newIdOpt.get();
          this.metaGroup.setText(newId.group().value());
          this.metaName.setText(newId.name().value());
          this.metaVersionMajor.setText(
            Long.toUnsignedString(
              Integer.toUnsignedLong(newId.version().major())
            )
          );
          this.metaVersionMinor.setText(
            Long.toUnsignedString(
              Integer.toUnsignedLong(newId.version().minor())
            )
          );
        } else {
          this.metaGroup.setText("");
          this.metaName.setText("");
          this.metaVersionMajor.setText("");
          this.metaVersionMinor.setText("");
        }
      });

    this.metaTableName.setSortable(true);
    this.metaTableName.setReorderable(false);
    this.metaTableName.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().name());
    });

    this.metaTableValue.setSortable(true);
    this.metaTableValue.setReorderable(false);
    this.metaTableValue.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().value());
    });

    this.metaTable.setPlaceholder(new Label());
    this.metaTable.getSelectionModel()
      .selectedItemProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.metaRemove.setDisable(newValue == null);
      });

    this.metaSearch.textProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.controller.setMetadataFilter(newValue);
      });

    final var metadata = this.controller.metadata();
    this.metaTable.setItems(metadata);
    metadata.comparatorProperty()
      .bind(this.metaTable.comparatorProperty());
  }

  /**
   * Configure the clips tab.
   */

  private void initializeClipsTab()
  {
    this.waveView = new WaveView();
    this.waveView.setWaveModel(this.waveModel);
    this.waveCanvasHolder.getChildren().add(this.waveView);

    AnchorPane.setBottomAnchor(this.waveView, 0.0);
    AnchorPane.setLeftAnchor(this.waveView, 0.0);
    AnchorPane.setRightAnchor(this.waveView, 0.0);
    AnchorPane.setTopAnchor(this.waveView, 0.0);

    this.clipTableID.setSortable(true);
    this.clipTableID.setReorderable(false);
    this.clipTableID.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().id().toString());
    });

    this.clipTableReferences.setSortable(true);
    this.clipTableReferences.setReorderable(false);
    this.clipTableReferences.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper("0");
    });

    this.clipTableName.setSortable(true);
    this.clipTableName.setReorderable(false);
    this.clipTableName.setCellValueFactory(param -> {
      return new ReadOnlyStringWrapper(param.getValue().name());
    });

    this.clipTable.setPlaceholder(new Label());
    this.clipTable.getSelectionModel()
      .selectedItemProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.onClipSelectionChanged(oldValue, newValue);
      });
    this.clipTable.getSelectionModel()
      .selectedItemProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.clipReplace.setDisable(newValue == null);
        this.clipRemove.setDisable(newValue == null);
      });

    this.clipSearch.textProperty()
      .addListener((observable, oldValue, newValue) -> {
        this.controller.setClipFilter(newValue);
      });

    final var clips = this.controller.clips();
    this.clipTable.setItems(clips);
    clips.comparatorProperty()
      .bind(this.clipTable.comparatorProperty());
  }

  private void onClipSelectionChanged(
    final AUClipDeclaration oldValue,
    final AUClipDeclaration newValue)
  {
    if (newValue == null) {
      for (final var field : this.fieldsClip) {
        field.setText("");
      }
      this.waveModel.setWave(Optional.empty());
      this.waveView.setWaveModel(this.waveModel);
      this.waveView.redraw();
      return;
    }

    this.fieldClipID
      .setText(newValue.id().toString());
    this.fieldClipChannels
      .setText(Long.toUnsignedString(newValue.channels()));
    this.fieldClipEndianness
      .setText(newValue.endianness().name());
    this.fieldClipFormat
      .setText(newValue.format().descriptor().value());
    this.fieldClipHash
      .setText(
        String.format(
          "%s: %s",
          newValue.hash().algorithm().name(),
          newValue.hash().value()
        )
      );
    this.fieldClipName
      .setText(newValue.name());
    this.fieldClipSampleDepth
      .setText(Long.toUnsignedString(newValue.sampleDepth()));
    this.fieldClipSampleRate
      .setText(Long.toUnsignedString(newValue.sampleRate()));
    this.fieldClipSize
      .setText(Long.toUnsignedString(newValue.size()));

    final var buffer = this.controller.clipBuffer(newValue.id());
    this.waveModel.setWave(Optional.ofNullable(buffer));
    this.waveView.setViewRange(0L, buffer.frames());
    this.waveView.setWaveModel(this.waveModel);
    this.waveView.redraw();
  }

  private AEUnit logException(
    final Throwable throwable)
  {
    if (throwable instanceof final CompletionException e) {
      if (e.getCause() instanceof CancellationException) {
        return UNIT;
      }
    }

    LOG.debug("Error: ", throwable);
    return UNIT;
  }

  private CompletableFuture<AEUnit> tryNew()
  {
    return onUIThread(this::tryNewActual);
  }

  private AEUnit tryNewActual()
    throws Exception
  {
    final var createDialog =
      this.createDialogs.createDialog(UNIT);

    createDialog.stage().showAndWait();

    final var chosenOpt = createDialog.view().result();
    if (chosenOpt.isEmpty()) {
      return null;
    }

    final var chosen = chosenOpt.get();
    this.addRecentFile(chosen);

    final var existingFileOpt =
      this.controller.file().getValue();

    if (existingFileOpt.isEmpty()) {
      this.controller.create(chosen);
    } else {
      final var viewAndStage =
        openForStage(this.services, new Stage());
      viewAndStage.view().controller.create(chosen);
      viewAndStage.stage().show();
    }
    return UNIT;
  }

  private AEUnit tryOpenActual()
    throws Exception
  {
    final var chooser =
      this.choosers.create(
        JWFileChooserConfiguration.builder()
          .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
          .build()
      );

    final var chosen = chooser.showAndWait();
    if (chosen.isEmpty()) {
      return UNIT;
    }

    final var file = chosen.get(0);
    this.addRecentFile(file);

    final var existingFileOpt =
      this.controller.file().getValue();

    if (existingFileOpt.isEmpty()) {
      LOG.debug("Opening file in the current controller.");
      this.controller.open(file, false);
    } else {
      LOG.debug("Opening file in a new stage and controller.");
      final var viewAndStage =
        openForStage(this.services, new Stage());
      viewAndStage.view().controller.open(file, false);
      viewAndStage.stage().show();
    }

    return UNIT;
  }

  private CompletableFuture<AEUnit> tryOpen()
  {
    return onUIThread(this::tryOpenActual);
  }

  private CompletableFuture<AEUnit> tryClose()
  {
    LOG.debug("Closing...");
    this.controller.closeFile();

    if (this.windowTracker.fileWindowsOpen() > 1) {
      this.windowTracker.closeWindow(this.stage);
    }

    return CompletableFuture.completedFuture(UNIT);
  }

  private CompletableFuture<AEUnit> tryExit()
  {
    LOG.debug("Exiting...");
    this.controller.closeFile();
    this.windowTracker.closeWindow(this.stage);
    return CompletableFuture.completedFuture(UNIT);
  }

  @FXML
  private void onNewSelected()
  {
    this.tryNew();
  }

  @FXML
  private void onOpenSelected()
  {
    this.tryOpen();
  }

  @FXML
  private void onCloseSelected()
  {
    this.tryClose();
  }

  @FXML
  private void onExitSelected()
  {
    this.appEvents.publish(new ExitRequested());
  }

  @FXML
  private void onClipAddSelected()
  {
    final var chooser =
      this.choosers.create(
        JWFileChooserConfiguration.builder()
          .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
          .build()
      );

    final var chosen = chooser.showAndWait();
    if (chosen.isEmpty()) {
      return;
    }

    final var file = chosen.get(0);
    this.addRecentFile(file);
    this.controller.clipAdd(file);
  }

  @FXML
  private void onClipRemoveSelected()
  {
    final var clipId =
      this.clipTable.getSelectionModel()
        .getSelectedItem()
        .id();

    this.controller.clipDelete(clipId);
  }

  @FXML
  private void onClipReplaceSelected()
  {
    final var clipId =
      this.clipTable.getSelectionModel()
        .getSelectedItem()
        .id();

    final var chooser =
      this.choosers.create(
        JWFileChooserConfiguration.builder()
          .setAction(JWFileChooserAction.OPEN_EXISTING_SINGLE)
          .build()
      );

    final var chosen = chooser.showAndWait();
    if (chosen.isEmpty()) {
      return;
    }

    final var file = chosen.get(0);
    this.addRecentFile(file);
    this.controller.clipReplace(clipId, file);
  }

  @FXML
  private void onMetaAddSelected()
    throws Exception
  {
    final var viewAndStage =
      this.metadataEditDialogs.createDialog(UNIT);

    final var view = viewAndStage.view();
    viewAndStage.stage().showAndWait();
    view.result().ifPresent(this.controller::metadataAdd);
  }

  @FXML
  private void onMetaRemoveSelected()
  {
    this.controller.metadataRemove(
      this.metaTable.getSelectionModel()
        .getSelectedItem()
    );
  }

  @FXML
  private void onMetaReplaceSelected()
    throws Exception
  {
    final var meta =
      this.metaTable.getSelectionModel()
        .getSelectedItem();

    final var viewAndStage =
      this.metadataEditDialogs.createDialog(UNIT);

    final var view = viewAndStage.view();
    view.setDataInitial(meta);
    viewAndStage.stage().showAndWait();
    view.result().ifPresent(metadataReplaceWith -> {
      this.controller.metadataReplace(meta, metadataReplaceWith);
    });
  }

  @FXML
  private void onNameSetSelected()
    throws Exception
  {
    final var viewAndStage =
      this.nameSet.createDialog(UNIT);

    final var view = viewAndStage.view();
    view.setNameInitial(
      this.controller.identifier()
        .getValue()
        .get()
        .name()
    );

    viewAndStage.stage().showAndWait();
    // view.result().ifPresent(this.controller::setName);
  }

  @FXML
  private void onVersionSetSelected()
    throws Exception
  {
    final var viewAndStage =
      this.versionSet.createDialog(UNIT);

    final var view = viewAndStage.view();
    view.setVersionInitial(
      this.controller.identifier()
        .getValue()
        .get()
        .version()
    );

    viewAndStage.stage().showAndWait();
    view.result().ifPresent(this.controller::setVersion);
  }

  @FXML
  private void onUndoSelected()
  {
    this.controller.undo();
  }

  @FXML
  private void onRedoSelected()
  {
    this.controller.redo();
  }

  private void onFileModelEvent(
    final AEFileModelEventType event)
  {
    Platform.runLater(() -> {
      switch (event) {
        case final AEFileModelEventError eventError -> {
          this.progressMajor.setProgress(0.0);
          this.progressMinor.setProgress(0.0);
          this.status.setText(eventError.message());
          this.error.setDisable(false);
          this.error.setVisible(true);
          this.errorLast = eventError;
        }
        case final AEFileModelEventProgress eventProgress -> {
          final AEProgress p = eventProgress.progress();
          this.progressMajor.setProgress(p.taskProgress());
          this.progressMinor.setProgress(p.subTaskProgress());
          this.status.setText("%s (%s)".formatted(p.task(), p.subTask()));
        }
      }
    });
  }

  @FXML
  private void onErrorDetailsSelected()
  {
    final var dialog =
      MSErrorDialogs.builder(this.errorLast)
        .setCSS(AECSS.defaultCSS())
        .setTitle(this.errorLast.message())
        .build();

    dialog.showAndWait();
  }

  @FXML
  private void onAboutSelected()
    throws IOException
  {
    final var newStage = new Stage();

    final var xml =
      AEFileView.class.getResource(
        "/com/io7m/aurantedit/ui/internal/about.fxml"
      );
    final var resources =
      this.strings.resources();
    final var loader =
      new FXMLLoader(xml, resources);

    loader.setControllerFactory(clazz -> new AEAboutView());

    final Pane pane = loader.load();
    AECSS.setCSS(pane);

    loader.getController();
    newStage.setScene(new Scene(pane));
    newStage.setResizable(false);
    newStage.setTitle("Aurantedit");
    newStage.show();
  }

  @FXML
  private void onImportSelected()
    throws Exception
  {
    final var dialog =
      this.importDialogs.openDialogAndWait(UNIT);
    final var result =
      dialog.result();

    if (result.isEmpty()) {
      return;
    }

    this.controller.importNow(result.get());
  }

  private void addRecentFile(
    final Path file)
  {
    try (var t = this.database.openTransaction()) {
      t.query(AERecentFileAddType.class).execute(file);
      t.commit();
    } catch (final DDatabaseException e) {
      // Nothing can be done.
    }
  }
}
