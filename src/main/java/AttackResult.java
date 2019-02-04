public class AttackResult {

  public double clientAverageTimePerRequest;
  public int varyingParameter;
  public double serverAverageRequestTime;
  public double serverAverageSortingTime;
  public boolean hasFailed;

  public AttackResult(int varyingParameter) {
    this.varyingParameter = varyingParameter;
  }

}
