import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;

public class NaiveBayesClassifier {

    public static final String GREEN = "\033[0;32m";   // GREEN
    public static final String RED = "\033[0;31m";     // RED
    public static final String CYAN = "\033[0;36m";    // CYAN
    public static final String RESET = "\033[0m";  // Text Reset
    String trainFile;
    String testFile;
    Map<String, List<Map<String, Integer>>> counts = new HashMap<>();
    Map<String, List<Map<String, Double>>> occurrences = new HashMap<>();
    Map<String, Double> apriori = new HashMap<>();
    public int TP=0;
    public int FP=0;
    public int FN=0;
    public int TN=0;
    List<String> classList = new ArrayList<>();

    public NaiveBayesClassifier(String trainFile, String testFile) {
        this.trainFile = trainFile;
        this.testFile = testFile;
    }

    public void train() throws IOException {
        List<String> trainData = readFile(trainFile);

        //1 -> Zliczamy ilość wystąpień
        initCounts(trainData);

        //2 -> zliczamy prawdopodobieństwa apriori
        Map<String, Double> classCounts = new HashMap<>();

        for (String line : trainData) {
            String clazz = line.split(",")[0];
            classCounts.put(clazz, classCounts.getOrDefault(clazz, 0.0) + 1.0);
        }

        int totalExamples = trainData.size();
        for (Map.Entry<String, Double> entry : classCounts.entrySet()) {
            apriori.put(entry.getKey(), entry.getValue() / totalExamples);
        }

        //3 -> Obliczamy prawdopodobieństwa warunkowe
        for (String clazz : counts.keySet()) {
            List<Map<String, Integer>> countCols = counts.get(clazz);
            List<Map<String, Double>> probCols = new ArrayList<>();

            for (Map<String, Integer> colMap : countCols) {
                int total = colMap.values().stream().mapToInt(Integer::intValue).sum();
                int V = colMap.keySet().size();
                Map<String, Double> probMap = new HashMap<>();
                for (Map.Entry<String, Integer> entry : colMap.entrySet()) {
                    double probability = (double) (entry.getValue()+1) / (total+V);
                    probMap.put(entry.getKey(), probability);
                }
                probCols.add(probMap);
            }
            occurrences.put(clazz, probCols);
        }

        classList = new ArrayList<>(apriori.keySet());
        Collections.sort(classList);

    }



    public void classify(String positiveClass) throws IOException {
        int[][] confusionMatrix = new int[classList.size()][classList.size()];

        List<String> testData = readFile(testFile);
        int counter=0;
        for (String line : testData) {
            String[] tokens = line.split(",");
            String actualClass = tokens[0];
            String[] labels = Arrays.copyOfRange(tokens, 1, tokens.length);

            String predictedClass =null;
            double maxScore = Double.NEGATIVE_INFINITY;

            for (String clazz : occurrences.keySet()) {
                double score = apriori.get(clazz);

                List<Map<String, Double>> probCols = occurrences.get(clazz);
                for (int i = 0; i < labels.length; i++) {
                    Map<String, Double> probMap = probCols.get(i);
                    double probability = probMap.getOrDefault(labels[i], Double.MIN_VALUE);
                    score += probability;
                }
                if (score > maxScore) {
                    maxScore = score;
                    predictedClass = clazz;
                }
            }

            if(predictedClass.equals(actualClass)) {
                System.out.printf(GREEN+"Example: %s | Predicted: %s | Actual: %s\n"+RESET,
                        String.join(",", labels), predictedClass, actualClass);
            }
            else {
                System.out.printf(RED+"Example: %s | Predicted: %s | Actual: %s\n"+RESET,
                        String.join(",", labels), predictedClass, actualClass);
            }


            if (predictedClass.equals(positiveClass) && actualClass.equals(positiveClass)) {
                TP++;
            } else if (predictedClass.equals(positiveClass) && !actualClass.equals(positiveClass)) {
                FP++;
            } else if (!predictedClass.equals(positiveClass) && actualClass.equals(positiveClass)) {
                FN++;
            } else {
                TN++;
            }
            int actualIdx = classList.indexOf(actualClass);
            int predictedIdx = classList.indexOf(predictedClass);
            confusionMatrix[actualIdx][predictedIdx]++;
            counter++;
        }
        int total = TP + FP + TN + FN;
        double accuracy = (double)(TP + TN) / total;
        double precision = TP + FP == 0 ? 0 : (double) TP / (TP + FP);
        double recall = TP + FN == 0 ? 0 : (double) TP / (TP + FN);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        System.out.println("\nProcessed examples: "+counter);
        System.out.println("Correctly classified examples: "+(TP+TN));

        System.out.println(CYAN+"Evaluation for positive class: '" + positiveClass + "'"+RESET);
        System.out.printf("  • Accuracy: %.2f%%%n", 100 * accuracy);
        System.out.printf("  • Precision: %.2f%%%n", 100 * precision);
        System.out.printf("  • Recall: %.2f%%%n", 100 * recall);
        System.out.printf("  • F1-score: %.2f%%%n", 100 * f1);

        printConfusionMatrix(confusionMatrix, classList);


    }

    public List<String> readFile(String fileName) throws IOException {
        return Files.readAllLines(Paths.get(fileName));
    }

    public void printCounts()
    {
        for(String clazz : counts.keySet())
        {
            System.out.println("Klasa: "+clazz);
            List<Map<String, Integer>> countList = counts.get(clazz);

            for(int i = 0; i<countList.size(); i++)
            {
                System.out.println("Kolumna " + (i+1) +":");
                for (Map.Entry<String, Integer> entry : countList.get(i).entrySet())
                {
                    System.out.println("\t• " + entry.getKey() + ": " + entry.getValue());
                }
            }
            System.out.println();
        }
    }

    public void printOccurrences()
    {
        for (String clazz : occurrences.keySet()) {
            System.out.println("Klasa: " + clazz);
            List<Map<String, Double>> cols = occurrences.get(clazz);

            for (int i = 0; i < cols.size(); i++) {
                System.out.println("  Kolumna " + (i + 1) + ":");
                for (Map.Entry<String, Double> entry : cols.get(i).entrySet()) {
                    System.out.printf("     • %s => %04.2f%%%n", entry.getKey(), entry.getValue() * 100);
                }
            }
            System.out.println();
        }
    }

    public void printApriori()
    {
        System.out.println("Prawdopodobieństwa apriori:");
        for (Map.Entry<String, Double> entry : apriori.entrySet()) {
            System.out.printf("    • %s => %06.3f%%%n", entry.getKey(), entry.getValue() * 100);
        }
        System.out.println();
    }


    public void initCounts(List<String> trainData) {
        for (String t : trainData) {
            String[] tokens = t.split(",");
            String isEdible = tokens[0];
            String[] labels = Arrays.copyOfRange(tokens, 1, tokens.length);

            counts.putIfAbsent(isEdible, new ArrayList<>());
            List<Map<String, Integer>> classCounts = counts.get(isEdible);

            while (classCounts.size() < labels.length) {
                classCounts.add(new HashMap<>());
            }

            for (int i = 0; i < labels.length; i++) {
                String label = labels[i];
                Map<String, Integer> colMap = classCounts.get(i);
                colMap.put(label, colMap.getOrDefault(label, 0) + 1);
            }
        }
    }

    private void printConfusionMatrix(int[][] matrix, List<String> classList) {
        System.out.println("\n\033[1m"+"Confusion Matrix:"+RESET);
        System.out.printf("%-22s|", "Classified as -> ");
        for (int i = 0; i < classList.size(); i++) System.out.printf(CYAN+"%8c"+RESET, 'a' + i);
        System.out.println();

        System.out.print("-----------------------");
        for (int i = 0; i < classList.size(); i++) System.out.print("---------");
        System.out.println();

        for (int i = 0; i < matrix.length; i++) {
            char rowLabel = (char) ('a' + i);
            MushroomClass mushroomClass = MushroomClass.fromCode(classList.get(i));
            System.out.printf(CYAN + "%c = %-18s" + RESET + "|", rowLabel, mushroomClass.getDescription());

            for (int j = 0; j < matrix[i].length; j++) {
                System.out.printf("%6d\t", matrix[i][j]);
            }
            System.out.println();
        }
        System.out.println("\n");
    }


}

