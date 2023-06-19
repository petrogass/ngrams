import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

public class SequentialNgrams {
    private int blockSize;
    private int overlap;
    private String filePath;
    private HashMap<String, Integer> ngrams;
    private int n;

    public SequentialNgrams(int blockSize, int overlap, String filePath, HashMap<String, Integer> ngrams, int n){
        this.blockSize = blockSize;
        this.overlap = overlap;
        this.filePath = filePath;
        this.ngrams = ngrams;
        this.n = n;
    }

    public HashMap<String, Integer> work(){
        try  {
            BufferedReader reader = new BufferedReader
                    (new FileReader(filePath));

            char[] workBlock = new char[blockSize];
            int readChars = 0;
            int c;
            // Reads chars until it has blockSize number of chars
            while ((c = reader.read()) != -1) {
                if(readChars == blockSize - overlap){
                    // Bookmarks where to start reading chars again
                   reader.mark(overlap);
                }
                if (readChars == blockSize) {
                    String workString = new String(workBlock);
                    // Extracts and adds ngrams to the HashMap
                    for (int i = 0; i < workString.length()-n+1; i++) {
                        String ngram = workString.substring(i, i + n);
                        //System.out.println(ngram);
                        if (ngrams.containsKey(ngram)) {
                            ngrams.put(ngram, ngrams.get(ngram) + 1);
                        } else {
                            ngrams.put(ngram, 1);
                        }
                    }
                    // Resets the reader for the next read cycle
                    reader.reset();
                    readChars = 0;
                    workBlock = new char[blockSize];
                    continue;
                }

                workBlock[readChars++] = (char) c;
            }
            //System.out.println(readChars);
            // Last read and compute cycle
            if (readChars > 0) {
                String workString = new String(workBlock);
                workString = workString.trim();
                for (int i = 0; i < workString.length()-n+1; i++) {
                    String ngram = workString.substring(i, i + n);
                    //System.out.println(ngram);
                    if (ngrams.containsKey(ngram)) {
                        ngrams.put(ngram, ngrams.get(ngram) + 1);
                    } else {
                        ngrams.put(ngram, 1);
                    }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }

    return(ngrams);
    }

    public static void main(String[] args) {
        long timeStart;         //start time
        long timeEnd;        //finish time
        float timeTotal = 0;    //total time
        // Define parameters
        String filePath = "textfiles/8mb.txt";
        int blockSize = 500;
        int n = 3; // ngram length
        int overlap = n;
        float total = 0; // total execution time
        // Average time over 10 consecutive executions
        for (int i = 0; i < 10; i++) {
            HashMap<String,Integer> ngrams = new HashMap<String, Integer>();
            SequentialNgrams seq = new SequentialNgrams(blockSize, overlap, filePath, ngrams, n);
            timeStart = System.nanoTime();
            HashMap<String, Integer> res = seq.work();
            timeEnd = System.nanoTime();
            timeTotal = (float)(timeEnd - timeStart) / 1000000000;
            System.out.println(res);
            System.out.println("The system has employed " + timeTotal + " seconds to perform the elaboration.");
            total += timeTotal;
        }
        System.out.println("Average elaboration time:" + total/10);
    }
}

