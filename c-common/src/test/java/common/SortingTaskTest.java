package common;

import common.IntArrayOuterClass.ArrayMsg;
import org.junit.Test;

import static common.SortingUtil.Status.*;
import static common.SortingUtil.*;
import static org.junit.Assert.assertEquals;

public class SortingTaskTest {

  @Test
  public void testOkCases() {
    assertEquals(OK, checkIsCompleted(sort(create(0)), 0));
    assertEquals(OK, checkIsCompleted(sort(create(1)), 1));
    assertEquals(OK, checkIsCompleted(sort(create(50)), 50));
    assertEquals(OK, checkIsCompleted(sort(create(20000)), 20000));
  }

  @Test
  public void testDifferentLengths() {
    assertEquals(LENGTH_DIFFERS, checkIsCompleted(sort(create(0)), 1));
    assertEquals(LENGTH_DIFFERS, checkIsCompleted(sort(create(1)), 0));
    assertEquals(LENGTH_DIFFERS, checkIsCompleted(sort(create(50)), 25));
    assertEquals(LENGTH_DIFFERS, checkIsCompleted(sort(create(25)), 50));
  }

  @Test
  public void testNotSorted() {
    // generate unsorted array
    ArrayMsg task;
    do task = create(5); while (task.equals(sort(task)));

    assertEquals(NOT_SORTED, checkIsCompleted(task, 5));
  }

}
