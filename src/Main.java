import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;

public class Main {

    static final int MINIMUM_TICK = 1000;
    static final String BID_AMOUNT = "0.0005";
    static final String ASK_AMOUNT = "0.0005";
	static int sleep = 2000;
	static String connectKey;
	static String secretKey;
	static String coin;
	static int acceleration = 1;
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
		System.out.println("가속화 하시겠습니까?(y/N)(2개 이상 계정 운영하면 N)");
		acceleration = new Scanner(System.in).nextLine().equals("y") ? 2 : 1;

		setSleep();
        order();

    }

	private static void setSleep() throws IOException {
		URL url = new URL("https://raw.githubusercontent.com/arothy1/B_setting/master/sleep");
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
		sleep = Integer.parseInt(stringBuilder.toString()) / acceleration;
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
			if (count % 500 == 0) {
				setSleep();
				if (Objects.equals("1", getMaintenanceStatus())) {
					return;
				}
			}
            try {
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
				} else if ("0000".equals(bidResult.get("status"))) {
					System.out.println("bid: " + bidResult.get("order_id"));
				} else {
					System.out.println(bidResult);
				}

                rgParams.put("units", ASK_AMOUNT);
                rgParams.put("type", "ask");
                rgParams.put("price", String.format("%d", askPrice));
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

