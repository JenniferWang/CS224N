package cs224n.wordaligner;  

import cs224n.util.*;
import java.util.List;

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
  private static final int maxIteration = 25; // Should be large enough
  
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
    this.init(trainingPairs);
    for (int iter = 0; iter < maxIteration; iter++) {
      // Set all counts to zero
      CounterMap<String, String> sourceTargetCounts = new CounterMap<String, String>();
      Counter<String> targetCounts = new Counter<String>();
      for (SentencePair pair: trainingPairs) {
        List<String> sourceWords = pair.getSourceWords();
        List<String> targetWords = pair.getTargetWords();

        for (String sourceWord: sourceWords) {
          // sum of t(f|ei), for possible ei
          double denominator = this.translation.getCounter(sourceWord).totalCount();

          for (String targetWord: targetWords) {
            double increment = 
              this.translation.getCount(sourceWord, targetWord) / denominator;

            sourceTargetCounts.incrementCount(sourceWord, targetWord, increment);
            targetCounts.incrementCount(targetWord, increment);
          }
        }
      }

      // update t(source|target) = count(source, target) / count(target)
      for (String sourceWord: this.translation.keySet()) {
        for (String targetWord: this.translation.getCounter(sourceWord).keySet()) {
          double normalizedValue = 
            sourceTargetCounts.getCount(sourceWord, targetWord) / targetCounts.getCount(targetWord);

          this.translation.setCount(sourceWord, targetWord, normalizedValue);
        }
      }
    }
  }
}
