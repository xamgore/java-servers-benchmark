import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UI {

  private static final Dispatcher dispatcher = new Dispatcher();

  @FXML
  private LineChart<Number, Number> chart;

  @FXML
  private Button submitButton;

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
        numberOfRequestsField.getText(),
        sleepDeltaField.getText(),
        numberOfClientsField.getText(),
        sizeOfArrayField.getText(),
        fromField.getText(),
        toField.getText(),
        stepField.getText(),
        architectureChoiceBox.getItems().indexOf(architectureChoiceBox.getValue()),
        variableParameterChoiceBox.getItems().indexOf(variableParameterChoiceBox.getValue()));


    Dispatcher.init(hostField.getText());
    dispatcher.startRemoteServer(config.getArchitecture());
    List<AttackResult> results = dispatcher.attack(config);
    dispatcher.stopRemoteServer();


    chart.getData().clear();

    chart.getData().add(newSeries(results,
        "client request avg time",
        res -> res.clientAverageTimePerRequest));

    chart.getData().add(newSeries(results,
        "server request avg time",
        res -> res.serverAverageRequestTime));

    chart.getData().add(newSeries(results,
        "server sorting avg time",
        res -> res.serverAverageSortingTime));
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
