package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Simple word alignment baseline model that maps source positions to target 
 * positions along the diagonal of the alignment grid.
 *
 * 
 * @author Dan Klein
 * @author Spence Green
 * @author Jiyue Wang
 */
public class IBMModel2Aligner extends AbstractAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  private static final int maxIteration = 5; // Should be large enough
  private static final int maxTrainingPairs = 20000;

  /*
   * q(targetPos | sourcePos, length(source), length(target))
   * 
   * key is "[m, l]", where m = length(source), n = length(target)
   * val is Counter<sourcePos, targetPos>
   */
  private HashMap<String, CounterMap<String, String>> q;

  /*
   * t(sourceWord | targetWord)
   * 
   * Counter<sourceWord, targetWord>
   */
  private CounterMap<String, String> t;

  /*
   * Helpter function to convert an array of int to string
   */
  protected static String convertToString(int... key) {
    return Arrays.toString(key);
  }

  public Alignment align(SentencePair pair) {
    Alignment alignment = new Alignment();
    int m = pair.getSourceWords().size();
    int l = pair.getTargetWords().size();
    String mlPairKey = convertToString(m, l);
    
    for (int s = 0; s < m; s++) {
      int maxIndex = l - 1; // As we append NULL to the end of the sentence
      double maxProb = 0;
      String sourceWord = pair.getSourceWords().get(s);
      String sourcePosKey = convertToString(s);
      
      for (int t = 0; t < l; t++) {
        String targetWord = pair.getTargetWords().get(t);
        String targetPosKey = convertToString(t);
        if (this.q.get(mlPairKey) == null)
          break;
        double currProb = 
          this.q.get(mlPairKey).getCount(sourcePosKey, targetPosKey)
          * this.t.getCount(sourceWord, targetWord);

        if (currProb > maxProb) {
          maxProb = currProb;
          maxIndex = t;
        }
      }
      alignment.addPredictedAlignment(maxIndex, s);
    }
    return alignment;
  }

  protected void init(List<SentencePair> trainingPairs) {
    IBMModel1Aligner model1 = new IBMModel1Aligner();
    trainingPairs = trainingPairs.subList(0, Math.min(maxTrainingPairs, trainingPairs.size()));
    model1.train(trainingPairs);

    this.t = model1.getTranslation();
    this.q = new HashMap<String, CounterMap<String, String>>();
    for (SentencePair pair: trainingPairs) {
      int l = pair.getTargetWords().size();
      int m = pair.getSourceWords().size();
      String mlPairKey = convertToString(m, l);

      if (!this.q.containsKey(mlPairKey))
        this.q.put(mlPairKey, new CounterMap<String, String>());

      CounterMap<String, String> posCounter = this.q.get(mlPairKey);
      for (int i = 0; i < m; i++) {
        for (int j = 0; j < l; j++) {
          posCounter.setCount(convertToString(i), convertToString(j), 1);
        }
      }
    }

    for (String mlPairKey: this.q.keySet()){
      this.q.put(
        mlPairKey, 
        Counters.conditionalNormalize(this.q.get(mlPairKey))
      );
    }
  }

  public void train(List<SentencePair> trainingPairs) {
    this.init(trainingPairs);
    trainingPairs = trainingPairs.subList(0, Math.min(maxTrainingPairs, trainingPairs.size()));
    for (int iter = 0; iter < maxIteration; iter++) {
      CounterMap<String, String> sourceTargetCounts = new CounterMap<String,String>();
      CounterMap<String, String> sourcePosCounts = new CounterMap<String, String>();
      HashMap<String, CounterMap<String, String>> sourceTargetPosCounts = 
        new HashMap<String, CounterMap<String, String>>(); 
      Counter<String> targetCounts = new Counter<String>();

      for (SentencePair pair: trainingPairs) {
        List<String> sourceWords = pair.getSourceWords();
        List<String> targetWords = pair.getTargetWords();
        int m = sourceWords.size();
        int l = targetWords.size();
        String mlPairKey = convertToString(m, l);
        CounterMap<String, String> currQ = this.q.get(mlPairKey);

        for (int s = 0; s < m; s++) {
          double denominator = 0;
          for (int t = 0; t < l; t++) {
            denominator += 
              currQ.getCount(convertToString(s), convertToString(t)) 
              * this.t.getCount(sourceWords.get(s), targetWords.get(t));
          }

          for (int t = 0; t < l; t++) {
            double increment = 
              currQ.getCount(convertToString(s), convertToString(t))
              * this.t.getCount(sourceWords.get(s), targetWords.get(t))
              / denominator;

            sourceTargetCounts.incrementCount(sourceWords.get(s), targetWords.get(t), increment);
            targetCounts.incrementCount(targetWords.get(t), increment);
            
            if (!sourceTargetPosCounts.containsKey(mlPairKey))
              sourceTargetPosCounts.put(mlPairKey, new CounterMap<String, String>());

            sourceTargetPosCounts
              .get(mlPairKey)
              .incrementCount(convertToString(s), convertToString(t), increment);
            
            sourcePosCounts.incrementCount(mlPairKey, convertToString(s), increment);
          }
        }
      }

      for (String sourceWord: this.t.keySet()) {
        for (String targetWord: this.t.getCounter(sourceWord).keySet()) {
          double normalizedValue = 
            sourceTargetCounts.getCount(sourceWord, targetWord) 
            / targetCounts.getCount(targetWord);

          this.t.setCount(sourceWord, targetWord, normalizedValue);
        }
      }

      for (String mlPairKey: this.q.keySet()) {
        CounterMap<String, String> currQ = this.q.get(mlPairKey);
        for (String sourcePosKey: currQ.keySet()) {
          for (String targetPosKey: currQ.getCounter(sourcePosKey).keySet()) {
            double normalizedValue = 
              sourceTargetPosCounts.get(mlPairKey).getCount(sourcePosKey, targetPosKey)
              / sourcePosCounts.getCount(mlPairKey, sourcePosKey);

            currQ.setCount(sourcePosKey, targetPosKey, normalizedValue);
          }
        }
      }
    }
  }
}
