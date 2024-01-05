import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;

public class Main {

	static int sleep = 100;
	static String coin = "btc";
    static ObjectMapper om = new ObjectMapper();
	static Api_Client globalApi;
	static Api_Client globalApi2;
	static Random random = new Random();

    public static void main(String args[]) throws IOException, InterruptedException {

		printNotice();
		if (Objects.equals("1", getMaintenanceStatus())) {
			return;
		}

		if (!setting() || !setting2()) {
			return;
		}
		setCoin();
        order();

    }

	private static void setCoin() {
//		System.out.println("주문할 코인을 입력하세요(ex: btc)(엔터)");
//		coin = new Scanner(System.in).nextLine().toUpperCase();
	}

	private static boolean setting() throws IOException, InterruptedException {
		String connectKey;
		String secretKey;
		Integer orderPrice = 30000;
		System.out.println("1번계정의 [connectKey]를 입력하세요(엔터)");
		connectKey = new Scanner(System.in).nextLine();
		if (!getMembers().contains(connectKey)) {
			System.out.printf("[%s]은 사용할 수 없습니다. 관리자에게 문의하세요.%n", connectKey);
			executeSleep(3000);
			return false;
		}
		System.out.println("1번계정의 [secretKey]를 입력하세요(엔터)");
		secretKey = new Scanner(System.in).nextLine();

		try {
			System.out.println("1번계정의 주문금액을 입력하세요. (시드가 10만원 일 경우 20000 이 적당합니다.)");
			orderPrice = Integer.parseInt(new Scanner(System.in).nextLine());
		} catch (Exception e) {
			System.out.println("잘못된 입력으로 기본 세팅으로 진행됩니다." + orderPrice);
		}

		globalApi = new Api_Client(connectKey, secretKey, orderPrice);
		return true;
	}

	private static boolean setting2() throws IOException, InterruptedException {
		System.out.println("2번계정을 사용하시겠습니까?");
		System.out.println("사용하지 않으시려면 [엔터] 사용하시려면 [Y] 를 입력하세요");
		if (!new Scanner(System.in).nextLine().equalsIgnoreCase("Y")) {
			System.out.println("1번 계정만 사용합니다.");
			globalApi2 = globalApi;
			return true;
		}

		String connectKey;
		String secretKey;
		Integer orderPrice = 30000;
		System.out.println("2번계정의 [connectKey]를 입력하세요(엔터)");
		connectKey = new Scanner(System.in).nextLine();
		if (!getMembers().contains(connectKey)) {
			System.out.printf("[%s]은 사용할 수 없습니다. 관리자에게 문의하세요.%n", connectKey);
			executeSleep(3000);
			return false;
		}
		System.out.println("2번계정의 [secretKey]를 입력하세요(엔터)");
		secretKey = new Scanner(System.in).nextLine();

		try {
			System.out.println("2번계정의 주문금액을 입력하세요. (시드가 10만원 일 경우 20000 이 적당합니다.)");
			orderPrice = Integer.parseInt(new Scanner(System.in).nextLine());
		} catch (Exception e) {
			System.out.println("잘못된 입력으로 기본 세팅으로 진행됩니다." + orderPrice);
		}
		globalApi2 = new Api_Client(connectKey, secretKey, orderPrice);
		return true;
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

	private static void printNotice() throws IOException {
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
			Api_Client api = count % 2 == 0 ? globalApi : globalApi2;
            try {
				if (successCount > 50) {
					decreaseSleep();
					successCount = 0;
				}

				if (errorCount > 10) {
					increaseSleep();
					errorCount = 0;
				}

				if (Math.abs(random.nextInt()) % 2 == 0) {
					bid(api);
					executeSleep(sleep);
					ask(api);
					executeSleep(sleep);
				} else {
					ask(api);
					executeSleep(sleep);
					bid(api);
					executeSleep(sleep);
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

	private static void ask(Api_Client api) throws IOException {
		HashMap<String, String> rgParamsOrderbook = new HashMap();
		rgParamsOrderbook.put("count", "2");
		String result = api.callApiGet(String.format("/public/orderbook/%s_KRW", coin), rgParamsOrderbook);
		Map<String, Object> map = om.readValue(result, Map.class);
		Map<String, Object> data = (Map) map.get("data");
		List<Map<String, String>> bids = (List) data.get("bids");
		List<Map<String, String>> asks = (List) data.get("asks");
		Double askPrice = Double.parseDouble(asks.get(1).get("price")) - TickSize.getSize(Double.parseDouble(asks.get(1).get("price")));

//		if (askPrice == Double.parseDouble(bids.get(0).get("price"))) {
//			askPrice = Double.parseDouble(asks.get(0).get("price"));
//		}

		HashMap<String, String> rgParams = new HashMap();
		rgParams.put("order_currency", coin);
		rgParams.put("payment_currency", "KRW");
		rgParams.put("units", String.format("%.4f", api.orderPrice / askPrice));
		rgParams.put("type", "ask");
		rgParams.put("price", String.format("%f", askPrice));

		try {
			String bidResult = api.callApiPost("/trade/place", rgParams);
			if (bidResult.contains("error : 429")) {
				increaseSleep();
			} else if (bidResult.contains("5600")) {
				cancelAsk(api);
			} else if (bidResult.contains("0000")) {
				System.out.println(api.api_key + " ask: " + bidResult.substring(28, bidResult.length() -1).replaceAll("\"", ""));
			} else {
				System.out.println(api.api_key + bidResult);
			}
		} catch (Exception e) {
			increaseSleep();
			cancelAsk(api);
		}
	}

	private static void bid(Api_Client api) throws IOException {

		HashMap<String, String> rgParamsOrderbook = new HashMap();
		rgParamsOrderbook.put("count", "2");
		String result = api.callApiGet(String.format("/public/orderbook/%s_KRW", coin), rgParamsOrderbook);
		Map<String, Object> map = om.readValue(result, Map.class);
		Map<String, Object> data = (Map) map.get("data");
		List<Map<String, String>> bids = (List) data.get("bids");
		List<Map<String, String>> asks = (List) data.get("asks");
		Double bidPrice = Double.parseDouble(bids.get(1).get("price")) + TickSize.getSize(Double.parseDouble(bids.get(1).get("price")));

		if (bidPrice == Double.parseDouble(asks.get(0).get("price"))) {
			bidPrice = Double.parseDouble(bids.get(0).get("price"));
		}

		HashMap<String, String> rgParams = new HashMap();
		rgParams.put("order_currency", coin);
		rgParams.put("payment_currency", "KRW");
		rgParams.put("units", String.format("%.4f", api.orderPrice / bidPrice));
		rgParams.put("type", "bid");
		rgParams.put("price", String.format("%f", bidPrice));

		try {
			String bidResult = api.callApiPost("/trade/place", rgParams);
			if (bidResult.contains("error : 429")) {
				increaseSleep();
			} else if (bidResult.contains("5600")) {
				cancelBid(api);
			} else if (bidResult.contains("0000")) {
				System.out.println(api.api_key + " bid: " + bidResult.substring(28, bidResult.length() -1).replaceAll("\"", ""));
			} else {
				System.out.println(api.api_key + bidResult);
			}
		} catch (Exception e) {
			increaseSleep();
			cancelBid(api);
		}
	}

	private static void cancelBid(Api_Client api) {
		if (Math.abs(random.nextInt()) % 3 == 0) {
			return;
		}

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
					if (!type.equals("bid")) {
						continue;
					}
					executeSleep(sleep);
					rgParams.put("order_id", orderId);
					rgParams.put("type", type);
					api.callApiPost("/trade/cancel", rgParams);
					System.out.println(api.api_key + " cancel_bid: " + orderId);
				}
			}
        } catch (Exception e) {
			e.printStackTrace();
        }
    }

	private static void cancelAsk(Api_Client api) {
		if (Math.abs(random.nextInt()) % 2 == 0) {
			return;
		}

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
					if (!type.equals("ask")) {
						continue;
					}
					executeSleep(sleep);
					rgParams.put("order_id", orderId);
					rgParams.put("type", type);
					api.callApiPost("/trade/cancel", rgParams);
					System.out.println(api.api_key + " cancel_ask: " + orderId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void increaseSleep() {
		double tobeSleep = sleep * 1.1;
		System.out.printf("set delay %,d -> %,d%n", sleep, (int) tobeSleep);
		sleep = (int) tobeSleep == 0 ? 1 : (int) tobeSleep;
	}

	private static void decreaseSleep() {
		double tobeSleep = sleep * 0.9;
		System.out.printf("set delay %,d -> %,d%n", sleep, (int) tobeSleep);
		sleep = (int) tobeSleep;
	}

	private static void executeSleep(int sleep) throws InterruptedException {
		if (sleep >= 0) {
			Thread.sleep(sleep);
		}
	}



}

