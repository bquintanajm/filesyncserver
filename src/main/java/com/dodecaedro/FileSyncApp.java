package com.dodecaedro;

import io.javalin.Javalin;
import io.javalin.security.AccessManager;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class FileSyncApp {
	public static void main(String[] args) throws Exception {
		var properties = buildProperties();
		var service = new FileStorageService(properties);
		var auth = new FileSyncAuth(properties);

		Javalin
				.create()
				.server(FileSyncApp::configureServer)
				.get("/pull", context -> {
					context.contentType("application/json");
					context.result(service.getFileInputStream());
				})
				.post("/push", context -> service.saveFile(context.req.getInputStream()))
				.accessManager(auth)
				.exception(FileNotFoundException.class, (e, ctx) -> ctx.status(404))
				.requestLogger((ctx, timeMs) -> log.trace("{} endpoint invoked with query string: {}",
						ctx.method(),
						ctx.req.getRequestURL().append('?').append(ctx.req.getQueryString())))
				.start();
	}

	private static FileSyncProperties buildProperties() {
		return FileSyncProperties.builder()
				.fileName(System.getenv("file-name"))
				.filePath(System.getenv("file-path"))
				.key(System.getenv("key"))
				.build();
	}

	private static Server configureServer() {
		Server server = new Server();
		ServerConnector sslConnector = new ServerConnector(server, getSslContextFactory());
		sslConnector.setPort(8443);
		server.setConnectors(new Connector[]{sslConnector});
		return server;
	}

	private static SslContextFactory getSslContextFactory() {
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(Thread.currentThread().getContextClassLoader()
				.getResource("keystore.p12").toExternalForm());
		sslContextFactory.setKeyStorePassword("changeit");
		return sslContextFactory;
	}
}
