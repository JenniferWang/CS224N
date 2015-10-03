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
  
  private CounterMap<String, String> sourceTargetCounts;
  private Counter<String> targetCounts;
  private CounterMap<String, String> t; /*source-to-target*/

  public Alignment align(SentencePair pair) {
    Alignment alignment = new Alignment();
    for (int s = 0; s < pair.getSourceWords().size(); s++) {
      String sourceWord = pair.getSourceWords().get(s);
      int maxIndex = pair.getTargetWords().indexOf(
        this.t.getCounter(sourceWord).argMax()
      );
      alignment.addPredictedAlignment(s, maxIndex);
      //if (maxIndex == 0) System.out.println("Get aligned to NULL");
    }
    return alignment;
  }

  protected void init(List<SentencePair> trainingPairs) {
    // for (SentencePair pair: trainingPairs)
    //   this.addNullToTargetWords(pair);

    this.t = new CounterMap<String, String>();
    for (SentencePair pair: trainingPairs) {
      for (String sourceWord: pair.getSourceWords()) {
        for (String targetWord: pair.getTargetWords()) {
          t.setCount(sourceWord, targetWord, 1);
        }
      }
    }
    
    //this.t = Counters.conditionalNormalize(this.t);
  }

  public void train(List<SentencePair> trainingPairs) {
    this.init(trainingPairs);
    for (int iter = 0; iter < maxIteration; iter++) {
      // Set all counts to zero
      this.sourceTargetCounts = new CounterMap<String, String>();
      this.targetCounts = new Counter<String>();
      for (SentencePair pair: trainingPairs) {
        List<String> sourceWords = pair.getSourceWords();
        List<String> targetWords = pair.getTargetWords();

        for (String sourceWord: sourceWords) {
          // sum of t(f|ei), for possible ei
          double denominator = this.t.getCounter(sourceWord).totalCount();

          for (String targetWord: targetWords) {
            double increment = 
              this.t.getCount(sourceWord, targetWord) / denominator;

            this.sourceTargetCounts.incrementCount(
              sourceWord, 
              targetWord,
              increment
            );
            this.targetCounts.incrementCount(targetWord, increment);
          }
        }
      }

      // update t(source|target) = count(source, target) / count(target)
      for (String sourceWord: this.t.keySet()) {
        for (String targetWord: this.t.getCounter(sourceWord).keySet()) {
          double normalizedValue = 
            this.sourceTargetCounts.getCount(sourceWord, targetWord) 
            / this.targetCounts.getCount(targetWord);

          this.t.setCount(sourceWord, targetWord, normalizedValue);
        }
      }
    }
  }

}
