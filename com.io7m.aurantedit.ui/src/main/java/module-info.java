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

import com.io7m.aurantedit.ui.internal.database.AEDatabaseQueryProviderType;
import com.io7m.aurantedit.ui.internal.database.AERecentFileAdd;
import com.io7m.aurantedit.ui.internal.database.AERecentFileList;

/**
 * Aurantium file editor (UI).
 */

open module com.io7m.aurantedit.ui
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.aurantedit.filemodel;

  requires com.io7m.aurantium.api;
  requires com.io7m.aurantium.parser.api;
  requires com.io7m.aurantium.validation.api;
  requires com.io7m.aurantium.vanilla;
  requires com.io7m.aurantium.writer.api;
  requires com.io7m.brackish.core;
  requires com.io7m.darco.api;
  requires com.io7m.darco.sqlite;
  requires com.io7m.ivoirax.core;
  requires com.io7m.jade.api;
  requires com.io7m.jaffirm.core;
  requires com.io7m.jattribute.core;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.jranges.core;
  requires com.io7m.jsamplebuffer.api;
  requires com.io7m.jsamplebuffer.vanilla;
  requires com.io7m.jsamplebuffer.xmedia;
  requires com.io7m.jwheatsheaf.api;
  requires com.io7m.jwheatsheaf.oxygen;
  requires com.io7m.jwheatsheaf.ui;
  requires com.io7m.jxe.core;
  requires com.io7m.jxtrand.api;
  requires com.io7m.lanark.core;
  requires com.io7m.miscue.core;
  requires com.io7m.miscue.fx.seltzer;
  requires com.io7m.repetoir.core;
  requires com.io7m.seltzer.api;
  requires io.opentelemetry.api;
  requires java.desktop;
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires org.jooq;
  requires org.slf4j;
  requires org.xerial.sqlitejdbc;

  provides AEDatabaseQueryProviderType
    with AERecentFileAdd,
      AERecentFileList;

  uses AEDatabaseQueryProviderType;

  exports com.io7m.aurantedit.ui;

  exports com.io7m.aurantedit.ui.internal
    to com.io7m.aurantedit.main;
  exports com.io7m.aurantedit.ui.internal.key_assignments
    to com.io7m.aurantedit.main;
}
