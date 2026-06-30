package jp.co.sony.csl.dcoes.apis.tools.ccc.impl.http_post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.Fixtures;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.HttpCaptureServer;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.TestConfigs;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class HttpPostUnitDataReportingImplTest {
	@Test
	void reportConvertsObjectMapToJsonArray(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");

		server.start(vertx).onComplete(testContext.succeeding(started -> {
			TestConfigs.apply(TestConfigs.configWithHttpEndpoint("unitDataReporting", started.port(), "/unit-data/report"));
			HttpPostUnitDataReportingImpl impl = new HttpPostUnitDataReportingImpl(vertx);
			Promise<Void> promise = Promise.promise();
			impl.report(Fixtures.validUnitDataMap(), promise);
			promise.future().onComplete(testContext.succeeding(done -> server.awaitRequest(vertx).onComplete(testContext.succeeding(captured -> testContext.verify(() -> {
				assertEquals("POST", captured.method());
				assertEquals("/unit-data/report", captured.path());
				assertTrue(captured.header("content-type").startsWith("application/json"));
				JsonArray body = captured.bodyAsJsonArray();
				assertEquals(2, body.size());
				JsonObject first = body.getJsonObject(0);
				assertTrue(first.containsKey("unitId"));
				assertTrue(first.getString("measuredtime").contains("T"));
				testContext.completeNow();
			})))));
		}));
	}

	@Test
	void nonSuccessHttpStatusFailsReport(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(500, "boom");

		server.start(vertx).onComplete(testContext.succeeding(started -> {
			TestConfigs.apply(TestConfigs.configWithHttpEndpoint("unitDataReporting", started.port(), "/unit-data/report"));
			HttpPostUnitDataReportingImpl impl = new HttpPostUnitDataReportingImpl(vertx);
			Promise<Void> promise = Promise.promise();
			impl.report(Fixtures.validUnitDataMap(), promise);
			promise.future().onComplete(testContext.failingThenComplete());
		}));
	}
}
