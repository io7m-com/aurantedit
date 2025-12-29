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

package com.io7m.aurantedit.filemodel;

import java.util.Objects;

/**
 * The progress of an operation.
 *
 * @param task            The main task
 * @param taskProgress    The main task progress
 * @param subTask         The subtask
 * @param subTaskProgress The subtask progress
 */

public record AEProgress(
  String task,
  double taskProgress,
  String subTask,
  double subTaskProgress)
{
  /**
   * The progress of an operation.
   *
   * @param task            The main task
   * @param taskProgress    The main task progress
   * @param subTask         The subtask
   * @param subTaskProgress The subtask progress
   */

  public AEProgress
  {
    Objects.requireNonNull(task, "Task");
    Objects.requireNonNull(subTask, "SubTask");

    taskProgress =
      Math.clamp(taskProgress, 0.0, 1.0);
    subTaskProgress =
      Math.clamp(subTaskProgress, 0.0, 1.0);
  }

  /**
   * @return {@code true} if this progress implies a completed operation
   */

  public boolean isDone()
  {
    return this.taskProgress == 1.0 && this.subTaskProgress == 1.0;
  }
}
