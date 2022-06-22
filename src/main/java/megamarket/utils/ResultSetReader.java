package megamarket.utils;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ResultSetReader {

    public final ResultSet rs;

    public ResultSetReader(ResultSet rs) {
        this.rs = rs;
    }

    public boolean next() throws SQLException {
        return rs.next();
    }

    @Nullable
    public String readStringNullable(String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Nullable
    public Long readLongNullable(String columnName) throws SQLException {
        long result = rs.getLong(columnName);
        return !rs.wasNull() ? result : null;
    }

    @Nullable
    public Timestamp readTimestampNullable(String columnName) throws SQLException {
        return rs.getTimestamp(columnName);
    }
}
