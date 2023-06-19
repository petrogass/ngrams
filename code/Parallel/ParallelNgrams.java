import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.*;


public class ParallelNgrams {
    public static class Producer implements Runnable {
        private ConcurrentLinkedQueue<String> sharedQueue; // coda di lavoro condivisa
        private String filePath;
        private int blockSize;
        private int overlap;

        public Producer(ConcurrentLinkedQueue<String> sharedQueue, String filePath, int blockSize, int overlap) {
            this.sharedQueue = sharedQueue;
            this.filePath = filePath;
            this.blockSize = blockSize;
            this.overlap = overlap;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(filePath));
                char[] block = new char[blockSize];
                int readChars = 0;
                int c;
                // Reads a blockSize number of chars and enques the new job
                while ((c = reader.read()) != -1) {
                    if(readChars == blockSize - overlap){
                        // Bookmarks where to start reading chars for the next job
                        reader.mark(overlap);
                    }
                    if (readChars == blockSize) {
                        sharedQueue.add(new String(block));
                        // Reset the reader
                        reader.reset();
                        readChars = 0;
                        continue;
                    }

                    block[readChars++] = (char) c;
                }
                // Enques the last job
                if (readChars > 0) {
                    sharedQueue.add(new String(block, 0, readChars));
                }
                // Signals the consumers that the producer has finished producing new jobs
                for (int i = 0; i < 16; i++) {
                    sharedQueue.add("WORK_OVER");
                }
                //System.out.println("over");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static class Consumer implements Runnable{
            private int n; // ngram length
            private ConcurrentHashMap<String, Integer> sharedMap; // shared ngram map
            private ConcurrentLinkedQueue<String> sharedQueue; // shared jobs queue
            private HashMap<String, Integer> ngrams;// local ngram map
            private String workBlock;

            public Consumer(ConcurrentHashMap<String, Integer> sharedMap, ConcurrentLinkedQueue<String> sharedQueue, int n){
                this.sharedMap = sharedMap;
                this.sharedQueue = sharedQueue;
                this.n = n;
                this.ngrams = new HashMap<String, Integer>();
            }

            @Override
            public void run(){
                try {
                    while(true) {
                        // Poll the queue for a job
                        if((this.workBlock = sharedQueue.poll()) != null){
                            //System.out.println(workBlock);}
                            // Stop condition
                            if(workBlock == "WORK_OVER"){
                                //System.out.println("over thread");
                                break;
                            }
                            // Compute ngrams and add them to the local map
                            for (int i = 0; i < workBlock.length()-n+1; i++) {
                                String ngram = workBlock.substring(i, i + n);
                                if (ngrams.containsKey(ngram)) {
                                    ngrams.put(ngram, ngrams.get(ngram) + 1);
                                } else {
                                    ngrams.put(ngram, 1);
                                }
                            }}
                    }
                }catch (Exception e) {
                    e.printStackTrace();}
                // Merge local map with shared map
                ngrams.forEach((k, v) -> sharedMap.merge(k, v, (v1, v2) -> v1 + v2));


            }
        }

        public static void main(String[] args) {
            long timeStart;         //start time
            long timeEnd;        //finish time
            float timeTotal = 0;    //total time
            float total = 0;
            // Define parameters
            String filePath = "textfiles/8mb.txt";
            int blockSize = 5000;
            int n = 2; // lunghezza ngram
            int overlap = n;
            // Average time over 10 consecutive executions
            for (int i = 0; i < 10; i++) {
                ConcurrentHashMap<String, Integer> sharedMap = new ConcurrentHashMap<String, Integer>();
                ConcurrentLinkedQueue<String> sharedQueue = new ConcurrentLinkedQueue<String>();

                int threadPoolSize = Runtime.getRuntime().availableProcessors();
                //System.out.println(threadPoolSize);
                timeStart = System.nanoTime();
                ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
                // Start the producer
                Producer prod = new Producer( sharedQueue, filePath, blockSize, overlap);
                executor.execute(prod);
                // Start the consumers
                for (int j = 0; j < threadPoolSize-1; j++) {
                    executor.execute(new Consumer(sharedMap, sharedQueue, n));
                }
                // Wait for consumers to terminate
                executor.shutdown();
                while(true) {
                    if(executor.isTerminated()) {
                        timeEnd = System.nanoTime();
                        System.out.println(sharedMap);
                        break;
                    }}

                timeTotal = (float)(timeEnd - timeStart) / 1000000000;
                total += timeTotal;
                System.out.println("The system has employed " + timeTotal + " seconds to perform the elaboration.");
        }
            System.out.println("Average elaboration time:" + total/10);
    }}

}
