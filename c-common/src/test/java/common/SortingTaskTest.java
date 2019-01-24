package common;

import common.IntArrayOuterClass.IntArray;
import org.junit.Test;

import static common.SortingTask.Status.*;
import static common.SortingTask.*;
import static org.junit.Assert.assertEquals;

public class SortingTaskTest {

  @Test
  public void testOkCases() {
    assertEquals(OK, checkIsCompleted(complete(create(0)), 0));
    assertEquals(OK, checkIsCompleted(complete(create(1)), 1));
    assertEquals(OK, checkIsCompleted(complete(create(50)), 50));
    assertEquals(OK, checkIsCompleted(complete(create(20000)), 20000));
  }

  @Test
  public void testDifferentLengths() {
    assertEquals(LENGTH_DIFFERS, checkIsCompleted(complete(create(0)), 1));
    assertEquals(LENGTH_DIFFERS, checkIsCompleted(complete(create(1)), 0));
    assertEquals(LENGTH_DIFFERS, checkIsCompleted(complete(create(50)), 25));
    assertEquals(LENGTH_DIFFERS, checkIsCompleted(complete(create(25)), 50));
  }

  @Test
  public void testNotSorted() {
    // generate unsorted array
    IntArray task;
    do task = create(5); while (task.equals(complete(task)));

    assertEquals(NOT_SORTED, checkIsCompleted(task, 5));
  }

}
