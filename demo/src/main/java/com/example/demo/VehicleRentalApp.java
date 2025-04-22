package com.example.demo;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.layout.StackPane;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VehicleRentalApp extends Application {

    private String currentRole = "";
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();

    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3306/vehicle_rental"; // Database name
    private static final String USER = "root"; // Your MySQL username
    private static final String PASSWORD = "Karabo2018"; // Your MySQL password

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Vehicle Rental Application");
        showRegistrationScreen(primaryStage);
    }

    private Connection connectToDatabase() {
        try {
            return DriverManager.getConnection(DB_URL, USER, PASSWORD);
        } catch (SQLException e) {
            showAlert("Database Connection Error", "Could not connect to the database: " + e.getMessage());
            return null;
        }
    }

    private void loadVehiclesFromDatabase() {
        String sql = "SELECT vehicle_id, brand, model, category, rental_price, availability_status FROM vehicles";
        try (Connection conn = connectToDatabase(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            vehicles.clear();
            while (rs.next()) {
                int id = rs.getInt("vehicle_id");
                String brand = rs.getString("brand");
                String model = rs.getString("model");
                String category = rs.getString("category");
                double rentalPrice = rs.getDouble("rental_price");
                boolean availabilityStatus = rs.getBoolean("availability_status");
                vehicles.add(new Vehicle(id, brand, model, category, rentalPrice, availabilityStatus));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load vehicles: " + e.getMessage());
        }
    }

    private void loadCustomersFromDatabase() {
        String sql = "SELECT customer_id, name, contact_info, driving_license FROM customers";
        try (Connection conn = connectToDatabase(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            customers.clear();
            while (rs.next()) {
                int id = rs.getInt("customer_id");
                String name = rs.getString("name");
                String contact = rs.getString("contact_info");
                String license = rs.getString("driving_license");
                customers.add(new Customer(id, name, contact, license)); // Updated to include ID
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load customers: " + e.getMessage());
        }
    }

    private void loadBookingsFromDatabase() {
        String sql = "SELECT c.name AS customer_name, v.brand, v.model, b.start_date, b.end_date " +
                "FROM bookings b " +
                "INNER JOIN vehicles v ON b.vehicle_id = v.vehicle_id " +
                "INNER JOIN customers c ON b.customer_id = c.customer_id";
        try (Connection conn = connectToDatabase();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            bookings.clear();
            while (rs.next()) {
                String customerName = rs.getString("customer_name");
                Vehicle vehicle = new Vehicle(0, rs.getString("brand"), rs.getString("model"), "", 0, false);
                LocalDate startDate = rs.getDate("start_date").toLocalDate();
                LocalDate endDate = rs.getDate("end_date").toLocalDate();
                bookings.add(new Booking(customerName, vehicle, startDate.toString(), endDate.toString()));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load bookings: " + e.getMessage());
        }
    }

    private void showRegistrationScreen(Stage stage) {
        // Registration screen setup
        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        TextField usernameField = new TextField();
        usernameField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #BDBDBD; -fx-border-radius: 5px; -fx-padding: 8px;");
        Label passLabel = new Label("Password:");
        passLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        PasswordField passwordField = new PasswordField();
        passwordField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #BDBDBD; -fx-border-radius: 5px; -fx-padding: 8px;");
        Label roleLabel = new Label("Select Role:");
        roleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton adminBtn = new RadioButton("Admin");
        adminBtn.setToggleGroup(roleGroup);
        RadioButton employeeBtn = new RadioButton("Employee");
        employeeBtn.setToggleGroup(roleGroup);
        adminBtn.setSelected(true);

        Button registerBtn = new Button("Register");
        registerBtn.setOnAction(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            currentRole = adminBtn.isSelected() ? "Admin" : "Employee";
            if (user.isEmpty() || pass.isEmpty()) {
                showAlert("Error", "Username and password cannot be empty!");
                return;
            }

            registerUserToDatabase(new User(user, pass, currentRole));
            showAlert("Success", "User registered successfully! You can now log in.");
            showLoginScreen(stage);
        });
        registerBtn.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");

        VBox vbox = new VBox(10, userLabel, usernameField, passLabel, passwordField, roleLabel, adminBtn, employeeBtn, registerBtn);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #FFC0CB;"); // Pink background for registration
        stage.setScene(new Scene(vbox, 300, 300));
        stage.show();
    }

    private void showLoginScreen(Stage stage) {
        // Login screen setup
        Label userLabel = new Label("Username:");
        userLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        TextField usernameField = new TextField();
        usernameField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #BDBDBD; -fx-border-radius: 5px; -fx-padding: 8px;");
        Label passLabel = new Label("Password:");
        passLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        PasswordField passwordField = new PasswordField();
        passwordField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #BDBDBD; -fx-border-radius: 5px; -fx-padding: 8px;");
        Button loginBtn = new Button("Login");
        loginBtn.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");
        loginBtn.setOnAction(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            User authenticatedUser = loginUserFromDatabase(user, pass);
            if (authenticatedUser != null) {
                currentRole = authenticatedUser.role;
                showDashboard(stage);
            } else {
                showAlert("Login Failed", "Incorrect credentials!");
            }
        });

        VBox vbox = new VBox(10, userLabel, usernameField, passLabel, passwordField, loginBtn);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #FFB6C1;"); // A light girlish color for login
        stage.setScene(new Scene(vbox, 300, 250));
        stage.show();
    }

    private void showDashboard(Stage stage) {
        loadVehiclesFromDatabase();
        loadCustomersFromDatabase(); // Load customers here
        loadBookingsFromDatabase();

        Label welcomeLabel = new Label("WELCOME TO PHATE'S VEHICLE RENTAL SYSTEM");
        welcomeLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 10px; -fx-alignment: center;");

        HBox topSection = new HBox();
        topSection.setStyle("-fx-background-color: purple; -fx-padding: 10px; -fx-alignment: center;");
        topSection.getChildren().add(welcomeLabel);

        TabPane tabPane = new TabPane(); // Create TabPane to hold the tabs
        if (currentRole.equals("Admin")) {
            tabPane.getTabs().addAll(createVehicleManagementTab(), createCustomerManagementTab(), createPaymentTab(), createReportsTab());
        } else {
            tabPane.getTabs().addAll(createBookingTab(), createPaymentTab());
        }

        VBox adminVBox = new VBox(15); // Use VBox to set spacing between the header and the tabs
        adminVBox.setPadding(new Insets(20));
        adminVBox.setStyle("-fx-background-color: #FFDDC1;"); // Light background color
        adminVBox.getChildren().add(tabPane); // Add the TabPane to the VBox

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> showRegistrationScreen(stage));
        logoutButton.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");

        topSection.getChildren().add(logoutButton);

        VBox mainLayout = new VBox(topSection, adminVBox); // Combine top section and admin VBox
        Scene scene = new Scene(mainLayout, 800, 600);
        stage.setTitle(currentRole + " Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private Tab createVehicleManagementTab() {
        Tab vehicleTab = new Tab("Vehicle Management");
        VBox vbox = new VBox(10);
        TableView<Vehicle> table = createVehicleTable();

        vbox.getChildren().add(table);
        HBox input = new HBox(10);
        TextField brandField = new TextField();
        brandField.setPromptText("Brand");
        TextField modelField = new TextField();
        modelField.setPromptText("Model");
        ComboBox<String> categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().addAll("Car", "Bike", "Van", "Truck");
        categoryComboBox.setPromptText("Category");
        TextField priceField = new TextField();
        priceField.setPromptText("Rental Price");
        ComboBox<String> statusBox = createStatusComboBox();

        Button addBtn = new Button("Add Vehicle");
        Button deleteBtn = new Button("Delete Selected");
        Button backButton = createBackButton();

        addBtn.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");
        deleteBtn.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");
        backButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");

        addBtn.setOnAction(e -> {
            try {
                Vehicle v = new Vehicle(
                        0, // ID will be assigned in the database
                        brandField.getText(),
                        modelField.getText(),
                        categoryComboBox.getValue(),
                        Double.parseDouble(priceField.getText()),
                        statusBox.getValue().equals("Available")
                );
                vehicles.add(v);
                addVehicleToDatabase(v);
                refreshVehicleTable(table);
                clearVehicleFields(brandField, modelField, categoryComboBox, priceField);
            } catch (Exception ex) {
                showAlert("Error", "Invalid input: " + ex.getMessage());
            }
        });

        deleteBtn.setOnAction(e -> {
            Vehicle selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                vehicles.remove(selected);
                deleteVehicleFromDatabase(selected);
                refreshVehicleTable(table);
            }
        });

        input.getChildren().addAll(brandField, modelField, categoryComboBox, priceField, statusBox, addBtn, deleteBtn);
        vbox.getChildren().addAll(input, backButton);
        vehicleTab.setContent(vbox);
        return vehicleTab;
    }

    private void addVehicleToDatabase(Vehicle vehicle) {
        String sql = "INSERT INTO vehicles (brand, model, category, rental_price, availability_status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vehicle.getBrand());
            pstmt.setString(2, vehicle.getModel());
            pstmt.setString(3, vehicle.getCategory());
            pstmt.setDouble(4, vehicle.getRentalPrice());
            pstmt.setBoolean(5, vehicle.getAvailabilityStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Could not add vehicle: " + e.getMessage());
        }
    }

    private void deleteVehicleFromDatabase(Vehicle vehicle) {
        String sql = "DELETE FROM vehicles WHERE vehicle_id = ?";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, vehicle.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Could not delete vehicle: " + e.getMessage());
        }
    }

    private TableView<Vehicle> createVehicleTable() {
        TableView<Vehicle> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(vehicles));

        TableColumn<Vehicle, Integer> idCol = new TableColumn<>("Vehicle ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Vehicle, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));

        TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Vehicle, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Vehicle, Double> priceCol = new TableColumn<>("Rental Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("rentalPrice"));

        TableColumn<Vehicle, Boolean> statusCol = new TableColumn<>("Availability Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("availabilityStatus"));

        table.getColumns().addAll(idCol, brandCol, modelCol, categoryCol, priceCol, statusCol);
        return table;
    }

    private ComboBox<String> createStatusComboBox() {
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Available", "Rented");
        statusBox.setValue("Available");
        return statusBox;
    }

    private void clearVehicleFields(TextField brandField, TextField modelField, ComboBox<String> categoryComboBox, TextField priceField) {
        brandField.clear();
        modelField.clear();
        categoryComboBox.setValue(null);
        priceField.clear();
    }

    private void refreshVehicleTable(TableView<Vehicle> table) {
        table.setItems(FXCollections.observableArrayList(vehicles));
    }

    private Tab createCustomerManagementTab() {
        Tab customerTab = new Tab("Customer Management");
        VBox vbox = new VBox(10);
        TableView<Customer> table = createCustomerTable();
        loadCustomersFromDatabase(); // Load customers when initializing the table
        vbox.getChildren().add(table);

        HBox input = new HBox(10);
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        TextField contactField = new TextField();
        contactField.setPromptText("Contact Info");
        TextField licenseField = new TextField();
        licenseField.setPromptText("Driving License");

        Button addCustomerBtn = new Button("Add Customer");
        Button updateCustomerBtn = new Button("Update Customer");
        Button deleteCustomerBtn = new Button("Delete Customer");
        Button backButton = createBackButton();

        // Add styles to buttons
        addCustomerBtn.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");
        updateCustomerBtn.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black; -fx-padding: 10px 20px; -fx-background-radius: 10px;");
        deleteCustomerBtn.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");

        addCustomerBtn.setOnAction(e -> {
            String name = nameField.getText();
            String contact = contactField.getText();
            String license = licenseField.getText();
            if (!name.isEmpty() && !contact.isEmpty() && !license.isEmpty()) {
                Customer newCustomer = new Customer(0, name, contact, license); // ID will be assigned in the database
                addCustomerToDatabase(newCustomer);
                customers.add(newCustomer);
                refreshCustomerTable(table);
                clearCustomerFields(nameField, contactField, licenseField);
            } else {
                showAlert("Error", "Fill in all fields!");
            }
        });

        updateCustomerBtn.setOnAction(e -> {
            Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selected.setName(nameField.getText());
                selected.setContact(contactField.getText());
                selected.setLicense(licenseField.getText());
                updateCustomerInDatabase(selected);
                refreshCustomerTable(table);
            }
        });

        deleteCustomerBtn.setOnAction(e -> {
            Customer selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteCustomerFromDatabase(selected);
                customers.remove(selected);
                refreshCustomerTable(table);
            }
        });

        input.getChildren().addAll(nameField, contactField, licenseField, addCustomerBtn, updateCustomerBtn, deleteCustomerBtn);
        vbox.getChildren().addAll(input, backButton);
        customerTab.setContent(vbox);
        return customerTab;
    }

    private void clearCustomerFields(TextField nameField, TextField contactField, TextField licenseField) {
    }

    private void refreshCustomerTable(TableView<Customer> table) {
        table.setItems(customers);
    }

    private void addCustomerToDatabase(Customer customer) {
        String sql = "INSERT INTO customers (name, contact_info, driving_license) VALUES (?, ?, ?)";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getContact());
            pstmt.setString(3, customer.getLicense());
            pstmt.executeUpdate();

            // Retrieve the generated ID
            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                customer.setId(generatedKeys.getInt(1)); // Set the generated ID for the customer
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not add customer: " + e.getMessage());
        }
    }

    private void updateCustomerInDatabase(Customer customer) {
        String sql = "UPDATE customers SET contact_info = ?, driving_license = ? WHERE customer_id = ?";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customer.getContact());
            pstmt.setString(2, customer.getLicense());
            pstmt.setInt(3, customer.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Could not update customer: " + e.getMessage());
        }
    }

    private void deleteCustomerFromDatabase(Customer customer) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customer.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Could not delete customer: " + e.getMessage());
        }
    }

    private TableView<Customer> createCustomerTable() {
        TableView<Customer> table = new TableView<>();
        table.setItems(customers);

        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Customer, String> contactCol = new TableColumn<>("Contact Info");
        contactCol.setCellValueFactory(new PropertyValueFactory<>("contact"));

        TableColumn<Customer, String> licenseCol = new TableColumn<>("Driving License Number");
        licenseCol.setCellValueFactory(new PropertyValueFactory<>("license"));

        table.getColumns().addAll(nameCol, contactCol, licenseCol);
        return table;
    }

    private Tab createReportsTab() {
        Tab reportsTab = new Tab("Reports");
        VBox vbox = new VBox(10);

        Button showChartsButton = new Button("Show Revenue and Vehicle Reports");
        showChartsButton.setOnAction(e -> showCharts());

        Button exportBookingsButton = new Button("Export Bookings to CSV");
        exportBookingsButton.setOnAction(e -> exportBookingsToCSV());

        Button exportVehiclesButton = new Button("Export Vehicles to CSV");
        exportVehiclesButton.setOnAction(e -> exportVehiclesToCSV());

        Button exportCustomersButton = new Button("Export Customers to CSV");
        exportCustomersButton.setOnAction(e -> exportCustomersToCSV());

        vbox.getChildren().addAll(showChartsButton, exportBookingsButton, exportVehiclesButton, exportCustomersButton, createBackButton());
        reportsTab.setContent(vbox);
        return reportsTab;
    }

    private void showCharts() {
        Stage chartStage = new Stage();
        TabPane tabPane = new TabPane();

        Tab pieChartTab = createPieChartTab();
        Tab barChartTab = createBarChartTab();
        Tab lineChartTab = createLineChartTab();

        tabPane.getTabs().addAll(pieChartTab, barChartTab, lineChartTab);

        Scene scene = new Scene(tabPane, 800, 600);
        chartStage.setTitle("Data Visualization");
        chartStage.setScene(scene);
        chartStage.show();
    }

    private Tab createPieChartTab() {
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Bookings by Vehicle Category");

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("Car", calculateBookingsByCategory("Car")),
                new PieChart.Data("Bike", calculateBookingsByCategory("Bike")),
                new PieChart.Data("Van", calculateBookingsByCategory("Van")),
                new PieChart.Data("Truck", calculateBookingsByCategory("Truck"))
        );

        pieChart.setData(pieChartData);
        return new Tab("Pie Chart", new StackPane(pieChart));
    }

    private Tab createBarChartTab() {
        BarChart<String, Number> barChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        barChart.setTitle("Revenue by Vehicle Category");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        series.getData().add(new XYChart.Data<>("Car", calculateRevenueByCategory("Car")));
        series.getData().add(new XYChart.Data<>("Bike", calculateRevenueByCategory("Bike")));
        series.getData().add(new XYChart.Data<>("Van", calculateRevenueByCategory("Van")));
        series.getData().add(new XYChart.Data<>("Truck", calculateRevenueByCategory("Truck")));

        barChart.getData().add(series);
        return new Tab("Bar Chart", new StackPane(barChart));
    }

    private Tab createLineChartTab() {
        LineChart<Number, Number> lineChart = new LineChart<>(new NumberAxis(), new NumberAxis());
        lineChart.setTitle("Revenue Over Time");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Revenue");

        for (int month = 1; month <= 12; month++) {
            series.getData().add(new XYChart.Data<>(month, getRevenueForMonth(month)));
        }

        lineChart.getData().add(series);
        return new Tab("Line Chart", new StackPane(lineChart));
    }

    private int calculateBookingsByCategory(String category) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM bookings INNER JOIN vehicles ON bookings.vehicle_id = vehicles.vehicle_id WHERE vehicles.category = ?";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not count bookings: " + e.getMessage());
        }
        return count;
    }

    private double calculateRevenueByCategory(String category) {
        double total = 0;
        String sql = "SELECT SUM(rental_price) FROM bookings INNER JOIN vehicles ON bookings.vehicle_id = vehicles.vehicle_id WHERE vehicles.category = ? AND bookings.status = 'Active'";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble(1);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not calculate revenue: " + e.getMessage());
        }
        return total;
    }

    private double getRevenueForMonth(int month) {
        double total = 0;
        String sql = "SELECT SUM(rental_price) FROM bookings INNER JOIN vehicles ON bookings.vehicle_id = vehicles.vehicle_id WHERE MONTH(start_date) = ? AND bookings.status = 'Active'";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, month);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                total = rs.getDouble(1);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not calculate revenue: " + e.getMessage());
        }
        return total;
    }

    private Tab createBookingTab() {
        Tab bookingTab = new Tab("Booking");
        VBox vbox = new VBox(10);
        TableView<Booking> table = createBookingTable();
        vbox.getChildren().add(table);
        loadBookingsFromDatabase();

        HBox input = new HBox(10);
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>();
        vehicleComboBox.getItems().addAll(vehicles);
        TextField customerNameField = new TextField();
        customerNameField.setPromptText("Customer Name");
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();

        Button bookVehicleBtn = new Button("Book Vehicle");
        Button cancelBookingBtn = new Button("Cancel Booking");

        bookVehicleBtn.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");
        cancelBookingBtn.setStyle("-fx-background-color: #E57373; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");

        bookVehicleBtn.setOnAction(e -> {
            Vehicle selectedVehicle = vehicleComboBox.getValue();
            String customerName = customerNameField.getText();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (selectedVehicle != null && !customerName.isEmpty() && startDate != null && endDate != null) {
                if (selectedVehicle.getAvailabilityStatus()) {
                    Booking booking = new Booking(customerName, selectedVehicle, startDate.toString(), endDate.toString());
                    bookings.add(booking);
                    selectedVehicle.setAvailabilityStatus(false);
                    addBookingToDatabase(booking);
                    refreshBookingTable(table);
                    showAlert("Success", "Vehicle booked successfully!");
                } else {
                    showAlert("Error", "Vehicle is already rented!");
                }
            } else {
                showAlert("Error", "Please fill all fields!");
            }
        });

        cancelBookingBtn.setOnAction(e -> {
            Booking selectedBooking = table.getSelectionModel().getSelectedItem();
            if (selectedBooking != null) {
                selectedBooking.getVehicle().setAvailabilityStatus(true);
                bookings.remove(selectedBooking);
                deleteBookingFromDatabase(selectedBooking);
                refreshBookingTable(table);
                showAlert("Success", "Booking canceled successfully!");
            }
        });

        input.getChildren().addAll(vehicleComboBox, customerNameField, startDatePicker, endDatePicker, bookVehicleBtn, cancelBookingBtn);
        vbox.getChildren().addAll(input);
        bookingTab.setContent(vbox);
        return bookingTab;
    }

    private void addBookingToDatabase(Booking booking) {
        String sql = "INSERT INTO bookings (customer_id, vehicle_id, start_date, end_date, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int customerId = getCustomerIdByName(booking.getCustomerName());
            if (customerId == -1) {
                showAlert("Error", "Customer not found!");
                return;
            }

            pstmt.setInt(1, customerId);
            pstmt.setInt(2, booking.getVehicle().getId());
            pstmt.setString(3, booking.getStartDate());
            pstmt.setString(4, booking.getEndDate());
            pstmt.setString(5, "Active");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Could not add booking: " + e.getMessage());
        }
    }

    private void deleteBookingFromDatabase(Booking booking) {
        String sql = "DELETE FROM bookings WHERE customer_id = ? AND vehicle_id = ? AND start_date = ? AND end_date = ?";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int customerId = getCustomerIdByName(booking.getCustomerName());
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, booking.getVehicle().getId());
            pstmt.setString(3, booking.getStartDate());
            pstmt.setString(4, booking.getEndDate());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Could not delete booking: " + e.getMessage());
        }
    }

    private TableView<Booking> createBookingTable() {
        TableView<Booking> table = new TableView<>();
        table.setItems(bookings);

        TableColumn<Booking, String> customerNameCol = new TableColumn<>("Customer Name");
        customerNameCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Booking, String> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getVehicle().toString()));

        TableColumn<Booking, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));

        TableColumn<Booking, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        table.getColumns().addAll(customerNameCol, vehicleCol, startDateCol, endDateCol);
        return table;
    }

    private void refreshBookingTable(TableView<Booking> table) {
        table.setItems(bookings);
    }

    private Tab createPaymentTab() {
        Tab paymentTab = new Tab("Payment");
        VBox vbox = new VBox(10);
        TextField customerNameField = new TextField();
        customerNameField.setPromptText("Customer Name");
        TextField rentalDaysField = new TextField();
        rentalDaysField.setPromptText("Rental Days");
        ComboBox<String> paymentMethodComboBox = new ComboBox<>();
        paymentMethodComboBox.getItems().addAll("Cash", "Credit Card", "Online");
        paymentMethodComboBox.setValue("Cash");
        CheckBox insuranceCheckBox = new CheckBox("Add Insurance ($10 per day)");
        CheckBox lateFeeCheckBox = new CheckBox("Add Late Fee ($20)");

        Button processPaymentBtn = new Button("Process Payment");
        TextArea invoiceArea = new TextArea();
        invoiceArea.setEditable(false);

        processPaymentBtn.setOnAction(e -> {
            String customerName = customerNameField.getText();
            int rentalDays;
            double totalFee = 0;

            try {
                rentalDays = Integer.parseInt(rentalDaysField.getText());
                double rentalFee = rentalDays * 30; // Assuming a constant rental price
                totalFee += rentalFee;

                if (insuranceCheckBox.isSelected()) {
                    totalFee += rentalDays * 10; // Additional fee for insurance
                }

                if (lateFeeCheckBox.isSelected()) {
                    totalFee += 20; // Flat fee for late return
                }

                String invoice = String.format("Customer Name: %s\nRental Days: %d\nRental Fee: $%.2f\n", customerName, rentalDays, rentalFee);
                invoice += "Payment Method: " + paymentMethodComboBox.getValue() + "\n";
                if (insuranceCheckBox.isSelected()) {
                    invoice += String.format("Insurance Fee: $%d\n", rentalDays * 10);
                }
                if (lateFeeCheckBox.isSelected()) {
                    invoice += "Late Fee: $20\n";
                }
                invoice += String.format("Total Amount Due: $%.2f\n", totalFee);
                invoiceArea.setText(invoice);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Please enter a valid number for rental days.");
            }
        });

        vbox.getChildren().addAll(
                customerNameField,
                rentalDaysField,
                paymentMethodComboBox,
                insuranceCheckBox,
                lateFeeCheckBox,
                processPaymentBtn,
                invoiceArea
        );
        paymentTab.setContent(vbox);
        return paymentTab;
    }

    private Button createBackButton() {
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> showDashboard((Stage) backButton.getScene().getWindow()));
        backButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-padding: 10px 20px; -fx-background-radius: 10px;");
        return backButton;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void exportBookingsToCSV() {
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Customer Name,Vehicle,Start Date,End Date\n");

        for (Booking booking : bookings) {
            csvContent.append(String.format("%s,%s,%s,%s\n",
                    booking.getCustomerName(),
                    booking.getVehicle().toString(),
                    booking.getStartDate(),
                    booking.getEndDate()));
        }

        saveToCSV("exported_reports", "bookings_report.csv", csvContent.toString());
    }

    private void exportVehiclesToCSV() {
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Vehicle ID,Brand,Model,Category,Rental Price,Availability Status\n");

        for (Vehicle vehicle : vehicles) {
            csvContent.append(String.format("%d,%s,%s,%s,%.2f,%s\n",
                    vehicle.getId(),
                    vehicle.getBrand(),
                    vehicle.getModel(),
                    vehicle.getCategory(),
                    vehicle.getRentalPrice(),
                    vehicle.getAvailabilityStatus() ? "Available" : "Rented"));
        }

        saveToCSV("exported_reports", "vehicles_report.csv", csvContent.toString());
    }

    private void exportCustomersToCSV() {
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Customer ID,Name,Contact Info,Driving License\n");

        for (Customer customer : customers) {
            csvContent.append(String.format("%d,%s,%s,%s\n",
                    customer.getId(),
                    customer.getName(),
                    customer.getContact(),
                    customer.getLicense()));
        }

        saveToCSV("exported_reports", "customers_report.csv", csvContent.toString());
    }

    private void saveToCSV(String directoryName, String fileName, String content) {
        File directory = new File(directoryName);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File csvFile = new File(directory, fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
            writer.write(content);
            showAlert("Export Success", "Data exported successfully to " + csvFile.getAbsolutePath());
        } catch (IOException e) {
            showAlert("Export Error", "Could not export data: " + e.getMessage());
        }
    }

    public static class User {
        String username, password, role;

        User(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }

    public static class Vehicle {
        private int id;
        private String brand;
        private String model;
        private String category;
        private double rentalPrice;
        private boolean availabilityStatus;

        public Vehicle(int id, String brand, String model, String category, double rentalPrice, boolean availabilityStatus) {
            this.id = id;
            this.brand = brand;
            this.model = model;
            this.category = category;
            this.rentalPrice = rentalPrice;
            this.availabilityStatus = availabilityStatus;
        }

        public int getId() {
            return id;
        }
        public String getBrand() {
            return brand;
        }
        public String getModel() {
            return model;
        }
        public String getCategory() {
            return category;
        }
        public double getRentalPrice() {
            return rentalPrice;
        }
        public boolean getAvailabilityStatus() {
            return availabilityStatus;
        }

        public void setAvailabilityStatus(boolean availabilityStatus) {
            this.availabilityStatus = availabilityStatus;
        }

        @Override
        public String toString() {
            return id + " - " + brand + " " + model;
        }
    }

    public static class Customer {
        private int id; // Added ID to store customer ID from database
        private String name;
        private String contact;
        private String license;

        public Customer(int id, String name, String contact, String license) {
            this.id = id; // Initialize ID
            this.name = name;
            this.contact = contact;
            this.license = license;
        }

        public int getId() {
            return id; // Getter for ID
        }
        public String getName() {
            return name;
        }
        public String getContact() {
            return contact;
        }
        public String getLicense() {
            return license;
        }

        public void setId(int id) {
            this.id = id; // Setter for ID
        }
        public void setName(String name) {
            this.name = name;
        }
        public void setContact(String contact) {
            this.contact = contact;
        }
        public void setLicense(String license) {
            this.license = license;
        }
    }

    public static class Booking {
        private String customerName;
        private Vehicle vehicle;
        private String startDate;
        private String endDate;

        public Booking(String customerName, Vehicle vehicle, String startDate, String endDate) {
            this.customerName = customerName;
            this.vehicle = vehicle;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public String getCustomerName() {
            return customerName;
        }
        public Vehicle getVehicle() {
            return vehicle;
        }
        public String getStartDate() {
            return startDate;
        }
        public String getEndDate() {
            return endDate;
        }
    }

    private void registerUserToDatabase(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.username);
            pstmt.setString(2, user.password);
            pstmt.setString(3, user.role);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("Database Error", "Could not register user: " + e.getMessage());
        }
    }

    private User loginUserFromDatabase(String username, String password) {
        String sql = "SELECT username, password, role FROM users WHERE username = ? AND password = ?";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getString("username"), rs.getString("password"), rs.getString("role"));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not authenticate user: " + e.getMessage());
        }
        return null;
    }

    private int getCustomerIdByName(String customerName) {
        String sql = "SELECT customer_id FROM customers WHERE name = ?";
        try (Connection conn = connectToDatabase(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, customerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("customer_id");
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Could not retrieve customer ID: " + e.getMessage());
        }
        return -1; // Not found
    }
}