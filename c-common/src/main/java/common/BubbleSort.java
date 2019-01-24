package common;

import java.util.List;

import static com.google.common.primitives.Ints.asList;
import static com.google.common.primitives.Ints.toArray;

public class BubbleSort {

  /**
   * O(n²) bubble sort. Unboxes the list before processing
   */
  public static List<Integer> process(List<Integer> list) {
    return asList(process(toArray(list)));
  }

  /**
   * O(n²) bubble sort
   */
  public static int[] process(int[] arr) {
    for (int i = 0; i < arr.length - 1; i++)
      for (int j = 0; j < arr.length - i - 1; j++)
        if (arr[j] > arr[j + 1]) {
          int value = arr[j];
          arr[j] = arr[j + 1];
          arr[j + 1] = value;
        }

    return arr;
  }

}
