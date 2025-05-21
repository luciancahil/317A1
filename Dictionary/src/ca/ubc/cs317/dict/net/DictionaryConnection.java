package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.*;

import java.net.*;
import java.net.Socket;
import java.util.*;

/**
 * Created by Jonatan on 2017-09-09.
 */

// https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoClient.java
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {	

		try {
		    clientSocket = new Socket(host, port);        // 1st statement
		    out = new PrintWriter(clientSocket.getOutputStream(), true);
		    in = new BufferedReader(
		            new InputStreamReader(clientSocket.getInputStream()));
		    BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		    String intial = in.readLine();
		    
		    if(intial == null || intial == "") {
	            throw new DictConnectionException("No inital message");
		    }
    	}catch(Exception e) {
            throw new DictConnectionException("Not implemented");
    	}
    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close() {

        out.println("QUIT");
        try {
			clientSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();

        // TODO This
        
    
        
        // DEFINE database word         -- look up word in database
        
	    try {
	        out.println("DEFINE " + database.getName() + " " + word);
	        
	        
	        String line = in.readLine();

	        // nothing here
	        if (line.startsWith("552")) {
	        	return set;
	        }
	        
	        // Read until the first definition
	        while(!line.startsWith("151")) {
	        	line = in.readLine();
	        	
	        }
	        
	        
	        Definition curDef = null;
	        while (true) {

	        	if(line.startsWith("250")) {
	        		
	        		// end of message
        			set.add(curDef);

	        		break;
	        	} else if(line.startsWith("151")) {
	        		
	        		// new definition
	        		
	        		// add curDef if it's not null
	        		if (curDef != null) {
	        			set.add(curDef);
	        		}
	        		
	        		
	        		// create a new curDef, and set the definition
        	        curDef = new Definition(word, line.split(" ")[2]);

        	        curDef.setDefinition(in.readLine());
	        		
	        	} else if(line.equals(".")) {
	        		// Do nothing
	        	} else {
	        		
	        		// just a regular line
	        		curDef.appendDefinition(line);
	        	}
	        		        	
	        	line = in.readLine();

	        }
			
		} catch (Exception e) {
			throw new DictConnectionException(e);
		}


        return set;
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();

       //TODO Why does every other instance fail?
        
        try {
            out.println("MATCH " + database.getName() + " "  + strategy.getName() + " " + word);
            
            
           
            // Read Metadata
            String line = in.readLine();
            
            
            if(!line.startsWith("152")) {
            	return set;
            }
            
            while ((line = in.readLine()) != null && !line.equals(".")) {
				String[] parts = line.split("\"");
				
				// the word that matches
                String match = parts[1];
                
                set.add(match);
            }

            // Read the 250
        	line = in.readLine();

		} catch (Exception e) {
			throw new DictConnectionException(e);
		}
        

        return set;
    }

    /** Requests and retrieves a map of database name to an equivalent database object for all valid databases used in the server.
     *
     * @return A map of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();

        try {
            out.println("SHOW DATABASES");
            
            // read 2 metadata lines
            String line = in.readLine();
                        
            if(!line.startsWith("110")) {
            	return databaseMap;
            }
            in.readLine();
            
            while ((line = in.readLine()) != null && !line.equals(".")) {
	            
				String[] parts = line.split("\"");
				

                String name = parts[0].strip();
                String descriiption = parts[1];
                
                Database db = new Database(name, descriiption);
                
                databaseMap.put(name,  db);

            }
			in.readLine();

		} catch (Exception e) {
			throw new DictConnectionException(e);
		}

        return databaseMap;
    }

    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();

        try {
            out.println("SHOW STRATEGIES");
            
            // read 2 metadata lines
            String line = in.readLine();
            
            // we don't have any strategies
            if(!line.contains("111")) {
            	return set;
            }
            in.readLine();
            
	        while (true) {
	            line = in.readLine();
				
				// We've parsed out all the strategies
				if(line.equals(".")) {
					
					// read the 250
					in.readLine();
					break;
				}
				
				String[] parts = line.split("\"");

                String name = parts[0].strip();
                String descriiption = parts[1];
                
                MatchingStrategy ms = new MatchingStrategy(name, descriiption);
                
                set.add(ms);
               
            }
		} catch (Exception e) {
			throw new DictConnectionException(e);
		}

        return set;
    }

    /** Requests and retrieves detailed information about the currently selected database.
     *
     * @return A string containing the information returned by the server in response to a "SHOW INFO <db>" command.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized String getDatabaseInfo(Database d) throws DictConnectionException {
    	StringBuilder sb = new StringBuilder();
    	
    	if(d.getName() == "*" || d.getName() == "!") {
    		return sb.toString();
    	}
        // Why is this sometimes really short, but sometimes really long?
	    try {
	        out.println("SHOW INFO " + d.getName());
	        
	        
	        in.readLine();
	        String line;
	        
	        while (true) {
	        	
	        	line = in.readLine();
	        	if(line.equals("250 ok")) {
	        		break;
	        	}
	        	sb.append(line + "\n");
	        	

	        }
			
		} catch (Exception e) {
			throw new DictConnectionException(e);
		}
        return sb.toString();
    }
}
