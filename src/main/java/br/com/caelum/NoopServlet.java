package br.com.caelum;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/noop" }, loadOnStartup = 1)
public class NoopServlet extends HttpServlet {

	@Override
	public void init() throws ServletException {
		System.out.println("init");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse arg1)
			throws ServletException, IOException {
	}

}
