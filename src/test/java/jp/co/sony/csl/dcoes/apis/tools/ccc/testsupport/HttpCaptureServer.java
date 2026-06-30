package jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class HttpCaptureServer {
	private final List<CapturedRequest> requests_ = Collections.synchronizedList(new ArrayList<>());
	private HttpServer server_;
	private int statusCode_ = 200;
	private String responseBody_ = "";
	private String responseContentType_ = "application/json";

	public HttpCaptureServer respond(int statusCode, String responseBody) {
		statusCode_ = statusCode;
		responseBody_ = responseBody == null ? "" : responseBody;
		return this;
	}

	public Future<HttpCaptureServer> start(Vertx vertx) {
		Promise<HttpCaptureServer> promise = Promise.promise();
		server_ = vertx.createHttpServer();
		server_.requestHandler(req -> req.bodyHandler(body -> {
			requests_.add(new CapturedRequest(req.method().name(), req.path(), req.headers(), body.toString(StandardCharsets.UTF_8)));
			req.response()
					.setStatusCode(statusCode_)
					.putHeader("content-type", responseContentType_)
					.end(responseBody_);
		}));
		server_.listen(0, "127.0.0.1", res -> {
			if (res.succeeded()) {
				promise.complete(this);
			} else {
				promise.fail(res.cause());
			}
		});
		return promise.future();
	}

	public Future<Void> close() {
		if (server_ == null) {
			return Future.succeededFuture();
		}
		return server_.close();
	}

	public int port() {
		return server_.actualPort();
	}

	public List<CapturedRequest> requests() {
		synchronized (requests_) {
			return new ArrayList<>(requests_);
		}
	}

	public Future<CapturedRequest> awaitRequest(Vertx vertx) {
		return awaitRequest(vertx, 1000L);
	}

	public Future<CapturedRequest> awaitRequest(Vertx vertx, long timeoutMsec) {
		Promise<CapturedRequest> promise = Promise.promise();
		long deadline = System.currentTimeMillis() + timeoutMsec;
		final long[] timerId = new long[1];
		timerId[0] = vertx.setPeriodic(10L, id -> {
			List<CapturedRequest> current = requests();
			if (!current.isEmpty()) {
				vertx.cancelTimer(timerId[0]);
				promise.complete(current.get(0));
			} else if (System.currentTimeMillis() >= deadline) {
				vertx.cancelTimer(timerId[0]);
				promise.fail("timed out waiting for HTTP request");
			}
		});
		return promise.future();
	}

	public Future<Void> assertNoRequest(Vertx vertx) {
		Promise<Void> promise = Promise.promise();
		vertx.setTimer(150L, id -> {
			if (requests().isEmpty()) {
				promise.complete();
			} else {
				promise.fail("expected no HTTP requests but captured " + requests().size());
			}
		});
		return promise.future();
	}

	public static class CapturedRequest {
		private final String method_;
		private final String path_;
		private final MultiMap headers_;
		private final String body_;

		CapturedRequest(String method, String path, MultiMap headers, String body) {
			method_ = method;
			path_ = path;
			headers_ = MultiMap.caseInsensitiveMultiMap().addAll(headers);
			body_ = body;
		}

		public String method() {
			return method_;
		}

		public String path() {
			return path_;
		}

		public String header(String name) {
			return headers_.get(name);
		}

		public String body() {
			return body_;
		}

		public JsonArray bodyAsJsonArray() {
			return new JsonArray(body_);
		}

		public JsonObject bodyAsJsonObject() {
			return new JsonObject(body_);
		}

		public Map<String, String> formParams() {
			Map<String, String> result = new LinkedHashMap<>();
			if (body_.isEmpty()) {
				return result;
			}
			for (String pair : body_.split("&")) {
				String[] parts = pair.split("=", 2);
				String key = decode(parts[0]);
				String value = parts.length == 2 ? decode(parts[1]) : "";
				result.put(key, value);
			}
			return result;
		}

		private static String decode(String value) {
			try {
				return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
