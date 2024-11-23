import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.neo4j.driver.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import com.mysql.cj.jdbc.MysqlDataSource;


public class API implements RequestHandler<Map<String, String>, String>{

    private static final String NEO4J_URI = "neo4j+s://hackatum-one.graphdatabase.ninja:443";
    private static final String NEO4J_USERNAME = "attendee11";
    private static final String NEO4J_PASSWORD = "betternotshare";

    private static final String MYSQL_URL = "jdbc:mysql://hack-db.ctq8aecwa2sl.eu-central-1.rds.amazonaws.com:3306/mydb";
    private static final String MYSQL_DATABASE = "mydb";
    private static final String MYSQL_USERNAME = "admin";
    private static final String MYSQL_PASSWORD = "betternotshare";

    @Override
    public String handleRequest(Map<String, String> input, Context context) {
        String cypherQuery = input.get("cypherQuery");

        try (Driver neo4jDriver = GraphDatabase.driver(NEO4J_URI, AuthTokens.basic(NEO4J_USERNAME, NEO4J_PASSWORD))) {
            SessionConfig sessionConfig = SessionConfig.forDatabase("hackatum11");

            try (Session session = neo4jDriver.session(sessionConfig)) {
                MysqlDataSource dataSource = new MysqlDataSource();

                // Configure the data source
                dataSource.setServerName("hack-db.ctq8aecwa2sl.eu-central-1.rds.amazonaws.com");
                dataSource.setPortNumber(3306);
                dataSource.setDatabaseName("mydb");
                dataSource.setUser("admin");
                dataSource.setPassword(MYSQL_PASSWORD);
                // MySQL Connection
                try (Connection mysqlConnection = dataSource.getConnection()) {
                    // Execute and Process Cypher Query
                    Result result = session.run(cypherQuery);

                    String prompt = result.single().get(0).toString().substring(1, Integer.parseInt(input.get("characters")));

                    System.out.println(prompt);

                    String response = chatGPT(prompt);

                    try (PreparedStatement stmt = mysqlConnection.prepareStatement("INSERT INTO roadmap (timestamp, name) VALUES (?, ?)")) {
                        stmt.setInt(1, (int) (System.currentTimeMillis()/1000));
                        stmt.setString(2, response);
                        stmt.executeUpdate();
                    }


                    mysqlConnection.close();
                } catch (Exception e) {
                    context.getLogger().log("Error connecting to MySQL: " + e.getMessage());
                    return "Error.";
                }

            } catch (Exception e) {
                context.getLogger().log("Error establishing Neo4j session: " + e.getMessage());
                return "Error: Unable to create Neo4j session.";
            }

        } catch (Exception e) {
            context.getLogger().log("Error connecting to Neo4j: " + e.getMessage());
            return "Error: Unable to connect to Neo4j.";
        }

        return "Data loaded successfully!";
    }

    public static void main(String[] args) {
        API api = new API();
        Map<String, String> map = new HashMap<>();
        map.put("cypherQuery", "MATCH (n:Person) RETURN n.name AS name");
        System.out.println(api.handleRequest(map, null));
    }

    public static String chatGPT(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        String apiKey = "betternotshare";
        String model = "gpt-4o";

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            // The request body
            String body = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt.replace("\"","") + "\"}]}";
            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            // Response from ChatGPT
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            StringBuffer response = new StringBuffer();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            // calls the method to extract the message.
            return extractMessageFromJSONResponse(response.toString());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String extractMessageFromJSONResponse(String response) {
        int start = response.indexOf("content")+ 11;

        int end = response.indexOf("\"", start);

        return response.substring(start, end);

    }
}
