package br.com.caelum.chat;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultClientIOEventDispatch;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.AsyncNHttpClientHandler;
import org.apache.http.nio.protocol.EventListener;
import org.apache.http.nio.protocol.NHttpRequestExecutionHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class NHttpClient {

	private static Logger log = Logger.getLogger(NHttpClient.class);

	public static void main(String[] args) throws Exception {
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
				.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
						8 * 1024)
				.setBooleanParameter(
						CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
				.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
				.setParameter(CoreProtocolPNames.USER_AGENT,
						"Testador do Paulo MegaBrowser");

		// duas threads pro reator... demorou com elas vai dançar.
		final ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(2,
				params);

		BasicHttpProcessor httpproc = new BasicHttpProcessor();

		// talvez vc nao precise de todos eles:
		httpproc.addInterceptor(new RequestContent());
		httpproc.addInterceptor(new RequestTargetHost());
		httpproc.addInterceptor(new RequestConnControl());
		httpproc.addInterceptor(new RequestUserAgent());
		httpproc.addInterceptor(new RequestExpectContinue());

		AsyncNHttpClientHandler handler = new AsyncNHttpClientHandler(
				httpproc, new MyHttpRequestExecutionHandler(),
				new DefaultConnectionReuseStrategy(), params);

		handler.setEventListener(new EventLogger());

		final IOEventDispatch ioEventDispatch = new DefaultClientIOEventDispatch(
				handler, params);

		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					// esse cara segura a thread daqui eqto nao for desligado
					ioReactor.execute(ioEventDispatch);
				} catch (InterruptedIOException ex) {
					log.error("Interrupted");
				} catch (IOException e) {
					log.error("I/O error: ", e);
				}
				log.info("por algum motivo temrinou antes do Control C");
			}

		});

		for (int i = 0; i < 10; i++) {
			ioReactor.connect(new InetSocketAddress("www.yahoo.com", 80), null,
					new HttpHost("www.yahoo.com"),
					new TesterSessionCallback());
		}

		// ioReactor.shutdown();
	}

}




class MyHttpRequestExecutionHandler implements
		NHttpRequestExecutionHandler {

	private static Logger log = Logger.getLogger(MyHttpRequestExecutionHandler.class);

	private final static String REQUEST_SENT = "request-sent";
	private final static String RESPONSE_RECEIVED = "response-received";

	public void initalizeContext(final HttpContext context,
			final Object attachment) {
		HttpHost targetHost = (HttpHost) attachment;
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, targetHost);
	}

	public void finalizeContext(final HttpContext context) {
		Object flag = context.getAttribute(RESPONSE_RECEIVED);
		if (flag == null) {
			// aqui temrinou requisicao
		}
	}

	public HttpRequest submitRequest(final HttpContext context) {
		HttpHost targetHost = (HttpHost) context
				.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		Object flag = context.getAttribute(REQUEST_SENT);
		if (flag == null) {
			context.setAttribute(REQUEST_SENT, Boolean.TRUE);
			log.info("Sending request to " + targetHost);

			return new BasicHttpRequest("GET", "/");
		} else {
			// ja foi, ta enviando headers
			return null;
		}
	}

	public void handleResponse( HttpResponse response,
			 HttpContext context) {
		HttpEntity entity = response.getEntity();
		try {
			entity.
			String content = EntityUtils.toString(entity);
			log.info("Dados: " + content);

			System.out.println("--------------");
		} catch (IOException ex) {
			log.error("I/O error: ", ex);
		}

		context.setAttribute(RESPONSE_RECEIVED, Boolean.TRUE);

		// aqui chegou uma parte.
	}

	@Override
	public ConsumingNHttpEntity responseEntity(HttpResponse response,
			HttpContext context) throws IOException {
		
		return null;
	}

}

class TesterSessionCallback implements SessionRequestCallback {

	private static Logger log = Logger
			.getLogger(TesterSessionCallback.class);

	public void cancelled(final SessionRequest request) {
		log.info("cancelled: " + request);
	}

	public void completed(final SessionRequest request) {
		log.info("completed: " + request);
	}

	public void failed(final SessionRequest request) {
		log.info("failed: " + request);
	}

	public void timeout(final SessionRequest request) {
		log.info("timeout: " + request);
	}

}

class EventLogger implements EventListener {

	private static Logger log = Logger.getLogger(EventLogger.class);

	public void connectionOpen(NHttpConnection conn) {
		log.info("opening: " + conn);
	}

	public void connectionTimeout(NHttpConnection conn) {
		log.info("time out: " + conn);
	}

	public void connectionClosed(NHttpConnection conn) {
		log.info("closed: " + conn);
	}

	public void fatalIOException(IOException ex, NHttpConnection conn) {
		log.fatal("error: ", ex);
	}

	public void fatalProtocolException(HttpException ex, NHttpConnection conn) {
		log.fatal("error: ", ex);
	}

}
