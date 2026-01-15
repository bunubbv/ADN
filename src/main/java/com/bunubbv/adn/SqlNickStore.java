package com.bunubbv.adn;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public final class SqlNickStore {
    private final File file;
    private Connection conn;

    public SqlNickStore(File file) {
        this.file = file;
    }

    public void open() throws Exception {
        file.getParentFile().mkdirs();
        conn = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA synchronous=NORMAL;");
            st.execute("""
                CREATE TABLE IF NOT EXISTS adn (
                    user_uuid TEXT PRIMARY KEY,
                    user_nick TEXT
                );
            """);
        }
    }

    public String getNick(UUID uuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT user_nick FROM adn WHERE user_uuid = ?;"
        )) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("user_nick") : null;
            }
        }
    }

    public void setNick(UUID uuid, String nick) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO adn(user_uuid, user_nick)
            VALUES(?, ?)
            ON CONFLICT(user_uuid) DO UPDATE SET user_nick = excluded.user_nick;
        """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, nick);
            ps.executeUpdate();
        }
    }

    public void removeNick(UUID uuid) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM adn WHERE user_uuid = ?;"
        )) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
