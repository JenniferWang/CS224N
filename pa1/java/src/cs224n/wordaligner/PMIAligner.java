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
public class PMIAligner extends AbstractAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  
  private CounterMap<String,String> sourceTargetCounts;
  private CounterMap<String, String> translation;
  private Counter<String> targetCounts;

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

  public void train(List<SentencePair> trainingPairs) {
    this.sourceTargetCounts = new CounterMap<String, String>();
    this.translation = new CounterMap<String, String>();
    this.targetCounts = new Counter<String>();
    for (SentencePair pair: trainingPairs) { 
      this.addNullToTargetWords(pair);    
      for (String targetWord: pair.getTargetWords()) {
        this.targetCounts.incrementCount(targetWord, 1);
        for (String sourceWord: pair.getSourceWords()) {
          this.sourceTargetCounts.incrementCount(sourceWord, targetWord, 1);
        }
      }
    }

    for (String sourceWord: this.sourceTargetCounts.keySet()) {
      for (String targetWord: this.sourceTargetCounts.getCounter(sourceWord).keySet()) {
        double prob = 
          this.sourceTargetCounts.getCount(sourceWord, targetWord) 
          / this.targetCounts.getCount(targetWord);
        this.translation.setCount(sourceWord, targetWord, prob);
      }
    }
  }
}
