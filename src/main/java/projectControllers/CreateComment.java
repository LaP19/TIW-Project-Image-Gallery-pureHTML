package projectControllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;

import projectBeans.Album;
import projectBeans.Image;
import projectBeans.User;
import projectDAO.AlbumDAO;
import projectDAO.CommentDAO;
import projectDAO.ImageDAO;
import projectUtils.HandleConnection;

@WebServlet("/CreateComment")
public class CreateComment extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public CreateComment() {
		super();
	}

	public void init() throws ServletException {
		connection = HandleConnection.getConnection(getServletContext());
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		//Checks if there is a user that is logged in
		String loginpath = getServletContext().getContextPath() + "/index.html";
		User user = null;
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;
		}
		
		String commentText = null;
		Integer image = null;
		Integer albumId = null;
		Integer j = null;
		
		//Takes data from the request and controls its validity
		try {
			commentText = StringEscapeUtils.escapeJava(request.getParameter("commentText"));
			user = (User)session.getAttribute("user");
			image = Integer.parseInt(request.getParameter("imageId"));
			albumId = Integer.parseInt(request.getParameter("album"));
			j = Integer.parseInt(request.getParameter("j"));
			
			if(commentText == null || commentText.isEmpty() || commentText.trim().equals("")) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You can't submit an empty comment");
				return;
			}
		}catch(NumberFormatException | NullPointerException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect parameters values");
			return;
		}
		
		AlbumDAO albumDao = new AlbumDAO(connection);
		ImageDAO imageDao = new ImageDAO(connection);
		
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
			
			if(!ids.contains(image)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This picture is not contained in this album or doesn't exist");
				return;
			}
		}catch(SQLException ex) {
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database update");
			return;
		}

		CommentDAO commentDao = new CommentDAO(connection);
		
		
		//Creates the comment and stores it in the DB
		try {
			commentDao.createComment(commentText, image, user.getId());
		}catch(SQLException ex) {
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database update");
			return;
		}
		
		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/GoToAlbumPage?imageID=" + image + "&album=" + albumId + "&j=" + j;
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
