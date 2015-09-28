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
public class PMIAligner implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  
  private CounterMap<String,String> sourceTargetCounts;
  private Counter<String> sourceCounts;
  private Counter<String> targetCounts;

  public Alignment align(SentencePair sentencePair) {
    Alignment alignment = new Alignment();
    for (int s = 0; s < sentencePair.getSourceWords().size(); s++) {
      String sourceWord = sentencePair.getSourceWords().get(s);
      // TODO: NULL?
      int maxIndex = -1;
      double maxProb = 0;
      double sourceProb = this.sourceCounts.getCount(sourceWord);
      if (sourceProb == 0) {
        // TODO: Is this correct?
        alignment.addPredictedAlignment(s, -1);
        continue;
      }
      for (int t = 0; t < sentencePair.getTargetWords().size(); t++) {
        String targetWord = sentencePair.getTargetWords().get(t);
        double targetProb = this.targetCounts.getCount(targetWord);
        if (targetProb == 0) 
          continue;
        double currProb = 
          this.sourceTargetCounts.getCount(sourceWord, targetWord) / targetProb;
        if (currProb > maxProb) {
          maxProb = currProb;
          maxIndex = t;
        }
      }
      alignment.addPredictedAlignment(s, maxIndex);
    }
    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
    this.sourceTargetCounts = new CounterMap<String,String>();
    this.sourceCounts = new Counter<String>();
    this.targetCounts = new Counter<String>();

    for (SentencePair pair: trainingPairs) {
      for (String sourceWord: pair.getSourceWords()) {
        this.sourceCounts.incrementCount(sourceWord, 1);
        for (String targetWord: pair.getTargetWords()) {
          this.sourceTargetCounts.incrementCount(sourceWord, targetWord, 1);
        }
      }
      for (String targetWord : pair.getTargetWords()) {
        this.targetCounts.incrementCount(targetWord, 1);
      }
    }
    this.sourceCounts = Counters.normalize(this.sourceCounts);
    this.targetCounts = Counters.normalize(this.targetCounts);
    this.sourceTargetCounts = 
      Counters.conditionalNormalize(this.sourceTargetCounts);
  }

}
