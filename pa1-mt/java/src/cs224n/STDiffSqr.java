package edu.stanford.nlp.mt.decoder.feat;

import java.util.List;

import edu.stanford.nlp.mt.util.FeatureValue;
import edu.stanford.nlp.mt.util.Featurizable;
import edu.stanford.nlp.mt.util.IString;
import edu.stanford.nlp.mt.decoder.feat.RuleFeaturizer;
import edu.stanford.nlp.util.Generics;

import java.lang.Math;

/**
 * A rule featurizer.
 */
public class STDiffSqr implements RuleFeaturizer<IString, String> {
  
  @Override
  public void initialize() {
    // Do any setup here.
  }

  @Override
  public List<FeatureValue<String>> ruleFeaturize(
      Featurizable<IString, String> f) {

    // TODO: Return a list of features for the rule. Replace these lines
    // with your own feature.
    List<FeatureValue<String>> features = Generics.newLinkedList();
    
		int tsize = f.targetPhrase.size();
		int ssize = f.sourcePhrase.size();

    features.add(new FeatureValue<String>(
			"STDiff", (Math.abs (tsize - ssize)) * (Math.abs (tsize - ssize))));

    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
