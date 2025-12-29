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

package com.io7m.aurantedit.filemodel.internal.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandSerialized;
import com.io7m.aurantedit.filemodel.internal.commands.AECommandStateType;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipRecorded;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipReplaceState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipsAddState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1ClipsDeleteState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1IdentifierPutState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1KeyAssignmentPutState;
import com.io7m.aurantedit.filemodel.internal.commands_v1.AEC1MetadataPutState;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;
import com.io7m.lanark.core.RDottedName;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * JSON mappers for the file format.
 */

public final class AE1Mappers
{
  /**
   * The JSON schema identifier.
   */

  public static final String SCHEMA_1 =
    "urn:com.io7m.aurantedit:1.0";

  private static final JsonMapper MAPPER =
    createMapper();

  private static JsonMapper createMapper()
  {
    final var dixBuilder =
      DmJsonRestrictedDeserializers.builder();

    dixBuilder.allowClass(AE1ClipDeclaration.class);
    dixBuilder.allowClass(AE1ClipDescriptions.class);
    dixBuilder.allowClass(AE1HashString.class);
    dixBuilder.allowClass(AE1HashValue.class);
    dixBuilder.allowClass(AE1Identifier.class);
    dixBuilder.allowClass(AE1KeyAssignments.class);
    dixBuilder.allowClass(AEC1ClipReplaceState.class);
    dixBuilder.allowClass(AEC1ClipsAddState.class);
    dixBuilder.allowClass(AEC1ClipsDeleteState.class);
    dixBuilder.allowClass(AEC1IdentifierPutState.class);
    dixBuilder.allowClass(AEC1KeyAssignmentPutState.class);
    dixBuilder.allowClass(AEC1MetadataPutState.class);
    dixBuilder.allowClass(AECommandSerialized.class);
    dixBuilder.allowClass(AECommandStateType.class);
    dixBuilder.allowClass(BigInteger.class);
    dixBuilder.allowClass(Map.Entry.class);
    dixBuilder.allowClass(OffsetDateTime.class);
    dixBuilder.allowClass(RDottedName.class);
    dixBuilder.allowClass(String.class);
    dixBuilder.allowClass(byte.class);
    dixBuilder.allowClass(double.class);
    dixBuilder.allowClass(int.class);
    dixBuilder.allowClass(long.class);
    dixBuilder.allowClassName("[B");
    dixBuilder.allowListsOfClass(AE1ClipDescription.class);
    dixBuilder.allowListsOfClass(AE1KeyAssignment.class);
    dixBuilder.allowListsOfClass(AE1MetadataValue.class);
    dixBuilder.allowListsOfClass(AEC1ClipRecorded.class);
    dixBuilder.allowListsOfClass(String.class);
    dixBuilder.allowOptionalOfClass(AE1ClipLoopRange.class);
    dixBuilder.allowSortedSetsOfClass(String.class);

    final var serializers =
      dixBuilder.build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(serializers);
    simpleModule.addDeserializer(
      RDottedName.class,
      new AE1DottedNameDeserializer()
    );
    simpleModule.addSerializer(
      RDottedName.class,
      new AE1DottedNameSerializer()
    );

    simpleModule.addDeserializer(
      AE1HashString.class,
      new AE1HashStringDeserializer()
    );
    simpleModule.addSerializer(
      AE1HashString.class,
      new AE1HashStringSerializer()
    );

    final var builder = JsonMapper.builder();
    builder.addModule(simpleModule);
    builder.changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_ABSENT));
    builder.changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_ABSENT));
    return builder.build();
  }

  /**
   * @return The main mapper
   */

  public static JsonMapper mapper()
  {
    return MAPPER;
  }

  private AE1Mappers()
  {

  }
}
