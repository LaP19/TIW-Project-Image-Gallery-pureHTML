package projectControllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import projectBeans.Album;
import projectBeans.User;
import projectDAO.AlbumDAO;
import projectDAO.UserDAO;
import projectUtils.HandleConnection;

@WebServlet("/GoToHomePage")
public class GoToHomePage extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private Connection connection;
	private TemplateEngine templateEngine;
	
    public GoToHomePage() {
        super();
    }
    
    public void init() throws ServletException{
    	
    	ServletContext servletContext = getServletContext();
    	ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
    	templateResolver.setTemplateMode(TemplateMode.HTML);
    	this.templateEngine = new TemplateEngine();
    	this.templateEngine.setTemplateResolver(templateResolver);
    	templateResolver.setSuffix(".html");
    	connection = HandleConnection.getConnection(getServletContext());
    	
    }

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
		//Checks if there is a user that is logged in
		String loginPath = getServletContext().getContextPath() + "/index.html";
		HttpSession session = request.getSession();
		
		if(session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginPath);
			return;
		}
		
		User user = (User) session.getAttribute("user");
		
		AlbumDAO albumDao = new AlbumDAO(connection);
		List<Album> myAlbums = new ArrayList<Album>();
		List<Album> othersAlbums = new ArrayList<Album>();
		List<User> otherUsers = new ArrayList<User>();
		UserDAO userDao = new UserDAO(connection);
		
		//Finds all the albums of the user in session
		try {
			myAlbums = albumDao.findAllAlbumsOfCreator(user.getId());
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover your albums");
			return;
		}
		
		//Finds all the albums of the other users and their owner
		try {
			List<Album> albums = albumDao.findAllAlbums();
			for (int i = 0; i < albums.size(); i++) {
				if(albums.get(i).getCreator() != user.getId()) {
					othersAlbums.add(albums.get(i));
					otherUsers.add(userDao.findUserById(albums.get(i).getCreator()));
				}
		    }
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover other users' albums");
			return;
		}
		
		String path = "/WEB-INF/HomePage.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("myAlbums", myAlbums);
		ctx.setVariable("othersAlbums", othersAlbums);
		ctx.setVariable("otherUsers", otherUsers);
		ctx.setVariable("numOfAlbums", othersAlbums.size());
		templateEngine.process(path, ctx, response.getWriter());
	}

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void destroy(){
		
		try {
			HandleConnection.closeConnection(connection);
		}catch(SQLException ex) {
			ex.printStackTrace();
		}
	}
}