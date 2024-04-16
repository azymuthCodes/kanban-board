package com.kanbanplus.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.util.*;

import com.kanbanplus.classes.Card;
import com.kanbanplus.classes.KanbanBoard;
import com.kanbanplus.classes.KanbanList;
import com.kanbanplus.classes.PriorityLevel;
import com.kanbanplus.database.database;

public class KanbanBoardGUI extends Application {
    private Map<String, KanbanList> lists; // Store lists and their corresponding notes
    private Map<String, String> listStages; // To store the stage for each list
    // Map to store the priority of each note. Note IDs as keys for simplicity in this example.
    private Map<String, PriorityLevel> notePriorities;
    // Class-level variable
    private Map<String, PriorityLevel> listPriorities; // Store the priority for each list

    private Connection connector = database.openConnection();
    private KanbanBoard board;
    private String user;

    @Override
    public void start(Stage primaryStage) {
        lists = new HashMap<>();
        listStages = new HashMap<>();
        notePriorities = new HashMap<>();
        listPriorities = new HashMap<>(); // Initialize list priorities map

        Scene scene = createLoginScene(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Kanban Board Plus Login");
        primaryStage.setOnCloseRequest(event->{
            board.getLists().clear();
            for (Map.Entry<String,KanbanList> entry : lists.entrySet()) {
                board.addToList(entry.getValue());
            }
            database.saveBoard(connector,board,database.getID(connector, user));
        });
        primaryStage.show();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null); // No header text
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private Scene createLoginScene(Stage primaryStage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Label userName = new Label("User Name:");
        TextField userTextField = new TextField();
        Label pw = new Label("Password:");
        PasswordField pwBox = new PasswordField();
        Button btn = new Button("Sign in");

        btn.setOnAction(e -> {
            user = userTextField.getText();
            String password = pwBox.getText();
            try{
                if (database.checkPassword(connector, user, password)) {
                    // Successfully authenticated, show Kanban Board
                    board  = database.getBoard(connector, database.getID(connector, user));
                    if(board == null){
                        board =  new KanbanBoard("#b1","Board 1");
                        database.storeBoard(connector,board, database.getID(connector, user));
                    }
                    getUserList();
                    primaryStage.setScene(createKanbanBoardScene(primaryStage));
                    primaryStage.setTitle("Kanban Board Plus");
                }
            }
            catch(Exception exception) {
                // Authentication failed, show error message
                showAlert("Authentication Failed", exception.getMessage());
            }
        });        

        grid.add(userName, 0, 1);
        grid.add(userTextField, 1, 1);
        grid.add(pw, 0, 2);
        grid.add(pwBox, 1, 2);
        grid.add(btn, 1, 4);

        return new Scene(grid, 300, 275);
    }

    private void getUserList(){
        List<KanbanList> list = board.getLists();
        Iterator<KanbanList> iterator  = list.iterator();
        while(iterator.hasNext()){
            KanbanList currentValue  = iterator.next();
            if(!lists.containsKey(currentValue.getTitle())) lists.put(currentValue.getTitle(),currentValue);
        }
    }

    private Scene createKanbanBoardScene(Stage primaryStage) {
        BorderPane borderPane = new BorderPane(); // Light background for the whole scene
        // Styling the toolbar and add list button
        BorderPane toolbarPane = new BorderPane();
        ToolBar toolBarLeft = new ToolBar();
        ToolBar toolBarRight = new ToolBar();
        toolbarPane.setLeft(toolBarLeft);
        toolbarPane.setRight(toolBarRight);
        toolbarPane.setStyle("-fx-background-color: #394867;");
        toolBarLeft.setStyle("-fx-background-color: #394867; -fx-padding: 10;"); // Darker background for Left toolbar
        toolBarRight.setStyle("-fx-background-color: #394867; -fx-padding: 10;"); // Darker background for Right toolbar
        Button addListButton = createStyledButton("Add List", "#5C6BC0", "#ffffff"); // Blue button
        Button logOutButton = createStyledButton("Log Out","#5C6BC0", "#ffffff");//Blue button for Log Out
        toolBarLeft.getItems().add(addListButton);
        toolBarRight.getItems().add(logOutButton);
        borderPane.setTop(toolbarPane);

        HBox mainContent = new HBox(1);
        mainContent.setAlignment(Pos.BASELINE_LEFT);
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(mainContent);
        scrollPane.setPadding(new Insets(5));
        borderPane.setCenter(scrollPane);

        addListButton.setOnAction(event -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add List");
            dialog.setHeaderText(null);
            dialog.setContentText("Enter the name of the list:");
            dialog.showAndWait().ifPresent(name -> {
                if (!lists.containsKey(name)) {
                    lists.put(name, new KanbanList("#1",name));
                    refreshLists(mainContent, primaryStage);
                }
            });
        });
        logOutButton.setOnAction(e->{
            board.getLists().clear();
            for (Map.Entry<String,KanbanList> entry : lists.entrySet()) {
                board.addToList(entry.getValue());
            }
            database.saveBoard(connector,board,database.getID(connector, user));
            lists.clear();
            primaryStage.setScene(createLoginScene(primaryStage));
        });

        refreshLists(mainContent, primaryStage);
        Scene returnScene = new Scene(borderPane,660, 500);
        returnScene.getStylesheets().add(getClass().getResource("css/kanban-board.css").toExternalForm());
        return returnScene;
    }

    private Button createStyledButton(String text, String bgColor, String textColor) {
        Button button = new Button(text);
        button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s;", bgColor, textColor));
        // Adding hover effect
        button.setOnMouseEntered(e -> button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-opacity: 0.85;", bgColor, textColor)));
        button.setOnMouseExited(e -> button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s;", bgColor, textColor)));
        return button;
    }

    private VBox createListPane(String listName, String listStage,KanbanList listIn) {
        VBox listPane = new VBox(10);
        listPane.setPadding(new Insets(10));
        listPane.setStyle(getListStyleByStage(listStage));
    
        // Header container for the priority ComboBox, aligned to the top-right
        HBox header = new HBox(10);
        header.setAlignment(Pos.TOP_RIGHT);
        header.setPadding(new Insets(0, 0, 10, 0)); // Add some padding at the bottom of the header
    
        // Priority label
        Label priorityLabel = new Label("Priority:");
        priorityLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333; -fx-font-size: 14px;");
    
        // Priority ComboBox
        ComboBox<PriorityLevel> priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll(PriorityLevel.LOW,PriorityLevel.MEDIUM,PriorityLevel.HIGH);
        PriorityLevel defaultPriority = listIn.getPriority() == null?PriorityLevel.LOW:listIn.getPriority();
        priorityComboBox.setValue(listPriorities.getOrDefault(listName,defaultPriority));
        priorityComboBox.setOnAction(event -> {
            // Update the priority in the listPriorities map when changed
            listPriorities.put(listName, priorityComboBox.getValue());
            listIn.setPriority(priorityComboBox.getValue());
        });

        // Add the priority label and ComboBox to the header
        header.getChildren().addAll(priorityLabel, priorityComboBox);
    
        // Styling the list name label and positioning it at the top of the list pane
        Label listNameLabel = new Label(listName);
        listNameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333; -fx-font-size: 16px;");
        listNameLabel.setMaxWidth(Double.MAX_VALUE);
        listNameLabel.setAlignment(Pos.CENTER); // Center the list name label
    
        // Add the list name label and header to the list pane
        listPane.getChildren().addAll(listNameLabel, header);
    
        return listPane;
    }
    
    
    
    private String getListStyleByStage(String listStage) {
        String color;
        switch (listStage) {
            case "To Do":
                color = "#F8DE7E"; // Yellow
                break;
            case "In Progress":
                color = "#52a9c4"; // Blue
                break;
            case "Done":
                color = "#81C784"; // Green
                break;
            default:
                color = "#eeeeee"; // Grey for undefined stages
                break;
        }
        return "-fx-background-color: " + color + "; " +
               "-fx-border-color: #222222; " +
               "-fx-background-radius: 5; " +
               "-fx-border-radius: 5;";
    }
    

    private void refreshLists(HBox mainContent, Stage primaryStage) {
        mainContent.getChildren().clear();
        lists.keySet().forEach(listName -> {
            // Retrieve the stage of the list to apply color styling
            String defaultProgress = lists.get(listName).getProgress() == null?"To Do":lists.get(listName).getProgress();
            String listStage = listStages.getOrDefault(listName,defaultProgress );
            
    
            // Create the list pane with the appropriate styling
            VBox listPane = createListPane(listName, listStage,lists.get(listName));
    
            HBox listItem = new HBox(20);
            listItem.setAlignment(Pos.CENTER_LEFT);
    
            ComboBox<String> stageComboBox = new ComboBox<>();
            stageComboBox.getItems().addAll("To Do", "In Progress", "Done");
            stageComboBox.setValue(listStage); // Use the current stage value
            stageComboBox.setOnAction(event -> {
                listStages.put(listName, stageComboBox.getValue());
                lists.get(listName).setProgress(stageComboBox.getValue());
                refreshLists(mainContent, primaryStage); // Refresh to update stage colors
            });
            listItem.getChildren().add(stageComboBox);
    
            Label listLabel = new Label(listName);
            listLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
            listItem.getChildren().add(listLabel);
    
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            listItem.getChildren().add(spacer);
    
            Button accessButton = new Button("Access");
            accessButton.setOnAction(event -> primaryStage.setScene(switchToListScene(primaryStage, listName)));
            listItem.getChildren().add(accessButton);
    
            Button deleteButton = new Button("Delete");
            deleteButton.setOnAction(event -> {
                lists.remove(listName);
                listStages.remove(listName); // Also remove the stage from the listStages map
                refreshLists(mainContent, primaryStage);
            });
            listItem.getChildren().add(deleteButton);
    
            listPane.getChildren().add(listItem);
            if(lists.get(listName).getCards()!=null){
                lists.get(listName).getCards().forEach(note -> {
                    HBox noteItem = new HBox(10);
                    noteItem.setAlignment(Pos.CENTER_LEFT);
                    
                    Label noteLabel = new Label(note.getTitle()); // Assuming 'note' is the note text or ID
                    noteLabel.setStyle("-fx-padding: 5;");
                    noteItem.getChildren().add(noteLabel);
                    
                    // Dropdown for note priority
                    ComboBox<PriorityLevel> priorityComboBox = new ComboBox<>();
                    priorityComboBox.getItems().addAll(PriorityLevel.LOW, PriorityLevel.MEDIUM, PriorityLevel.HIGH);
                    PriorityLevel defaultPriority = note.getPriority() == null?PriorityLevel.LOW:note.getPriority();
                    priorityComboBox.setValue(notePriorities.getOrDefault(note.getTitle(),defaultPriority));
                    priorityComboBox.setOnAction(e -> {
                        notePriorities.put(note.getTitle(), priorityComboBox.getValue());
                        note.setPriority(priorityComboBox.getValue());
                    });
                    noteItem.getChildren().add(priorityComboBox);
                    
                    listPane.getChildren().add(noteItem);
                });
            }   
            mainContent.getChildren().add(listPane);
        });
    }

    private Scene switchToListScene(Stage primaryStage, String listName) {
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(10));
    
        ToolBar toolBar = new ToolBar();
        Button backButton = new Button("Go Back");
        backButton.setOnAction(event -> primaryStage.setScene(createKanbanBoardScene(primaryStage)));
        toolBar.getItems().add(backButton);
        borderPane.setTop(toolBar);
    
        // Main content area where the list and its tasks will be shown
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(10));
    
        // Title/header for the list
        Label listHeader = new Label("List: " + listName);
        listHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 24px;");
        mainContent.getChildren().add(listHeader);
    
        // Input field for adding a new task
        TextField newTaskField = new TextField();
        newTaskField.setPromptText("Enter a new task...");
        Button addTaskButton = new Button("Add Task");
        addTaskButton.setOnAction(event -> {
            String task = newTaskField.getText();
            if (!task.isEmpty()) {
                Card card = new Card("#1", task);
                lists.get(listName).addCard(card); // Add new task to the list
                newTaskField.clear(); // Clear the input field
                refreshListDetails(mainContent, listName); // Refresh the list details view
            }
        });
    
        HBox addTaskBar = new HBox(5, newTaskField, addTaskButton);
        mainContent.getChildren().add(addTaskBar);
    
        // Method to refresh the list details view
        refreshListDetails(mainContent, listName);
    
        borderPane.setCenter(new ScrollPane(mainContent)); // Use a ScrollPane for the content area
    
        return new Scene(borderPane, 600, 400);
    }

    private void refreshListDetails(VBox mainContent, String listName) {
        // Clear previous content but keep the header and add task bar
        if (mainContent.getChildren().size() > 2) {
            mainContent.getChildren().remove(2, mainContent.getChildren().size());
        }
    
        // Display tasks for the list
        List<Card> tasks = lists.get(listName).getCards();
        for (int i = 0; i < tasks.size(); i++) {
            Card task = tasks.get(i); // Get the current task
            HBox taskItem = new HBox(10);
            taskItem.setPadding(new Insets(5));
            taskItem.setStyle("-fx-border-color: #cccccc; -fx-padding: 10; -fx-border-radius: 5;");
    
            TextField taskField = new TextField(task.getTitle());
            taskField.setEditable(false); // Start as non-editable
            taskField.setMaxWidth(Double.MAX_VALUE);
            // HBox.setHgrow(taskField, Priority.ALWAYS);
    
            Button editButton = new Button("Edit");
            int finalI = i; // Index for use within the lambda
            editButton.setOnAction(event -> {
                taskField.setEditable(true);
                taskField.requestFocus();
                editButton.setVisible(false); // Hide the edit button when in edit mode
            });
    
            Button saveButton = new Button("Save");
            saveButton.setOnAction(event -> {
                tasks.get(finalI).setTitle(taskField.getText()); // Update the task name
                taskField.setEditable(false); // Make the field non-editable again
                editButton.setVisible(true); // Show the edit button again
                refreshListDetails(mainContent, listName); // Refresh the list details view
            });
    
            Button deleteButton = new Button("Delete");
            deleteButton.setOnAction(event -> {
                tasks.remove(finalI); // Remove task from the list
                refreshListDetails(mainContent, listName); // Refresh the list details view
            });
    
            // Only show the save button if the text field is editable
            taskField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && taskField.isEditable()) {
                    saveButton.fire();
                }
            });
    
            taskItem.getChildren().addAll(taskField, editButton, saveButton, deleteButton);
            mainContent.getChildren().add(taskItem);
        }
    }
    public static void main(String[] args){
        launch(args);
    }
            
}            
