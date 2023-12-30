import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import org.codehaus.jackson.map.ObjectMapper;

public class Main {

    static final int MINIMUM_TICK = 1000;
    static final String BID_AMOUNT = "0.001";
    static final String ASK_AMOUNT = "0.001";
	static final int sleep = 100;
	static String connectKey;
	static String secretKey;
    static ObjectMapper om = new ObjectMapper();

    public static void main(String args[]) {

		connectKey = "";
		System.out.println("[secretKey]를 입력하세요(엔터)");
		secretKey = new Scanner(System.in).nextLine();

        order();

    }

    private static void order() {
        Api_Client api = new Api_Client(connectKey,
            secretKey);

        HashMap<String, String> rgParamsOrderbook = new HashMap();
        rgParamsOrderbook.put("count", "2");

		int count = 0;
		while (true) {
            try {
                System.out.println("count: " + count);
                String result = api.callApiGet("/public/orderbook/BTC_KRW", rgParamsOrderbook);
                Map<String, Object> map = om.readValue(result, Map.class);
                Map<String, Object> data = (Map) map.get("data");
                List<Map<String, String>> bids = (List) data.get("bids");
                List<Map<String, String>> asks = (List) data.get("asks");
                Integer bidPrice = Integer.parseInt(bids.get(0).get("price")) + MINIMUM_TICK;
                Integer askPrice = Integer.parseInt(asks.get(0).get("price")) - MINIMUM_TICK;

				if (bidPrice == Integer.parseInt(asks.get(0).get("price"))) {
					bidPrice = Integer.parseInt(bids.get(0).get("price"));
				}
				if (askPrice == Integer.parseInt(bids.get(0).get("price"))) {
					askPrice = Integer.parseInt(asks.get(0).get("price"));
				}

                HashMap<String, String> rgParams = new HashMap();
                rgParams.put("order_currency", "BTC");
                rgParams.put("payment_currency", "KRW");
                rgParams.put("units", BID_AMOUNT);
                rgParams.put("type", "bid");
                rgParams.put("price", String.format("%d", bidPrice));
				Map<String, Object> bidResult = om.readValue(api.callApiPost("/trade/place", rgParams), Map.class);
				if ("5600".equals(bidResult.get("status"))) {
					cancelBid();
				}

                rgParams.put("units", ASK_AMOUNT);
                rgParams.put("type", "ask");
                rgParams.put("price", String.format("%d", askPrice));
                Map<String, Object> askResult = om.readValue(api.callApiPost("/trade/place", rgParams), Map.class);
				if ("5600".equals(askResult.get("status"))) {
					cancelAsk();
				}
                Thread.sleep(sleep);
            } catch (Exception e) {
				cancelOrder();
            } finally {
				count++;
			}
        }
    }

    private static void cancelBid() {
        Api_Client api = new Api_Client(connectKey,
            secretKey);

        HashMap<String, String> rgParams = new HashMap();
        rgParams.put("order_currency", "BTC");
        rgParams.put("payment_currency", "KRW");

        try {
            String result = api.callApiPost("/info/orders", rgParams);
            Map<String, Object> map = om.readValue(result, Map.class);
            List<Map<String, String>> data = (List) map.get("data");
            for (Map<String, String> ele : data) {
                String orderId = ele.get("order_id");
                String type = ele.get("type");
				if (type.equals("ask")) {
					continue;
				}
                rgParams.put("order_id", orderId);
                rgParams.put("type", type);
                api.callApiPost("/trade/cancel", rgParams);
				System.out.println("cancel_bid: " + orderId);
            }
        } catch (Exception e) {
            // ignore
        }
    }

	private static void cancelAsk() {

		Api_Client api = new Api_Client(connectKey,
			secretKey);

		HashMap<String, String> rgParams = new HashMap();
		rgParams.put("order_currency", "BTC");
		rgParams.put("payment_currency", "KRW");

		try {
			String result = api.callApiPost("/info/orders", rgParams);
			Map<String, Object> map = om.readValue(result, Map.class);
			List<Map<String, String>> data = (List) map.get("data");
			for (Map<String, String> ele : data) {
				String orderId = ele.get("order_id");
				String type = ele.get("type");
				if (type.equals("bid")) {
					continue;
				}
				rgParams.put("order_id", orderId);
				rgParams.put("type", type);
				api.callApiPost("/trade/cancel", rgParams);
				System.out.println("cancel_ask: " + orderId);
			}
		} catch (Exception e) {
			// ignore
		}
	}

	private static void cancelOrder() {

		Api_Client api = new Api_Client(connectKey,
			secretKey);

		HashMap<String, String> rgParams = new HashMap();
		rgParams.put("order_currency", "BTC");
		rgParams.put("payment_currency", "KRW");

		try {
			String result = api.callApiPost("/info/orders", rgParams);
			Map<String, Object> map = om.readValue(result, Map.class);
			List<Map<String, String>> data = (List) map.get("data");
			for (Map<String, String> ele : data) {
				String orderId = ele.get("order_id");
				String type = ele.get("type");
				rgParams.put("order_id", orderId);
				rgParams.put("type", type);
				api.callApiPost("/trade/cancel", rgParams);
				System.out.println("cancel_order: " + orderId);
			}
		} catch (Exception e) {
			// ignore
		}
	}
}

