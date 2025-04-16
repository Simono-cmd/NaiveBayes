import java.io.IOException;

public class Main {
    public static String test = "agaricus-lepiota.test.data";
    public static String train = "agaricus-lepiota.data";
    public static void main(String[] args) throws IOException {
        NaiveBayesClassifier classifier = new NaiveBayesClassifier(train, test);
        classifier.train();
        classifier.classify("e");

    }
}
