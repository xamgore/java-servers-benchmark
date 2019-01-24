package common;

import com.google.common.primitives.Ints;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

public class BubbleSortTest {

  @Test
  public void sortedArray() {
    int[] arr = arrayOf(1, 2, 3, 4, 5);
    BubbleSort.process(arr);
    assertArrayEquals(arrayOf(1, 2, 3, 4, 5), arr);
  }

  @Test
  public void reverseSortedArray() {
    int[] arr = arrayOf(5, 4, 3, 2, 1);
    BubbleSort.process(arr);
    assertArrayEquals(arrayOf(1, 2, 3, 4, 5), arr);
  }

  @Test
  public void shuffledArray() {
    int[] arr = arrayOf(2, 1, 2, 1, 3, 4, 5, 1, 2);
    BubbleSort.process(arr);
    assertArrayEquals(arrayOf(1, 1, 1, 2, 2, 2, 3, 4, 5), arr);
  }

  @Test
  public void emptyArray() {
    int[] arr = arrayOf();
    BubbleSort.process(arr);
    assertArrayEquals(arrayOf(), arr);
  }

  @Test
  public void smokeTest() {
    int[] arr = new Random().ints(2000).toArray();
    int[] sorted = arr.clone();

    Arrays.sort(sorted);
    BubbleSort.process(arr);

    assertArrayEquals(sorted, arr);
  }

  private static int[] arrayOf(int... nums) {
    return nums;
  }
}
