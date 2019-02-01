package common;

import com.google.common.primitives.Ints;
import common.IntArrayOuterClass.ArrayMsg;

import java.util.List;
import java.util.Random;

public class SortingUtil {

  private static Random random = new Random();


  public static ArrayMsg create(int size) {
    List<Integer> randomInts = Ints.asList(random.ints(size).toArray());
    return ArrayMsg.newBuilder().addAllNumbers(randomInts).build();
  }

  public static ArrayMsg sort(ArrayMsg task) {
    List<Integer> numbers = task.getNumbersList();
    List<Integer> sorted = BubbleSort.process(numbers);
    return ArrayMsg.newBuilder().addAllNumbers(sorted).build();
  }


  public enum Status {
    OK(0),
    LENGTH_DIFFERS(1),
    NOT_SORTED(2),
    ;

    public final int code;

    Status(int code) { this.code = code; }
  }

  public static Status checkIsCompleted(ArrayMsg task, int expectedArraySize) {
    int[] numbers = Ints.toArray(task.getNumbersList());

    // test #1: same length
    if (numbers.length != expectedArraySize) return Status.LENGTH_DIFFERS;

    // test #2: is ordered
    if (numbers.length >= 2) {
      for (int idx = 0; idx + 1 < numbers.length; idx++)
        if (numbers[idx] > numbers[idx + 1])
          return Status.NOT_SORTED;
    }

    return Status.OK;
  }

}
