public class MyMath {

    public static int randomInt(int minInclusive, int maxExclusive) {
        return (int) (Math.random() * (maxExclusive - minInclusive)) + minInclusive;
    }

    public static double randomDouble(double minInclusive, double maxExclusive) {
        return Math.random() * (maxExclusive - minInclusive) + minInclusive;
    }

    public static <T> T randomFromArray(T[] array) {
        return array[randomInt(0, array.length)];
    }

}
