import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.util.Arrays;

public class Kek {

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
  private TextField fromField;

  @FXML
  private TextField toField;

  @FXML
  private TextField stepField;

  @FXML
  private ChoiceBox variableParameterChoiceBox;

  @FXML
  private ChoiceBox architectureChoiceBox;


  @FXML
  protected void handleSubmitButtonAction(ActionEvent event) throws InterruptedException {
    Window owner = submitButton.getScene().getWindow();

    if (checkIsEmpty(numberOfRequestsField, "Fill the number of requests per client") ||
        checkIsEmpty(numberOfClientsField, "Fill the number of clients to start") ||
        checkIsEmpty(sizeOfArrayField, "Fill the size of array to sort") ||
        checkIsEmpty(fromField, "Fill the \"from\" field") ||
        checkIsEmpty(toField, "Fill the \"to\" field") ||
        checkIsEmpty(stepField, "Fill the \"step\" field"))
      return;

    int requestsNumber = Integer.parseUnsignedInt(numberOfRequestsField.getText());
    int clientsNumber = Integer.parseUnsignedInt(numberOfClientsField.getText());
    int arraySize = Integer.parseUnsignedInt(sizeOfArrayField.getText());
    int from = Integer.parseUnsignedInt(fromField.getText());
    int to = Integer.parseUnsignedInt(toField.getText());
    int step = Integer.parseUnsignedInt(stepField.getText());
    int architecture = architectureChoiceBox.getItems().indexOf(architectureChoiceBox.getValue());
    int param = variableParameterChoiceBox.getItems().indexOf(variableParameterChoiceBox.getValue());

    // todo: run clients & fetch results

    chart.getData().clear();

    XYChart.Series<Number, Number> firstSeries = new XYChart.Series<>();
    firstSeries.setName("1. thread (read, process, write) per client");
    firstSeries.getData().addAll(Arrays.asList(
        new XYChart.Data<>(10, 0),
        new XYChart.Data<>(20, 10),
        new XYChart.Data<>(30, 30),
        new XYChart.Data<>(40, 55)
    ));

    XYChart.Series<Number, Number> secondSeries = new XYChart.Series<>();
    secondSeries.setName("2. thread (read), thread executor (write) per client, processing in the common executor");
    secondSeries.getData().addAll(Arrays.asList(
        new XYChart.Data<>(10, 0),
        new XYChart.Data<>(20, 25),
        new XYChart.Data<>(30, 50),
        new XYChart.Data<>(40, 75)
    ));


    XYChart.Series<Number, Number> thirdSeries = new XYChart.Series<>();
    thirdSeries.setName("3. selector for read, selector for write, processing in the common executor");
    thirdSeries.getData().addAll(Arrays.asList(
        new XYChart.Data<>(10, 0),
        new XYChart.Data<>(20, 5),
        new XYChart.Data<>(30, 20),
        new XYChart.Data<>(40, 30)
    ));

    chart.getData().addAll(firstSeries, secondSeries, thirdSeries);

    //    AlertHelper.showAlert(Alert.AlertType.INFORMATION, owner, "Kek!", "param: " + param);
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
