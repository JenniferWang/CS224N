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
  private Counter<String> targetCounts;

  public Alignment align(SentencePair pair) {
    Alignment alignment = new Alignment();
    for (int s = 0; s < pair.getSourceWords().size(); s++) {
      String sourceWord = pair.getSourceWords().get(s);
      int maxIndex = 0;
      double maxProb = 0;
      for (int t = 0; t < pair.getTargetWords().size(); t++) {
        String targetWord = pair.getTargetWords().get(t);
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
      // index??
      alignment.addPredictedAlignment(s, maxIndex);
      if (maxIndex == 0) System.out.println("Get aligned to NULL");
    }
    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
    this.sourceTargetCounts = this.getSourceTargetCounts(trainingPairs);
    this.targetCounts = this.getTargetCounts(trainingPairs);
  }

}
