package br.com.caelum;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import javax.swing.plaf.SliderUI;

import org.apache.log4j.Logger;

public class PureNioClient {

	private static Logger log = Logger.getLogger(PureNioClient.class);

	public static void main(String[] args) throws Exception {
		SocketChannel channel = SocketChannel.open();

		channel.configureBlocking(false);

		channel.connect(new InetSocketAddress("www.google.com.br", 80));

		Selector selector = Selector.open();
		SelectionKey key = channel.register(selector, SelectionKey.OP_READ
				| SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);

		while (true) {
			selector.select();

			for (SelectionKey k : selector.selectedKeys()) {

				SocketChannel c = (SocketChannel) k.channel();
				System.out.println(k.isReadable());
				System.out.println(k.isWritable());

				if (k.isConnectable()) {
					log.info("conectando");
					c.finishConnect();
				}
				if (k.isReadable() && k.attachment() != null) {
					log.info("lendo");

					ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
					int read = c.read(buffer);
					System.out.println(read);
					c.write(buffer);
				}
				if (k.isWritable() && k.attachment() == null) {
					log.info("escrevendo");
					ByteBuffer buffer = ByteBuffer.wrap("GET /\r\n".getBytes());
					log.info(buffer.limit());
					log.info(buffer.capacity());
					log.info(buffer.position());
					
					c.write(buffer);
					log.info(buffer.limit());
					log.info(buffer.capacity());
					log.info(buffer.position());
					
					k.attach(new Object());
				}

				Thread.sleep(5000);
			}
		}

	}
}
