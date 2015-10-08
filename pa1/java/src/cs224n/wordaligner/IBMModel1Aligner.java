package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Simple word alignment baseline model that maps source positions to target 
 * positions along the diagonal of the alignment grid.
 * 
 * IMPORTANT: Make sure that you read the comments in the
 * cs224n.wordaligner.WordAligner interface.
 * 
 * @author Dan Klein
 * @author Spence Green
 */
public class IBMModel1Aligner extends AbstractAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  private static final int maxIteration = 5; // Should be large enough
  private static final int maxTrainingPairs = 20000;
  private static final Lock sourceTargetLock = new ReentrantLock();
  private static final Lock targetLock = new ReentrantLock();
  private static final int numThreads = 4; // Corn has 4 cores

  private CounterMap<String, String> translation;

  public CounterMap<String, String> getTranslation() {
    return this.translation;
  }

  public Alignment align(SentencePair pair) {
    Alignment alignment = new Alignment();
    for (int s = 0; s < pair.getSourceWords().size(); s++) {
      String sourceWord = pair.getSourceWords().get(s);
      int maxIndex = 0;
      double maxProb = 0;
      for (int t = 0; t < pair.getTargetWords().size(); t++) {
        String targetWord = pair.getTargetWords().get(t);
        if (this.translation.getCount(sourceWord, targetWord) > maxProb) {
          maxProb = this.translation.getCount(sourceWord, targetWord);
          maxIndex = t;
        }
      }
      alignment.addPredictedAlignment(maxIndex, s);
    }
    return alignment;
  }

  protected void init(List<SentencePair> trainingPairs) {
    for (SentencePair pair: trainingPairs)
      this.addNullToTargetWords(pair);

    this.translation = new CounterMap<String, String>();
    for (SentencePair pair: trainingPairs) {
      for (String sourceWord: pair.getSourceWords()) {
        for (String targetWord: pair.getTargetWords()) {
          this.translation.setCount(sourceWord, targetWord, 1);
        }
      }
    }
    this.translation = Counters.conditionalNormalize(this.translation);
  }

  public void train(List<SentencePair> trainingPairs) {
    trainingPairs = trainingPairs.subList(0, Math.min(maxTrainingPairs, trainingPairs.size()));
    this.init(trainingPairs);
    for (int iter = 0; iter < maxIteration; iter++) {
      // Set all counts to zero
      CounterMap<String, String> sourceTargetCounts = new CounterMap<String, String>();
      Counter<String> targetCounts = new Counter<String>();
      ExecutorService service = Executors.newFixedThreadPool(numThreads);
      List<Future<Runnable>> futures = new ArrayList<Future<Runnable>>();

      for (int i = 0; i < trainingPairs.size(); i++) {
        Future f = service.submit(new IBM1TrainThread(
          trainingPairs.get(i),
          sourceTargetCounts, 
          targetCounts, 
          this.translation
        ));
        futures.add(f);
      }

      for (Future<Runnable> f : futures) {
        try {
          f.get();
        } catch (ExecutionException e) {
        } catch (InterruptedException e) {
        }
      }
      service.shutdownNow();

      // update t(source|target) = count(source, target) / count(target)
      for (String sourceWord: this.translation.keySet()) {
        for (String targetWord: this.translation.getCounter(sourceWord).keySet()) {
          double normalizedValue = 
            sourceTargetCounts.getCount(sourceWord, targetWord) / targetCounts.getCount(targetWord);

          this.translation.setCount(sourceWord, targetWord, normalizedValue);
        }
      }
    long endTime   = System.currentTimeMillis();
    System.out.println("use " + (endTime - startTime));
    }
  }

  public class IBM1TrainThread implements Runnable {

    private SentencePair pair = null;
    private CounterMap<String, String> sourceTargetCounts = null;
    private Counter<String> targetCounts = null;
    private CounterMap<String, String> translation = null;

    public IBM1TrainThread(
      SentencePair pair, 
      CounterMap<String, String> sourceTargetCounts,
      Counter<String> targetCounts,
      CounterMap<String, String> translation
    ) {
      this.pair = pair;
      this.sourceTargetCounts = sourceTargetCounts;
      this.targetCounts = targetCounts;
      this.translation = translation;
    }

    public void run(){
      List<String> sourceWords = this.pair.getSourceWords();
      List<String> targetWords = this.pair.getTargetWords();
      for (String sourceWord: sourceWords) {
        // sum of t(f|ei), for possible ei
        double denominator = this.translation.getCounter(sourceWord).totalCount();

        for (String targetWord: targetWords) {
          double increment = 
            this.translation.getCount(sourceWord, targetWord) / denominator;

          sourceTargetLock.lock();
          this.sourceTargetCounts.incrementCount(
            sourceWord, 
            targetWord, 
            increment
          );
          sourceTargetLock.unlock();

          targetLock.lock();
          this.targetCounts.incrementCount(targetWord, increment);
          targetLock.unlock();
        }
      }
    }
  }

}
