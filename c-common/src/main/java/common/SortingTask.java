package common;

import com.google.common.primitives.Ints;
import common.IntArrayOuterClass.IntArray;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SortingTask {

  private static Random random = new Random();


  public static IntArray create(int size) {
    List<Integer> randomInts = Ints.asList(random.ints(size).toArray());
    return IntArray.newBuilder().addAllNumbers(randomInts).build();
  }

  public static IntArray complete(IntArray task) {
    List<Integer> numbers = task.getNumbersList();
    List<Integer> sorted = BubbleSort.process(numbers);
    return IntArray.newBuilder().addAllNumbers(sorted).build();
  }


  public enum Status {
    OK(0),
    LENGTH_DIFFERS(1),
    NOT_SORTED(2),
    ;

    public final int code;

    Status(int code) { this.code = code; }
  }

  public static Status checkIsCompleted(IntArray task, int expectedArraySize) {
    int[] numbers = Ints.toArray(task.getNumbersList());

    // test #1: same length
    if (numbers.length != expectedArraySize) return Status.LENGTH_DIFFERS;

    // test #2: is sorted
    int[] sorted = numbers.clone();
    Arrays.sort(sorted);
    return Arrays.equals(numbers, sorted) ? Status.OK : Status.NOT_SORTED;
  }

}
