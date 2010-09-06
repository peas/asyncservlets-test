package br.com.caelum.chat;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentDecoderChannel;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.entity.ConsumingNHttpEntityTemplate;
import org.apache.http.nio.entity.ContentListener;
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
import org.apache.log4j.Logger;

public class NHttpClient {

	private static final int PORT = 80;
	private static final String HOST_NAME = "canrailsscale.com";
	private static final String URI = "/";
	private static Logger log = Logger.getLogger(NHttpClient.class);

	public static void main(String[] args) throws Exception {
		HttpParams params = new BasicHttpParams();
		params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 50000)
				.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
				.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
						8 * 1024)
				.setBooleanParameter(
						CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
				.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
				.setParameter(CoreProtocolPNames.USER_AGENT,
						"Testador do Paulo MegaBrowser");

		// duas threads pro reator... demorou com elas vai dançar.
		final ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(1,
				params);

		BasicHttpProcessor httpproc = new BasicHttpProcessor();

		// talvez vc nao precise de todos eles:
		httpproc.addInterceptor(new RequestContent());
		httpproc.addInterceptor(new RequestTargetHost());
		httpproc.addInterceptor(new RequestConnControl());
		httpproc.addInterceptor(new RequestUserAgent());
		httpproc.addInterceptor(new RequestExpectContinue());

		AsyncNHttpClientHandler handler = new AsyncNHttpClientHandler(httpproc,
				new HandlerExecucaoAssincrono(URI),
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

		for (int i = 0; i < 1; i++) {
			ioReactor.connect(new InetSocketAddress(HOST_NAME, PORT),
					null, null, new TesterSessionCallback());
		}

		// ioReactor.shutdown();
	}

}

class HandlerExecucaoAssincrono implements NHttpRequestExecutionHandler {

	private static Logger log = Logger
			.getLogger(HandlerExecucaoAssincrono.class);

	private static final String DONE_FLAG = "done";
	private static final String ENTITY_RECEIVED = "received";

	private String uri;
	
	public HandlerExecucaoAssincrono(String uri) {
		this.uri = uri;
	}

	public void initalizeContext(final HttpContext context,
			final Object attachment) {
		HttpHost targetHost = (HttpHost) attachment;
		context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, targetHost);
	}

	public void finalizeContext(final HttpContext context) {
		context.removeAttribute(DONE_FLAG);
		log.info(String.format("finalizado "));
	}

	public HttpRequest submitRequest(final HttpContext context) {
		Object done = context.getAttribute(DONE_FLAG);
		if (done == null) {
			HttpHost targetHost = (HttpHost) context
					.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			log.info(String.format("Sending request %s to %s ", context,
					targetHost));
			context.setAttribute(DONE_FLAG, true);

			return new BasicHttpRequest("GET", uri);
		} else {
			return null;
		}
	}

	public void handleResponse(HttpResponse response, HttpContext context) {
		HttpEntity entity = response.getEntity();

		//log.info("Entidade: " + entity);
	}

	@Override
	public ConsumingNHttpEntity responseEntity(HttpResponse response,
			final HttpContext context) throws IOException {

		if (context.getAttribute(ENTITY_RECEIVED) != null) return null;

		
		ContentListener listener = new ContentListener() {

			@Override
			public void finished() {
				context.setAttribute(ENTITY_RECEIVED, null);
			}

			@Override
			public void contentAvailable(ContentDecoder decoder,
					IOControl ioctrl) throws IOException {
				context.setAttribute(ENTITY_RECEIVED, true);
				
				
				ByteBuffer buffer = ByteBuffer.allocate(10 * 1024);

				ContentDecoderChannel c = new ContentDecoderChannel(decoder);
				int read = c.read(buffer);
				log.info("bytes lido " + read);
				// TODO: acertar encoding
				log.info("conteudo total " + new String(buffer.array()).length());
			}
		};
		return new ConsumingNHttpEntityTemplate(response.getEntity(), listener);
	}

	/*
	 * public ConsumingNHttpEntity responseEntity(HttpResponse response,
	 * HttpContext context) throws IOException {
	 * 
	 * return new BufferingNHttpEntity(response.getEntity(), new
	 * HeapByteBufferAllocator()); }
	 */

}

class TesterSessionCallback implements SessionRequestCallback {

	private static Logger log = Logger.getLogger(TesterSessionCallback.class);

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
