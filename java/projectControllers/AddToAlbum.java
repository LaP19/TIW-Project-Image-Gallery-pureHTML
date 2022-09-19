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
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import projectBeans.Album;
import projectBeans.Image;
import projectBeans.User;
import projectDAO.AlbumDAO;
import projectDAO.ImageDAO;
import projectUtils.HandleConnection;

@WebServlet("/AddToAlbum")
public class AddToAlbum extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private TemplateEngine templateEngine;

    public AddToAlbum() {
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

		doPost(request, response);
		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//If no user has logged in redirect to the login page
		String loginPath = getServletContext().getContextPath() + "/index.html";
		HttpSession session = request.getSession();
		if(session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginPath);
			return;
		}

		Integer imageId = null; 
		Integer j = null;
		Integer albumId = null;
		
		User user = (User)session.getAttribute("user");
	
		//Takes the parameters from the request
		try {
			albumId = Integer.parseInt(request.getParameter("album"));
			j = Integer.parseInt(request.getParameter("j"));
			imageId = Integer.parseInt(request.getParameter("imageID"));
		}catch(NumberFormatException | NullPointerException ex) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect parameters values");
			return;
		}
		
		Image image = null;
		ImageDAO imageDao = new ImageDAO(connection);
		AlbumDAO albumDao = new AlbumDAO(connection);
		
		try {
			
			//Controls if the picture that is being added to the album is available
			List<Image> imagesOfUser = imageDao.findAllImagesOfUser(user.getId());
			List<Integer> imageIds = new ArrayList<>();
			
			for(Image image1 : imagesOfUser) {
				imageIds.add(image1.getId());
			}
			
			if(!imageIds.contains(imageId)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You are trying add a picture that is not available or doesn't exist");
				return;
			} else {
				image = imageDao.findImageById(imageId);
			}
		}catch(SQLException e) {
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database extraction");
		}
		
		String path = null;;
		
		try {	
			
			//Controls if the album to which the image is being added exists and is owned by the user
			List<Integer> albumsOfUser = new ArrayList<>();
			List<Album> albums = albumDao.findAllAlbumsOfCreator(user.getId());
			
			for(Album album1 : albums) {
				albumsOfUser.add(album1.getId());
			}
			if(!albumsOfUser.contains(albumId)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You are trying to access an album that is not yours or doesn't exist");
				return;
			}
			
			imageDao.addImageToAlbum(albumId, image.getId());
			
		}catch(SQLException e) {
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database update");
		}
		
		String ctxpath = getServletContext().getContextPath();
		path = ctxpath + "/GoToAddPictures?album=" + albumId + "&j=" + j;
		response.sendRedirect(path);

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
