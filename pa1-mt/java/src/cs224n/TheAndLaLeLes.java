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
public class TheAndLaLeLes implements RuleFeaturizer<IString, String> {
  
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

    boolean theInTarget = f.sourcePhrase.toString().contains("the");
		boolean lalelesInSource = f.targetPhrase.toString().contains("la") ||
													f.targetPhrase.toString().contains("le") ||
													f.targetPhrase.toString().contains("les");


    features.add(new FeatureValue<String>(
			"TheAndLaLeLes", (theInTarget == lalelesInSource) ? 1 : 0));

    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
