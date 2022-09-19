package projectControllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import projectBeans.User;
import projectDAO.UserDAO;
import projectUtils.HandleConnection;

@WebServlet("/Registration")
public class Registration extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;

    public Registration() {
        super();
    }
    
    public void init() throws ServletException{
    	connection = HandleConnection.getConnection(getServletContext());
    	ServletContext servletContext = getServletContext();
    	ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
    	templateResolver.setTemplateMode(TemplateMode.HTML);
    	this.templateEngine = new TemplateEngine();
    	this.templateEngine.setTemplateResolver(templateResolver);
    	templateResolver.setSuffix(".html");
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String username = null;
		String password = null;
		String repeatedPassword = null;
		String name = null;
		String surname = null;
		String email = null;
		
		//Take the parameters from the request and check their validity
		username = StringEscapeUtils.escapeJava(request.getParameter("registrationUsername"));
		password = StringEscapeUtils.escapeJava(request.getParameter("registrationPassword"));
		repeatedPassword = StringEscapeUtils.escapeJava(request.getParameter("repeatedPassword"));
		name = StringEscapeUtils.escapeJava(request.getParameter("name"));
		surname = StringEscapeUtils.escapeJava(request.getParameter("surname"));
		email = StringEscapeUtils.escapeJava(request.getParameter("email"));
		
		if(username == null || username.isEmpty() || username.trim().equals("") || password == null || password.isEmpty() || password.trim().equals("")|| 
				name == null || name.isEmpty() || name.trim().equals("") || surname == null || surname.isEmpty() || surname.trim().equals("") ||
				repeatedPassword == null || repeatedPassword.isEmpty() || repeatedPassword.trim().equals("") || email == null || email.isEmpty() ||
				email.trim().equals("")) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameters values");
			return;
		}

		
		UserDAO userDao = new UserDAO(connection);
		User checkUser = null;
		
		//Checks if the user already exists
		try {
			checkUser = userDao.checkUsername(username, email);
		}catch(SQLException ex) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to check username");
			return;
		}
		
		Pattern p = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,3}$");
		Matcher m = p.matcher(email);
		boolean matchFound = m.matches();
		
		String path;
		
		//If all the fields are valid registers new user
		if(checkUser == null && password.equals(repeatedPassword) && matchFound) {
			try {
				userDao.registerNewUser(username, password, name, surname, email);
			}catch(SQLException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to register new user");
				return;
			}

			String ctxpath = getServletContext().getContextPath();
			path = ctxpath + "/index.html?msg=";
			response.sendRedirect(path);			
		}
		else { //otherwise sends error message
			
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			
			if(checkUser != null && checkUser.getUsername().equals(username)) {
				ctx.setVariable("errorMsgUsername", "This username is already taken");
			}
			
			if(checkUser != null && checkUser.getEmail().equals(email)) {
				ctx.setVariable("errorMsgEmail", "Another user with this email is already registered");
			}
			
			if(!password.equals(repeatedPassword)) {
				ctx.setVariable("errorMsgPassword", "You inserted two different passwords");
			}
			
			if(!matchFound) {
				ctx.setVariable("errorMsgFormatEmail", "The email format is not correct");
			}			
			
			path = "/WEB-INF/RegistrationForm.html";
			templateEngine.process(path, ctx, response.getWriter());
		}
		
	}
	
	public void destroy() {
		try {
			HandleConnection.closeConnection(connection);
		}catch(SQLException ex) {
			ex.printStackTrace();
		}
	}


}
