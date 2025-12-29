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


package com.io7m.aurantedit.main.internal;

import com.io7m.aurantedit.main.AEMain;
import com.io7m.jade.api.ApplicationDirectories;
import com.io7m.jade.api.ApplicationDirectoryConfiguration;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo key assignment.
 */

public final class AEKeyAssignDemoMain
{
  private static final Logger LOG =
    LoggerFactory.getLogger(AEMain.class);

  private AEKeyAssignDemoMain()
  {

  }

  /**
   * The main entry point.
   *
   * @param args The command-line arguments
   */

  public static void main(
    final String[] args)
  {
    System.setProperty("org.jooq.no-logo", "true");
    System.setProperty("org.jooq.no-tips", "true");

    final var directoryConfiguration =
      ApplicationDirectoryConfiguration.builder()
        .setApplicationName("com.io7m.aurantedit")
        .setPortablePropertyName("com.io7m.aurantedit.portable")
        .setOverridePropertyName("com.io7m.aurantedit.override")
        .build();

    final var directories =
      ApplicationDirectories.get(directoryConfiguration);

    Platform.startup(() -> {
      try {
        start();
      } catch (final Exception e) {
        LOG.error("Startup failed: ", e);
        System.exit(1);
      }
    });
  }

  private static void start()
  {

  }
}
