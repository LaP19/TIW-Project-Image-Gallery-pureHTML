package projectControllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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

@WebServlet("/CheckLogin")
public class CheckLogin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;

    public CheckLogin() {
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
		
		//Takes the parameters from the request and checks if they are correct (not null or empty)

		username = StringEscapeUtils.escapeJava(request.getParameter("loginUsername"));
		password = StringEscapeUtils.escapeJava(request.getParameter("loginPassword"));
		
		if(username == null || username.isEmpty() || password == null || password.isEmpty()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or empty credential values");
			return;
		}
		
		
		UserDAO userDao = new UserDAO(connection);
		User user = null;
		
		//Checks the existence of the user in the database
		try {
			user = userDao.checkUserCredentials(username, password);
		}catch(SQLException ex) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not Possible to check credentials");
			return;
		}
		
		String path;
		
		//If the user is not found sends an error message and goes back to the index.html page
		if(user == null) { 
			ServletContext servletContext = getServletContext();
			final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
			ctx.setVariable("errorMsg", "Wrong username or password");
			path = "/index.html";
			templateEngine.process(path, ctx, response.getWriter());
		}
		else { //If the user is correct redirects to the home page and saves the user in the session
			request.getSession().setAttribute("user", user);
			path = getServletContext().getContextPath() + "/GoToHomePage";
			response.sendRedirect(path);
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
