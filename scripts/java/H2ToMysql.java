import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;

/** Offline, exact copy into an empty MySQL schema. Never disables foreign keys. */
public class H2ToMysql {
    record Column(String name,int type,boolean identity) {}
    record Summary(long rows,String sha256) {}
    static String phase="initialization";
    static void require(boolean ok,String message) { if(!ok) throw new IllegalStateException(message); }
    static String quoted(String name,boolean mysql) {
        require(name.matches("[a-z_]+"),"Unexpected SQL identifier");
        return (mysql?"`":"\"")+name+(mysql?"`":"\"");
    }
    static SortedSet<String> tables(Connection connection) throws SQLException {
        var result=new TreeSet<String>();
        boolean mysql=connection.getMetaData().getDatabaseProductName().equals("MySQL");
        try(var rs=connection.getMetaData().getTables(connection.getCatalog(),mysql?null:"public","%",new String[]{"TABLE","BASE TABLE"})) {
            while(rs.next()) {
                String name=rs.getString("TABLE_NAME");
                if(!name.equals("flyway_schema_history")) result.add(name);
            }
        }
        return result;
    }
    static List<Column> columns(Connection connection,String table) throws SQLException {
        var result=new ArrayList<Column>();boolean mysql=connection.getMetaData().getDatabaseProductName().equals("MySQL");
        try(var rs=connection.getMetaData().getColumns(connection.getCatalog(),mysql?null:"public",table,"%")) {
            while(rs.next()) result.add(new Column(rs.getString("COLUMN_NAME"),rs.getInt("DATA_TYPE"),"YES".equals(rs.getString("IS_AUTOINCREMENT"))));
        }
        result.sort(Comparator.comparing(Column::name));return result;
    }
    static List<String> orderedTables(Connection connection,Set<String> names) throws SQLException {
        var result=new ArrayList<String>();var pending=new TreeSet<>(names);var dependencies=new HashMap<String,Set<String>>();
        for(String table:names) {
            var parents=new HashSet<String>();
            try(var keys=connection.getMetaData().getImportedKeys(connection.getCatalog(),"public",table)) {
                while(keys.next()) parents.add(keys.getString("PKTABLE_NAME"));
            }
            dependencies.put(table,parents);
        }
        while(!pending.isEmpty()) {
            String next=pending.stream().filter(t->result.containsAll(dependencies.get(t))).findFirst().orElseThrow(()->new IllegalStateException("Foreign-key cycle or unknown dependency"));
            result.add(next);pending.remove(next);
        }
        return result;
    }
    static Object value(ResultSet rs,int index,int type) throws SQLException {
        return switch(type) {
            case Types.BOOLEAN,Types.BIT -> { boolean v=rs.getBoolean(index);yield rs.wasNull()?null:v; }
            case Types.BLOB,Types.BINARY,Types.VARBINARY,Types.LONGVARBINARY -> rs.getBytes(index);
            case Types.TIMESTAMP,Types.TIMESTAMP_WITH_TIMEZONE -> rs.getObject(index,LocalDateTime.class);
            case Types.DATE -> rs.getObject(index,LocalDate.class);
            case Types.TIME -> rs.getObject(index,LocalTime.class);
            case Types.TINYINT,Types.SMALLINT,Types.INTEGER,Types.BIGINT,Types.NUMERIC,Types.DECIMAL -> rs.getBigDecimal(index);
            default -> rs.getString(index);
        };
    }
    static byte[] canonical(Object value) {
        if(value instanceof byte[] bytes) return bytes;
        if(value instanceof java.math.BigDecimal number) return number.stripTrailingZeros().toPlainString().getBytes(StandardCharsets.UTF_8);
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }
    static Summary summarize(Connection connection,String table,List<Column> columns,boolean mysql) throws Exception {
        var rows=new ArrayList<String>();String select=String.join(",",columns.stream().map(c->quoted(c.name(),mysql)).toList());
        try(var statement=connection.createStatement();var rs=statement.executeQuery("SELECT "+select+" FROM "+quoted(table,mysql))) {
            while(rs.next()) {
                var digest=MessageDigest.getInstance("SHA-256");
                for(int i=0;i<columns.size();i++) {
                    Object value=value(rs,i+1,columns.get(i).type());
                    if(value==null) digest.update((byte)0);
                    else {byte[] bytes=canonical(value);digest.update((byte)1);digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);}
                }
                rows.add(HexFormat.of().formatHex(digest.digest()));
            }
        }
        Collections.sort(rows);var digest=MessageDigest.getInstance("SHA-256");
        rows.forEach(row->digest.update(row.getBytes(StandardCharsets.UTF_8)));
        return new Summary(rows.size(),HexFormat.of().formatHex(digest.digest()));
    }
    static void emptyDestination(Connection connection) throws SQLException {
        for(String table:tables(connection)) {
            try(var statement=connection.createStatement();var rs=statement.executeQuery("SELECT COUNT(*) FROM "+quoted(table,true))) {
                rs.next();long count=rs.getLong(1);
                if(table.equals("account_admin_guard") || table.equals("activity_seed_runs")) require(count<=1,"Destination marker has unexpected rows");
                else require(count==0,"Destination is not empty: "+table);
            }
        }
        if(tables(connection).contains("activity_seed_runs")) {
            try(var s=connection.createStatement();var r=s.executeQuery("SELECT completed_at FROM activity_seed_runs")) {
                if(r.next()) require(r.getObject(1)==null,"Destination already contains imported activity");
            }
        }
    }
    static void copyTable(Connection source,Connection destination,String table,List<Column> columns) throws SQLException {
        String sourceColumns=String.join(",",columns.stream().map(c->quoted(c.name(),false)).toList());
        String targetColumns=String.join(",",columns.stream().map(c->quoted(c.name(),true)).toList());
        String parameters=String.join(",",Collections.nCopies(columns.size(),"?"));
        try(var select=source.createStatement();var rows=select.executeQuery("SELECT "+sourceColumns+" FROM "+quoted(table,false));
            var insert=destination.prepareStatement("INSERT INTO "+quoted(table,true)+" ("+targetColumns+") VALUES ("+parameters+")")) {
            int batch=0;
            while(rows.next()) {
                for(int i=0;i<columns.size();i++) {
                    Object data=value(rows,i+1,columns.get(i).type());
                    if(data instanceof byte[] bytes) insert.setBytes(i+1,bytes);else insert.setObject(i+1,data);
                }
                insert.addBatch();if(++batch%100==0) insert.executeBatch();
            }
            if(batch%100!=0) insert.executeBatch();
        }
    }
    static long sourceIdentity(Connection source,String table,String column) throws SQLException {
        try(var statement=source.prepareStatement("SELECT identity_base FROM information_schema.columns WHERE table_schema='public' AND table_name=? AND column_name=?")) {
            statement.setString(1,table);statement.setString(2,column);
            try(var rs=statement.executeQuery()) {require(rs.next(),"Missing source identity");return rs.getLong(1);}
        }
    }
    static long targetIdentity(Connection target,String table) throws SQLException {
        try(var statement=target.prepareStatement("SELECT AUTO_INCREMENT FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?")) {
            statement.setString(1,table);try(var rs=statement.executeQuery()) {require(rs.next(),"Missing destination identity");return rs.getLong(1);}
        }
    }
    static void run(String[] args) throws Exception {
        require(args.length==3||args.length==4,"Usage: H2ToMysql source-H2-base-path mysql.properties report.json [--verify-only]");
        boolean verifyOnly=args.length==4 && args[3].equals("--verify-only");
        var reportPath=Path.of(args[2]);require(!Files.exists(reportPath),"Report already exists; choose a new path");
        var properties=new Properties();try(var input=Files.newInputStream(Path.of(args[1]))) {properties.load(input);}
        String url=properties.getProperty("spring.datasource.url"),user=properties.getProperty("spring.datasource.username"),password=properties.getProperty("spring.datasource.password");
        require(url!=null && url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/sunicoffee[a-zA-Z0-9_]*(?:\\?.*)?"),"Destination must be a local sunicoffee MySQL schema");
        require(user!=null && password!=null && !password.isBlank(),"Missing destination credentials");
        try(var source=DriverManager.getConnection("jdbc:h2:file:"+Path.of(args[0]).toAbsolutePath()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;ACCESS_MODE_DATA=r","sa","");
            var target=DriverManager.getConnection(url,user,password)) {
            require(target.getMetaData().getDatabaseProductName().equals("MySQL"),"Destination is not MySQL");
            if(!verifyOnly) {
                phase="empty destination check";emptyDestination(target);
                phase="schema migrations";Flyway.configure().dataSource(url,user,password).locations("classpath:db/migration").load().migrate();
                emptyDestination(target);
            }
            var names=tables(source);require(names.equals(tables(target)),"Source and destination business table sets differ");
            var allColumns=new TreeMap<String,List<Column>>();
            for(String table:names) {
                var cols=columns(source,table);
                require(cols.stream().map(Column::name).toList().equals(columns(target,table).stream().map(Column::name).toList()),"Column mismatch: "+table);
                allColumns.put(table,cols);
            }
            if(!verifyOnly) {
                phase="transactional data copy";target.setAutoCommit(false);
                try {
                    try(var statement=target.createStatement()) {
                        statement.executeUpdate("DELETE FROM account_admin_guard");statement.executeUpdate("DELETE FROM activity_seed_runs");
                    }
                    for(String table:orderedTables(source,names)) {phase="copy "+table;copyTable(source,target,table,allColumns.get(table));}
                } catch(Exception ex) {target.rollback();throw ex;}
            }
            var reports=new ArrayList<Map<String,Object>>();
            try {
                for(String table:names) {
                    phase="verify "+table;
                    var a=summarize(source,table,allColumns.get(table),false);var b=summarize(target,table,allColumns.get(table),true);
                    require(a.equals(b),"Full row comparison failed: "+table);
                    reports.add(Map.of("table",table,"source_rows",a.rows(),"mysql_rows",b.rows(),"sha256",a.sha256(),"matched",true));
                }
                if(!verifyOnly) target.commit();
            } catch(Exception ex) {if(!verifyOnly) target.rollback();throw ex;}
            if(!verifyOnly) target.setAutoCommit(true);
            try(var statement=target.createStatement()) {statement.execute("SET SESSION information_schema_stats_expiry=0");}
            var identities=new ArrayList<Map<String,Object>>();
            for(var entry:allColumns.entrySet()) for(var column:entry.getValue()) if(column.identity()) {
                phase="identity counter "+entry.getKey();long expected=sourceIdentity(source,entry.getKey(),column.name());
                require(expected>0,"Invalid identity counter");
                if(!verifyOnly) try(var statement=target.createStatement()) {statement.execute("ALTER TABLE "+quoted(entry.getKey(),true)+" AUTO_INCREMENT="+expected);}
                long actual=targetIdentity(target,entry.getKey());require(expected==actual,"Identity counter mismatch: "+entry.getKey());
                identities.add(Map.of("table",entry.getKey(),"next_id",expected,"matched",true));
            }
            var report=new LinkedHashMap<String,Object>();
            report.put("result","PASS");report.put("mode",verifyOnly?"verify-only":"import");report.put("source","Immutable H2 backup");
            report.put("destination","MySQL");report.put("comparison","All columns, nulls, Unicode, microsecond timestamps, decimals and binary content; sorted SHA-256 row digests");
            report.put("foreign_keys_disabled",false);report.put("tables",reports);report.put("identity_counters",identities);
            report.put("flyway_history","Native MySQL migration history retained; H2 history remains in backup");
            Files.createDirectories(reportPath.toAbsolutePath().getParent());new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(),report);
            System.out.println("PASS: "+names.size()+" business tables and "+identities.size()+" identity counters match exactly.");
        }
    }
    public static void main(String[] args) {
        try {run(args);}
        catch(Exception error) {
            System.err.println("Migration stopped during "+phase+" ("+error.getClass().getSimpleName()+"). No credential or row values are printed.");
            if(error instanceof IllegalStateException) System.err.println(error.getMessage());
            if(error instanceof SQLException sql) System.err.println("SQL state="+sql.getSQLState()+", code="+sql.getErrorCode());
            System.exit(1);
        }
    }
}
