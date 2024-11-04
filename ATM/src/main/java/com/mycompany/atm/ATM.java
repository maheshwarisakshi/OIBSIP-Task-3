/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.mycompany.atm;

/**
 *
 * @author sakshi
 */
import java.sql.*;
import java.util.Scanner;
import javax.swing.JOptionPane;

public class ATM {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/atm";
    private static final String DB_USER = "root"; 
    private static final String DB_PASSWORD = "sakshi@18";  
    
    private static Connection conn;
    private static final Scanner scanner = new Scanner(System.in);
    private static String currentUserId;
    
    
    public static void main(String[] args) {
         try {
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            if (login()) {
                showMenu();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,"Error"+e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
               JOptionPane.showMessageDialog(null,"Error"+e.getMessage());
            }
        }
    }

    private static boolean login() throws SQLException {
        System.out.print("Enter your User ID: ");
        String userId = scanner.nextLine();
        System.out.print("Enter your PIN: ");
        String pin = scanner.nextLine();

        // Check user credentials from the database
        String query = "SELECT * FROM users WHERE user_id = ? AND pin = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, userId);
        ps.setString(2, pin);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            currentUserId = userId;
            System.out.println("Login successful!");
            return true;
        } else {
            System.out.println("Invalid User ID or PIN.");
            return false;
        }
    }
    
    private static void showMenu() throws SQLException {
        while (true) {
            System.out.println("\nATM Menu:");
            System.out.println("1. Transaction History");
            System.out.println("2. Withdraw");
            System.out.println("3. Deposit");
            System.out.println("4. Transfer");
            System.out.println("5. Quit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> showTransactionHistory();
                case 2 -> withdraw();
                case 3 -> deposit();
                case 4 -> transfer();
                case 5 -> {
                    System.out.println("Thank you for using the ATM. Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static void showTransactionHistory() throws SQLException {
        String query = "SELECT * FROM transactions WHERE user_id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, currentUserId);
        ResultSet rs = ps.executeQuery();

        System.out.println("\nTransaction History:");
        while (rs.next()) {
            System.out.println("Type: " + rs.getString("type") + 
                               ", Amount: " + rs.getBigDecimal("amount") + 
                               ", Date: " + rs.getTimestamp("timestamp"));
        }
    }

    private static void withdraw() throws SQLException {
        System.out.print("Enter amount to withdraw: ");
        double amount = scanner.nextDouble();

        if (amount <= 0) {
            System.out.println("Invalid amount.");
            return;
        }

        String query = "SELECT balance FROM users WHERE user_id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, currentUserId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            double balance = rs.getDouble("balance");
            if (balance >= amount) {
                balance -= amount;

                query = "UPDATE users SET balance = ? WHERE user_id = ?";
                ps= conn.prepareStatement(query);
                ps.setDouble(1, balance);
                ps.setString(2, currentUserId);
                ps.executeUpdate();

               
                recordTransaction("Withdraw", amount);
                System.out.println("Withdrawal successful. Your new balance is: $" + balance);
            } else {
                System.out.println("Insufficient balance.");
            }
        }
    }

    // Deposit money
    private static void deposit() throws SQLException {
        System.out.print("Enter amount to deposit: ");
        double amount = scanner.nextDouble();

        if (amount <= 0) {
            System.out.println("Invalid amount.");
            return;
        }

        String query = "SELECT balance FROM users WHERE user_id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, currentUserId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            double balance = rs.getDouble("balance");
            balance += amount;

        
            query = "UPDATE users SET balance = ? WHERE user_id = ?";
            ps = conn.prepareStatement(query);
            ps.setDouble(1, balance);
            ps.setString(2, currentUserId);
            ps.executeUpdate();

            recordTransaction("Deposit", amount);
            System.out.println("Deposit successful. Your new balance is: $" + balance);
        }
    }

    private static void transfer() throws SQLException {
        System.out.print("Enter the User ID to transfer to: ");
        String targetUserId = scanner.next();
        System.out.print("Enter amount to transfer: ");
        double amount = scanner.nextDouble();

        if (amount <= 0 || targetUserId.equals(currentUserId)) {
            System.out.println("Invalid transfer");
            return;
        }

        String query = "SELECT balance FROM users WHERE user_id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, currentUserId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            double balance = rs.getDouble("balance");
            if (balance >= amount) {
                
                balance -= amount;
                query = "UPDATE users SET balance = ? WHERE user_id = ?";
                ps = conn.prepareStatement(query);
                ps.setDouble(1, balance);
                ps.setString(2, currentUserId);
                ps.executeUpdate();

                query = "UPDATE users SET balance = balance + ? WHERE user_id = ?";
                ps = conn.prepareStatement(query);
                ps.setDouble(1, amount);
                ps.setString(2, targetUserId);
                int rowsAffected = ps.executeUpdate();

                if (rowsAffected > 0) {
                    
                    recordTransaction("Transfer", amount);
                    System.out.println("Transfer successful. Your new balance is: $" + balance);
                } else {
                    System.out.println("Recipient User ID not found");
                }
            } else {
                System.out.println("Insufficient balance");
            }
        }
    }

   
    private static void recordTransaction(String type, double amount) throws SQLException {
        String query = "INSERT INTO transactions (user_id, type, amount) VALUES (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, currentUserId);
        ps.setString(2, type);
        ps.setDouble(3, amount);
        ps.executeUpdate();
    }

}
 