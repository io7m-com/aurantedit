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

import com.io7m.aurantedit.ui.internal.database.AEDatabaseType;
import com.io7m.aurantedit.ui.internal.database.AERecentFileListType;
import com.io7m.jwheatsheaf.api.JWFileChooserConfiguration;
import com.io7m.jwheatsheaf.api.JWFileChooserFilterType;
import com.io7m.jwheatsheaf.api.JWFileChooserType;
import com.io7m.jwheatsheaf.api.JWFileChoosersType;
import com.io7m.jwheatsheaf.oxygen.JWOxygenIconSet;
import com.io7m.jwheatsheaf.ui.JWFileChoosers;
import com.io7m.repetoir.core.RPServiceDirectoryType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;

import static com.io7m.darco.api.DDatabaseUnit.UNIT;

/**
 * The file chooser service.
 */

public final class AEFileChoosers implements AEFileChoosersType
{
  private static final JWOxygenIconSet OXYGEN_ICON_SET =
    new JWOxygenIconSet();

  private static final JWFileChooserFilterType AUEDIT_FILTER =
    new JWFileChooserFilterType() {

    @Override
    public String description()
    {
      return "Aurantedit files (*.auedit)";
    }

    @Override
    public boolean isAllowed(
      final Path path)
    {
      return Files.isDirectory(path)
        || path.getFileName().toString().endsWith(".auedit");
    }
  };

  private static final JWFileChooserFilterType AURANTIUM_FILTER =
    new JWFileChooserFilterType() {

      @Override
      public String description()
      {
        return "Aurantium sample maps (*.aam)";
      }

      @Override
      public boolean isAllowed(
        final Path path)
      {
        return Files.isDirectory(path)
          || path.getFileName().toString().endsWith(".aam");
      }
    };

  private final JWFileChoosersType choosers;
  private final AEDatabaseType database;
  private Path mostRecentDirectory;

  /**
   * The file chooser service.
   *
   * @param services The service directory
   */

  public AEFileChoosers(
    final RPServiceDirectoryType services)
  {
    this.database =
      services.requireService(AEDatabaseType.class);

    this.choosers =
      JWFileChoosers.createWith(
        Executors.newVirtualThreadPerTaskExecutor(),
        Locale.getDefault()
      );
  }

  @Override
  public JWFileChooserType create(
    final JWFileChooserConfiguration configuration)
  {
    List<Path> recentFiles;
    try (var t = this.database.openTransaction()) {
      recentFiles = t.query(AERecentFileListType.class).execute(UNIT);
    } catch (final Exception e) {
      recentFiles = List.of();
    }

    final var builder =
      JWFileChooserConfiguration.builder()
        .from(configuration)
        .setFileImageSet(OXYGEN_ICON_SET)
        .addFileFilters(AUEDIT_FILTER)
        .addFileFilters(AURANTIUM_FILTER)
        .setRecentFiles(recentFiles);

    if (this.mostRecentDirectory != null) {
      builder.setInitialDirectory(this.mostRecentDirectory);
    }

    try {
      builder.setCssStylesheet(AECSS.defaultCSS().toURL());
    } catch (final MalformedURLException e) {
      throw new IllegalStateException(e);
    }

    return this.choosers.create(builder.build());
  }

  @Override
  public JWFileChooserFilterType filterForAllFiles()
  {
    return this.choosers.filterForAllFiles();
  }

  @Override
  public JWFileChooserFilterType filterForOnlyDirectories()
  {
    return this.choosers.filterForOnlyDirectories();
  }

  @Override
  public String description()
  {
    return "File chooser service.";
  }

  @Override
  public void close()
    throws IOException
  {
    this.choosers.close();
  }

  @Override
  public String toString()
  {
    return String.format(
      "[%s 0x%08x]",
      this.getClass().getName(),
      Integer.valueOf(this.hashCode())
    );
  }

  @Override
  public void setMostRecentDirectory(
    final Path file)
  {
    this.mostRecentDirectory = Objects.requireNonNull(file, "file");
  }

  @Override
  public JWFileChooserFilterType aueditFilter()
  {
    return AUEDIT_FILTER;
  }

  @Override
  public JWFileChooserFilterType aurantiumFilter()
  {
    return AURANTIUM_FILTER;
  }
}
