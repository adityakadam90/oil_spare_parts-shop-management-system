import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ShopManagementApp extends JFrame {
    private Connection connection;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    public ShopManagementApp() {
        super("*****  Ganesh Auto-Mobile Bambwade ******");

        // Initialize GUI components
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        JButton viewProductsButton = new JButton("View Products");
        JButton viewSalesButton = new JButton("View Sales");
        JButton addProductButton = new JButton("Add Product");
        JButton recordSaleButton = new JButton("Record Sale");
        JButton searchButton = new JButton("Search");
        searchField = new JTextField();

        // Add action listeners
        viewProductsButton.addActionListener(e -> viewProducts());
        viewSalesButton.addActionListener(e -> viewSales());
        addProductButton.addActionListener(e -> addProduct());
        recordSaleButton.addActionListener(e -> recordSale());
        searchButton.addActionListener(e -> searchProducts());

        // Add components to the frame
        JPanel buttonPanel = new JPanel(new GridLayout(5, 1));
        buttonPanel.add(viewProductsButton);
        buttonPanel.add(viewSalesButton);
        buttonPanel.add(addProductButton);
        buttonPanel.add(recordSaleButton);

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        Container contentPane = getContentPane();
        contentPane.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(buttonPanel, BorderLayout.EAST);
        contentPane.add(searchPanel, BorderLayout.NORTH);

        // Connect to the database
        connectToDatabase();

        // Set up frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null); // Center the frame
        setVisible(true);
    }

    private void connectToDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/myshop";
            String user = "root";
            String password = "@#aditya2006";
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to connect to the database.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void viewProducts() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM products");
            tableModel.setColumnIdentifiers(new String[]{"ID", "Name", "Quantity", "Price"});
            tableModel.setRowCount(0); // Clear existing rows
            while (resultSet.next()) {
                tableModel.addRow(new Object[]{
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("quantity"),
                        String.format("$%.2f", resultSet.getBigDecimal("price"))
                });
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to retrieve products.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewSales() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM sales");
            tableModel.setColumnIdentifiers(new String[]{"ID", "Product ID", "Quantity", "Total Price", "Sale Date"});
            tableModel.setRowCount(0); // Clear existing rows
            while (resultSet.next()) {
                tableModel.addRow(new Object[]{
                        resultSet.getInt("id"),
                        resultSet.getInt("product_id"),
                        resultSet.getInt("quantity"),
                        String.format("$%.2f", resultSet.getBigDecimal("total_price")),
                        resultSet.getDate("sale_date")
                });
            }
            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to retrieve sales.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addProduct() {
        String name = JOptionPane.showInputDialog(this, "Enter product name:");
        if (name != null && !name.trim().isEmpty()) {
            try {
                int quantity = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter product quantity:"));
                double price = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter product price:"));
                PreparedStatement statement = connection.prepareStatement("INSERT INTO products (name, quantity, price) VALUES (?, ?, ?)");
                statement.setString(1, name);
                statement.setInt(2, quantity);
                statement.setDouble(3, price);
                int rowsInserted = statement.executeUpdate();
                if (rowsInserted > 0) {
                    JOptionPane.showMessageDialog(this, "Product added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add product.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                statement.close();
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void recordSale() {
        String productIdStr = JOptionPane.showInputDialog(this, "Enter product ID:");
        if (productIdStr != null && !productIdStr.trim().isEmpty()) {
            try {
                int productId = Integer.parseInt(productIdStr);
                int quantitySold = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter quantity sold:"));
                double totalPrice = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter total price:"));

                connection.setAutoCommit(false); // Start transaction

                // Check product availability
                PreparedStatement checkProductStmt = connection.prepareStatement("SELECT quantity FROM products WHERE id = ?");
                checkProductStmt.setInt(1, productId);
                ResultSet resultSet = checkProductStmt.executeQuery();
                if (resultSet.next()) {
                    int currentQuantity = resultSet.getInt("quantity");
                    if (currentQuantity >= quantitySold) {
                        // Update product quantity
                        PreparedStatement updateProductStmt = connection.prepareStatement("UPDATE products SET quantity = quantity - ? WHERE id = ?");
                        updateProductStmt.setInt(1, quantitySold);
                        updateProductStmt.setInt(2, productId);
                        updateProductStmt.executeUpdate();

                        // Record the sale
                        PreparedStatement insertSaleStmt = connection.prepareStatement("INSERT INTO sales (product_id, quantity, total_price, sale_date) VALUES (?, ?, ?, CURDATE())");
                        insertSaleStmt.setInt(1, productId);
                        insertSaleStmt.setInt(2, quantitySold);
                        insertSaleStmt.setDouble(3, totalPrice);
                        insertSaleStmt.executeUpdate();

                        connection.commit(); // Commit transaction
                        JOptionPane.showMessageDialog(this, "Sale recorded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Insufficient stock.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Product not found.", "Error", JOptionPane.ERROR_MESSAGE);
                }

                connection.setAutoCommit(true); // End transaction
                resultSet.close();
                checkProductStmt.close();
            } catch (SQLException | NumberFormatException e) {
                e.printStackTrace();
                try {
                    connection.rollback(); // Rollback transaction on error
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Invalid input or transaction failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void searchProducts() {
        String searchQuery = searchField.getText().trim();
        if (!searchQuery.isEmpty()) {
            try {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM products WHERE name LIKE ?");
                statement.setString(1, "%" + searchQuery + "%");
                ResultSet resultSet = statement.executeQuery();
                tableModel.setColumnIdentifiers(new String[]{"ID", "Name", "Quantity", "Price"});
                tableModel.setRowCount(0); // Clear existing rows
                while (resultSet.next()) {
                    tableModel.addRow(new Object[]{
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("quantity"),
                            String.format("$%.2f", resultSet.getBigDecimal("price"))
                    });
                }
                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to search products.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ShopManagementApp app = new ShopManagementApp();
            app.setTableCellCenterAlignment();
        });
    }

    private void setTableCellCenterAlignment() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }
}
