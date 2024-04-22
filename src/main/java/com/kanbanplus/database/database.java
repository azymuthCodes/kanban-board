package com.kanbanplus.database;

import com.kanbanplus.classes.KanbanBoard;
import java.sql.*;
import org.apache.commons.lang3.SerializationUtils;

public class database{

    //To open a secured connection to the database
    public static Connection openConnection(){
        
        final String url = "jdbc:postgresql://localhost/kanbanplus";
        final String username  = "postgres";
        final String password = "vignesh3";
        

        
        Connection connect  = null;
        try{
            
            Class.forName("org.postgresql.Driver");
            connect = DriverManager.getConnection(url,username,password);
        }
        catch(Exception e){
            System.out.println("Could not establish connection "+e.getMessage());
        }
        return connect;
    }

    //To check if the user exists or not
    private static String checkUser(String userIn, Connection connectorIn){
        try{
            String usersQuery = "select password from users where username = ?;";

            PreparedStatement userStatement = connectorIn.prepareStatement(usersQuery);

            userStatement.setString(1,userIn);

            ResultSet usersResult = userStatement.executeQuery();

            if(!usersResult.next()) return null;

            else return usersResult.getString(1);
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
            return null;
        }
        
    }

    //To check if the password is correct or incorrect

    public static boolean checkPassword(Connection connectorIn,String userIn,String passIn) throws Exception{
        boolean success = false;  
        String pass = checkUser(userIn, connectorIn);
        if(pass!=null){
            if(passIn.equals(pass)){
                success = true;
            }
            else throw new Exception("Password Incorrect ");
        }
        else throw new Exception("Username Invalid");
        return success;
    }

    //To retrieve the user ID of user that is logged in
    public static int getID(Connection connectorIn,String userIn){
        int userID = 0;
        
        try{
            String usersQuery = "select userID from users where username = ?;";
            PreparedStatement userStatement = connectorIn.prepareStatement(usersQuery);

            userStatement.setString(1,userIn);

            ResultSet usersResult = userStatement.executeQuery();
            
            while(usersResult.next()) userID=usersResult.getInt(1);

            return userID;
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
            return userID;
        }
    }

    //To save the user's work done on the board
    public static void saveBoard (Connection connectorIn,KanbanBoard board,int userID){
        String query  = "update boards set board = ? where userID = ?";
        //Convert Object to bytes
        byte data[] = SerializationUtils.serialize(board);
        try{
            PreparedStatement statement = connectorIn.prepareStatement(query);
            statement.setBytes(1, data);
            statement.setInt(2, userID);
            statement.executeUpdate();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    //To store user's board for the first time
    public static void storeBoard (Connection connectorIn,KanbanBoard board,int userID){
        String query  = "insert into boards values(?,?)";
        //Convert Object to bytes
        byte data[] = SerializationUtils.serialize(board);
        try{
            PreparedStatement statement = connectorIn.prepareStatement(query);
            statement.setInt(1, userID);
            statement.setBytes(2, data);
            statement.executeUpdate();
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    //To retrieve the board from the database
    public static KanbanBoard getBoard(Connection connectorIn,int userID){
        String query = "select board from boards where userID = ?";
        KanbanBoard board = null ;
        try{
            PreparedStatement statement = connectorIn.prepareStatement(query);
            statement.setInt(1, userID);
            ResultSet set = statement.executeQuery();
            //Converting bytes back to object and adding them to the arraylist
            while(set.next()) board = (KanbanBoard)SerializationUtils.deserialize(set.getBytes(1));
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
        
        return board;
        
    }
}
