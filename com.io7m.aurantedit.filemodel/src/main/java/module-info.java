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

import com.io7m.aurantedit.filemodel.internal.AEDatabaseQueryProviderType;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandCompact;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandExport;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipReplace;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipsAdd;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandFactoryType;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandLoad;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipsDelete;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1IdentifierPut;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1KeyAssignmentPut;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1MetadataPut;

/**
 * Aurantium file editor (File model)
 */

module com.io7m.aurantedit.filemodel
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.aurantium.api;
  requires com.io7m.aurantium.parser.api;
  requires com.io7m.aurantium.vanilla;
  requires com.io7m.aurantium.writer.api;
  requires com.io7m.aurantium.xmedia;

  requires com.fasterxml.jackson.annotation;
  requires com.io7m.darco.api;
  requires com.io7m.darco.sqlite;
  requires com.io7m.dixmont.core;
  requires com.io7m.jattribute.core;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.jxe.core;
  requires com.io7m.lanark.core;
  requires com.io7m.seltzer.api;
  requires com.io7m.wendover.core;
  requires io.opentelemetry.api;
  requires java.desktop;
  requires org.jooq;
  requires org.slf4j;
  requires org.xerial.sqlitejdbc;
  requires tools.jackson.databind;

  opens com.io7m.aurantedit.filemodel.internal.json
    to tools.jackson.databind;
  opens com.io7m.aurantedit.filemodel.internal.commands
    to tools.jackson.databind;
  opens com.io7m.aurantedit.filemodel.internal.commands_v1
    to tools.jackson.databind;

  provides AECommandFactoryType with
    AEC1ClipReplace,
    AEC1ClipsAdd,
    AEC1ClipsDelete,
    AEC1IdentifierPut,
    AEC1KeyAssignmentPut,
    AEC1MetadataPut,
    AECommandCompact,
    AECommandExport,
    AECommandLoad
    ;

  uses AECommandFactoryType;
  uses AEDatabaseQueryProviderType;

  exports com.io7m.aurantedit.filemodel;
  opens com.io7m.aurantedit.filemodel to tools.jackson.databind;
}
