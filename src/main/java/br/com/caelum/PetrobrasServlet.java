package br.com.caelum;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns="/subscribe", loadOnStartup=1, asyncSupported=true)
public class PetrobrasServlet extends HttpServlet {

	private Queue<AsyncContext> clients = new ConcurrentLinkedQueue<AsyncContext>();
	private BlockingQueue<String> messages = new LinkedBlockingQueue<String>();
	private AtomicInteger contador = new AtomicInteger();
	private AtomicInteger clientes = new AtomicInteger();

	@Override
	public void init() throws ServletException {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			// nosso reactor
			public void run() {
				while (true) {

					try {	
						String message = messages.take();
						// nao pode ter nada blocante depois daqui!
						for (AsyncContext ctx : clients) {
							PrintWriter writer = ctx.getResponse().getWriter();
							writer.println(message);
							writer.flush();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

		});
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse arg1)
			throws ServletException, IOException {
		AsyncContext ctx = req.startAsync();
		ctx.setTimeout(3000000);
		clients.add(ctx);
		System.out.println("novo cliente. id: " + clientes.incrementAndGet());

		// thread será liberada!!!!
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse arg1)
			throws ServletException, IOException {
		System.out
				.println("enviando mensagem para   " + clientes + " clientes");

		messages.add(String.format("novo valor PETR4: %d %n",
				contador.incrementAndGet()));
	}
}
