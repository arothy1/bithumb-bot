import java.util.Arrays;

public enum TickSize {

    PRICE_1_10(1, 10, 0.001),
    PRICE_10_100(10, 100, 0.01),
    PRICE_100_5000(100, 5000, 1),
    PRICE_5000_10000(5000, 10000, 5),
    PRICE_10000_50000(10000, 50000, 10),
    PRICE_50000_100000(50000, 100000, 50),
    PRICE_100000_500000(100000, 500000, 100),
    PRICE_500000_1000000(500000, 1000000, 500),
    PRICE_1000000_X(1000000, 1999999999, 1000);


    int min;
    int max;
    double size;


    TickSize(int min, int max, double size) {
        this.min = min;
        this.max = max;
        this.size = size;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public double getSize() {
        return size;
    }

    public static double getSize(double price) {
        return Arrays.stream(values())
                .filter(t -> t.min <= price && t.max > price)
                .map(t -> t.size)
                .findFirst()
                .orElse(null);
    }





}
