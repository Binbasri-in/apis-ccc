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
class HttpPostDealReportingImplTest {
	@Test
	void reportSingleDealPostsJsonArray(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");

		server.start(vertx).onComplete(testContext.succeeding(started -> {
			TestConfigs.apply(TestConfigs.configWithHttpEndpoint("dealReporting", started.port(), "/deal/report"));
			HttpPostDealReportingImpl impl = new HttpPostDealReportingImpl(vertx);
			Promise<Void> promise = Promise.promise();
			impl.report(Fixtures.saveworthyDeal("single-http-deal"), promise);
			promise.future().onComplete(testContext.succeeding(done -> server.awaitRequest(vertx).onComplete(testContext.succeeding(captured -> testContext.verify(() -> {
				assertEquals("POST", captured.method());
				assertEquals("/deal/report", captured.path());
				assertTrue(captured.header("content-type").startsWith("application/json"));
				JsonArray body = captured.bodyAsJsonArray();
				assertEquals(1, body.size());
				JsonObject posted = body.getJsonObject(0);
				assertEquals("single-http-deal", posted.getString("dealId"));
				assertTrue(posted.getString("activateDateTime").contains("T"));
				testContext.completeNow();
			})))));
		}));
	}

	@Test
	void reportDealArrayPostsJsonArray(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(200, "");

		server.start(vertx).onComplete(testContext.succeeding(started -> {
			TestConfigs.apply(TestConfigs.configWithHttpEndpoint("dealReporting", started.port(), "/deal/report"));
			HttpPostDealReportingImpl impl = new HttpPostDealReportingImpl(vertx);
			Promise<Void> promise = Promise.promise();
			impl.report(new JsonArray()
					.add(Fixtures.saveworthyDeal("array-deal-1"))
					.add(Fixtures.saveworthyDeal("array-deal-2")), promise);
			promise.future().onComplete(testContext.succeeding(done -> server.awaitRequest(vertx).onComplete(testContext.succeeding(captured -> testContext.verify(() -> {
				JsonArray body = captured.bodyAsJsonArray();
				assertEquals(2, body.size());
				assertEquals("array-deal-1", body.getJsonObject(0).getString("dealId"));
				assertEquals("array-deal-2", body.getJsonObject(1).getString("dealId"));
				testContext.completeNow();
			})))));
		}));
	}

	@Test
	void nonSuccessHttpStatusFailsReport(Vertx vertx, VertxTestContext testContext) {
		HttpCaptureServer server = new HttpCaptureServer().respond(500, "boom");

		server.start(vertx).onComplete(testContext.succeeding(started -> {
			TestConfigs.apply(TestConfigs.configWithHttpEndpoint("dealReporting", started.port(), "/deal/report"));
			HttpPostDealReportingImpl impl = new HttpPostDealReportingImpl(vertx);
			Promise<Void> promise = Promise.promise();
			impl.report(Fixtures.saveworthyDeal("failed-http-deal"), promise);
			promise.future().onComplete(testContext.failingThenComplete());
		}));
	}
}
