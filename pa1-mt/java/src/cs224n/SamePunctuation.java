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
public class SamePunctuation implements RuleFeaturizer<IString, String> {
  
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
    
		boolean commaInTarget = f.sourcePhrase.toString().contains(",");
		boolean commaInSource = f.targetPhrase.toString().contains(",");		
		boolean periodInTarget = f.sourcePhrase.toString().contains(".");
    boolean periodInSource = f.targetPhrase.toString().contains(".");
		boolean questionInTarget = f.sourcePhrase.toString().contains("?");
		boolean questionInSource = f.targetPhrase.toString().contains("?");
		boolean exclamationInTarget = f.sourcePhrase.toString().contains("!");
		boolean exclamationInSource = f.targetPhrase.toString().contains("!");
		
		boolean samePunctuation = (commaInTarget == commaInSource) && 
															(periodInTarget == periodInSource) &&
															(questionInTarget == questionInSource) && 
															(exclamationInTarget == exclamationInSource);
    
		features.add(new FeatureValue<String>(
			"SamePunctuation", samePunctuation ? 1 : 0));

    return features;
  }

  @Override
  public boolean isolationScoreOnly() {
    return false;
  }
}
