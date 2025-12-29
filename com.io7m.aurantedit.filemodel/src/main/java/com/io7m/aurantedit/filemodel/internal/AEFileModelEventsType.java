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

package com.io7m.aurantedit.filemodel.internal;

/**
 * Event publication functions.
 */

public interface AEFileModelEventsType
{
  /**
   * Publish an event with progress.
   *
   * @param task            The task
   * @param taskProgress    The task progress
   * @param subTask         The subtask
   * @param subTaskProgress The subtask progress
   */

  void eventProgress(
    String task,
    double taskProgress,
    String subTask,
    double subTaskProgress
  );

  /**
   * Publish an event with progress.
   *
   * @param task             The task
   * @param taskProgress     The task progress
   * @param subTask          The subtask
   * @param subTaskProgress  The subtask progress
   * @param subTaskArguments The subtask format arguments
   */

  default void eventProgressFormat(
    final String task,
    final double taskProgress,
    final double subTaskProgress,
    final String subTask,
    final Object... subTaskArguments)
  {
    this.eventProgress(
      task,
      taskProgress,
      String.format(subTask, subTaskArguments),
      subTaskProgress
    );
  }

  /**
   * Publish an event with progress.
   *
   * @param task             The task
   * @param taskIndex        The task index
   * @param taskMax          The task max
   * @param subTask          The subtask
   * @param subTaskProgress  The subtask progress
   * @param subTaskArguments The subtask format arguments
   */

  default void eventProgressMaxFormat(
    final String task,
    final int taskIndex,
    final int taskMax,
    final double subTaskProgress,
    final String subTask,
    final Object... subTaskArguments)
  {
    this.eventProgressFormat(
      task,
      (double) taskIndex / (double) taskMax,
      subTaskProgress,
      String.format(subTask, subTaskArguments)
    );
  }
}
