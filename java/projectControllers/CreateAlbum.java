package projectControllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;

import projectBeans.User;
import projectDAO.AlbumDAO;
import projectUtils.HandleConnection;

@WebServlet("/CreateAlbum")
public class CreateAlbum extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public CreateAlbum() {
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
		
		//Checks if there is a user logged in
		String loginpath = getServletContext().getContextPath() + "/index.html";
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;
		}
		
		String albumTitle = null;
		LocalDate localDate = null;
		Date date = null;
		ZoneId defaultZoneId = ZoneId.systemDefault();
		User creator = null;
		User user = (User)session.getAttribute("user");
		
		//Takes data from the request and controls its validity
		try {
			albumTitle = StringEscapeUtils.escapeJava(request.getParameter("title"));
			localDate = LocalDate.now();
	        date = Date.from(localDate.atStartOfDay(defaultZoneId).toInstant());
			creator = user;
			
			if(albumTitle == null || albumTitle.trim().equals("") || albumTitle.isEmpty()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "You can't create an album with an empty title");
				return;
			}
			
			if(date == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "It wasn't possible to assign today's date to the album");
				return;
			}
		} catch (NumberFormatException | NullPointerException ex) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect parameters values");
			return;
		}
		
		AlbumDAO albumDao = new AlbumDAO(connection);
		
		//Creates the album and stores it in the DB
		try {
			albumDao.createAlbum(albumTitle, date, creator.getId());
		}catch(SQLException ex) {
			response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failure in database update");
			return;
		}
		
		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/GoToHomePage";
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
