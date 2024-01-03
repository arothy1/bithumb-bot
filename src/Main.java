import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;

public class Main {

	static int orderPrice = 50000;
	static int sleep = 500;
	static String connectKey;
	static String secretKey;
	static String coin;
    static ObjectMapper om = new ObjectMapper();
	static Api_Client api;

    public static void main(String args[]) throws IOException, InterruptedException {

		getNotice();
		if (Objects.equals("1", getMaintenanceStatus())) {
			return;
		}

		System.out.println("[connectKey]를 입력하세요(엔터)");
		connectKey = new Scanner(System.in).nextLine();
		if (!getMembers().contains(connectKey)) {
			System.out.printf("[%s]은 사용할 수 없습니다. 관리자에게 문의하세요.%n", connectKey);
			Thread.sleep(3000);
			return;
		}
		System.out.printf("%s 사용자님 안녕하세요%n", connectKey);
		System.out.println("[secretKey]를 입력하세요(엔터)");
		secretKey = new Scanner(System.in).nextLine();
		System.out.println("주문할 코인을 입력하세요(ex: btc)(엔터)");
		coin = new Scanner(System.in).nextLine().toUpperCase();
		System.out.println("한번에 주문할 원화가치를 입력하세요(시드가 10만원 일 경우 20000 이 적당합니다.)");
		orderPrice = Integer.parseInt(new Scanner(System.in).nextLine());

		api = new Api_Client(connectKey, secretKey);

        order();
    }

	private static String getMembers() throws IOException {
		StringBuilder members = new StringBuilder();
		URL url = new URL("https://raw.githubusercontent.com/arothy1/B_setting/master/member");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("GET");
		connection.setUseCaches(false);

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
		String inputLine;

		while ((inputLine = bufferedReader.readLine()) != null)  {
			members.append(inputLine);
			members.append("\n");
		}
		bufferedReader.close();
		return members.toString();
	}

	private static String getNotice() throws IOException {
		URL url = new URL("https://raw.githubusercontent.com/arothy1/B_setting/master/notice");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("GET");
		connection.setUseCaches(false);

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
		StringBuilder stringBuilder = new StringBuilder();
		String inputLine;

		while ((inputLine = bufferedReader.readLine()) != null)  {
			stringBuilder.append(inputLine);
			stringBuilder.append("\n");
		}
		bufferedReader.close();

		String response = stringBuilder.toString();
		System.out.println(response);
		return response;
	}

	private static String getMaintenanceStatus() throws IOException {
		URL url = new URL("https://raw.githubusercontent.com/arothy1/B_setting/master/maintenence");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.setRequestMethod("GET");
		connection.setUseCaches(false);

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
		StringBuilder stringBuilder = new StringBuilder();
		String inputLine;

		while ((inputLine = bufferedReader.readLine()) != null)  {
			stringBuilder.append(inputLine);
		}
		bufferedReader.close();

        return stringBuilder.toString();
	}

    private static void order() throws IOException {


		int count = 0;
		int successCount = 0;
		int errorCount = 0;
		while (true) {
			if (count % 300 == 0) {
				if (Objects.equals("1", getMaintenanceStatus())) {
					return;
				}
			}
            try {
				if (successCount > 500) {
					System.out.printf("set delay %,d -> %,d%n", sleep, sleep - 50);
					sleep = sleep - 50;
					successCount = 0;
				}

				if (errorCount > 10) {
					System.out.printf("set delay %,d -> %,d%n", sleep, sleep + 50);
					sleep = sleep + 50;
					errorCount = 0;
				}

				if (count % 2 == 0) {
					bid(count);
					Thread.sleep(sleep);
					ask(count);
					Thread.sleep(sleep);
				} else {
					ask(count);
					Thread.sleep(sleep);
					bid(count);
					Thread.sleep(sleep);
				}

				successCount++;
            } catch (Exception e) {
				errorCount++;
				e.printStackTrace();
            } finally {
				count++;
			}
        }
    }

	private static void ask(int count) throws IOException {
		HashMap<String, String> rgParamsOrderbook = new HashMap();
		rgParamsOrderbook.put("count", "2");
		String result = api.callApiGet(String.format("/public/orderbook/%s_KRW", coin), rgParamsOrderbook);
		Map<String, Object> map = om.readValue(result, Map.class);
		Map<String, Object> data = (Map) map.get("data");
		List<Map<String, String>> bids = (List) data.get("bids");
		List<Map<String, String>> asks = (List) data.get("asks");
		Double askPrice = Double.parseDouble(asks.get(0).get("price")) - TickSize.getSize(Double.parseDouble(bids.get(0).get("price")));

		if (askPrice == Double.parseDouble(bids.get(0).get("price"))) {
			askPrice = Double.parseDouble(asks.get(0).get("price"));
		}

		HashMap<String, String> rgParams = new HashMap();
		rgParams.put("order_currency", coin);
		rgParams.put("payment_currency", "KRW");
		rgParams.put("units", String.format("%.4f", orderPrice / askPrice));
		rgParams.put("type", "ask");
		rgParams.put("price", String.format("%f", askPrice));

		try {
			String bidResult = api.callApiPost("/trade/place", rgParams);
			if (bidResult.contains("error : 429")) {
				System.out.printf("set delay %,d -> %,d%n", sleep, sleep + 50);
				sleep = sleep + 50;
			} else if (bidResult.contains("5600")) {
				cancelAsk(count);
			} else if (bidResult.contains("0000")) {
				System.out.println("ask: " + bidResult.substring(28, bidResult.length() -1).replaceAll("\"", ""));
			} else {
				System.out.println(bidResult);
			}
		} catch (Exception e) {
			System.out.printf("set delay %,d -> %,d%n", sleep, sleep + 50);
			sleep = sleep + 50;
			cancelAsk(count);
		}
	}

	private static void bid(int count) throws IOException {

		HashMap<String, String> rgParamsOrderbook = new HashMap();
		rgParamsOrderbook.put("count", "2");
		String result = api.callApiGet(String.format("/public/orderbook/%s_KRW", coin), rgParamsOrderbook);
		Map<String, Object> map = om.readValue(result, Map.class);
		Map<String, Object> data = (Map) map.get("data");
		List<Map<String, String>> bids = (List) data.get("bids");
		List<Map<String, String>> asks = (List) data.get("asks");
		Double bidPrice = Double.parseDouble(bids.get(0).get("price")) + TickSize.getSize(Double.parseDouble(bids.get(0).get("price")));

		if (bidPrice == Double.parseDouble(asks.get(0).get("price"))) {
			bidPrice = Double.parseDouble(bids.get(0).get("price"));
		}

		HashMap<String, String> rgParams = new HashMap();
		rgParams.put("order_currency", coin);
		rgParams.put("payment_currency", "KRW");
		rgParams.put("units", String.format("%.4f", orderPrice / bidPrice));
		rgParams.put("type", "bid");
		rgParams.put("price", String.format("%f", bidPrice));

		try {
			String bidResult = api.callApiPost("/trade/place", rgParams);
			if (bidResult.contains("error : 429")) {
				System.out.printf("set delay %,d -> %,d%n", sleep, sleep + 50);
				sleep = sleep + 50;
			} else if (bidResult.contains("5600")) {
				cancelBid(count);
			} else if (bidResult.contains("0000")) {
				System.out.println("bid: " + bidResult.substring(28, bidResult.length() -1).replaceAll("\"", ""));
			} else {
				System.out.println(bidResult);
			}
		} catch (Exception e) {
			System.out.printf("set delay %,d -> %,d%n", sleep, sleep + 50);
			sleep = sleep + 50;
			cancelBid(count);
		}
	}

	private static void cancelBid(int count) {
		if (count % 3 == 0) {
			return;
		}
        Api_Client api = new Api_Client(connectKey,
            secretKey);

        HashMap<String, String> rgParams = new HashMap();
        rgParams.put("order_currency", coin);
        rgParams.put("payment_currency", "KRW");

        try {
            String result = api.callApiPost("/info/orders", rgParams);
            Map<String, Object> map = om.readValue(result, Map.class);
			List<Map<String, String>> data = (List) map.get("data");
			if (data != null) {
				for (Map<String, String> ele : data) {
					String orderId = ele.get("order_id");
					String type = ele.get("type");
					if (type.equals("ask")) {
						continue;
					}
					Thread.sleep(sleep);
					rgParams.put("order_id", orderId);
					rgParams.put("type", type);
					api.callApiPost("/trade/cancel", rgParams);
					System.out.println("cancel_bid: " + orderId);
				}
			}
        } catch (Exception e) {
            // ignore
        }
    }

	private static void cancelAsk(int count) {
		if (count % 3 == 0) {
			return;
		}

		Api_Client api = new Api_Client(connectKey,
			secretKey);

		HashMap<String, String> rgParams = new HashMap();
		rgParams.put("order_currency", coin);
		rgParams.put("payment_currency", "KRW");

		try {
			String result = api.callApiPost("/info/orders", rgParams);
			Map<String, Object> map = om.readValue(result, Map.class);
			List<Map<String, String>> data = (List) map.get("data");
			if (data != null) {
				for (Map<String, String> ele : data) {
					String orderId = ele.get("order_id");
					String type = ele.get("type");
					if (type.equals("bid")) {
						continue;
					}
					Thread.sleep(sleep);
					rgParams.put("order_id", orderId);
					rgParams.put("type", type);
					api.callApiPost("/trade/cancel", rgParams);
					System.out.println("cancel_ask: " + orderId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void cancelOrder() {

		Api_Client api = new Api_Client(connectKey,
			secretKey);

		HashMap<String, String> rgParams = new HashMap();
		rgParams.put("order_currency", coin);
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
			e.printStackTrace();
		}
	}
}

