import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;

public class Main {

	static final int sleep = 100;
	static int orderPrice = 50000;
	static String connectKey;
	static String secretKey;
	static String coin;
    static ObjectMapper om = new ObjectMapper();

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
		System.out.println("주문할 코인을 입력하세요(엔터)");
		coin = new Scanner(System.in).nextLine().toUpperCase();
		System.out.println("한번에 주문할 수량을 입력하세요(100,000원 시드일 경우 20000 입력)");
		orderPrice = Integer.parseInt(new Scanner(System.in).nextLine());
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
        Api_Client api = new Api_Client(connectKey,
            secretKey);

        HashMap<String, String> rgParamsOrderbook = new HashMap();
        rgParamsOrderbook.put("count", "2");

		int count = 0;
		while (true) {
			if (count % 1000 == 0) {
				if (Objects.equals("1", getMaintenanceStatus())) {
					return;
				}
			}
            try {
                String result = api.callApiGet(String.format("/public/orderbook/%s_KRW", coin), rgParamsOrderbook);
                Map<String, Object> map = om.readValue(result, Map.class);
                Map<String, Object> data = (Map) map.get("data");
                List<Map<String, String>> bids = (List) data.get("bids");
                List<Map<String, String>> asks = (List) data.get("asks");
                Double bidPrice = Double.parseDouble(bids.get(0).get("price")) + TickSize.getSize(Double.parseDouble(bids.get(0).get("price")));
                Double askPrice = Double.parseDouble(asks.get(0).get("price")) - TickSize.getSize(Double.parseDouble(bids.get(0).get("price")));

				if (bidPrice == Double.parseDouble(asks.get(0).get("price"))) {
					bidPrice = Double.parseDouble(bids.get(0).get("price"));
				}
				if (askPrice == Double.parseDouble(bids.get(0).get("price"))) {
					askPrice = Double.parseDouble(asks.get(0).get("price"));
				}

                HashMap<String, String> rgParams = new HashMap();
                rgParams.put("order_currency", coin);
                rgParams.put("payment_currency", "KRW");
                rgParams.put("units", String.format("%.4f", orderPrice / bidPrice));
                rgParams.put("type", "bid");
                rgParams.put("price", String.format("%f", bidPrice));
				Map<String, Object> bidResult = om.readValue(api.callApiPost("/trade/place", rgParams), Map.class);
				if ("5600".equals(bidResult.get("status"))) {
					cancelBid();
				} else if ("0000".equals(bidResult.get("status"))) {
					System.out.println("bid: " + bidResult.get("order_id"));
				} else {
					System.out.println(bidResult);
				}

				rgParams.put("units", String.format("%.4f", orderPrice / askPrice));
                rgParams.put("type", "ask");
                rgParams.put("price", String.format("%f", askPrice));
                Map<String, Object> askResult = om.readValue(api.callApiPost("/trade/place", rgParams), Map.class);
				if ("5600".equals(askResult.get("status"))) {
					cancelAsk();
				} else if ("0000".equals(askResult.get("status"))) {
					System.out.println("ask: " + askResult.get("order_id"));
				} else {
					System.out.println(askResult);
				}
                Thread.sleep(sleep);
            } catch (Exception e) {
				cancelOrder();
				e.printStackTrace();
            } finally {
				count++;
			}
        }
    }

    private static void cancelBid() {
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
		rgParams.put("order_currency", coin);
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
			// ignore
		}
	}
}

