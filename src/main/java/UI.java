import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UI {

  private static final Dispatcher dispatcher = new Dispatcher();

  @FXML
  private LineChart<Number, Number> serverChart;

  @FXML
  private LineChart<Number, Number> clientChart;

  @FXML
  private Button submitButton;

  @FXML
  private Button saveChartsButton;

  @FXML
  private TextField numberOfRequestsField;

  @FXML
  private TextField sizeOfArrayField;

  @FXML
  private TextField numberOfClientsField;

  @FXML
  private TextField sleepDeltaField;

  @FXML
  private TextField fromField;

  @FXML
  private TextField toField;

  @FXML
  private TextField stepField;

  @FXML
  private TextField hostField;

  @FXML
  private ChoiceBox variableParameterChoiceBox;

  @FXML
  private ChoiceBox architectureChoiceBox;

  private String fileName = "empty";

  @FXML
  protected void handleSaveChartsButtonAction(ActionEvent event) throws IOException {
    WritableImage snapShot = saveChartsButton.getScene().snapshot(null);
    ImageIO.write(SwingFXUtils.fromFXImage(snapShot, null), "png", new File(fileName + ".png"));
  }

  @FXML
  protected void handleSubmitButtonAction(ActionEvent event) throws InterruptedException, IOException {
    if (checkIsEmpty(numberOfRequestsField, "Fill the number of requests per client") ||
        checkIsEmpty(numberOfClientsField, "Fill the number of clients to start") ||
        checkIsEmpty(sizeOfArrayField, "Fill the size of array to sort") ||
        checkIsEmpty(fromField, "Fill the \"from\" field") ||
        checkIsEmpty(toField, "Fill the \"to\" field") ||
        checkIsEmpty(stepField, "Fill the \"step\" field") ||
        checkIsEmpty(hostField, "Fill the \"host\" field") ||
        checkIsEmpty(sleepDeltaField, "Fill the sleep delta field"))
      return;


    AttackConfig config = new AttackConfig(
        hostField.getText(),
        numberOfRequestsField.getText(),
        sleepDeltaField.getText(),
        numberOfClientsField.getText(),
        sizeOfArrayField.getText(),
        fromField.getText(),
        toField.getText(),
        stepField.getText(),
        architectureChoiceBox.getItems().indexOf(architectureChoiceBox.getValue()),
        variableParameterChoiceBox.getItems().indexOf(variableParameterChoiceBox.getValue()));

    List<AttackResult> results = dispatcher.attack(config);
    dumpResultsToDisk(results, config);

    clientChart.getData().clear();

    clientChart.getData().add(newSeries(results,
        "client request avg time",
        res -> res.clientAverageTimePerRequest));


    serverChart.getData().clear();

    serverChart.getData().add(newSeries(results,
        "server request avg time",
        res -> res.serverAverageRequestTime));

    serverChart.getData().add(newSeries(results,
        "server sorting avg time",
        res -> res.serverAverageSortingTime));
  }

  private void dumpResultsToDisk(List<AttackResult> results, AttackConfig config) throws IOException {
    List<String> lines = new ArrayList<>(1);
    lines.add("ClientTime,RequestTime,SortingTime,ClientsNumber,SleepDelta,ArraySize,RequestsNumber,VaryingParam");

    results.forEach(res -> lines.add(
        String.format("%.2f,%.2f,%.2f,%d,%d,%d,%d,%d",
            res.clientAverageTimePerRequest,
            res.serverAverageRequestTime,
            res.serverAverageSortingTime,
            config.getClientsNumber(),
            config.getSleepDelta(),
            config.getArraySize(),
            config.getRequestsNumber(),
            res.varyingParameter
        )));

    String variableParameterName = (String) variableParameterChoiceBox.getValue();
    fileName = String.format("arch%d.%s", config.getArchitecture() + 1, variableParameterName);
    Files.write(Paths.get(fileName + ".csv"), lines);
  }


  private static XYChart.Series<Number, Number>
  newSeries(List<AttackResult> results,
            String name,
            Function<AttackResult, Number> fieldGetter) {
    XYChart.Series<Number, Number> series = new XYChart.Series<>();
    series.setName(name);
    series.getData().addAll(
        results.stream()
            .map(res -> new Data<Number, Number>(res.varyingParameter, fieldGetter.apply(res)))
            .collect(Collectors.toList()));
    return series;
  }

  private boolean checkIsEmpty(TextField field, String message) {
    if (field.getText().isEmpty()) {
      Window owner = submitButton.getScene().getWindow();
      AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Form Error!", message);
      return true;
    }

    return false;
  }

}
