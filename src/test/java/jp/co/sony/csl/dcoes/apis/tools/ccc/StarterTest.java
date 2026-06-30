package jp.co.sony.csl.dcoes.apis.tools.ccc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jp.co.sony.csl.dcoes.apis.common.ServiceAddress;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.Fixtures;
import jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport.TestConfigs;
import jp.co.sony.csl.dcoes.apis.tools.ccc.util.Starter;

@ExtendWith(VertxExtension.class)
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class StarterTest {
	@Test
	void deploysStarterAndExposesDisabledServices(Vertx vertx, VertxTestContext testContext) {
		TestConfigs.apply(TestConfigs.disabledConfig());

		DeliveryOptions credentials = new DeliveryOptions()
				.addHeader("account", "acct")
				.addHeader("password", "secret")
				.addHeader("unitId", "unit-1");

		vertx.deployVerticle(new Starter()).onComplete(testContext.succeeding(deploymentId -> {
			vertx.eventBus().<String>request(ServiceAddress.Mediator.dealLogging(), Fixtures.saveworthyDeal("deal-disabled"))
					.compose(dealReply -> {
						testContext.verify(() -> assertEquals("N/A", dealReply.body()));
						return vertx.eventBus().<Object>request(ServiceAddress.ControlCenterClient.scenario(), null, credentials);
					})
					.compose(scenarioReply -> {
						testContext.verify(() -> assertNull(scenarioReply.body()));
						return vertx.eventBus().<Object>request(ServiceAddress.ControlCenterClient.policy(), null, credentials);
					})
					.onComplete(testContext.succeeding((Message<Object> policyReply) -> testContext.verify(() -> {
						assertNull(policyReply.body());
						testContext.completeNow();
					})));
		}));
	}
}
