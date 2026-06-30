package jp.co.sony.csl.dcoes.apis.tools.ccc.testsupport;

import io.vertx.core.json.JsonObject;

public final class Fixtures {
	private Fixtures() {
	}

	public static JsonObject saveworthyDeal(String dealId) {
		return new JsonObject()
				.put("dealId", dealId)
				.put("type", "test")
				.put("requestUnitId", "request-unit")
				.put("acceptUnitId", "accept-unit")
				.put("chargeUnitId", "charge-unit")
				.put("dischargeUnitId", "discharge-unit")
				.put("dealAmountWh", 12.5)
				.put("activateDateTime", "2026/01/02-03:04:05");
	}

	public static JsonObject nonSaveworthyDeal() {
		return new JsonObject()
				.put("dealId", "ignored-deal")
				.put("activateDateTime", "--");
	}

	public static JsonObject unitDataMap() {
		return new JsonObject()
				.put("unit-1", new JsonObject()
						.put("unitId", "unit-1")
						.put("value", 10)
						.put("measuredtime", "2026/01/02-03:04:05"))
				.put("unit-2", new JsonObject()
						.put("unitId", "unit-2")
						.put("value", 20))
				.put("invalid", "not-a-json-object");
	}

	public static JsonObject validUnitDataMap() {
		return new JsonObject()
				.put("unit-1", new JsonObject()
						.put("unitId", "unit-1")
						.put("value", 10)
						.put("measuredtime", "2026/01/02-03:04:05"))
				.put("unit-2", new JsonObject()
						.put("unitId", "unit-2")
						.put("value", 20));
	}

	public static JsonObject scenarioResponse() {
		return new JsonObject()
				.put("scenarioId", "scenario-1")
				.put("status", "current");
	}

	public static JsonObject policyResponse() {
		return new JsonObject()
				.put("policyId", "policy-1")
				.put("status", "current");
	}
}
