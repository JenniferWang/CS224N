package cs224n.wordaligner; 

import cs224n.util.*;
import java.util.List;

abstract class AbstractAligner implements WordAligner {

  protected void addNullToTargetWords(SentencePair pair) {
    if (pair.getTargetWords().size() > 0 
        && pair.getTargetWords().get(pair.getTargetWords().size() - 1) 
          == NULL_WORD)
      return;
    pair.getTargetWords().add(NULL_WORD);
  }
}
