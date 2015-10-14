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
public class AAnAnd implements RuleFeaturizer<IString, String> {
  
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

    boolean inTarget = f.sourcePhrase.toString().contains("a") ||
													f.sourcePhrase.toString().contains("an") ||
													f.sourcePhrase.toString().contains("one");
		boolean inSource = f.targetPhrase.toString().contains("un") ||
													f.targetPhrase.toString().contains("une");


    features.add(new FeatureValue<String>(
			"AAnAnd", (inTarget == inSource) ? 1 : 0));

    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
