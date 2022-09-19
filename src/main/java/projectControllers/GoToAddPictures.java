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
import projectBeans.Image;
import projectBeans.User;
import projectDAO.AlbumDAO;
import projectDAO.ImageDAO;
import projectUtils.HandleConnection;

@WebServlet("/GoToAddPictures")
public class GoToAddPictures extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;

    public GoToAddPictures() {
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

		String loginPath = getServletContext().getContextPath() + "/index.html";
		HttpSession session = request.getSession();
		
		if(session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginPath);
			return;
		}
		
		User user = null; 
		ImageDAO imageDao = new ImageDAO(connection);
		List<Image> images = new ArrayList<Image>();
		Integer albumId = null;
		Integer j = null;
		
		//Needs the casting in order to convert the Object retrieved from the session to the type User
		user = (User)session.getAttribute("user");
		
		//Takes the data from the request and checks its validity
		try {
			albumId = Integer.parseInt(request.getParameter("album"));
			j = Integer.parseInt(request.getParameter("j"));

		}catch(NumberFormatException | NullPointerException ex) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect parameters values");
			return;
		}
		
		Album album = null;
		AlbumDAO albumDao = new AlbumDAO(connection);
		
		try {
			//Checks if the album asked for is existent and is owned by the user in session
			List<Integer> albumsOfUser = new ArrayList<>();
			List<Album> albums = albumDao.findAllAlbumsOfCreator(user.getId());
			
			for(Album album1 : albums) {
				albumsOfUser.add(album1.getId());
			}
			if(!albumsOfUser.contains(albumId)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You are trying to access an album that is not yours or doesn't exist");
				return;
			}
		}catch(SQLException e) {
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database extraction");
		}
		
		//Finds all the images of the user that are not contained in any album and finds the album to which we want to add the pictures
		try {
			images = imageDao.findAllImagesOfUser(user.getId());			
			album = albumDao.findAlbumById(albumId);
		}catch(SQLException e) {
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database extraction");
		}
		
		//If the set of image asked for is bigger or equal to the number of images available the last set of images is displayed
		if(j > images.size()) {
			j = images.size()- images.size()%18;
		}else if(j%18  != 0) { //If a number that is not multiple of 18 is put in the url, j is changed to the closest multiple of 18 that is less than that
			j = j - j%18;
		}
		
		if(j < 0) {
			j=0;
		}
		
		if(j == images.size() && j%18 == 0) {
			j = images.size() - 18;
		}
		
		String path = "/WEB-INF/ChoosePictures.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		
		ctx.setVariable("images", images);
		ctx.setVariable("numOfImages", images.size());
		ctx.setVariable("j", j);
		ctx.setVariable("album", album);	
		
		templateEngine.process(path, ctx, response.getWriter());
		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public void destroy() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException sqle) {
	}
	}

}
