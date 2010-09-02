package br.com.caelum.chat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/subscribe" }, asyncSupported = true, loadOnStartup = 1)
public class ChatServlet extends HttpServlet {

	private Queue<AsyncContext> clients = new ConcurrentLinkedQueue<AsyncContext>();
	private BlockingQueue<String> messages = new LinkedBlockingQueue<String>();
	private int contador;
	private AtomicInteger clientes = new AtomicInteger();

	@Override
	public void init() throws ServletException {
		final ExecutorService executors = Executors.newCachedThreadPool();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				while (true) {
					try {
						final String message = messages.take();

						for (final AsyncContext ctx : clients) {
							executors.execute(new Runnable() {
								public void run() {
									try {
										PrintWriter writer = ctx.getResponse()
												.getWriter();
										writer.println(message);
										writer.flush();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}

							});
						}
					} catch (InterruptedException e) {
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
		System.out.println("novo cliente " + clientes.incrementAndGet());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse arg1)
			throws ServletException, IOException {
		System.out.println("sending message para " + clientes);
		messages.add(String.format("mensagem %d %n", contador));
	}
}
