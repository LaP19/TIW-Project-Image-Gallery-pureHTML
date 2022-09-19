package projectDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import projectBeans.User;

public class UserDAO {
	
	private Connection connection;
	
	public UserDAO(Connection connection) {
		this.connection = connection;
	}
	
	
	//Checks if the user is present in the database by looking for the username/email and password
	public User checkUserCredentials(String username, String password) throws SQLException {
		String query = "SELECT  id, username, email, name, surname FROM user WHERE ((username = ? AND password =?) OR (email = ? AND password = ?))";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, username);
			pstatement.setString(2, password);
			pstatement.setString(3, username);
			pstatement.setString(4, password);
			try (ResultSet result = pstatement.executeQuery();) {
				if (!result.isBeforeFirst())
					return null;
				else {
					result.next();
					User user = new User();
					user.setId(result.getInt("id"));
					user.setUsername(result.getString("username"));
					user.setName(result.getString("name"));
					user.setSurname(result.getString("surname"));
					user.setEmail(result.getString ("email"));
					return user;
				}
			}
		}
	}
	
	//Registers a new user in the database
	public void registerNewUser(String username, String password, String name, String surname, String email) throws SQLException{
		
		String query = "INSERT INTO user (username, password, name, surname, email) VALUES (?,?,?,?, ?)";
		
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, username);
			pstatement.setString(2, password);
			pstatement.setString(3, name);
			pstatement.setString(4, surname);
			pstatement.setString(5,  email);
			pstatement.executeUpdate();
		}
		
	}
	
	//Checks if a user is already present in the database, if so returns the user, otherwise it returns null (used for controls in registration)
	public User checkUsername(String username, String email) throws SQLException{
		
		String query = "SELECT * FROM user WHERE (username = ? OR email = ?)";
		
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setString(1, username);
			pstatement.setString(2, email);
			try(ResultSet result = pstatement.executeQuery();){
				if (!result.isBeforeFirst())
					return null;
				else {
					result.next();
					User user = new User();
					user.setUsername(result.getString("username"));
					user.setEmail(result.getString("email"));
					return user;
				}
			}
		}
	}
	
	//Finds a user given his/her id
	public User findUserById(int id) throws SQLException{
		
		String query = "SELECT username, name, surname FROM user WHERE id = ?";
		
		User user = null;
		
		try(PreparedStatement pstatement = connection.prepareStatement(query);){
			pstatement.setInt(1, id);
			try(ResultSet result = pstatement.executeQuery();){
				if (result.next()){
					user = new User();
					user.setUsername(result.getString("username"));
					user.setName(result.getString("name"));
					user.setSurname(result.getString("surname"));
					
				}
			}
		}
		return user;
	}
	
	//Finds all the user registered in the database
	public List<User> findAllUsers() throws SQLException{
		
		String query = "SELECT * FROM user";
		
		List<User> creators = new ArrayList<User>();
		
		try(PreparedStatement pstatement = connection.prepareStatement(query);){
			try(ResultSet result = pstatement.executeQuery();){
				while(result.next()) {
					User user = new User();
					user.setId(result.getInt("id"));
					user.setUsername(result.getString("username"));
					user.setName(result.getString("name"));
					user.setSurname(result.getString("surname"));
					user.setEmail(result.getString ("email"));
					creators.add(user);
				}
			}
		}
		
		return creators;
	}
}
