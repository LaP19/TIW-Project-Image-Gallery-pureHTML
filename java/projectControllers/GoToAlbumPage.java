package projectControllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import projectBeans.Album;
import projectBeans.Comment;
import projectBeans.Image;
import projectBeans.User;
import projectDAO.AlbumDAO;
import projectDAO.CommentDAO;
import projectDAO.ImageDAO;
import projectDAO.UserDAO;
import projectUtils.HandleConnection;


@WebServlet("/GoToAlbumPage")
public class GoToAlbumPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;

    public GoToAlbumPage() {
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
		//retrieve the current session, and if one doesn't exist yet, create it
		HttpSession session = request.getSession();
		
		if(session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginPath);
			return;
		}
		
		Integer albumId = null; 
		Integer j = null;
		AlbumDAO albumDao = new AlbumDAO(connection);
		Integer imageToShowId = null;
	
		//Takes the parameters from the reqeust and checks the validity
		try {
			albumId = Integer.parseInt(request.getParameter("album"));
			j = Integer.parseInt(request.getParameter("j"));
			String image = StringEscapeUtils.escapeJava(request.getParameter("imageID"));
			if(image != null) {
				imageToShowId = Integer.parseInt(image);
			}
			
		}catch(NumberFormatException | NullPointerException ex) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect parameters values");
			return;
		}
		
		Album album = null;
		ImageDAO imageDao = new ImageDAO(connection);
		List<Image> images = new ArrayList<Image>();
		List<Comment> comments = new ArrayList<Comment>();
		LinkedHashMap<Comment, User> userComment = new LinkedHashMap<Comment, User>();
		Image imageToShow = new Image();
		UserDAO userDao = new UserDAO(connection);
		User user = null;
		CommentDAO commentDao = new CommentDAO(connection);
		
		try {
			
			//Check if the album asked for exists
			List<Integer> albums = new ArrayList<>();
			List<Album> allAlbums = albumDao.findAllAlbums();
			
			for(Album album1 : allAlbums) {
				albums.add(album1.getId());
			}
			if(!albums.contains(albumId)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You are trying to access an album that doesn't exist");
				return;
			}
			
			//Checks if the image that we want to show is contained in the album selected
			List<Image> imagesOfAlbum = imageDao.findAllImagesOfAlbum(albumId);
			List<Integer> ids = new ArrayList<>();
			
			for(Image image1: imagesOfAlbum) {
				ids.add(image1.getId());
			}
			
			if(imageToShowId != null && !ids.contains(imageToShowId)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This picture is not contained in this album or doesn't exist");
				return;
			}
			album = albumDao.findAlbumById(albumId);
			images = imageDao.findAllImagesOfAlbum(albumId);
			
			
		}catch(SQLException e) {
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database extraction");
		}
		
		//If the set of image asked for is bigger or equal to the number of images available the last set of images is displayed
		if(j > images.size()) {
			j = images.size()- images.size()%5;
		}else if(j%5  != 0) {//If a number that is not multiple of 18 is put in the url, j is changed to the closest multiple of 5 that is less than that
			j = j - j%5;
		}
		
		if(j < 0) {
			j=0;
		}
		
		if(j == images.size() && j%5 == 0) {
			j = j - 5;
		}
		
		if(images.size()>0) {
			
			//If no image has been selected only the grid containing the thumbnails is displayed
			if(imageToShowId==null) {
				imageToShow = null;
			}
			
			else {
				//Gets the details of the image and its comments
				try {
					imageToShow = imageDao.findImageById(imageToShowId);
					comments = commentDao.getAllImageComments(imageToShowId);
					for(Comment comment : comments) {
						user = userDao.findUserById(comment.getCreatorId());
						userComment.put(comment, user);
					}
				}catch(SQLException e) {
					response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database extraction");
				}
			}
		}

		String path = "/WEB-INF/AlbumPage.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("album", album);
		ctx.setVariable("j", j);
		ctx.setVariable("images", images);
		ctx.setVariable("numOfImages", images.size());
		ctx.setVariable("imageToShow", imageToShow);
		ctx.setVariable("comments", userComment);
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
