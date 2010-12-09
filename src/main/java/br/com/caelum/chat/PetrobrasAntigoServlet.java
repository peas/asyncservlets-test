package br.com.caelum.chat;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PetrobrasAntigoServlet extends HttpServlet {

	private AtomicInteger valor = new AtomicInteger();
	private AtomicInteger clientes = new AtomicInteger();

	private Queue<BlockingQueue<String>> clients = new ConcurrentLinkedQueue<BlockingQueue<String>>();

	@Override
	protected void doGet(HttpServletRequest req,
			final HttpServletResponse response) throws ServletException,
			IOException {
		System.out.println("[antigo] novo cliente id: "
				+ clientes.incrementAndGet());

		BlockingQueue<String> messages = new LinkedBlockingQueue<String>();
		clients.add(messages);

		try {
			while (true) {
				String message = messages.take();
				PrintWriter writer = response.getWriter();
				writer.println(message);
				writer.flush();
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		System.out
				.println("enviando mensagem para   " + clientes + " clientes");

		String mensagem =  String.format("novo valor PETR4 %d %n", valor.incrementAndGet());

		for (BlockingQueue<String> queue : clients) {
			queue.add(mensagem);
		}

	}
}
