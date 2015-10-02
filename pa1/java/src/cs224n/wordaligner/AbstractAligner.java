package cs224n.wordaligner; 

import cs224n.util.*;
import java.util.List;

abstract class AbstractAligner implements WordAligner {

  protected void addNullToTargetWords(SentencePair pair) {
    if (pair.getTargetWords().size() > 0 && 
        pair.getTargetWords().get(0) == NULL_WORD)
      return;
    pair.getTargetWords().add(0, NULL_WORD);
  }

  protected Counter<String> getTargetCounts(List<SentencePair> trainingPairs) {
    Counter<String> counter = new Counter<String>();
    for (SentencePair pair: trainingPairs) {
      for (String targetWord : pair.getTargetWords()) {
        counter.incrementCount(targetWord, 1);
      }
    }
    return Counters.normalize(counter);
  }

  protected CounterMap<String,String> getSourceTargetCounts(List<SentencePair> trainingPairs) {
    CounterMap<String,String> counter = new CounterMap<String,String>();
    for (SentencePair pair: trainingPairs) {
      for (String sourceWord: pair.getSourceWords()) {
        for (String targetWord: pair.getTargetWords()) {
          counter.incrementCount(sourceWord, targetWord, 1);
        }
      }
    }
    return Counters.conditionalNormalize(counter);
  }
}