package shreyasapis;

import java.sql.*;
import java.util.Scanner;

import dbconnection.DBConnection;

public class UpdateCharacterStats {
    public static void Client_UpdateCharacterStats(Scanner scanner) {
        System.out.println("--- Update Character Stats ---");
        System.out.print("Enter character name: ");
        String charName = scanner.nextLine().trim();

        Integer hpCurrent = null;
        Integer hpMax = null;
        String hpChoice;
        do {
            System.out.print("Update HP? (c = current, m = max, n = none): ");
            hpChoice = scanner.nextLine().trim().toLowerCase();
        } while (!hpChoice.equals("c") && !hpChoice.equals("m") && !hpChoice.equals("n"));

        Integer mpCurrent = null;
        Integer mpMax = null;
        String mpChoice;
        do {
            System.out.print("Update MP? (c = current, m = max, n = none): ");
            mpChoice = scanner.nextLine().trim().toLowerCase();
        } while (!mpChoice.equals("c") && !mpChoice.equals("m") && !mpChoice.equals("n"));

        Integer stamCurrent = null;
        Integer stamMax = null;
        String stamChoice;
        do {
            System.out.print("Update Stamina? (c = current, m = max, n = none): ");
            stamChoice = scanner.nextLine().trim().toLowerCase();
        } while (!stamChoice.equals("c") && !stamChoice.equals("m") && !stamChoice.equals("n"));

        Integer currency = null;
        String currencyChoice;
        do {
            System.out.print("Update Currency? (y/n): ");
            currencyChoice = scanner.nextLine().trim().toLowerCase();
        } while (!currencyChoice.equals("y") && !currencyChoice.equals("n"));

        // If any "current" was chosen, fetch existing max values so we can validate input.
        Integer dbMaxHp = null;
        Integer dbMaxMp = null;
        Integer dbMaxStam = null;
        if (hpChoice.equals("c") || mpChoice.equals("c") || stamChoice.equals("c")) {
            int[] maxs = fetchMaxStats(charName);
            if (maxs == null) {
                System.out.println("Error: Character '" + charName + "' not found.");
                return;
            }
            dbMaxHp = maxs[0];
            dbMaxMp = maxs[1];
            dbMaxStam = maxs[2];
        }

        if (hpChoice.equals("c")) {
            hpCurrent = readIntBounded(scanner, "Enter current HP: ", dbMaxHp);
        } else if (hpChoice.equals("m")) {
            hpMax = readInt(scanner, "Enter max HP: ");
        }

        if (mpChoice.equals("c")) {
            mpCurrent = readIntBounded(scanner, "Enter current MP: ", dbMaxMp);
        } else if (mpChoice.equals("m")) {
            mpMax = readInt(scanner, "Enter max MP: ");
        }

        if (stamChoice.equals("c")) {
            stamCurrent = readIntBounded(scanner, "Enter current Stamina: ", dbMaxStam);
        } else if (stamChoice.equals("m")) {
            stamMax = readInt(scanner, "Enter max Stamina: ");
        }

        if (currencyChoice.equals("y")) {
            currency = readInt(scanner, "Enter currency amount: ");
        }

        System.out.println(Server_UpdateCharacterStats(
            charName,
            hpCurrent, hpMax,
            mpCurrent, mpMax,
            stamCurrent, stamMax,
            currency
        ));
    }

    private static Integer readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number, please try again.");
            }
        }
    }

    private static Integer readIntBounded(Scanner scanner, String prompt, Integer maxAllowed) {
        while (true) {
            Integer value = readInt(scanner, prompt);
            if (maxAllowed != null && value > maxAllowed) {
                System.out.println("Value cannot exceed max (" + maxAllowed + "), please try again.");
                continue;
            }
            return value;
        }
    }

    private static int[] fetchMaxStats(String charName) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement sel = conn.prepareStatement(
                     "SELECT MaxHP, MaxMP, MaxStam FROM Character WHERE Name ILIKE ?")) {
            sel.setString(1, charName);
            try (ResultSet rs = sel.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Integer maxHp = rs.getObject("MaxHP", Integer.class);
                Integer maxMp = rs.getObject("MaxMP", Integer.class);
                Integer maxStam = rs.getObject("MaxStam", Integer.class);
                return new int[] {
                    maxHp != null ? maxHp : Integer.MAX_VALUE,
                    maxMp != null ? maxMp : Integer.MAX_VALUE,
                    maxStam != null ? maxStam : Integer.MAX_VALUE
                };
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // keep backwards compatibility with the old 4-arg call
    public static String Server_UpdateCharacterStats(
            String charName,
            Integer hpCurrent,
            Integer mpCurrent,
            Integer stamCurrent) {
        return Server_UpdateCharacterStats(
                charName,
                hpCurrent, null,
                mpCurrent, null,
                stamCurrent, null,
                null);
    }

    public static String Server_UpdateCharacterStats(
            String charName,
            Integer hpCurrent, Integer hpMax,
            Integer mpCurrent, Integer mpMax,
            Integer stamCurrent, Integer stamMax,
            Integer currency) {

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // If we're updating current values but not providing max, fetch existing max values for validation.
            boolean needMaxLookup = (hpCurrent != null && hpMax == null)
                    || (mpCurrent != null && mpMax == null)
                    || (stamCurrent != null && stamMax == null);

            Integer dbMaxHp = null;
            Integer dbMaxMp = null;
            Integer dbMaxStam = null;

            if (needMaxLookup) {
                try (PreparedStatement sel = conn.prepareStatement(
                        "SELECT MaxHP, MaxMP, MaxStam FROM Character WHERE Name ILIKE ?")) {
                    sel.setString(1, charName);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (!rs.next()) {
                            return "Error: Character '" + charName + "' not found.";
                        }
                        dbMaxHp = rs.getObject("MaxHP", Integer.class);
                        dbMaxMp = rs.getObject("MaxMP", Integer.class);
                        dbMaxStam = rs.getObject("MaxStam", Integer.class);
                    }
                }
            }

            // validate current <= max for each stat
            if (hpCurrent != null) {
                int maxHpToUse = (hpMax != null ? hpMax : (dbMaxHp != null ? dbMaxHp : Integer.MAX_VALUE));
                if (hpCurrent > maxHpToUse) {
                    return "Error: Current HP (" + hpCurrent + ") cannot exceed max HP (" + maxHpToUse + ").";
                }
            }
            if (mpCurrent != null) {
                int maxMpToUse = (mpMax != null ? mpMax : (dbMaxMp != null ? dbMaxMp : Integer.MAX_VALUE));
                if (mpCurrent > maxMpToUse) {
                    return "Error: Current MP (" + mpCurrent + ") cannot exceed max MP (" + maxMpToUse + ").";
                }
            }
            if (stamCurrent != null) {
                int maxStamToUse = (stamMax != null ? stamMax : (dbMaxStam != null ? dbMaxStam : Integer.MAX_VALUE));
                if (stamCurrent > maxStamToUse) {
                    return "Error: Current Stamina (" + stamCurrent + ") cannot exceed max Stamina (" + maxStamToUse + ").";
                }
            }

            StringBuilder sql = new StringBuilder("UPDATE Character SET ");
            boolean first = true;

            if (hpCurrent != null) {
                if (!first) sql.append(", ");
                sql.append("CurrentHP = ?");
                first = false;
            }
            if (hpMax != null) {
                if (!first) sql.append(", ");
                sql.append("MaxHP = ?");
                first = false;
            }
            if (mpCurrent != null) {
                if (!first) sql.append(", ");
                sql.append("CurrentMP = ?");
                first = false;
            }
            if (mpMax != null) {
                if (!first) sql.append(", ");
                sql.append("MaxMP = ?");
                first = false;
            }
            if (stamCurrent != null) {
                if (!first) sql.append(", ");
                sql.append("CurrentStam = ?");
                first = false;
            }
            if (stamMax != null) {
                if (!first) sql.append(", ");
                sql.append("MaxStam = ?");
                first = false;
            }
            if (currency != null) {
                if (!first) sql.append(", ");
                sql.append("Currency = ?");
                first = false;
            }

            if (first) { // nothing to update
                return "No updates specified.";
            }

            sql.append(" WHERE Name ILIKE ?");

            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            int paramIndex = 1;
            if (hpCurrent != null) stmt.setInt(paramIndex++, hpCurrent);
            if (hpMax != null) stmt.setInt(paramIndex++, hpMax);
            if (mpCurrent != null) stmt.setInt(paramIndex++, mpCurrent);
            if (mpMax != null) stmt.setInt(paramIndex++, mpMax);
            if (stamCurrent != null) stmt.setInt(paramIndex++, stamCurrent);
            if (stamMax != null) stmt.setInt(paramIndex++, stamMax);
            if (currency != null) stmt.setInt(paramIndex++, currency);

            stmt.setString(paramIndex, charName);

            int rowsUpdated = stmt.executeUpdate();
            stmt.close();

            if (rowsUpdated > 0) {
                return "Success: Updated stats for character '" + charName + "'.";
            } else {
                return "Error: Character '" + charName + "' not found.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error: Database error occurred while updating character stats.";
        } finally {
            if (conn != null) {
                DBConnection.closeConnection(conn); //dummy method to not break
            }
        }
    }
}
